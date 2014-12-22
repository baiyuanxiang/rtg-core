/*
 * Copyright (c) 2014. Real Time Genomics Limited.
 *
 * Use of this source code is bound by the Real Time Genomics Limited Software Licence Agreement
 * for Academic Non-commercial Research Purposes only.
 *
 * If you did not receive a license accompanying this file, a copy must first be obtained by email
 * from support@realtimegenomics.com.  On downloading, using and/or continuing to use this source
 * code you accept the terms of that license agreement and any amendments to those terms that may
 * be made from time to time by Real Time Genomics Limited.
 */
package com.rtg.position.output;

import java.io.IOException;
import java.util.Arrays;

import com.rtg.util.integrity.Exam;
import com.rtg.util.integrity.IntegralAbstract;

/**
 * Holds all the sequence id and position entries.
 */
public class SegmentCollector extends IntegralAbstract {

  private final long[] mValues;

  private final int mWordSize;

  private final int mStepSize;

  private final SegmentWriter mWriter;

  private int mSize = 0;

  /**
   * @param length maximum size, that is, number of entries.
   * @param wordSize length of word.
   * @param stepSize offset expected between abutting segment hits.
   * @param writer where to send the results once all collected and sorted.
   */
  public SegmentCollector(final int length, final int wordSize, final int stepSize, final SegmentWriter writer) {
    mValues = new long[length];
    mWordSize = wordSize;
    mStepSize = stepSize;
    mWriter = writer;
  }

  /**
   * add hit to collection
   * @param seqId build sequence id
   * @param posn build position, 0 based
   */
  public void add(final int seqId, final int posn) {
    assert seqId >= 0;
    assert posn >= mWordSize - 1 : "posn=" + posn + " wordSize=" + mWordSize;
    final long v = pack(seqId, posn);
    assert v >= 0;
    mValues[mSize] = v;
    mSize++;
  }

  /**
   * I have no idea
   * @param oldS no idea
   * @param newS no idea
   * @param free no idea
   * @param searchPosition no idea
   * @throws IOException the usual
   */
  public void endPosition(final SegmentCollection oldS, final SegmentCollection newS, final SegmentCollection free, final int searchPosition) throws IOException {
    assert newS.size() == 0;
    Arrays.sort(mValues, 0, mSize);
    int i = 0; //index into mValues
    int j = 0; //index into oldS
    final int size = oldS.size();
    while (true) {
      //System.err.println("SegmentCollector.endPosition free.size=" + free.size());
      if (i == mSize) {
        if (j == size) {
          //both values and old exhausted
          break;
        }
        //values exhausted write old
        write(oldS.get(j), free, searchPosition);
        j++;
        continue;
      }
      final long v = mValues[i];
      final int seqId = seqId(v);
      final int posn = position(v);
      final int posStep = posn - mStepSize;
      if (j == size) {
        //after all values
        //create new segment and move to new
        create(seqId, posn, newS, free);
        i++;
        continue;
      }
      final Segment seg = oldS.get(j);
      if (seqId == seg.seqId() && posStep == seg.end()) {
        //extend old and move to new
        seg.extend(posn);
        newS.add(seg);
        i++;
        j++;
        continue;
      }
      if (seqId < seg.seqId() || (seqId == seg.seqId() && posStep < seg.end())) {
        //v < old
        //create new segment and move to new
        create(seqId, posn, newS, free);
        i++;
        continue;
      }
      // v > old
      write(seg, free, searchPosition);
      j++;
      continue;
    }
    oldS.clear();
    mSize = 0;
  }

  //The next three methods pack and unpack pairs of seqId and position into a long
  //for easy sorting

  static long pack(final int seqId, final int posn) {
    assert seqId >= 0;
    assert posn >= 0;
    final long v = ((long) seqId << 32) | posn;
    assert v >= 0;
    return v;
  }

  static int seqId(final long v) {
    final int seqId = (int) (v >> 32);
    assert seqId >= 0;
    return seqId;
  }

  static int position(final long v) {
    final int posn = (int) v;
    assert posn >= 0;
    return posn;
  }


  private void write(final Segment seg, final SegmentCollection free, final int searchPosition) throws IOException {
    mWriter.write(seg, searchPosition);
    seg.clear();
    free.add(seg);
  }

  private void create(final int seqid, final int posn, final SegmentCollection newS, final SegmentCollection free) {
    final Segment newSeg = free.removeNext();
    if (newSeg == null) {
      throw new IllegalArgumentException("free list empty");
    }
    newSeg.initialize(seqid, posn - mWordSize + 1, posn);
    newS.add(newSeg);
  }

  /**
   * Get number of entries.
   * @return number of entries.
   */
  int size() {
    return mSize;
  }

  @Override
  public void toString(final StringBuilder arg0) {

  }

  @Override
  public boolean globalIntegrity() {
    integrity();
    for (int i = 0; i < mSize; i++) {
      final int seqId = seqId(mValues[i]);
      Exam.assertTrue(seqId >= 0);
      final int posn = position(mValues[i]);
      Exam.assertTrue(posn >= 0);
    }
    return true;
  }

  @Override
  public boolean integrity() {
    Exam.assertTrue(mStepSize >= 1);
    Exam.assertTrue(mWordSize >= mStepSize);
    Exam.assertTrue(mSize >= 0 && mSize <= mValues.length);
    Exam.assertTrue(mWriter != null);
    return true;
  }
}
