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

import static com.rtg.util.StringUtils.LS;

import com.rtg.assembler.graph.Graph;
import com.rtg.launcher.AbstractCli;
import com.rtg.launcher.AbstractCliTest;
import com.rtg.util.io.MemoryPrintStream;

/**
 */
public class CountAssemblyEndsTest extends AbstractCliTest {
  @Override
  protected AbstractCli getCli() {
    return new CountAssemblyEnds();
  }
  public void testHelp() {
    checkHelp("rtg countends"
        , "input graph directory"
    );
  }

  public void testEnds() {
    Graph g = GraphMapCliTest.makeGraph(0, new String[]{"AACCACCAGT", "TTGTGAGAGTAG", "ACGACAATAT", "ACTTTGTGG"}, new long[][]{{1, 2}, {2, 4}, {1, 3}, {3, 4}});
    MemoryPrintStream mps = new MemoryPrintStream();
    CountAssemblyEnds.showEnds(mps.printStream(), g);
    assertEquals("-1" + LS + "4" + LS, mps.toString());


  }

}
