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

package com.rtg.variant.cnv.segment;

import java.io.IOException;

import com.rtg.launcher.AbstractNanoTest;
import com.rtg.reader.ArraySequencesReader;
import com.rtg.util.StringUtils;
import com.rtg.util.TestUtils;

/**
 * Test the corresponding class.
 */
public class SegmentVcfOutputFormatterTest extends AbstractNanoTest {

  public void test() throws IOException {
    final SegmentVcfOutputFormatter formatter = new SegmentVcfOutputFormatter(new ArraySequencesReader(StringUtils.repeat("A", 1000), StringUtils.repeat("G", 1000), StringUtils.repeat("AG", 500)), 0.0, 1, "SAMPLE");
    final String header = formatter.header().toString();
    TestUtils.containsAll(header,
      "##contig=<ID=sequence 0,length=1000>",
      "##contig=<ID=sequence 1,length=1000>",
      "##contig=<ID=sequence 2,length=1000>",
      "##ALT=<ID=DEL,Description=\"Deletion\">",
      "##ALT=<ID=DUP,Description=\"Duplication\">",
      "##INFO=<ID=END,Number=1,Type=Integer,Description=\"End position of the variant described in this record\">",
      "##INFO=<ID=IMPRECISE,Number=0,Type=Flag,Description=\"Imprecise structural variation\">",
      "##INFO=<ID=SVTYPE,Number=1,Type=String,Description=\"Type of structural variant\">",
      "##INFO=<ID=BC,Number=1,Type=Integer,Description=\"Number of bins contained within the region\">",
      "##INFO=<ID=CIPOS,Number=2,Type=Integer,Description=\"Confidence interval around POS for imprecise variants\">",
      "##INFO=<ID=CIEND,Number=2,Type=Integer,Description=\"Confidence interval around END for imprecise variants\">",
      "##FORMAT=<ID=GT,Number=1,Type=String,Description=\"Genotype\">",
      "#FORMAT=<ID=SQS,Number=1,Type=Float,Description=\"Segment quality score\">",
      "##FORMAT=<ID=RDR,Number=1,Type=Float,Description=\"Mean normalized RD ratio with respect to control\">",
      "##FORMAT=<ID=LR,Number=1,Type=Float,Description=\"Log2 of RD ratio with respect to control\">",
      "#CHROM\tPOS\tID\tREF\tALT\tQUAL\tFILTER\tINFO\tFORMAT\tSAMPLE"
      );
    final StringBuilder sb = new StringBuilder();
    sb.append(formatter.vcfRecord("sequence 1", null, new Segment(1, 42, 4.3, 0), new Segment(60, 70, 8.0, 48.0))).append('\n');
    sb.append(formatter.vcfRecord("sequence 1", new Segment(1, 42, 4.3, 0), new Segment(60, 70, 8.0, 48.0), null)).append('\n');
    mNano.check("svof-example.txt", sb.toString());
  }
}
