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
package com.rtg.sam;

import net.sf.samtools.SAMRecord;

import junit.framework.TestCase;

/**
 */
public class ReadGroupUtilsTest extends TestCase {

  public void testSamRecord() {
    final SAMRecord record = new SAMRecord(null);
    assertEquals("unspecified", ReadGroupUtils.getReadGroup(record));
    record.setAttribute("RG", "blah");
    assertEquals("blah", ReadGroupUtils.getReadGroup(record));
  }

  public void testSamBamRecord() {
    final MockSamBamRecord record = new MockSamBamRecord();
    assertEquals("unspecified", ReadGroupUtils.getReadGroup(record));
    record.addAttribute("RG:Z:blah");
    assertEquals("blah", ReadGroupUtils.getReadGroup(record));
  }
}
