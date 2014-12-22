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
package com.rtg.util.array.shortindex;

import junit.framework.TestCase;

/**
 * Tests for <code>ShortIndex</code>.
 *
 */
public abstract class AbstractShortIndexTest extends TestCase {

  protected static final int STEP = 1000;
  protected static final int LENGTH = 1000007; //this isnt a multiple of two because then it might not catch many tricky cases

  /** Local new line convention */
  private static final String LS = System.lineSeparator();

  protected abstract ShortIndex create(final long length);

  protected abstract ShortIndex create(final long length, final int bits);

  public void testMasks() {
    assertEquals(ShortIndex.SHORT_MASK & ShortIndex.HIGH_MASK, 0);
    assertEquals(ShortIndex.HIGH_SIGNED_MASK & ShortIndex.HIGH_MASK, ShortIndex.HIGH_MASK);
    assertEquals(ShortIndex.HIGH_SIGNED_MASK & ShortIndex.SHORT_MASK, 1L << 15);
  }

  private static final String TO_STR = ""
      + "Index [100]" + LS
      + "[0]     0,     1,     2,     0,     0,     0,     0,     0,     0,     0" + LS
      + "[10]     0,     0,    12,     0,     0,     0,     0,     0,     0,     0" + LS
      + "[50]     0,     0,    52,     0,     0,     0,     0,     0,     0,     0" + LS;

  public void testToString() {
    final ShortIndex index = create(100);
    index.setShort(1, (short) 1);
    index.setShort(2, (short) 2);
    index.setShort(12, (short) 12);
    index.setShort(52, (short) 52);
    final String str = index.toString();
    assertEquals(TO_STR, str);
  }

  private static final String TO_STR15 = ""
      + "Index [15]" + LS
      + "[0]     0,     1,     2,     0,     0,     0,     0,     0,     0,     0" + LS
      + "[10]     0,     0,    12,     0,     0" + LS;

  public void testToString15() {
    final ShortIndex index = create(15);
    index.setShort(1, (short) 1);
    index.setShort(2, (short) 2);
    index.setShort(12, (short) 12);
    final String str = index.toString();
    assertEquals(TO_STR15, str);
  }


  public void testShortToString() {
    final ShortIndex index = create(3);
    index.setShort(1, (short) 1);
    final String str = index.toString();
    assertEquals("Index [3]" + LS + "[0]     0,     1,     0" + LS, str);
  }

  public void testLength() {
    final int le = 42000;
    final ShortIndex a = create(le);
    a.integrity();
    assertEquals(le, a.length());
    assertEquals(2 * le, a.bytes());
    a.integrity();
  }

  public void testLength0() {
    final int le = 0;
    final ShortIndex a = create(le);
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
    final ShortIndex a = create(length, 3);
    a.integrity();
    assertEquals(length, a.length());
    for (int i = 0; i < a.length(); i++) {
      assertEquals(0, a.getShort(i));
      final short j = (short) (i * 3);
      a.setShort(i, j);
    }
    for (int i = 0; i < a.length(); i++) {
      final short j = (short) (i * 3);
      assertEquals(j, a.getShort(i));
    }
    a.integrity();
  }

  public void testGetSetLong() {
    final ShortIndex a = create(LENGTH);
    a.integrity();
    assertEquals(LENGTH, a.length());
    for (int i = 0; i < a.length(); i += STEP) {
      assertEquals(0, a.getShort(i));
      final short j = (short) (i * 3);
      a.setShort(i, j);
    }
    for (int i = 0; i < a.length(); i += STEP) {
      final short j = (short) (i * 3);
      assertEquals(j, a.getShort(i));
    }
  }

  public void testSwap() {
    final ShortIndex a = create(LENGTH, 7);
    a.integrity();
    assertEquals(LENGTH, a.length());
    for (int i = 0; i < a.length(); i += STEP) {
      assertEquals(0, a.getShort(i));
      final short j = (short) (i * 3);
      a.setShort(i, j);
      assertEquals(j, a.getShort(i));
    }
    for (int i = 0; i < a.length() - 1; i += STEP) {
      final short j = (short) (i * 3);
      assertEquals(i + ":" + 0, 0, a.getShort(i + 1));
      assertEquals(i + ":" + j, j, a.getShort(i));
      a.swap(i, i + 1);
      assertEquals(i + ":" + j, j, a.getShort(i + 1));
      assertEquals(i + ":" + 0, 0, a.getShort(i));
      a.swap(i, i + 1);
      assertEquals(i + ":" + 0, 0, a.getShort(i + 1));
      assertEquals(i + ":" + j, j, a.getShort(i));
    }
  }


  public void testEdges() {
    final ShortIndex a = create(LENGTH);
    a.integrity();
    assertEquals(LENGTH, a.length());
    a.setShort(LENGTH - 1, (short) 1);
    assertEquals(1L, a.getShort(LENGTH - 1));
    checkLimits(a, LENGTH);
  }

  private void checkLimits(final ShortIndex a, final int length) {
    a.setShort(length - 1, (short) 42);
    assertEquals(42, a.getShort(length - 1));
    try {
      a.getShort(length);
      fail("Exception expected");
    } catch (final RuntimeException | AssertionError e) {
      //expected
    }
    try {
      a.setShort(length, (short) 0);
      fail("Exception expected");
    } catch (final RuntimeException e) {
      //expected
    }
    a.setShort(0, (short) 1);
    assertEquals(1L, a.getShort(0));
    try {
      a.getShort(-1);
      fail("Exception expected");
    } catch (final RuntimeException | AssertionError e) {
      //expected
    }
    try {
      a.setShort(-1, (short) 0);
      fail("Exception expected");
    } catch (final RuntimeException | AssertionError e) {
      //expected
    }
  }
}


