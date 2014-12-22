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

package com.rtg.variant.bayes.snp;

/**
 * Provide names for SNP hypotheses
 */
public final class DescriptionSnp extends DescriptionCommon {

  /** name for T */
  public static final String T = "T";

  /** name for G */
  public static final String G = "G";

  /** name for C */
  public static final String C = "C";

  /** name for A */
  public static final String A = "A";

  /** SNPs without delete. */
  public static final DescriptionSnp SINGLETON = new DescriptionSnp();

  /**
   * Make one.
   */
  private DescriptionSnp() {
    super(A, C, G, T);
  }

  @Override
  public int minLength() {
    return 1;
  }

  @Override
  public int maxLength() {
    return 1;
  }
}
