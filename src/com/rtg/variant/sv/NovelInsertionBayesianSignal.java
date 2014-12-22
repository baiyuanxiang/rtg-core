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
package com.rtg.variant.sv;

import com.reeltwo.jumble.annotations.TestClass;

/**
 * Produces a bayesian signal for a breakpoint representing the site of a novel insertion.
 *
 */
@TestClass(value = {"com.rtg.variant.sv.SvToolTaskTest"})
public class NovelInsertionBayesianSignal extends BayesianSignal {

  private static Distribution d1(int lo, int hi, ReadGroupStats stats, boolean reverse) {
    final double offset = offsetLeft(stats, stats.fragmentMean() - stats.alignmentStartIgnored(), reverse);
    return DistributionUtils.distribution(lo, hi, stats.properRate(), offset, stats.fragmentStdDev(), reverse);
  }
  private static Distribution d2(int lo, int hi, ReadGroupStats stats, boolean reverse) {
    final double offSet = offsetLeft(stats, stats.fragmentMean() - stats.alignmentStartIgnored(), reverse);
    return DistributionUtils.distribution(lo, hi, stats.properRate(), offSet, stats.fragmentStdDev(), !reverse);
  }

  NovelInsertionBayesianSignal(boolean debug) {
    super(debug);
  }

  NovelInsertionBayesianSignal() {
    super();
  }

  @Override
  Distribution leftArmProper(ReadGroupStats stats, boolean reverse) {
    final int lo = stats.lo();
    final int hi = stats.hi();
    final Distribution d1 = d1(lo, hi, stats, reverse);
    final double offset = offsetLeft(stats, stats.alignmentStartIgnored(), reverse);
    final Distribution s = new DistributionStep(lo, hi, (int) offset, 0.0, stats.properRate(), reverse);
    final Distribution m = DistributionUtils.add(d1, s);
    return DistributionUtils.add(m, stats.properRandomRate());
  }
  @Override
  Distribution leftArmDiscordant(ReadGroupStats stats, boolean reverse) {
    final int lo = stats.lo();
    final int hi = stats.hi();
    return new DistributionConstant(lo, hi, stats.discordantRate());
  }
  @Override
  Distribution leftArmUnmated(ReadGroupStats stats, boolean reverse) {
    final int lo = stats.lo();
    final int hi = stats.hi();
    final Distribution d2 = d2(lo, hi, stats, reverse);
    final double offset = offsetLeft(stats, stats.alignmentStartIgnored(), reverse);
    final Distribution s = new DistributionStep(lo, hi, (int) offset, 1.0, 0.0, reverse);
    final Distribution m = DistributionUtils.multiply(d2, s);
    return DistributionUtils.add(m, stats.unmatedRate());
  }
  @Override
  Distribution rightArmProper(ReadGroupStats stats, boolean reverse) {
    return leftArmProper(stats, !reverse);
  }
  @Override
  Distribution rightArmDiscordant(ReadGroupStats stats, boolean reverse) {
    final int lo = stats.lo();
    final int hi = stats.hi();
    return new DistributionConstant(lo, hi, stats.discordantRate());
  }
  @Override
  Distribution rightArmUnmated(ReadGroupStats stats, boolean reverse) {
    return leftArmUnmated(stats, !reverse);
  }
}

