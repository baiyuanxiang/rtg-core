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
package com.rtg.usage;

import java.util.UUID;

/**
 * Interface for usage tracking clients used by {@link UsageTracking}
 */
public interface UsageTrackingClient {

  /**
   * records a usage tracking message indicating the end of a run
   * @param metric module specific usage metric
   * @param module name of module being run
   * @param runId unique id for run
   * @param success true if run succeeded
   */
  void recordEnd(long metric, String module, UUID runId, boolean success);

  /**
   * records a usage tracking message indicating the start of a run
   * @param module name of module being run
   * @param runId unique id for run
   */
  void recordBeginning(String module, UUID runId);
}
