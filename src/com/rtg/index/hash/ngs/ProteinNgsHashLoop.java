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

import com.rtg.index.hash.ngs.protein.ProteinMask;
import com.rtg.launcher.HashingRegion;
import com.rtg.launcher.ISequenceParams;
import com.rtg.launcher.SequenceParams;
import com.rtg.reader.SequencesReader;
import com.rtg.util.IORunnable;
import com.rtg.util.SimpleThreadPool;
import com.rtg.util.diagnostic.Diagnostic;

/**
 * Protein version of hash loop
 */
public class ProteinNgsHashLoop implements NgsHashLoop {

  private final int mWindowSize;
  private final int mStepSize;
  /** Used to compute the maximum number of sequences before multiple sequences are allocated to each thread. */
  public static final int MAX_SEQUENCES = 10;

  /**
   * Constructs the loop.
   * Window size and step size provided should probably be the read length for correct behaviour (-1 for the missing last frame)
   * Note: step size is only used by the build phase, the search phase always uses 1
   * @param windowSize size of window
   * @param stepSize distance to step per hash call
   */
  public ProteinNgsHashLoop(int windowSize, int stepSize) {
    mWindowSize = windowSize;
    mStepSize = stepSize;
  }

  @Override
  public long readLoop(ISequenceParams params, ReadHashFunction hashFunction, ReadEncoder encoder, boolean reverse) throws IOException {
    final ProteinReadHashLoop hashLoop = new ProteinReadHashLoop(mWindowSize, mStepSize, (ProteinMask) hashFunction);
    return hashLoop.execLoop(params);
  }

  @Override
  public void templateLoop(ISequenceParams params, TemplateHashFunction hashFunction) throws IOException {
    final ProteinTemplateHashLoop hashLoop = new ProteinTemplateHashLoop(mWindowSize, 1, (ProteinMask) hashFunction);
    hashLoop.execLoop(params);
  }

  @Override
  public void templateLoopMultiCore(ISequenceParams params, NgsHashFunction hf, int numberThreads, int threadMultiplier) throws IOException {
    //throw new UnsupportedOperationException("Not supported yet.");
    final HashingRegion region = params.region();
    final long start;
    final long end;
    if (region != HashingRegion.NONE) {
      start = region.getStart();
      end = region.getEnd();
    } else {
      start = 0;
      end = params.reader().numberSequences();
    }
    final SequencesReader reader0 = params.reader();
    final long ntCount = reader0.lengthBetween(start, end);
    final long t0 = System.currentTimeMillis();
    final SimpleThreadPool pool = new SimpleThreadPool(numberThreads, "ProteinSearch", true);
    final int maxSequences = MAX_SEQUENCES * numberThreads;
    final int[] sequenceLengths = reader0.sequenceLengths(start, end);
    long currentLength = 0;
    long first = start;
    long numberJobs = 0;
    // Horrible method for calculating the number of jobs ahead of time by replicating the loop
    for (long templateId = start, i = 0; i < end - start; i++, templateId++) {
      currentLength += sequenceLengths[(int) i];
      if (currentLength >= ntCount / maxSequences) {
        numberJobs++;
        currentLength = 0;
        first = templateId + 1;
      }
    }
    if (first < end) {
      numberJobs++;
    }
    pool.enableBasicProgress(numberJobs);
    currentLength = 0;
    first = start;
    for (long templateId = start, i = 0; i < end - start; i++, templateId++) {
      currentLength += sequenceLengths[(int) i];
      if (currentLength >= ntCount / maxSequences) {
        schedule(params, (ProteinMask) hf, t0, pool, first, templateId);
        currentLength = 0;
        first = templateId + 1;
      }
    }
    if (first < end) {
      schedule(params, (ProteinMask) hf, t0, pool, first, end - 1);
    }
    timeLog(t0, "parent", "Terminating");
    pool.terminate();
    timeLog(t0, "parent", "Finished");
  }

  private void schedule(final ISequenceParams params, final ProteinMask hf, final long t0, final SimpleThreadPool pool, long first, long end) throws IOException {
    final String name = first < end ? first + ":" + end : "" + first;
    timeLog(t0, name, "Scheduling");
    pool.execute(new ProteinSequenceLoop(params, (ProteinMask) hf.threadClone(HashingRegion.NONE), new HashingRegion(first, end + 1), name, t0, mWindowSize));
  }

  private static void timeLog(final long t0, final String name, final String label) {
    Diagnostic.userLog("Thread Search " + name + " " + label + " " + (System.currentTimeMillis() - t0) / 1000 + " s");
  }

  private static class ProteinSequenceLoop implements IORunnable {

    private long mT0;
    private final ISequenceParams mParams;
    private final ProteinMask mProteinMask;
    private final String mName;
    private final int mWindowSize;


    ProteinSequenceLoop(ISequenceParams params, ProteinMask mask, HashingRegion region, String name, long t0, int windowSize) {
      mT0 = t0;
      mProteinMask = mask;
      mName = name;
      mWindowSize = windowSize;
      mParams = SequenceParams.builder().directory(params.directory()).region(region).mode(params.mode()).create();
    }

    @Override
    public void run() throws IOException {
      timeLog(mT0, mName, "Start");
      mT0 = System.currentTimeMillis();
      final ProteinTemplateHashLoop hashLoop = new ProteinTemplateHashLoop(mWindowSize, 1, mProteinMask);
      try {
        mProteinMask.reset();
        hashLoop.execLoop(mParams);
      } finally {
        mParams.close();
      }
      timeLog(mT0, mName, "Finish");
    }
  }
}
