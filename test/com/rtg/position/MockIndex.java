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
package com.rtg.position;

import java.io.IOException;
import java.io.PrintStream;
import java.io.StringWriter;

import com.rtg.index.Finder;
import com.rtg.index.FinderHashValue;
import com.rtg.index.Index;
import com.rtg.index.SparseFrequencyHistogram;
import com.rtg.util.StringUtils;
import com.rtg.util.integrity.IntegralAbstract;

/**
 * Mock implementation of index, used for testing.
 */
public class MockIndex extends IntegralAbstract implements Index {

  private final Appendable mOut = new StringWriter();
  private int mTimesFrozen = 0;
  private long mInitialHashes;
  private final int[] mCounts;
  private int mI = 0;

  /**
   * Construct a new mock index with counts.
   * @param counts the counts
   */
  public MockIndex(final int[] counts) {
    mCounts = counts;
  }

  /** Construct a new mock index. */
  public MockIndex() {
    this(new int[0]);
  }

  @Override
  public void add(final long hash, final long id) {
    try {
      ++mInitialHashes;
      mOut.append(String.valueOf(id)).append(StringUtils.LS);
    } catch (final IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public long getInitialHashes() {
    return mInitialHashes;
  }

  @Override
  public SparseFrequencyHistogram getSparseFrequencyHistogram() {
    throw new UnsupportedOperationException("Not implemented yet");
  }

  @Override
  public long bytes() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void freeze() {
    ++mTimesFrozen;
  }

  public int getTimesFrozen() {
    return mTimesFrozen;
  }

  @Override
  public long getHash(final long found) {
    throw new UnsupportedOperationException();
  }

  @Override
  public long getValue(long found) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public String infoString() {
    return "";
  }

  @Override
  public long numberEntries() {
    throw new UnsupportedOperationException();
  }

  @Override
  public long numberHashes() {
    throw new UnsupportedOperationException();
  }

  @Override
  public String perfString() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void search(final long hash, final Finder finder) {
    //do nothing
  }

  @Override
  public void scan(FinderHashValue finder) throws IOException, IllegalStateException {
    //do nothing
  }

  @Override
  public boolean contains(final long hash) {
    throw new UnsupportedOperationException();
  }

  @Override
  public int count(final long hash) {
    final int res = mCounts[mI % mCounts.length];
    ++mI;
    return res;
  }

  @Override
  public long first(long hash) throws IllegalStateException {
    return 0;
  }

  @Override
  public void toString(final StringBuilder sb) {
    sb.append(mOut);
  }

  @Override
  public boolean globalIntegrity() {
    return true;
  }

  @Override
  public boolean integrity() {
    return true;
  }
  @Override
  public void dumpValues(final PrintStream out) {
  }

  @Override
  public int maxHashCount() {
    return 10;
  }


}
