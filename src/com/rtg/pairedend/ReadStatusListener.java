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
package com.rtg.pairedend;

/**
 * Reports when mated pairs, unmated reads or  are written to results
 * Keeps also statistics about number of unmated and unmapped reads written to output
 * Further number of blocked mappings by scoring
 */
public interface ReadStatusListener {


  /**
   * adds a status flag to the reads status.
   * The caller must pass the correct flag for the desired side (first/second).
   * @param readId the read id (= pair id)
   * @param status status flag to be added
   */
  void addStatus(int readId, int status);
}


