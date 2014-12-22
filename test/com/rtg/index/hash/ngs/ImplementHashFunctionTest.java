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
package com.rtg.index.hash.ngs;

import java.io.IOException;

import com.rtg.index.hash.ngs.instances.AbstractSplitTest;
import com.rtg.index.hash.ngs.instances.SplitL36w18s2e1Test;
import com.rtg.launcher.HashingRegion;
import com.rtg.util.Utils;
import com.rtg.util.integrity.IntegralAbstract;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 */
public class ImplementHashFunctionTest extends TestCase {

  public static Test suite() {
    final TestSuite suite = new TestSuite();

    suite.addTestSuite(SplitL36w18s2e1Test.class);
    suite.addTestSuite(ImplementHashFunctionTest.class);
    suite.addTestSuite(ImplementHashFunctionCloneTest.class);
    suite.addTestSuite(TemplateCallImplementationTest.class);

    return suite;
  }

  private static class ReadCallMock implements ReadCall {
    @Override
    public void readCall(final int id, final long hash, final int index) {
    }
  }

  protected static class TemplateCallMock extends IntegralAbstract implements TemplateCall, Cloneable {
    @Override
    public void done() {
    }
    @Override
    public void endSequence() {
    }
    @Override
    public void set(final long name, final int length) {
    }

    @Override
    public void setReverse(final boolean reverse) {
      // do nothing
    }

    @Override
    public boolean isReverse() {
      return false;
    }
    @Override
    public void setHashFunction(final NgsHashFunction hashFunction) {
    }
    @Override
    public void templateCall(final int endPosition, final long hash, final int index) {
    }
    /**
     */
    @Override
    public TemplateCallMock clone() throws CloneNotSupportedException {
      return (TemplateCallMock) super.clone();
    }
    @Override
    public void toString(final StringBuilder sb) {
    }
    @Override
    public boolean integrity() {
      return true;
    }

    @Override
    public TemplateCall threadClone(final HashingRegion region) {
      try {
      return clone();
      } catch (final CloneNotSupportedException e) {
        throw new RuntimeException(e);
      }
    }

  @Override
  public void threadFinish() {
  }

    @Override
    public void logStatistics() {
      // do nothing
    }
  }

  protected ImplementHashFunction getHashFunction() throws IOException {
    final ImplementHashFunction hf = new ImplementHashFunction(36, 18, new ReadCallMock(), new TemplateCallMock()) {
      @Override
      public int numberWindows() {
        return 0;
      }
      @Override
      public void readAll(final int readId, final long v0, final long v1) {
      }
      @Override
      public void templateAll(final int endPosition, final long v0, final long v1) {
      }
    };
    hf.integrity();
    assertEquals(36, hf.readLength());
    return hf;
  }

  public void test() throws IOException {
    final ImplementHashFunction hf = getHashFunction();
    hf.integrity(0, 0, 0);
    hf.hashStep((byte) 0);
    hf.integrity(0, 0, 1);
    hf.hashStep((byte) 1);
    hf.integrity(0, 1, 2);
    hf.hashStep((byte) 2);
    hf.integrity(1, 2, 3);
    hf.hashStep((byte) 3);
    hf.integrity(3, 5, 4);
    assertEquals("ImplementHashFunction read length=36 window size=18", hf.toString());
    assertEquals("00000000:00000000:00000000:00000000:00000000:00000000:00000000:00000011", Utils.toBitsSep(hf.mValuesF0));
    assertEquals("00000000:00000000:00000000:00000000:00000000:00000000:00000000:00000101", Utils.toBitsSep(hf.mValuesF1));
    hf.reset();
    hf.integrity(0, 0, 0);
  }

  public void testHash1() throws IOException {
    final ImplementHashFunction hf = getHashFunction();
    hf.integrity();
    final long h25 = hf.hash(25);
    assertTrue(h25 < (1L << 36));
    assertTrue(h25 > 0);
    assertTrue(h25 != hf.hash(143));
    final long h1 = hf.hash(1);
    assertTrue(h1 < (1L << 36));
    assertTrue(h1 > (1L << 35));
    assertTrue(h1 > 0);
    assertTrue(h1 != hf.hash(143));
    assertTrue(h1 != h25);
  }

  public void testHash2() {
    final ImplementHashFunction hf = new ImplementHashFunction(5, 3, new ReadCallMock(), new TemplateCallMock()) {
      @Override
      public int numberWindows() {
        return 0;
      }
      @Override
      public void readAll(final int readId, final long v0, final long v1) {
      }
      @Override
      public void templateAll(final int endPosition, final long v0, final long v1) {
      }
    };
    hf.integrity();
    assertEquals(5, hf.readLength());
    hf.integrity();
    final long h25 = hf.hash(25);
    assertTrue(h25 < (1L << 6));
    assertTrue(h25 > 0);
    assertTrue(h25 != hf.hash(143));
    assertEquals(51, h25);
    final long h1 = hf.hash(1);
    assertTrue(h1 < (1L << 6));
    assertTrue(h1 > (1L << 5));
    assertTrue(h1 > 0);
    assertTrue(h1 != hf.hash(143));
    assertTrue(h1 != h25);
    assertEquals(43, h1);
  }

  public void testHash3() {
    final ImplementHashFunction hf = new ImplementHashFunction(32, 32, new ReadCallMock(), new TemplateCallMock()) {
      @Override
      public int numberWindows() {
        return 0;
      }
      @Override
      public void readAll(final int readId, final long v0, final long v1) {
      }
      @Override
      public void templateAll(final int endPosition, final long v0, final long v1) {
      }
    };
    hf.integrity();
    assertEquals(32, hf.readLength());
    final long h25 = hf.hash(25);
    assertEquals(6148914691236517647L, h25);
    final long h1 = hf.hash(1);
    assertEquals(6148914691236517223L, h1);
  }

  public void testSetReadSequencesBad() throws IOException {
    final ImplementHashFunction hf = getHashFunction();
    try {
      hf.setReadSequences(-1);
      fail();
    } catch (final RuntimeException e) {
      assertEquals("numberReads=-1", e.getMessage());
    }
    try {
      hf.setReadSequences(Integer.MAX_VALUE + 2L);
      fail();
    } catch (final RuntimeException e) {
      assertEquals("numberReads=2147483649", e.getMessage());
    }
  }

  public void testBitFlip() {
    assertEquals(-1L, ImplementHashFunction.bitFlip(0L));
    assertEquals(Long.MAX_VALUE, ImplementHashFunction.bitFlip(1L));
    assertEquals(0L, ImplementHashFunction.bitFlip(-1L));
    assertEquals(1L, ImplementHashFunction.bitFlip(Long.MAX_VALUE));
  }

  public void testHashScore() throws IOException {
    final ImplementHashFunction hf = getHashFunction();
    hf.setReadSequences(6);
    AbstractSplitTest.encode(hf, "acgt");
    hf.setValues(0, false);

    assertEquals(0, hf.fastScore(0));
    assertEquals(3, hf.fastScore(1));

    assertEquals(0, hf.indelScore(0));
    assertEquals(3, hf.indelScore(1));

  }

}

