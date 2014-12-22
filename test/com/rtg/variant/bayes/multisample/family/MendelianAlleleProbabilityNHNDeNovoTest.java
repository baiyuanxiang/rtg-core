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

package com.rtg.variant.bayes.multisample.family;

import com.rtg.variant.bayes.Code;
import com.rtg.variant.bayes.CodeDiploid;
import com.rtg.variant.bayes.CodeHaploid;

import junit.framework.TestCase;

/**
 */
public class MendelianAlleleProbabilityNHNDeNovoTest extends TestCase {

  public void test() {
    final Code code = new CodeHaploid(4);
    assertEquals(Double.NEGATIVE_INFINITY, MendelianAlleleProbabilityNHNDeNovo.SINGLETON_HN.probabilityLn(code, 0, -1, -1));
    assertEquals(Double.NEGATIVE_INFINITY, MendelianAlleleProbabilityNHNDeNovo.SINGLETON_NH.probabilityLn(code, -1, 0, -1));
  }

  //test there is no overlap between non de novo and de novo tables
  public void testOverlap() {
    final Code code = new CodeDiploid(4);
    for (int i = 0; i < code.rangeSize(); i++) {
      if (MendelianAlleleProbabilityNHN.SINGLETON_HN.probabilityLn(code, i, -1, -1) > Double.NEGATIVE_INFINITY) {
        assertEquals(Double.NEGATIVE_INFINITY, MendelianAlleleProbabilityNHNDeNovo.SINGLETON_HN.probabilityLn(code, i, -1, -1));
      }
      if (MendelianAlleleProbabilityNHN.SINGLETON_NH.probabilityLn(code, -1, i, -1) > Double.NEGATIVE_INFINITY) {
        assertEquals(Double.NEGATIVE_INFINITY, MendelianAlleleProbabilityNHNDeNovo.SINGLETON_NH.probabilityLn(code, -1, i, -1));
      }
    }
  }
}
