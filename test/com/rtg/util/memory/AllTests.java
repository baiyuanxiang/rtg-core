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
package com.rtg.util.memory;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Test class for all tests in this directory. Run from the command
 * line with:<p>
 *
 * java -ea com.reeltwo.util.AllTests
 *
 */
public class AllTests extends TestSuite {

  public static Test suite() {
    TestSuite suite = new TestSuite("com.rtg.util.memory");

    suite.addTest(MemoryUsageTest.suite());
    suite.addTest(ClassMemoryTest.suite());
    suite.addTest(IdentitySetTest.suite());
    suite.addTest(ObjectWalkerTest.suite());
    suite.addTestSuite(ExcludeClassFilterTest.class);

    return suite;
  }


  public static void main(final String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
