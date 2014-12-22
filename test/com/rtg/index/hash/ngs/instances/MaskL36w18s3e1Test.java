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
package com.rtg.index.hash.ngs.instances;

import java.io.IOException;

import com.rtg.index.hash.ngs.NgsHashFunction;
import com.rtg.index.hash.ngs.ReadCall;
import com.rtg.index.hash.ngs.TemplateCall;

/**
 */
public class MaskL36w18s3e1Test extends AbstractSplitTest {

  @Override
  protected NgsHashFunction getHashFunction(final ReadCall readCall, final TemplateCall templateCall) {
    return new MaskL36w18s3e1(readCall, templateCall) {
      @Override
      protected long hash(final long x) {
        return x;
      }
    };
  }

  public void testFactory() {
    assertEquals(36, MaskL36w18s3e1.FACTORY.hashBits());
    assertEquals(36, MaskL36w18s3e1.FACTORY.windowBits());
    assertEquals(20, MaskL36w18s3e1.FACTORY.numberWindows());
    final NgsHashFunction hf = MaskL36w18s3e1.FACTORY.create(new ReadCallMock(null), new TemplateCallMock(null));
    assertTrue(hf != null);
    assertTrue(hf instanceof MaskL36w18s3e1);
    assertEquals(hf.numberWindows(), MaskL36w18s3e1.FACTORY.numberWindows());
    assertEquals("MaskL36w18s3e1 l=36 w=18 s=3 e=1", hf.toString());
  }

  /**
   * Check that all 0, 1, 2 substitutions on the string are found.
   */
  public void testAllSubstitutions() throws IOException {
    final String str = "acgacgtgacacccgtacgtaccccgtgacacccgtccgtacgtaccccgt";
    assertEquals(51, str.length());
    final Substitute sub = new Substitute(str, MaskL36w18s3e1.FACTORY, true);
    sub.substituteProtected(3);

  }

  /**
   * Check that all 0, 1, 2 substitutions on the string are found.
   * @throws IOException
   */
  public void testIndel() throws IOException {
    final String str = "acgacgtgacacccgtacgtaccccgtgacacccgt";
    assertEquals(36, str.length());
    SubstituteIndel.checkIndel(MaskL36w18s3e1.FACTORY, str, 1, 0/*cg*/, 7);
  }
}

