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

package com.rtg.variant.bayes.multisample.forwardbackward;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.rtg.relation.Family;
import com.rtg.relation.GenomeRelationships;
import com.rtg.variant.GenomePriorParams;
import com.rtg.variant.bayes.Description;
import com.rtg.variant.bayes.Factor;
import com.rtg.variant.bayes.ModelInterface;
import com.rtg.variant.bayes.multisample.HaploidDiploidHypotheses;
import com.rtg.variant.bayes.multisample.family.AbstractFamilyPosterior;
import com.rtg.variant.bayes.multisample.family.AbstractFamilyPosteriorTest;
import com.rtg.variant.bayes.multisample.family.MendelianAlleleProbabilityFactory;
import com.rtg.variant.bayes.snp.DescriptionCommon;
import com.rtg.variant.bayes.snp.DescriptionNone;
import com.rtg.variant.bayes.snp.HypothesesNone;
import com.rtg.variant.bayes.snp.HypothesesPrior;
import com.rtg.variant.util.arithmetic.PossibilityArithmetic;
import com.rtg.variant.util.arithmetic.SimplePossibility;

/**
 */
public class FamilyPosteriorFBTest extends AbstractFamilyPosteriorTest {

  private HashMap<String, Double> mExpectedNonIdentity;
  @Override
  public void setUp() throws Exception {
    super.setUp();
    mExpectedNonIdentity = new HashMap<>();
    mExpectedNonIdentity.put("testHaploidNoneHaploid", 0.3848);
    mExpectedNonIdentity.put("testNoneHaploidHaploid", 0.3848);
    mExpectedNonIdentity.put("testHaploidDiploidHaploid", 1.0327);
    mExpectedNonIdentity.put("testHaploidDiploidHaploidAvian", 1.0327);
    mExpectedNonIdentity.put("testHaploidDiploidDiploid", 1.0657);
    mExpectedNonIdentity.put("testHaploidDiploidDiploidAvian", 1.0657);
    mExpectedNonIdentity.put("testDiploidDiploidDiploid", 3.7339);
    mExpectedNonIdentity.put("testDiploidDiploidDiploidSon", 3.7339);
    mExpectedNonIdentity.put("testDiploidDiploidDiploidDiploid", 4.2480);
    mExpectedNonIdentity.put("testDiploidDiploidDiploidDenovo", 3.7330);
    mExpectedNonIdentity.put("testDiploidDiploidDiploidDiploidDiploid", 5.8237);
    mExpectedNonIdentity.put("testHaploidNoneHaploidNoneHaploid", -0.7078);
    mExpectedNonIdentity.put("testHaploidDiploidHaploidDiploidHaploid", 3.1549);
    mExpectedNonIdentity.put("testDiploidDiploidDiploidDenovoSon", 3.4544);
    mExpectedNonIdentity.put("testDiploidDiploidDiploidDenovoSonNoPrior", 2.6278);
    mExpectedNonIdentity.put("testHaploidHaploidHaploid", 0.6877); // <- Not  externally validated
  }

  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
    mExpectedNonIdentity = null;
  }

  @Override
  protected double getExpectedNonIdentity(String method) {
    return mExpectedNonIdentity.get(method);
  }

  @Override
  protected AbstractFamilyPosterior getFamilyPosterior(final GenomePriorParams priors, final HaploidDiploidHypotheses<HypothesesPrior<Description>> hdh, final List<ModelInterface<?>> models, final Family family) {
    final FamilyPosteriorFB ret = new FamilyPosteriorFB(family, priors, models, hdh, MendelianAlleleProbabilityFactory.COMBINED);
    ret.computeAlleles();
    return ret;
  }

  protected FamilyPosteriorFB getFamilyPosteriorRaw(final GenomePriorParams priors, final HaploidDiploidHypotheses<HypothesesPrior<Description>> hdh, final List<ModelInterface<?>> models, final Family family) {
    return new FamilyPosteriorFB(family, priors, models, hdh, MendelianAlleleProbabilityFactory.COMBINED);
  }

  //all autosomes daughter
  public void testDiploidDiploidDiploidHalfSib() {
    final GenomePriorParams priors = getGenomePriorParams();
    final PossibilityArithmetic arith = SimplePossibility.SINGLETON;
    final Description desc = new DescriptionCommon("A", "C");
    final HypothesesPrior<Description> none = new HypothesesNone<>(DescriptionNone.SINGLETON, arith, 0);
    final HypothesesPrior<Description> haploid = new MockHyp(desc, arith, true, new double[] {0.7, 0.3});
    final HypothesesPrior<Description> diploid = new MockHyp(desc, arith, false, new double[] {0.20, 0.70, 0.10});
    final HaploidDiploidHypotheses<HypothesesPrior<Description>> hdh = new HaploidDiploidHypotheses<>(none, haploid, diploid, false, null);
    final List<ModelInterface<?>> models = new ArrayList<>();
    models.add(new MockModel(diploid, new double[] {0.1, 0.2, 0.7}));
    models.add(new MockModel(diploid, new double[] {0.60, 0.25, 0.15}));
    models.add(new MockModel(diploid, new double[] {0.65, 0.15, 0.20}));
    models.add(new MockModel(diploid, new double[] {0.55, 0.20, 0.25}));
    models.add(new MockModel(diploid, new double[] {0.50, 0.10, 0.40}));
    final Family family = makeFamilySex(GenomeRelationships.SEX_FEMALE);
    GenomeRelationships genome = new GenomeRelationships();
    genome.addGenome(FATHER, GenomeRelationships.SEX_MALE);
    genome.addGenome(MOTHER + "2", GenomeRelationships.SEX_FEMALE);
    genome.addGenome("child2", GenomeRelationships.SEX_FEMALE);
    genome.addParentChild(FATHER, "child2");
    genome.addParentChild(MOTHER + "2", "child2");
    final Family family2 = new Family(genome, FATHER, MOTHER + "2", "child2");
    family.setFatherDistinctMates(2);
    family.setMotherDistinctMates(1);
    family2.setFatherDistinctMates(2);
    family2.setFatherFamilyId(1);
    family2.setMotherDistinctMates(1);
    family2.setSampleId(Family.MOTHER_INDEX, 3);
    family2.setSampleId(Family.FIRST_CHILD_INDEX, 4);
    Factor<?>[] as = CommonFormulas.initialA(models, hdh);
    BContainer[] bs = CommonFormulas.initialB(models, new int[]{2, 1, 1, 1, 1});
    final FamilyPosteriorFB fp = getFamilyPosteriorRaw(priors, hdh, models, family);
    final FamilyPosteriorFB fp2 = getFamilyPosteriorRaw(priors, hdh, models, family2);
    Factor<?>[] child1A = fp.computeChildAs(as, bs);
    as[2] = child1A[0];
    Factor<?>[] child2A = fp2.computeChildAs(as, bs);
    as[4] = child2A[0];
    Factor<?>[] parentBs1 = fp.computeParentBs(as, bs);
    bs[0].setB(0, parentBs1[Family.FATHER_INDEX]);
    bs[1].setB(0, parentBs1[Family.MOTHER_INDEX]);
    Factor<?>[] parentBs2 = fp2.computeParentBs(as, bs);
    bs[0].setB(1, parentBs2[Family.FATHER_INDEX]);
    bs[3].setB(0, parentBs2[Family.MOTHER_INDEX]);
    fp.computeMarginals(as, bs);
    fp.findBest(new double[1]);
    fp2.computeMarginals(as, bs);
    fp2.findBest(new double[1]);
    checkBest(fp.bestFather(), 2, -0.3113, 1.2685);
    checkBest(fp.bestMother(), 0, 0.1471, -0.1471);
    checkBest(fp.bestChild(0), 2, -0.3681, 0.9044);
    checkBest(fp2.bestFather(), 2, -0.3113, 1.2685);
    checkBest(fp2.bestMother(), 0, 0.2743, -0.2743);
    checkBest(fp2.bestChild(0), 2, 0.6811, 1.4664);
    assertEquals(1, fp.numberChildren());
    assertEquals(1, fp2.numberChildren());
    assertTrue(fp.isInteresting());
    assertTrue(fp2.isInteresting());
    //assertEquals(getExpectedNonIdentity("testDiploidDiploidDiploid"), fp.getNonIdentityPosterior(), 1e-4);
  }
}
