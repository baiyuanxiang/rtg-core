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
package com.rtg.util.array.byteindex;

import junit.framework.TestCase;

/**
 * Tests for <code>ByteIndex</code>.
 *
 */
public abstract class AbstractByteIndexTest extends TestCase {

  protected static final int STEP = 1000;
  protected static final int LENGTH = 1000007; //this isnt a multiple of two because then it might not catch many tricky cases

  /** Local new line convention */
  private static final String LS = System.lineSeparator();

  protected abstract ByteIndex create(final long length);

  protected abstract ByteIndex create(final long length, final int bits);

  public void testMasks() {
    assertEquals(ByteIndex.BYTE_MASK & ByteIndex.HIGH_MASK, 0);
    assertEquals(ByteIndex.HIGH_SIGNED_MASK & ByteIndex.HIGH_MASK, ByteIndex.HIGH_MASK);
    assertEquals(ByteIndex.HIGH_SIGNED_MASK & ByteIndex.BYTE_MASK, 1L << 7);
  }


  private static final String TO_STR = ""
    + "Index [100]" + LS
    + "[0]     0,     1,     2,     0,     0,     0,     0,     0,     0,     0" + LS
    + "[10]     0,     0,    12,     0,     0,     0,     0,     0,     0,     0" + LS
    + "[50]     0,     0,    52,     0,     0,     0,     0,     0,     0,     0" + LS;

  public void testToString() {
    final ByteIndex index = create(100);
    index.setByte(1, (byte) 1);
    index.setByte(2, (byte) 2);
    index.setByte(12, (byte) 12);
    index.setByte(52, (byte) 52);
    final String str = index.toString();
    assertEquals(TO_STR, str);
  }

  private static final String TO_STR15 = ""
    + "Index [15]" + LS
    + "[0]     0,     1,     2,     0,     0,     0,     0,     0,     0,     0" + LS
    + "[10]     0,     0,    12,     0,     0" + LS;

  public void testToString15() {
    final ByteIndex index = create(15);
    index.setByte(1, (byte) 1);
    index.setByte(2, (byte) 2);
    index.setByte(12, (byte) 12);
    final String str = index.toString();
    assertEquals(TO_STR15, str);
  }


  public void testShortToString() {
    final ByteIndex index = create(3);
    index.setByte(1, (byte) 1);
    final String str = index.toString();
    assertEquals("Index [3]" + LS + "[0]     0,     1,     0" + LS, str);
  }

  public void testLength() {
    final int le = 42000;
    final ByteIndex a = create(le);
    a.integrity();
    assertEquals(le, a.length());
    assertEquals(le, a.bytes());
    a.integrity();
  }

  public void testLength0() {
    final int le = 0;
    final ByteIndex a = create(le);
    a.integrity();
    assertEquals(le, a.length());
    assertEquals(2 * le, a.bytes());
    a.integrity();
  }

  public void testBadLength1() {
    try {
      create(Short.MIN_VALUE);
      fail("Expected NegativeArraySizeException");
    } catch (final NegativeArraySizeException e) {
      // expected
    }
    try {
      create(-1);
      fail("Expected NegativeArraySizeException");
    } catch (final NegativeArraySizeException e) {
      // expected
    }
  }

  public void testIntensiveSetGet() {
    //needed for subtle errors in underlying mapping in disk backed cases
    final int length = 100; // > 2^5 (see ShortDiskChunksTest and ShortChunksTest - also not a multiple of 2^3
    final ByteIndex a = create(length, 3);
    a.integrity();
    assertEquals(length, a.length());
    for (int i = 0; i < a.length(); i++) {
      assertEquals(0, a.getByte(i));
      final byte j = (byte) (i * 3);
      a.setByte(i, j);
    }
    for (int i = 0; i < a.length(); i++) {
      final short j = (byte) (i * 3);
      assertEquals(j, a.getByte(i));
    }
    a.integrity();
  }

  public void testGetSetLong() {
    final ByteIndex a = create(LENGTH);
    a.integrity();
    assertEquals(LENGTH, a.length());
    for (int i = 0; i < a.length(); i += STEP) {
      assertEquals(0, a.getByte(i));
      final byte j = (byte) (i * 3);
      a.setByte(i, j);
    }
    for (int i = 0; i < a.length(); i += STEP) {
      final short j = (byte) (i * 3);
      assertEquals(j, a.getByte(i));
    }
  }

  public void testSwap() {
    final ByteIndex a = create(LENGTH, 16);
    a.integrity();
    assertEquals(LENGTH, a.length());
    for (int i = 0; i < a.length(); i += STEP) {
      assertEquals(0, a.getByte(i));
      final byte j = (byte) (i * 3);
      a.setByte(i, j);
      assertEquals(j, a.getByte(i));
    }
    for (int i = 0; i < a.length() - 1; i += STEP) {
      final short j = (byte) (i * 3);
      assertEquals(i + ":" + 0, 0, a.getByte(i + 1));
      assertEquals(i + ":" + j, j, a.getByte(i));
      a.swap(i, i + 1);
      assertEquals(i + ":" + j, j, a.getByte(i + 1));
      assertEquals(i + ":" + 0, 0, a.getByte(i));
      a.swap(i, i + 1);
      assertEquals(i + ":" + 0, 0, a.getByte(i + 1));
      assertEquals(i + ":" + j, j, a.getByte(i));
    }
  }


  public void testEdges() {
    final ByteIndex a = create(LENGTH);
    a.integrity();
    assertEquals(LENGTH, a.length());
    a.setByte(LENGTH - 1, (byte) 1);
    assertEquals(1L, a.getByte(LENGTH - 1));
    checkLimits(a, LENGTH);
  }

  private void checkLimits(final ByteIndex a, final int length) {
    a.setByte(length - 1, (byte) 42);
    assertEquals(42, a.getByte(length - 1));
    try {
      a.getByte(length);
      fail("Exception expected");
    } catch (final RuntimeException | AssertionError e) {
      //expected
    }
    try {
      a.setByte(length, (byte) 0);
      fail("Exception expected");
    } catch (final RuntimeException e) {
      //expected
    }
    a.setByte(0, (byte) 1);
    assertEquals(1L, a.getByte(0));
    try {
      a.getByte(-1);
      fail("Exception expected");
    } catch (final RuntimeException | AssertionError e) {
      //expected
    }
    try {
      a.setByte(-1, (byte) 0);
      fail("Exception expected");
    } catch (final RuntimeException | AssertionError e) {
      //expected
    }
  }
}


