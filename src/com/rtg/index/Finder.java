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
package com.rtg.index;

import java.io.IOException;

import com.rtg.util.integrity.IntegralAbstract;

/**
 * Used to get results back from a <code>HashIndex</code>
 */
public abstract class Finder extends IntegralAbstract {

  /**
   * Called once for each entry found.
   * @param id value found.
   * @throws IOException If an I/O error occurs
   */
  public abstract void found(long id) throws IOException;

  @Override
  public void toString(final StringBuilder sb) {
    sb.append("Finder");
  }

  @Override
  public boolean integrity() {
    return true;
  }

}
