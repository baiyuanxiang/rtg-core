/*
 * Copyright (c) 2016. Real Time Genomics Limited.
 *
 * Use of this source code is bound by the Real Time Genomics Limited Software Licence Agreement
 * for Academic Non-commercial Research Purposes only.
 *
 * If you did not receive a license accompanying this file, a copy must first be obtained by email
 * from support@realtimegenomics.com.  On downloading, using and/or continuing to use this source
 * code you accept the terms of that license agreement and any amendments to those terms that may
 * be made from time to time by Real Time Genomics Limited.
 */

package com.rtg.variant.cnv.preprocess;

import junit.framework.TestCase;

/**
 * Tests the corresponding class.
 */
public class AddLogTest extends TestCase {

  public void test() {
    final IntColumn col = new IntColumn("x");
    col.add(8);
    final RegionDataset ds = new RegionDataset(new String[0]);
    ds.addColumn(col);
    new AddLog(0).process(ds);
    assertEquals(2, ds.columns());
    assertEquals("log2(x)", ds.getColumns().get(1).getName());
    assertEquals("3.00000", ds.getColumns().get(1).toString(0));
  }

  public void testName() {
    final IntColumn col = new IntColumn("x");
    col.add(8);
    final RegionDataset ds = new RegionDataset(new String[0]);
    ds.addColumn(col);
    new AddLog(0, "testName").process(ds);
    assertEquals(2, ds.columns());
    assertEquals("testName", ds.getColumns().get(1).getName());
    assertEquals("3.00000", ds.getColumns().get(1).toString(0));
  }
}
