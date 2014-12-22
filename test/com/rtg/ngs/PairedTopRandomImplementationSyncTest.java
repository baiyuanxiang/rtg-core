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

package com.rtg.ngs;


/**
 * Test class
 */
public class PairedTopRandomImplementationSyncTest extends PairedTopRandomImplementationTest {

  @Override
  public PairedTopRandomImplementation getImpl() {
    return new PairedTopRandomImplementationSync(3, 7);
  }

  public void testSyncId() {
    assertEquals(0, PairedTopRandomImplementationSync.syncId(0));
    assertEquals(0, PairedTopRandomImplementationSync.syncId(65536));
    assertEquals(1, PairedTopRandomImplementationSync.syncId(1));
    assertEquals(1, PairedTopRandomImplementationSync.syncId(65536 + 1));
  }

  public void testToString() {
    assertEquals("PairedTopRandomImplementationSync", getImpl().toString());
  }
}
