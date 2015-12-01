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

import java.io.IOException;
import java.util.List;

import com.reeltwo.jumble.annotations.TestClass;
import com.rtg.mode.DnaUtils;
import com.rtg.reference.Ploidy;
import com.rtg.util.MathUtils;
import com.rtg.util.diagnostic.Diagnostic;
import com.rtg.util.integrity.Exam;
import com.rtg.util.integrity.IntegralAbstract;
import com.rtg.util.intervals.RangeList;
import com.rtg.util.intervals.ReferenceRanges;
import com.rtg.variant.Variant;
import com.rtg.variant.VariantLocus;
import com.rtg.variant.VariantOutputLevel;
import com.rtg.variant.VariantOutputOptions;
import com.rtg.variant.VariantParams;
import com.rtg.variant.VariantSample;
import com.rtg.variant.bayes.Code;
import com.rtg.variant.bayes.Description;
import com.rtg.variant.bayes.GenotypeMeasure;
import com.rtg.variant.bayes.Hypotheses;
import com.rtg.variant.bayes.ModelInterface;
import com.rtg.variant.bayes.multisample.HaploidDiploidHypotheses;
import com.rtg.variant.bayes.multisample.MultisampleJointCaller;
import com.rtg.variant.bayes.multisample.Utils;
import com.rtg.variant.bayes.snp.HypothesesPrior;
import com.rtg.variant.dna.DNARange;
import com.rtg.variant.util.VariantUtils;

/**
 * The bulk of the implementation for the somatic caller.  Takes the models from the normal and cancer
 * samples and examines the joint (possibly contaminated) distribution to determine the best call for
 * the pair of samples.
 */
@TestClass(value = {"com.rtg.variant.bayes.multisample.cancer.ContaminatedSomaticCallerTest", "com.rtg.variant.bayes.multisample.cancer.PureSomaticCallerTest"})
public abstract class AbstractSomaticCaller extends IntegralAbstract implements MultisampleJointCaller {

  static final int NORMAL = 0;
  static final int CANCER = 1;

  protected final SomaticPriorsFactory<?> mQHaploidFactory;
  protected final SomaticPriorsFactory<?> mQDiploidFactory;
  private final VariantParams mParams;
  private final ReferenceRanges<Double> mSiteSpecificSomaticPriors;
  private final double mIdentityInterestingThreshold;
  protected final double mPhi;
  protected final double mPsi;

  /**
   * @param qHaploidFactory haploid Q matrix factory
   * @param qDiploidFactory diploid Q matrix factory
   * @param params variant params
   * @param phi probability of seeing contrary evidence in the original
   * @param psi probability of seeing contrary evidence in the derived
   */
  public AbstractSomaticCaller(final SomaticPriorsFactory<?> qHaploidFactory, final SomaticPriorsFactory<?> qDiploidFactory, final VariantParams params, final double phi, final double psi) {
    mQHaploidFactory = qHaploidFactory;
    mQDiploidFactory = qDiploidFactory;
    mParams = params;
    mSiteSpecificSomaticPriors = mParams.siteSpecificSomaticPriors();
    mIdentityInterestingThreshold = mParams.interestingThreshold() * MathUtils.LOG_10;
    mPhi = phi;
    mPsi = psi;
  }

  /**
   * Construct an appropriate posterior. Differs in the contaminated and non-contaminated case.
   * @param normal bayesian for the normal genome
   * @param cancer bayesian for the cancer genome
   * @param hypotheses the hypotheses containing priors
   * @param mu somatic mutation rate
   * @return the posterior
   */
  protected abstract AbstractSomaticPosterior makePosterior(final ModelInterface<?> normal, final ModelInterface<?> cancer, final HypothesesPrior<?> hypotheses, final double mu);

  private VariantSample setCallValues(GenotypeMeasure posterior, int cat, Hypotheses<?> hypotheses, ModelInterface<?> model, VariantOutputOptions params, Ploidy ploidy, VariantSample.DeNovoStatus dns, Double dnp) {
    final VariantSample sample = new VariantSample(ploidy, hypotheses.name(cat), hypotheses.reference() == cat, posterior, dns, dnp);
    model.statistics().addCountsToSample(sample, model, params);
    return sample;
  }

  private double getSomaticPrior(final String seqName, final int pos) {
    if (mSiteSpecificSomaticPriors != null) {
      final RangeList<Double> rangeList = mSiteSpecificSomaticPriors.get(seqName);
      if (rangeList != null) {
        final List<Double> v = rangeList.find(pos);
        // Take the maximum of the supplied priors
        if (v != null) {
          double p = 0;
          for (final double pv : v) {
            if (pv > p) {
              p = pv;
            }
          }
          return p;
        }
      }
    }
    return mParams.somaticRate();
  }

  private double getSomaticPrior(final String seqName, final int start, final int end) {
    if (end <= start + 1) {
      return getSomaticPrior(seqName, start); // handles zero and one length regions
    }
    // For an extended region choose a prior that is the mean of the point priors in the region
    double s = 0;
    for (int k = start; k < end; k++) {
      s += getSomaticPrior(seqName, k);
    }
    return s / (end - start);
  }

  private double loh(final Hypotheses<?> hypotheses, final int normal, final int cancer) {
    final Code code = hypotheses.code();
    if (!hypotheses.code().homozygous(normal) && code.homozygous(cancer)) {
      return 1;
    }
    if (cancer == normal) {
      return 0;
    }
    return -1;
  }

  // Counts for making LOH and somatic rate estimations, see multiscoring.tex theory doc
  private double mEll = 0;
  private double mOneMinusEll = 0;
  private double mMu = 0;
  private double mOneMinusMu = 0;

  private void updateParameterEstimationCounts(final Ploidy normalPloidy, final Code code, final int normal, final int cancer, final double loh) {
    // See multiscoring.tex for an explanation.
    // A more thorough treatment should probably increment mEll and mOneMinusEll in proportion to loh
    // but we often have loh of 0 when we haven't actually made a call, so this could be tricky.
    // The theory really only covers the SNP cases, but here we just "make it work" for complex
    // cases as well.
    if (loh <= 0) {
      mOneMinusEll++;
      if (normalPloidy == Ploidy.HAPLOID) {
        if (normal == cancer) {
          mOneMinusMu++;
        } else {
          mMu++;
        }
      } else {
        if (normal == cancer) {
          mOneMinusMu += 2; // efficiency, this case would also be handled below
        } else {
          final int na = code.a(normal);
          final int nb = code.bc(normal);
          final int ca = code.a(cancer);
          final int cb = code.bc(cancer);
          final int m = ((na == ca || na == cb) ? 1 : 0) + ((nb == ca || nb == cb) ? 1 : 0);
          mOneMinusMu += m;
          mMu += 2 - m;
        }
      }
    } else {
      assert code.homozygous(cancer);
      mEll++;
      if (code.a(normal) == cancer || code.bc(normal) == cancer) {
        mOneMinusMu++;
      } else {
        mMu++;
      }
    }
  }

  @Override
  public <D extends Description, T extends HypothesesPrior<D>> Variant makeCall(String templateName, int position, int endPosition, byte[] ref, List<ModelInterface<?>> models, HaploidDiploidHypotheses<T> normalHypotheses) {
    assert DNARange.valid(ref[position], DNARange.N, DNARange.T);
    assert models.size() == 2;

    final ModelInterface<?> modelNormal = models.get(NORMAL);
    final ModelInterface<?> modelCancer = models.get(CANCER);
    final HypothesesPrior<?> hypotheses = normalHypotheses.get(modelNormal);
    final Code code = hypotheses.code();

    // Boring keeps track of any call which is not-interesting to the cancer mode.
    // Such calls must never have isInteresting set to true when they are returned
    // so that later complex calling does not muck with them.

    boolean boring = Utils.hasOnlyRefCoverage(models);
    if (mParams.callLevel() != VariantOutputLevel.ALL && boring) {
      // Short-circuit case where all evidence matches the reference.
      updateParameterEstimationCounts(null, code, 0, 0, 0);
      return null;
    }

    final AbstractSomaticPosterior posterior = makePosterior(modelNormal, modelCancer, hypotheses, getSomaticPrior(templateName, position, endPosition));
    final boolean sameCall = posterior.isSameCall();
    final boolean isSomatic;
    final int bestNormal = posterior.bestNormal();
    final int bestCancer = posterior.bestCancer();
    // Simple LOH test based on ploidy of results alone, could be done with Bayesian calculation later
    final double loh = loh(hypotheses, bestNormal, bestCancer);
    final Ploidy normalPloidy = hypotheses.haploid() ? Ploidy.HAPLOID : Ploidy.DIPLOID;
    final boolean doLoh = mParams.lohPrior() > 0;
    final String refAllele = DnaUtils.bytesToSequenceIncCG(ref, position, endPosition - position);
    final double ratio = posterior.posteriorScore();
    if (sameCall || bestNormal == bestCancer) {
      // Call is same for both samples.  It still could be a germline call.
      if (hypotheses.reference() == bestNormal && ratio >= mIdentityInterestingThreshold) {
        if (mParams.callLevel() != VariantOutputLevel.ALL) {
          // Call was same for both samples and equal to the reference, this is really boring
          // only retain it if ALL mode is active
          return null;
        }
        boring = true;
      }
      isSomatic = false;
    } else if (!doLoh && loh > 0) {
      // LOH event, even though the LOH prior was 0, force it to have no cause
      isSomatic = false;
    } else if (!mParams.includeGainOfReference() && refAllele.equals(hypotheses.name(bestCancer))) {
      // Gain of reference, if such calls are not allowed then this should have no cause
      isSomatic = false;
    } else {
      isSomatic = true;
    }

    final VariantSample normalSample = setCallValues(posterior.normalMeasure(), bestNormal, hypotheses, modelNormal, mParams, normalPloidy, VariantSample.DeNovoStatus.UNSPECIFIED, null);
    final VariantSample cancerSample;
    if (isSomatic) {
      cancerSample = setCallValues(posterior.cancerMeasure(), bestCancer, hypotheses, modelCancer, mParams, normalPloidy, VariantSample.DeNovoStatus.IS_DE_NOVO, ratio);
    } else {
      cancerSample = setCallValues(posterior.cancerMeasure(), bestCancer, hypotheses, modelCancer, mParams, normalPloidy, VariantSample.DeNovoStatus.NOT_DE_NOVO, null);
    }
    final VariantLocus locus = new VariantLocus(templateName, position, endPosition, refAllele, VariantUtils.getPreviousRefNt(ref, position));
    final Variant v = new Variant(locus, normalSample, cancerSample);
    if (modelNormal.statistics().overCoverage(mParams, templateName) || modelCancer.statistics().overCoverage(mParams, templateName)) {
      v.addFilter(Variant.VariantFilter.COVERAGE);
      boring = true;
    } else if (modelNormal.statistics().ambiguous(mParams) || modelCancer.statistics().ambiguous(mParams)) {
      v.addFilter(Variant.VariantFilter.AMBIGUITY);
    }
    if (!boring) {
      v.setInteresting();
    }
    if (doLoh) {
      v.setLoh(loh);
    }
    if (isSomatic) {
      v.setNormalCancerScore(posterior.ncScore());
    }
    updateParameterEstimationCounts(normalPloidy, code, bestNormal, bestCancer, loh);
    return v;
  }

  @Override
  public void toString(StringBuilder sb) {
    // Note this dump of Q does not deal with any somatic site-specific priors that might be active
    final double[][] qMatrix = (mQHaploidFactory != null ? mQHaploidFactory : mQDiploidFactory).somaticQ(mParams.somaticRate());
    sb.append("length=").append(qMatrix.length).append(LS);
    for (final double[] q : qMatrix) {
      for (final double v : q) {
        sb.append(com.rtg.util.Utils.realFormat(v, 3)).append(" ");
      }
      sb.append(LS);
    }
  }

  @Override
  public void close() throws IOException {
  }

  @Override
  public void endOfSequence() {
    Diagnostic.developerLog("count(l)=" + mEll + " count(1-l)=" + mOneMinusEll);
    Diagnostic.developerLog("count(mu)=" + mMu + " count(1-mu)=" + mOneMinusMu);
    Diagnostic.userLog("hat l=" + ((mEll + 1) / (mEll + mOneMinusEll + 2)));
    Diagnostic.userLog("hat mu=" + ((mMu + 1) / (mMu + mOneMinusMu + 2)));
  }

  @Override
  public boolean integrity() {
    Exam.assertTrue(mQHaploidFactory != null || mQDiploidFactory != null);
    if (mQHaploidFactory != null) {
      checkQ(mQHaploidFactory.somaticQ(mParams.somaticRate()));
    }
    if (mQDiploidFactory != null) {
      checkQ(mQDiploidFactory.somaticQ(mParams.somaticRate()));
    }
    return true;
  }

  private void checkQ(double[][] qa) {
    final int length = qa.length;
    for (final double[] aQa : qa) {
      Exam.assertEquals(length, aQa.length);
      double sum = 0.0;
      for (final double q : aQa) {
        sum += q;
        Exam.assertTrue(q >= 0.0 && q <= 1.0 && !Double.isNaN(q));
      }
      Exam.assertEquals(1.0, sum, 0.0000001);
    }
  }

}
