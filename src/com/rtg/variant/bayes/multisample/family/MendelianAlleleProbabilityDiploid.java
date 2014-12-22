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

import com.rtg.util.integrity.Exam;
import com.rtg.variant.bayes.Code;

/**
 * Calculates the probability of a particular child allele combination given parents
 */
public final class MendelianAlleleProbabilityDiploid extends MendelianAlleleProbabilityNormal {
  /** Utility class private constructor */
  private MendelianAlleleProbabilityDiploid() { }

  /**
   * lookup table for Mendelian inheritance probability
   * indexes are father, mother, mother, child, child
   */
  static final double[][][][][] LOOKUP = new double[2][][][][];
  static {
    LOOKUP[0] = new double[2][][][];
    LOOKUP[0][0] = new double[2][][];
    LOOKUP[0][0][0] = new double[1][1];
    LOOKUP[0][0][1] = new double[2][2];
    LOOKUP[0][1] = new double[3][][];
    LOOKUP[0][1][0] = new double[2][2];
    LOOKUP[0][1][1] = new double[2][2];
    LOOKUP[0][1][2] = new double[3][3];

    LOOKUP[1] = new double[3][][][];
    LOOKUP[1][0] = new double[3][][];
    LOOKUP[1][0][0] = new double[2][2];
    LOOKUP[1][0][1] = new double[2][2];
    LOOKUP[1][0][2] = new double[3][3];
    LOOKUP[1][1] = new double[3][][];
    LOOKUP[1][1][0] = new double[2][2];
    LOOKUP[1][1][1] = new double[2][2];
    LOOKUP[1][1][2] = new double[3][3];
    LOOKUP[1][2] = new double[4][][];
    LOOKUP[1][2][0] = new double[3][3];
    LOOKUP[1][2][1] = new double[3][3];
    LOOKUP[1][2][2] = new double[3][3];
    LOOKUP[1][2][3] = new double[4][4];

    Exam.assertEquals(99, init(LOOKUP));

    setLookup(0, 0, 0, 0, 0, 1.0);

    setLookup(1, 0, 0, 0, 0, 0.5);
    setLookup(1, 0, 0, 0, 1, 0.5);

    setLookup(0, 0, 1, 0, 0, 0.5);
    setLookup(0, 0, 1, 0, 1, 0.5);

    setLookup(1, 0, 1, 0, 0, 0.25);
    setLookup(1, 0, 1, 0, 1, 0.5);
    setLookup(1, 0, 1, 1, 1, 0.25);

    setLookup(0, 1, 1, 0, 1, 1.0);

    setLookup(1, 1, 1, 0, 1, 0.5);
    setLookup(1, 1, 1, 1, 1, 0.5);

    setLookup(1, 0, 2, 0, 0, 0.25);
    setLookup(1, 0, 2, 0, 2, 0.25);
    setLookup(1, 0, 2, 1, 0, 0.25);
    setLookup(1, 0, 2, 1, 2, 0.25);

    setLookup(1, 1, 2, 0, 1, 0.25);
    setLookup(1, 1, 2, 0, 2, 0.25);
    setLookup(1, 1, 2, 1, 1, 0.25);
    setLookup(1, 1, 2, 1, 2, 0.25);

    setLookupA(0, 1, 2, 0, 1, 0.5);
    setLookupA(0, 1, 2, 0, 2, 0.5);

    setLookup(1, 2, 2, 0, 2, 0.5);
    setLookup(1, 2, 2, 1, 2, 0.5);

    setLookupA(1, 2, 3, 0, 2, 0.25);
    setLookupA(1, 2, 3, 0, 3, 0.25);
    setLookupA(1, 2, 3, 1, 2, 0.25);
    setLookupA(1, 2, 3, 1, 3, 0.25);
  }

  private static int init(double[][][][][] lookup) {
    final double z = Double.NEGATIVE_INFINITY;
    int cnt = 0;
    for (int i = 0; i < lookup.length; i++) {
      for (int j = 0; j < lookup[i].length; j++) {
        for (int k = 0; k < lookup[i][j].length; k++) {
          for (int l = 0; l < lookup[i][j][k].length; l++) {
            for (int m = 0; m < lookup[i][j][k][l].length; m++) {
              cnt++;
              lookup[i][j][k][l][m] = z;
            }
          }
        }
      }
    }
    return cnt;
  }

  private static void setLookup(int fab, int moa, int mob, int cha, int chb, double val) {
    final double log = Math.log(val);
    LOOKUP[fab][moa][mob][cha][chb] = log;
    LOOKUP[fab][moa][mob][chb][cha] = log;
    LOOKUP[fab][mob][moa][cha][chb] = log;
    LOOKUP[fab][mob][moa][chb][cha] = log;
  }

  private static void setLookupA(int fab, int moa, int mob, int cha, int chb, double val) {
    final double log = Math.log(val);
    LOOKUP[fab][moa][mob][cha][chb] = log;
    LOOKUP[fab][moa][mob][chb][cha] = log;
  }

  double getLookup(int fab, int moa, int mob, int cha, int chb) {
    final double[][] parents = LOOKUP[fab][moa][mob];
    if ((parents.length > cha) && (parents[cha].length > chb)) {
      return parents[cha][chb];
    } else {
      return Double.NEGATIVE_INFINITY;
    }
  }

  /** Single instance for diploid-diploid case. */
  public static final MendelianAlleleProbability SINGLETON = new MendelianAlleleProbabilityDiploid();

  @Override
  public double probabilityLn(Code code, int father, int mother, int child) {
    final UniqueId uid = new UniqueId(code.rangeSize());
    final int fa0 = uid.addId(code.a(father));
    assert fa0 == 0;
    final int fa1 = uid.addId(code.bc(father));
    final int mo0 = uid.addId(code.a(mother));
    final int mo1 = uid.addId(code.bc(mother));
    final int ch0 = uid.id(code.a(child));
    if (ch0 < 0) {
      return Double.NEGATIVE_INFINITY;
    }
    final int ch1 = uid.id(code.bc(child));
    if (ch1 < 0) {
      return Double.NEGATIVE_INFINITY;
    }

    return LOOKUP[fa1][mo0][mo1][ch0][ch1];
  }

}
