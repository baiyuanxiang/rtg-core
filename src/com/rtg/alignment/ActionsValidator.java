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
package com.rtg.alignment;

import com.rtg.mode.DnaUtils;
import com.rtg.mode.Protein;
import com.rtg.mode.ProteinScoringMatrix;
import com.rtg.util.StringUtils;

/**
 * Provides validation methods for the packed actions arrays returned by EditDistance classes.
 *
 */
public class ActionsValidator {

  private final int mGapOpenPenalty;
  private final int mGapExtendPenalty;
  private final Integer mSubstitutionPenalty;
  private final Integer mUnknownsPenalty;
  private final ProteinScoringMatrix mScoreMatrix;

  /** The unknown residue. */
  private final byte mUnknown;

  /** Error message from last failure detected. */
  String mErrorMsg;

  /**
   * Construct a DNA actions validator.
   * Call <code>setVerbose(true)</code> if you want to see messages
   * about why a action array did not validate.
   *
   * @param gapOpenPenalty additional penalty for starting an insert/delete.
   * @param gapExtendPenalty additional penalty for extending an insert/delete.
   * @param substitutionPenalty penalty for a substitution.
   * @param unknownsPenalty penalty for an unknown nucleotide.
   */
  public ActionsValidator(int gapOpenPenalty, int gapExtendPenalty, int substitutionPenalty, int unknownsPenalty) {
    mGapOpenPenalty = gapOpenPenalty;
    mGapExtendPenalty = gapExtendPenalty;
    mSubstitutionPenalty = substitutionPenalty;
    mUnknownsPenalty = unknownsPenalty;
    mScoreMatrix = null;
    mUnknown = DnaUtils.UNKNOWN_RESIDUE;
  }

  /**
   * Construct a actions validator for Protein edit distance classes.
   *
   * @param matrix the protein scoring matrix.
   */
  ActionsValidator(final ProteinScoringMatrix matrix) {
    mGapExtendPenalty = (int) -matrix.getExtend();
    mGapOpenPenalty = (int) -matrix.getGap();
    mSubstitutionPenalty = null;
    mUnknownsPenalty = null;
    mScoreMatrix = matrix;
    mUnknown = (byte) Protein.X.ordinal();
  }

  /**
   * Error reporting method.  All failures should call this.
   *
   * @param msg Explanation of why we are returning false.
   * @return false.
   */
  private boolean error(final String msg) {
    mErrorMsg = msg;
    return false;
  }

  /**
   * Get a detailed error message that shows why the actions were wrong.
   *
   * @param actions the actions array
   * @param read the read sequence
   * @param rlen the length of the read
   * @param template the whole template sequence
   * @param zeroBasedStart the original expected starting position in the template
   * @return a detailed multi-line message.
   */
  public String getErrorDetails(int[] actions, byte[] read, int rlen, byte[] template, int zeroBasedStart) {
    final StringBuilder sb = new StringBuilder();
    sb.append("ValidatingEditDistance action problem: ").append(mErrorMsg).append(StringUtils.LS);
    if (actions != null && template != null) {
      final int realStart = ActionsHelper.zeroBasedTemplateStart(actions);
      if (realStart < zeroBasedStart) {
        sb.append(" tmpl[").append(realStart).append("..]: ").append(DnaUtils.bytesToSequenceIncCG(template, realStart, zeroBasedStart - realStart)).append(StringUtils.LS);
      }
      sb.append(String.format("%10d|", zeroBasedStart));
      for (int i = 1; i < 10; i++) {
        sb.append(String.format("%9d|", (zeroBasedStart + 10 * i) % 1000));
      }
      sb.append(StringUtils.LS);
      sb.append(" tmpl:    ").append(DnaUtils.bytesToSequenceIncCG(template, zeroBasedStart, rlen)).append(StringUtils.LS);
      sb.append(" read:    ").append(DnaUtils.bytesToSequenceIncCG(read, 0, rlen)).append(StringUtils.LS);
      sb.append(" actions: ").append(ActionsHelper.toString(actions)).append(" score=").append(ActionsHelper.alignmentScore(actions));
    }
    return sb.toString();
  }

  /**
   * Checks that an edit-distance result is valid.
   *
   * This method handles DNA and Protein, but not CG yet.
   * Reverse complement unsupported by this method, must be converted first.
   *
   * @param actions a packed actions array from an edit-distance class.
   * @param read the read sequence
   * @param rlen length of read (assumed that given read is at least this long)
   * @param template the template sequence
   * @param maxScore if the real score is greater than this, actions is allowed to have <code>score=MAX_INTEGER</code>.
   * @return true iff <code>actions</code> is a valid alignment for read and template.
   */
  public boolean isValid(final int[] actions, final byte[] read, final int rlen, final byte[] template, final int maxScore) {
    if (actions == null) {
      return error("actions array is null");
    }
    if (actions.length < ActionsHelper.ACTIONS_START_INDEX) {
      return error("actions array is too short: " + actions.length);
    }
    final int claimedScore = ActionsHelper.alignmentScore(actions);
    final int len = ActionsHelper.actionsCount(actions);
    final int zeroBasedStart = ActionsHelper.zeroBasedTemplateStart(actions);

//    System.err.println("DEBUG: score=" + finalscore + ", len=" + len + ", start=" + zeroBasedStart);
//    System.err.println("RD: ");
//    for (int i = 0; i < read.length; i++) {
//      System.err.print("  " + read[i]);
//    }
//    System.err.println();
//    System.err.println("TM: ");
//    for (int i = 0; i < template.length; i++) {
//      System.err.print("  " + template[i]);
//    }
//    System.err.println();

    // detect action arrays that say 'too hard' (no good alignment is possible).
    if (claimedScore == Integer.MAX_VALUE && len == 0) {
      return true;
    }

    // Some low-level sanity checking on the length and contents of actions.
    final int pos = ActionsHelper.ACTIONS_START_INDEX + ((len - 1) >> ActionsHelper.ACTIONS_PER_INT_SHIFT);
    if (pos >= actions.length) {
      return error("int[] is shorter than number of actions requires");
    }
    final int actionsLeftInThisInt = ((len - 1) & ActionsHelper.ACTIONS_COUNT_MASK) + 1;
    final int bitsToIgnore = ActionsHelper.BITS_PER_ACTION * (ActionsHelper.ACTIONS_PER_INT - actionsLeftInThisInt);
    if (((actions[pos] >>> bitsToIgnore) << bitsToIgnore) != actions[pos]) {
      return error("bits were set beyond numActions");
    }

    // now iterate through the commands and check the alignment score.
    int rpos = 0;
    int tpos = zeroBasedStart;
    int score = 0;
    int prevAction = Integer.MAX_VALUE; // any non-action
    int actionNum = 0;
    final ActionsHelper.CommandIterator iter = ActionsHelper.iterator(actions);
    while (iter.hasNext()) {
      actionNum++;
      final int currAction = iter.next();
      if (rpos > rlen) {
        return error("too many actions - past end of read");
      }
      if (rpos == rlen && currAction != ActionsHelper.DELETION_FROM_REFERENCE) {
        // once we have reached the end of the read the only command allowed is an insert.
        return error("non-insert action at end of the read");
      }
      final byte residue;  // the current read residue (or -1 if none).
      final byte tresidue; // the current template residue (or -1 if none).
      final int nextRpos;
      final int nextTpos;
      switch (currAction) {
        case ActionsHelper.SAME:
          residue = read[rpos];
          tresidue = 0 <= tpos && tpos < template.length ? template[tpos] : mUnknown;
          final int sameScore = scoreSame(residue, tresidue);
          if (residue != tresidue && residue != mUnknown && tresidue != mUnknown) {
            return error("action " + actionNum + ": read[" + rpos + "] (" + residue + ") != template[" + tpos + "] (" + tresidue + ")"
                + " score=" + sameScore);
          }
          score += sameScore;
          nextRpos = rpos + 1;
          nextTpos = tpos + 1;
          break;
        case ActionsHelper.INSERTION_INTO_REFERENCE:
          //residue = read[rpos];
          //tresidue = (byte) 255; // TODO: CG r == overlapPos2 || r == overlapPos1 ? Utils.CG_SPACER_CHAR : '-';
          score += scoreDelete(prevAction);
          nextRpos = rpos + 1;
          nextTpos = tpos;
          break;
        case ActionsHelper.DELETION_FROM_REFERENCE:
          //residue = (byte) 255;
          //tresidue = 0 <= tpos && tpos < template.length ? template[tpos] : mUnknown;
          nextRpos = rpos;
          nextTpos = tpos + 1;
          score += scoreInsert(prevAction);
          break;
        default: // MISMATCH
          residue = read[rpos];
          tresidue = 0 <= tpos && tpos < template.length ? template[tpos] : mUnknown;
          final int subScore = scoreSubs(residue, tresidue);
          if (residue == tresidue) {
            return error("action " + actionNum + ": read[" + rpos + "] (" + residue + ") == template[" + tpos + "] (" + tresidue + ")"
                + " score=" + subScore);
          }
          score += subScore;
          nextRpos = rpos + 1;
          nextTpos = tpos + 1;
          break;
      }
//      System.err.println("read[" + rpos + "]=" + residue + " "
//          + ActionsHelper.TO_STRING_CHARACTERS[currAction]
//          + " template[" + tpos + "]=" + tresidue + ", score=" + score);
      prevAction = currAction;
      rpos = nextRpos;
      tpos = nextTpos;
    }
    if (rpos < rlen) {
      return error("actions cover only " + rpos + " residues, but the read has " + rlen);
    }
    if (claimedScore == Integer.MAX_VALUE && score <= maxScore) {
      return error("actual score " + score + " < max (" + maxScore + ") but score was MAX_VALUE");
    }
    if (claimedScore != Integer.MAX_VALUE && score != claimedScore) {
      return error("actual score " + score + " != claimed score " + claimedScore);
    }
    mErrorMsg = null;
    return true;
  }

  /**
   * Checks that an edit-distance result is valid.
   *
   * @param actions a packed actions array from an edit-distance class.
   * @param read the read sequence
   * @param rlen length of read (assumed that given read is at least this long)
   * @param template the template sequence
   * @param rc true means the read is reverse complement
   * @param maxScore if the real score is greater than this, actions is allowed to have <code>score=MAX_INTEGER</code>.
   * @return true iff <code>actions</code> is a valid alignment for read and template.
   */
  public boolean isValid(int[] actions, byte[] read, int rlen, byte[] template, boolean rc, int maxScore) {
    if (!rc) {
      return isValid(actions, read, rlen, template, maxScore);
    } else {
      assert mScoreMatrix == null;  // not Protein.
      final String revTemplateStr = DnaUtils.reverseComplement(DnaUtils.bytesToSequenceIncCG(template));
      final byte[] revTemplate = DnaUtils.encodeString(revTemplateStr);
      return isValid(actions, read, rlen, revTemplate, maxScore);
    }
  }

  private int scoreSame(final byte base, final byte tbase) {
    if (mScoreMatrix == null) {
      return 0; //DNA case
    } else {
      return -mScoreMatrix.score(base, tbase);
    }
  }

  private int scoreSubs(final byte base, final byte tbase) {
    if (mScoreMatrix == null) {
      if (base == mUnknown || tbase == mUnknown) {
        return mUnknownsPenalty;
      }
      return mSubstitutionPenalty; //DNA case
    } else {
      return -mScoreMatrix.score(base, tbase);
    }
  }

  private int scoreDelete(final int prevAction) {
    return mGapExtendPenalty + (prevAction == ActionsHelper.INSERTION_INTO_REFERENCE ? 0 : mGapOpenPenalty);
  }

  private int scoreInsert(final int prevAction) {
    return mGapExtendPenalty + (prevAction == ActionsHelper.DELETION_FROM_REFERENCE ? 0 : mGapOpenPenalty);
  }
}
