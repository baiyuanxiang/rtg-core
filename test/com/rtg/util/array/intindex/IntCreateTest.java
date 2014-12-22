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


import junit.framework.TestCase;

/**
 * Test Create
 */
public class IntCreateTest extends TestCase {

  public void testBad() {
    try {
      IntCreate.createIndex(-1);
      fail("NegativeArraySizeException expected");
    } catch (final NegativeArraySizeException e) {
      //expected
      assertEquals("Negative length=-1", e.getMessage());
    }

    final IntIndex index = IntCreate.createIndex(0);
    index.integrity();
    final IntIndex i = IntCreate.createIndex(20);
    assertTrue(i instanceof IntArray);



  }
}


