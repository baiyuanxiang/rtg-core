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
package com.rtg.util.array.intindex;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import com.rtg.util.array.IndexType;
import com.rtg.util.integrity.Exam;

/**
 * Index is implemented using a straight forward long array.
 * This is so that short instances of IntIndex can be as efficient as possible.
 *
 */
public final class IntArray extends IntIndex {

  private final int[] mArray;

  /**
   * This should be called directly only in tests.
   *
   * @param length number of items to be stored.
   */
  public IntArray(final long length) {
    super(length);
    //assert length <= MAX_LENGTH;
    mArray = new int[(int) length];
  }

  private IntArray(int[] data, long length) {
    super(length);
    mArray = data;
  }


  @Override
  public int getInt(final long index) {
    final int ii = (int) index;
    if (ii != index) {
      throw new IndexOutOfBoundsException(String.valueOf(index));
    }
    return mArray[ii];
  }

  @Override
  public void setInt(final long index, final int value) {
    final int ii = (int) index;
    if (ii != index) {
      throw new IndexOutOfBoundsException(String.valueOf(index));
    }
    mArray[ii] = value;
  }

  @Override
  public void swap(long index1, long index2) {
    final int ii1 = (int) index1;
    final int ii2 = (int) index2;
    final int tmp = mArray[ii1];
    mArray[ii1] = mArray[ii2];
    mArray[ii2] = tmp;
  }

  @Override
  public boolean integrity() {
    super.integrity();
    Exam.assertEquals(mArray.length, mLength);
    return true;
  }

  @Override
  public boolean safeFromWordTearing() {
    return true;
  }

  @Override
  public void save(ObjectOutputStream dos) throws IOException {
    dos.writeInt(IndexType.ARRAY.ordinal());
    dos.writeLong(mLength);
    dos.writeObject(mArray);
  }

  /**
   * Should only be called from {@link IntCreate#loadIndex(java.io.ObjectInputStream)}
   * @param ois stream to load from
   * @return index loaded from stream
   * @throws IOException if an IO error occurs
   */
  public static IntArray loadIndex(ObjectInputStream ois) throws IOException {
    final long length = ois.readLong();
    final int[] data;
    try {
      data = (int[]) ois.readObject();
    } catch (ClassNotFoundException e) {
      throw new IOException("Unrecognized index type: " + e.getMessage());
    }
    return new IntArray(data, length);
  }
}


