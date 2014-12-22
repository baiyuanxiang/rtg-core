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

package com.rtg.util;

import junit.framework.TestCase;

/**
 */
public class DoubleMultiSetTest extends TestCase {
  public void test() {
    final DoubleMultiSet<Integer> set = new DoubleMultiSet<>();
    assertEquals("[ ]", set.toString());
    assertEquals(0.0, set.get(0));

    set.add(0);
    assertEquals("[ 0->1.0]", set.toString());
    assertEquals(1.0, set.get(0));

    set.add(0);
    assertEquals("[ 0->2.0]", set.toString());
    assertEquals(2.0, set.get(0));

    for (int i = 1; i < 10; i++) {
      set.add(i);
    }
    final String exp10 = "[ 0->2.0, 1->1.0, 2->1.0, 3->1.0, 4->1.0, 5->1.0, 6->1.0, 7->1.0, 8->1.0, 9->1.0";
    assertEquals(exp10 + "]", set.toString());
    assertEquals(2.0, set.get(0));
    for (int i = 1; i < 10; i++) {
      assertEquals((double) 1, set.get(i));
    }

    set.add(12);
    assertEquals(exp10 + StringUtils.LS + ", 12->1.0"  + StringUtils.LS + "]", set.toString());
    assertEquals(2.0, set.get(0));
    for (int i = 1; i < 10; i++) {
      assertEquals((double) 1, set.get(i));
    }
    assertEquals(0.0, set.get(11));
    assertEquals(1.0, set.get(12));

  }
}
