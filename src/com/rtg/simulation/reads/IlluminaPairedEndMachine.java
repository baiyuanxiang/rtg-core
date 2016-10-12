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

package com.rtg.simulation.reads;

import static com.rtg.launcher.globals.GlobalFlags.getIntegerValue;

import java.io.IOException;

import com.rtg.launcher.globals.CoreGlobalFlags;
import com.rtg.launcher.globals.GlobalFlags;
import com.rtg.mode.DnaUtils;
import com.rtg.util.InvalidParamsException;
import com.rtg.util.StringUtils;
import com.rtg.util.machine.MachineType;
import com.rtg.variant.AbstractMachineErrorParams;

/**
 * Illumina paired end read simulator
 */
public class IlluminaPairedEndMachine extends AbstractIlluminaMachine {

  private static final int READ_DIRECTION = getIntegerValue(CoreGlobalFlags.READ_STRAND); // -1 = reverse, 0 = random, 1 = forward

  // PE Read 2 sequencing primer
  private static final String PE_EXTENSION = "CGGTCTCGGCATTCCTGCTGAACCGCTCTTCCGATCT"  + "NNNNN" + StringUtils.reverse("ACACTCTTTCCCTACACGACGCTCTTCCGATCT");

  protected int mLeftReadLength;
  protected int mRightReadLength;

  {
    mExtension = DnaUtils.encodeString(GlobalFlags.isSet(CoreGlobalFlags.READ_THROUGH) ? GlobalFlags.getStringValue(CoreGlobalFlags.READ_THROUGH) : PE_EXTENSION);
  }

  /**
   * Constructs with seed and specific priors
   * @param params priors to use
   * @param randomSeed random seed
   */
  public IlluminaPairedEndMachine(AbstractMachineErrorParams params, long randomSeed) {
    super(params, randomSeed);
  }

  /**
   * Constructs with seed and default Illumina priors
   * @param randomSeed random seed
   * @throws InvalidParamsException if fails to construct priors
   * @throws IOException whenever
   */
  public IlluminaPairedEndMachine(long randomSeed) throws InvalidParamsException, IOException {
    super(randomSeed);
  }

  private void setBuffers() {
    final int len = Math.max(mLeftReadLength, mRightReadLength);
    mQualityBytes = new byte[len];
    mReadBytes = new byte[len];
    mWorkspace = new int[Math.max(20, len)];
  }

  /**
   * Sets length of left arm of generated reads
   * @param val the length
   */
  public void setLeftReadLength(int val) {
    mLeftReadLength = val;
    setBuffers();
  }

  /**
   * Sets length of right arm of generated reads
   * @param val the length
   */
  public void setRightReadLength(int val) {
    mRightReadLength = val;
    setBuffers();
  }

  @Override
  public void processFragment(String id, int fragmentStart, byte[] data, int length) throws IOException {
    reseedErrorRandom(mFrameRandom.nextLong());
    final boolean forwardFirst = READ_DIRECTION == 0 ? mFrameRandom.nextBoolean() : READ_DIRECTION > 0;
    final String nameLeft = generateRead(id, fragmentStart, data, length, forwardFirst, mLeftReadLength);
    if (mReadBytesUsed == mLeftReadLength) {
      mReadWriter.writeLeftRead(nameLeft, mReadBytes, mQualityBytes, mLeftReadLength);
      mResidueCount += mLeftReadLength;
    } else {
      throw new FragmentTooSmallException(length, mLeftReadLength);
    }
    final String nameRight = generateRead(id, fragmentStart, data, length, !forwardFirst, mRightReadLength);

    if (mReadBytesUsed == mRightReadLength) {
      mReadWriter.writeRightRead(nameRight, mReadBytes, mQualityBytes, mRightReadLength);
      mResidueCount += mRightReadLength;
    } else {
      throw new FragmentTooSmallException(length, mRightReadLength);
    }
  }

  @Override
  public boolean isPaired() {
    return true;
  }

  @Override
  public MachineType machineType() {
    return MachineType.ILLUMINA_PE;
  }
}
