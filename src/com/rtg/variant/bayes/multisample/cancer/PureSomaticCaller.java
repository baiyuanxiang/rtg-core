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

package com.rtg.variant.bayes.multisample.cancer;

import com.rtg.variant.VariantParams;
import com.rtg.variant.bayes.ModelInterface;
import com.rtg.variant.bayes.snp.HypothesesPrior;

/**
 * A simple cancer caller that assumes no contamination.
 */
public class PureSomaticCaller extends AbstractSomaticCaller {

  /**
   * @param qHaploidFactory haploid Q matrix factory
   * @param qDiploidFactory diploid Q matrix factory
   * @param params variant params
   */
  public PureSomaticCaller(SomaticPriorsFactory<?> qHaploidFactory, SomaticPriorsFactory<?> qDiploidFactory, VariantParams params) {
    super(qHaploidFactory, qDiploidFactory, params);
  }

  @Override
  protected AbstractPosterior makePosterior(final ModelInterface<?> normal, final ModelInterface<?> cancer, HypothesesPrior<?> hypotheses, double mu) {
    return new PosteriorPure((normal.haploid() ? mQHaploidFactory : mQDiploidFactory).somaticQ(mu), normal, cancer, hypotheses);
  }
}
