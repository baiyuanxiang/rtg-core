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
package com.rtg.util.io;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;

/**
 * Record what is written to a log file and return it in <code>toString</code>.
 */
public class LogRecord implements LogStream {

  private final PrintStream mStream;

  private final ByteArrayOutputStream mByteStream;

  /**
   * Creates a new record.
   */
  public LogRecord() {
    mByteStream = new ByteArrayOutputStream();
    mStream = new PrintStream(mByteStream);
  }

  @Override
  public void close() {
    // do nothing
  }

  @Override
  public void removeLog() {
    //do nothing
  }

  @Override
  public PrintStream stream() {
    return mStream;
  }

  @Override
  public File file() {
    return null;
  }

  @Override
  public String toString() {
    return mByteStream.toString();
  }
}

