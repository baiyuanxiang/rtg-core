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

package com.rtg.assembler;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import com.rtg.assembler.graph.MutableGraph;
import com.rtg.assembler.graph.implementation.GraphKmerAttribute;
import com.rtg.reader.ReaderTestUtils;
import com.rtg.reader.SdfId;
import com.rtg.util.TestUtils;
import com.rtg.util.diagnostic.Diagnostic;
import com.rtg.util.io.MemoryPrintStream;
import com.rtg.util.io.TestDirectory;

import junit.framework.TestCase;

/**
 */
public class GraphMapTaskTest extends TestCase {
  public void testReadHitsTask() throws IOException {
    try (final TestDirectory tmpDir = new TestDirectory()) {
      final File reads = new File(tmpDir, "reads");
      final File graph = new File(tmpDir, "graph");
      assertTrue(graph.mkdir());
      final File output = new File(tmpDir, "output");
      assertTrue(output.mkdir());
      final MutableGraph g = GraphMapCliTest.makeGraph(4, new String[]{"ACGTTTT", "TTTTCC", "TTTTAA"}, new long[][]{{1, 2}, {1, 3}});
      ReaderTestUtils.createPairedReaderDNA(ReadPairSourceTest.LEFT_SEQUENCE, ReadPairSourceTest.RIGHT_SEQUENCE, reads, new SdfId());
      final GraphMapParams params = GraphMapParams.builder()
          .reads(Arrays.asList(reads))
          .graph(g)
          .directory(output)
          .wordSize(4)
          .stepSize(4)
          .create();
      final MemoryPrintStream out = new MemoryPrintStream();
      final MemoryPrintStream mps = new MemoryPrintStream();
      Diagnostic.setLogStream(mps.printStream());
      try {
      new GraphMapTask(params, out.printStream()).run();
      assertTrue(new File(output, "contig.1.fa").exists());
      assertTrue(new File(output, "path.1.tsv").exists());
      assertTrue(new File(output, "header.tsv").exists());

      TestUtils.containsAll(mps.toString()
          , "1 Too many paths"
          , "3 No paths"
      );
      } finally {
        Diagnostic.setLogStream();
      }
    }
  }
  public void testReadHitsTask454() throws IOException {
    try (final TestDirectory tmpDir = new TestDirectory()) {
      final File reads = new File(tmpDir, "reads");
      final File graph = new File(tmpDir, "graph");
      assertTrue(graph.mkdir());
      final File output = new File(tmpDir, "output");
      assertTrue(output.mkdir());
      final MutableGraph g = GraphMapCliTest.makeGraph(4, new String[]{"ACGTTTT", "TTTTCC", "TTTTAA"}, new long[][]{{1, 2}, {1, 3}});
      ReaderTestUtils.createPairedReaderDNA(ReadPairSourceTest.LEFT_SEQUENCE, ReadPairSourceTest.RIGHT_SEQUENCE, reads, new SdfId());
      final GraphMapParams params = GraphMapParams.builder()
          .reads454(Arrays.asList(reads))
          .graph(g)
          .directory(output)
          .wordSize(4)
          .stepSize(4)
          .create();
      final MemoryPrintStream mps = new MemoryPrintStream();
      Diagnostic.setLogStream(mps.printStream());
      try {
        new GraphMapTask(params, TestUtils.getNullOutputStream()).run();
        assertTrue(new File(output, "contig.1.fa").exists());
        assertTrue(new File(output, "path.1.tsv").exists());
        assertTrue(new File(output, "header.tsv").exists());

        TestUtils.containsAll(mps.toString()
            , "1 Too many paths"
            , "3 No paths"
        );
      } finally {
        Diagnostic.setLogStream();
      }
    }
  }

  public void testReadHitsTaskCalculateInsert() throws IOException {
    try (final TestDirectory tmpDir = new TestDirectory()) {
      final Map<String, String> readCount = new HashMap<>();
      readCount.put(GraphKmerAttribute.READ_COUNT, "count of reads");
      final MutableGraph g = GraphMapCliTest.makeGraph(4, new String[]{"ACAACAC", "CGGGGT", "TCCCCTACTACAGCAG"}, new long[][]{{1, 2}, {2, 3}}, readCount, readCount);
      final MemoryPrintStream mps =  new MemoryPrintStream();
      Diagnostic.setLogStream(mps.printStream());
      final File reads = new File(tmpDir, "reads");
      final File reads2 = new File(tmpDir, "reads2");
      final File reads3 = new File(tmpDir, "reads3");
      final File graph = new File(tmpDir, "graph");
      assertTrue(graph.mkdir());
      final File output = new File(tmpDir, "output");
      assertTrue(output.mkdir());
      ReaderTestUtils.createPairedReaderDNA(ReaderTestUtils.fasta("TCCCCTACT", "CCTACTA"), ReaderTestUtils.fasta("CTGCTGTAGTA", "CTGCTGTA"), reads, new SdfId());
      ReaderTestUtils.createPairedReaderDNA(ReaderTestUtils.fasta("TCCCCTACT", "CCTACTA"), ReaderTestUtils.fasta("CTGCTGTAGTA", "CTGCTGTA"), reads2, new SdfId());
      // make single end long enough that it will time out if blocked
      final String[] singleEndStrings = new String[1049];
      for (int i = 0; i < 1049; ++i) {
        singleEndStrings[i] = "TCCCCTACT";
      }
      ReaderTestUtils.getDNADir(ReaderTestUtils.fasta(singleEndStrings), reads3);
      final GraphMapParams params = GraphMapParams.builder()
          .reads(Arrays.asList(reads, reads2, reads3))
          .graph(g)
          .directory(output)
          .wordSize(4)
          .stepSize(4)
          .create();
      try {
        new GraphMapTask(params, TestUtils.getNullOutputStream()).run();
        assertTrue(new File(output, "contig.1.fa").exists());
        assertTrue(new File(output, "path.1.tsv").exists());
        assertTrue(new File(output, "header.tsv").exists());

        TestUtils.containsAll(mps.toString()
            , "4 Paired end reads"
            , "4 Successfully paired"
            , "Min Insert: -8"
            , "Max Insert: 4"
        );
      } finally {
        Diagnostic.setLogStream();
      }
    }
  }
}
