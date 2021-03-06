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

import java.io.IOException;

import com.rtg.util.io.MemoryPrintStream;

import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SAMFileWriterFactory;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SAMSequenceRecord;
import junit.framework.TestCase;

/**
 *
 */
public class SmartSamWriterTest extends TestCase {

  static SAMRecord createRecord(int position) {
    final SAMFileHeader header = new SAMFileHeader();
    header.getSequenceDictionary().addSequence(new SAMSequenceRecord("A", 100000));
    final SAMRecord rec = new SAMRecord(header);
    rec.setReferenceName("A");
    rec.setReadName("read");
    rec.setReadString("ACGT");
    rec.setBaseQualityString("####");
    rec.setCigarString("4=");
    rec.setAlignmentStart(position);
    return rec;
  }

  public void test() throws IOException {
    final SAMRecord[] records = {
      createRecord(1300),
      createRecord(1400),
      createRecord(1500),
      createRecord(1600),
      createRecord(2000),
    };
    final MemoryPrintStream mps = new MemoryPrintStream();
    final SmartSamWriter smartSamWriter = new SmartSamWriter(new SAMFileWriterFactory().makeSAMWriter(records[0].getHeader(), true, mps.outputStream()));

    smartSamWriter.addRecord(records[2]);
    smartSamWriter.addRecord(records[0]);
    smartSamWriter.addRecord(records[4]);
    smartSamWriter.addRecord(records[3]);
    smartSamWriter.addRecord(records[1]);
    smartSamWriter.close();
    final StringBuilder sb = new StringBuilder();
    sb.append(SamUtils.getHeaderAsString(records[0].getHeader()));
    for (SAMRecord r : records) {
      sb.append(r.getSAMString());
    }
    assertEquals(sb.toString(), mps.toString());
  }

}