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

package com.rtg.variant.cnv.segment;

import junit.framework.TestCase;

/**
 * Tests the corresponding class.
 */
public class SegmentTest extends TestCase {

  public void test() {
    final Segment s = new Segment(42, 43, 100, 1);
    assertEquals(1, s.bins());
    assertEquals(100.0, s.sum());
    assertEquals(10000.0, s.sumSquares());
    assertEquals(100.0, s.mean());
    assertEquals(0.0, s.meanDistanceBetween());
    assertEquals(42, s.getStart());
    assertEquals(43, s.getEnd());
    final Segment m = s.merge(new Segment(43, 45, 50, 1));
    assertEquals(2, m.bins());
    assertEquals(150.0, m.sum());
    assertEquals(12500.0, m.sumSquares());
    assertEquals(75.0, m.mean());
    assertEquals(1.0, m.meanDistanceBetween());
    assertEquals(42, m.getStart());
    assertEquals(45, m.getEnd());
    assertEquals("2", m.toString());
    assertEquals(1.0, m.distanceToPrevious());
    assertEquals(1, m.firstBinLength());
    assertEquals(2, m.lastBinLength());
  }
}
