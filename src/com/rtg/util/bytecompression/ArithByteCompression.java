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

package com.rtg.util.bytecompression;

import com.rtg.util.arithcode.ArithCodeModel;
import com.rtg.util.arithcode.ArithDecoder;
import com.rtg.util.arithcode.ArithEncoder;
import com.rtg.util.arithcode.ArithModelBuilder;
import com.rtg.util.arithcode.InputBytes;
import com.rtg.util.arithcode.OutputBytes;
import com.rtg.util.array.byteindex.ByteChunks;
import com.rtg.util.array.longindex.LongChunks;
import com.rtg.util.integrity.Exam;
import com.rtg.util.integrity.IntegralAbstract;

/**
 */
public class ArithByteCompression extends IntegralAbstract implements ByteCompression {

  private final int mRange;

  private final int mInitialCount;

  private ByteBaseCompression mInitial;

  private ArithModelBuilder mBuilder;

  private ArithCodeModel mModel = null;

  private final ByteChunks mBytes = new ByteChunks(0);

  private final ArithEncoder mEncoder = new ArithEncoder(new OutputBytes(mBytes));

  private final LongChunks mPointers = new LongChunks(0);

  private long mCountBlocks = 0;

  private long mCount = 0;

  private boolean mFrozen = false;

  /**
   * @param range number of symbols (all symbols are assumed to lie in range 0 to range (exclusive) ).
   * @param initialCount number of symbols in blocks before model is frozen and used to pack sequences.
   * @param builder used to construct model once counts accumulated.
   */
  public ArithByteCompression(final int range, final int initialCount, final ArithModelBuilder builder) {
    mRange = range;
    mInitial = new ByteBaseCompression(mRange);
    mInitialCount = initialCount;
    mBuilder = builder;
  }

  @Override
  public void add(byte[] buffer, int offset, int length) {
    if (mFrozen) {
      throw new RuntimeException("Adding to a frozen ByteCompression");
    }
    if (mInitial == null) {
      addComp(buffer, offset, length);
    } else {
      mInitial.add(buffer, offset, length);
      mBuilder.add(buffer, offset, length);
      mCountBlocks++;
      mCount += length;
      if (mCount >= mInitialCount) {
        pack();
      }
    }
    assert integrity();
  }

  private void pack() {
    mModel = mBuilder.model();
    mBuilder = null;
    mPointers.append(mEncoder.endBlock());
    byte[] buf = new byte[0];
    for (int i = 0; i < mCountBlocks; i++) {
      final int length = mInitial.length(i);
      if (buf.length < length) {
        buf = new byte[length];
      }
      mInitial.get(buf, i, 0, length);
      addComp(buf, 0, length);
    }
    mInitial = null;
  }

  private void addComp(byte[] buffer, int offset, int length) {
    for (int j = offset; j < offset + length; j++) {
      mModel.encode(mEncoder, buffer[j]);
    }
    mPointers.append(mEncoder.endBlock());
  }

  @Override
  public void get(byte[] buffer, long index, int offset, int length) {
    if (mInitial != null) {
      mInitial.get(buffer, index, offset, length);
    } else {
      final InputBytes ib = new InputBytes(mBytes, mPointers.get(index), mPointers.get(index + 1));
      final ArithDecoder de = new ArithDecoder(ib);
      for (int j = 0; j < offset + length; j++) {
        final int sym = mModel.decode(de);
        if (j >= offset) {
          buffer[j - offset] = (byte) sym;
        }
      }
    }
  }

  @Override
  public void freeze() {
    if (mInitial != null) {
      pack();
    }
    mEncoder.close();
    mPointers.trim();
    mBytes.trim();
    mFrozen = true;
    assert integrity();
  }

  @Override
  public long bytes() {
    if (mInitial != null) {
      return mInitial.bytes();
    }
    return mPointers.bytes() + mBytes.bytes();
  }

  @Override
  public boolean integrity() {
    Exam.assertTrue(0 <= mRange && mRange <= Byte.MAX_VALUE);
    Exam.assertTrue(0 <= mInitialCount);
    if (mModel == null) {
      Exam.assertTrue(mInitial != null);
      Exam.assertTrue(mBuilder != null);
      Exam.assertFalse(mFrozen);
    } else {
      Exam.assertTrue(mInitial == null);
      Exam.assertTrue(mBuilder == null);
    }
    Exam.assertNotNull(mBytes);
    Exam.assertNotNull(mEncoder);
    Exam.assertNotNull(mPointers);

    Exam.assertTrue(0 <= mCountBlocks);
    Exam.assertTrue(0 <= mCount);
    return true;
  }
}
