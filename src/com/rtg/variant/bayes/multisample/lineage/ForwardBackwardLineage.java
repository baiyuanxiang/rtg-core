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
package com.rtg.variant.bayes.multisample.lineage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.rtg.util.Pair;
import com.rtg.variant.bayes.Hypotheses;
import com.rtg.variant.bayes.ModelInterface;
import com.rtg.variant.util.arithmetic.PossibilityArithmetic;

/**
 * Forward backward variable elimination for cell lineages.
 */
public class ForwardBackwardLineage {

  // Althouth there is technically a different de novo variable N_i for each sample
  // they are never used in conjunction, hence we can get away with same variable
  // name to represent it in all samples.
  /** The name of the de novo variable in the Bayesian network. */
  static final Variable DE_NOVO = new Variable("N", 2);

  private final Lineage mLineage;
  private final PossibilityArithmetic mArith;
  private final List<Factor> mModel = new ArrayList<>();
  private final List<Hypotheses<?>> mHypotheses = new ArrayList<>();
  private final Factor[] mRootGenotypePrior;

  /**
   * Construct a new forward backward variable elimination instance for a cell lineage over
   * the given models.
   * @param arith arithmetic to use
   * @param lineage lineage information
   * @param rootGenotypePrior <code>P(G|C)</code> for the root node
   * @param models singleton models for samples in order of the sample numbers in the lineage
   */
  ForwardBackwardLineage(final PossibilityArithmetic arith, final Lineage lineage, Factor[] rootGenotypePrior, List<ModelInterface<?>> models) {
    mLineage = lineage;
    mArith = arith;
    mRootGenotypePrior = rootGenotypePrior;
    int k = 0;
    for (final ModelInterface<?> m : models) {
      mModel.add(new ModelFactor(new Variable("G" + k, m.hypotheses().size()), m));
      mHypotheses.add(m.hypotheses());
      k++;
    }
  }

  private Set<Variable> makeScope(final int node) {
      // todo is there a better way to do this
    final HashSet<Variable> v = new HashSet<>();
    v.add(getGenotypeVariable(node));
    final Variable coverageVariable = mLineage.getCoverageVariable(node);
    if (coverageVariable != null) {
      v.add(coverageVariable);
    }
    return v;
  }

  private Factor psi2(final int node) {
    assert mLineage.isRoot(node);
    final Factor modelFactor = mModel.get(node); // P(snode|Gnode)
    // todo coverage
    return modelFactor.multiply(mRootGenotypePrior[node]);
  }

  private Variable getGenotypeVariable(final int node) {
    return new Variable("G" + node, mHypotheses.get(node).size());
  }

  // P(G|G',N,C)P(N)
  private Factor mendelian(final int child, final int parent) {
    if (mLineage.getCoverageVariable(child) != null) {
      throw new UnsupportedOperationException("Too hard for now");
    }
    return new MendelianLineageFactor(mArith, getGenotypeVariable(child), getGenotypeVariable(parent), DE_NOVO, mLineage.deNovoPrior(child), mHypotheses.get(child), mHypotheses.get(parent));
  }

  private final HashMap<Integer, Factor> mPsi5Cache = new HashMap<>();
  private Factor computePsi5(int node) {
    assert !mLineage.isRoot(node);
    final Factor modelFactor = mModel.get(node); // P(snode|Gnode)
    // TODO Coverage
    // TODO cache?
    final Factor mendelian = mendelian(node, mLineage.parent(node));
    return modelFactor.multiply(mendelian);
  }
  private Factor psi5(int node) {
    final Factor psi = mPsi5Cache.get(node);
    if (psi != null) {
      return psi;
    }
    final Factor f = computePsi5(node);
    mPsi5Cache.put(node, f);
    return f;
  }

  private Factor psi4(int node) {
    assert !mLineage.isRoot(node);
    // cache?
    return psi5(node).sumOut(DE_NOVO);
  }

  private final HashMap<Integer, Factor> mPhi1Cache = new HashMap<>();

  private Factor computePhi1(final int node) {
    Factor product = DefaultFactor.unit(mArith, makeScope(node));
    // note if there are no children this returns a unit factor "1"
    for (final int child : mLineage.children(node)) {
      product = product.multiply(phi2(node, child));
    }
    return product;
  }

  private Factor phi1(int node) {
    final Factor phi = mPhi1Cache.get(node);
    if (phi != null) {
      return phi;
    }
    final Factor f = computePhi1(node);
    mPhi1Cache.put(node, f);
    return f;
  }

  private boolean isIndividual(Variable v, String node) {
    // Assumes variables have one letter "type" followed by individual number
    return v.toString().substring(1).equals(node);
  }

  private Factor sumOutIndividual(Factor f, int node) {
    final String n = String.valueOf(node);
    final Set<Variable> s = new HashSet<>();
    for (final Variable v : f.scope()) {
      if (!isIndividual(v, n)) {
        s.add(v);
      }
    }
    return f.marginal(s);
  }

  private final HashMap<Pair<Integer, Integer>, Factor> mPhi2Cache = new HashMap<>();

  private Factor computePhi2(int child) {
    final Factor phiChild = phi1(child);
    final Factor psiChild = psi4(child);
    final Factor product = phiChild.multiply(psiChild);
    return sumOutIndividual(product, child);
  }

  private Factor phi2(int node, int child) {
    final Pair<Integer, Integer> key = new Pair<>(node, child);
    final Factor phi = mPhi2Cache.get(key);
    if (phi != null) {
      return phi;
    }
    final Factor f = computePhi2(child);
    mPhi2Cache.put(key, f);
    return f;
  }

  private final HashMap<Integer, Factor> mTau2Cache = new HashMap<>();

  private Factor computeTau2(int node) {
    if (mLineage.isRoot(node)) {
      return psi2(node);
    } else {
      final Factor factor = tau3(node);
      return factor.sumOut(DE_NOVO);
    }
  }

  private Factor tau2(int node) {
    final Factor tau = mTau2Cache.get(node);
    if (tau != null) {
      return tau;
    }
    final Factor f = computeTau2(node);
    mTau2Cache.put(node, f);
    return f;
  }

  private final HashMap<Integer, Factor> mTau3Cache = new HashMap<>();

  private Factor computeTau3(int node) {
    if (mLineage.isRoot(node)) {
      throw new IllegalArgumentException("There is no denovo variable for a root");
    }
    final int parent = mLineage.parent(node);
    final Factor tauParent = tau2(parent);
    final Factor psi5 = psi5(node);
    Factor product = tauParent.multiply(psi5);
    for (final int sib : mLineage.children(parent)) {
      if (sib != node) {
        product = product.multiply(phi2(parent, sib));
      }
    }
    return sumOutIndividual(product, parent);
  }

  private Factor tau3(int node) {
    final Factor tau = mTau3Cache.get(node);
    if (tau != null) {
      return tau;
    }
    final Factor f = computeTau3(node);
    mTau3Cache.put(node, f);
    return f;
  }

  Factor posterior(final int node) {
    //System.out.println("node=" + node + " phi1=" + phi1(node) + " tau2=" + tau2(node));
    return phi1(node).multiply(tau2(node));
  }

  Factor posteriorDeNovo(final int node) {
    return phi1(node).multiply(tau3(node));
  }
}
