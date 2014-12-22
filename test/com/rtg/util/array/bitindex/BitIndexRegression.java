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

package com.rtg.util.array.bitindex;

import com.rtg.util.array.AbstractCommonIndexRegression;
import com.rtg.util.array.CommonIndex;
import com.rtg.util.bytecompression.CompressedByteArray;

/**
 * Test that <code>BitIndex</code> can actually handle having lots of data put into it.
 */
public class BitIndexRegression extends AbstractCommonIndexRegression {

  private static final int RANGE = 32; //4 bits worth

  @Override
  protected long getNumElements() {
    //More elements to fully exercise due to bit packing
    return 16L * Integer.MAX_VALUE + 9000L;
  }

  @Override
  protected long getRange() {
    return RANGE;
  }

  @Override
  protected CommonIndex createIndex(long elements) {
    return new BitIndex(elements, CompressedByteArray.minBits(RANGE));
  }

}
