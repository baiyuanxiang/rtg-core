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
package com.rtg.metagenomics.metasnp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.rtg.variant.util.arithmetic.PossibilityArithmetic;

/**
 */
public final class AlphaSelector {

  /** Highest allele value */
  static final int MAX_VALUE = 3;

  private AlphaSelector() { }

  static void updateThetaMask(int[] masks, int strain, int base) {
    for (int i = 0; i < masks.length; i++) {
      masks[i] &= ~(1 << strain);
    }
    masks[base] |= 1 << strain;
  }

  /**
   * Faster implementation of alpha position using a precomputed theta table and match patterns
   *
   * @param referenceAllele base in the reference at this position
   * @param probSpaceBeta probability of a variant in each strain (in prob space)
   * @param reads evidence for this position as count of reads with each base. first index is sample, second index is allele
   * @param thetaLookup precomputed theta table
   * @param arith arithmetic object
   * @param nStrains number of strains
   * @return assignments and a score.
   */
  static AlphaScore alphaPosition(int referenceAllele, double[] probSpaceBeta, int[][] reads, double[][] thetaLookup, PossibilityArithmetic arith, int nStrains) {
    return alphaPosition(referenceAllele, new ProbAlphaSimpleBeta(probSpaceBeta), reads, thetaLookup, arith, nStrains);
  }
  static AlphaScore alphaPosition(int referenceAllele, ProbAlpha pAlpha, int[][] reads, double[][] thetaLookup, PossibilityArithmetic arith, int nStrains) {
    final int[] strainVariants = new int[nStrains];
    int stackPos = 0;
    double bestScore = arith.zero();
    double bestEvidenceScore = arith.zero();
    double restScore = arith.zero();
    List<Integer> best = new ArrayList<>();
    for (int i = 0; i < nStrains ; i++) {
      best.add(0);
    }
    final int[] thetaMask = new int[MAX_VALUE + 1];
    int last = -1;
    // Loop over all possible alpha_x assigments (assigments of alleles to strains)
    while (stackPos > 0 || last != MAX_VALUE) {
      strainVariants[stackPos++] = last + 1;
      updateThetaMask(thetaMask, stackPos - 1, strainVariants[stackPos - 1]);

      while (stackPos < nStrains) {
        strainVariants[stackPos++] = 0;
        updateThetaMask(thetaMask, stackPos - 1, 0);
      }

      double evidenceScore = arith.one();
      for (int sampleIndex = 0; sampleIndex < reads.length; sampleIndex++) {
        final int[] sample = reads[sampleIndex];
        for (int i = 0; i < sample.length; i++) {
          evidenceScore = arith.multiply(evidenceScore, arith.pow(thetaLookup[sampleIndex][thetaMask[i]], sample[i]));
        }
      }
      final double alphaScore = arith.prob2Poss(pAlpha.pAlpha(referenceAllele, strainVariants));
      final double currentScore = arith.multiply(alphaScore, evidenceScore);
//      System.err.println("score=" + currentScore + " best= " + bestScore + " strainVariants=" + strainVariants);
      if (currentScore > bestScore) {
        restScore = arith.add(restScore, bestScore);
        bestScore = currentScore;
        bestEvidenceScore = evidenceScore;
        best = new ArrayList<>();
        for (int base : strainVariants) {
          best.add(base);
        }
      } else {
        restScore = arith.add(restScore, currentScore);
      }

      while (stackPos > 0 && (last = strainVariants[--stackPos]) == MAX_VALUE) {
        // nop
      }
    }
    final int[] res = new int[best.size()];
    for (int i = 0; i < res.length; i++) {
      res[i] = best.get(i);
    }
    return new AlphaScore(arith.divide(bestScore, restScore), bestEvidenceScore, res);
  }

  /**
   * Compute an array where the first index is sample and the second index is a bit mask for the matching strains and the value is the corresponding theta
   * @param xi current xi estimate
   * @param arith arithmetic used for scoring
   * @param notError probability of no error in poss space
   * @param thirdError one third the probability of error in poss space
   * @return an theta lookup table per sample
   */
  static double[][] computeThetaLookup(double[][] xi, PossibilityArithmetic arith, double notError, double thirdError) {
    final int combinations = 1 << xi[0].length;
    final double[][] thetaLookup = new double[xi.length][combinations];
    for (int sample = 0; sample < xi.length; sample++) {
      final double[] x = xi[sample];
      final double[] theta = thetaLookup[sample];
      Arrays.fill(theta, arith.zero());
      for (int comb = 0; comb < combinations; comb++) {
        double oneMinus = arith.zero();
        for (int strain = 0; strain < x.length; strain++) {
          if ((comb & (1 << strain)) != 0) {
            theta[comb] = arith.add(theta[comb], x[strain]);
          } else {
            oneMinus = arith.add(oneMinus, x[strain]);
          }
        }
        theta[comb] = arith.add(arith.multiply(notError, theta[comb]), arith.multiply(thirdError, oneMinus));
      }
    }

    return thetaLookup;
  }
}
