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

package com.rtg.assembler;

import static com.rtg.launcher.BuildCommon.RESOURCE;
import static com.rtg.util.cli.CommonFlagCategories.INPUT_OUTPUT;
import static com.rtg.util.cli.CommonFlagCategories.SENSITIVITY_TUNING;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;

import com.rtg.launcher.CommonFlags;
import com.rtg.launcher.ParamsCli;
import com.rtg.sam.SamFilterOptions;
import com.rtg.util.IORunnable;
import com.rtg.util.InvalidParamsException;
import com.rtg.util.intervals.RegionRestriction;
import com.rtg.util.cli.CFlags;
import com.rtg.util.cli.CommonFlagCategories;
import com.rtg.util.cli.Flag;
import com.rtg.util.cli.Validator;

/**
 *         Date: 11/05/12
 *         Time: 10:51 AM
 */
public class DeBruijnAssemblerCli extends ParamsCli<DeBruijnParams> {
  static final String MODULE_NAME = "debruijn";

  static final String KMER_SIZE = "kmer-size";
  static final String MIN_HASH_FREQUENCY = "minimum-kmer-frequency";
  static final String XSTRING_KMER = "Xstring-kmer";
  static final String DIPLOID_RATIO = "preserve-bubbles";

  @Override
  protected IORunnable task(DeBruijnParams params, OutputStream out) {
    return new DeBruijnAssemblerTask(params, out);
  }

  @Override
  protected DeBruijnParams makeParams() throws InvalidParamsException, IOException {
    return makeParamsLocal(mFlags);
  }

  protected static DeBruijnParams makeParamsLocal(CFlags flags) throws IOException {
    final DeBruijnParams.Builder builder = DeBruijnParams.builder();
    return builder.directory((File) flags.getValue(CommonFlags.OUTPUT_FLAG))
        .inputFiles(CommonFlags.getFileList(flags, CommonFlags.INPUT_LIST_FLAG, null, true))
        .kmerSize((Integer) flags.getValue(KMER_SIZE))
        .minHashFrequency((Integer) flags.getValue(MIN_HASH_FREQUENCY))
        .useStringKmers(flags.isSet(XSTRING_KMER))
        .mergeRatio((Double) flags.getValue(DIPLOID_RATIO))
        .region(CommonFlags.getReaderRestriction(flags))
        .create();
  }

  @Override
  protected File outputDirectory() {
    return (File) mFlags.getValue(CommonFlags.OUTPUT_FLAG);
  }

  @Override
  protected void initFlags() {
    initLocalFlags(mFlags);
  }

  private static class DeBruijnValidator implements Validator {

    /**
     * Check the file list and anonymous file input flags.
     * @param flags the flags to check
     * @return <code>true</code> if all okay <code>false</code> otherwise
     */
    public static boolean checkSdfFileList(CFlags flags, Collection<File> files) {
      if (files.size() == 0) {
        flags.setParseMessage("No input files specified.");
        return false;
      }
      return true;
    }
    private static Collection<File> getFiles(CFlags flags) {
      final Collection<File> files;
      try {
        files = CommonFlags.getFileList(flags, CommonFlags.INPUT_LIST_FLAG, null, true);
      } catch (final IOException e) {
        flags.setParseMessage("An error occurred reading " + flags.getValue(CommonFlags.INPUT_LIST_FLAG));
        return null;
      }
      return files;
    }
    @Override
    public boolean isValid(CFlags flags) {
      if (!CommonFlags.validateOutputDirectory(flags)) {
        return false;
      }
      final Collection<File> files = getFiles(flags);
      if (!checkSdfFileList(flags, files)) {
        return false;
      }
      if (flags.isSet(SamFilterOptions.RESTRICTION_FLAG) && !RegionRestriction.validateRegion((String) flags.getValue(SamFilterOptions.RESTRICTION_FLAG))) {
        flags.error("Invalid region specification");
        return false;
      }
      if (((Integer) flags.getValue(KMER_SIZE)) < 1) {
        flags.error("--" + KMER_SIZE + " should be positive");
        return false;
      }
      if (flags.isSet(DIPLOID_RATIO)) {
        final double ratio = (Double) flags.getValue(DIPLOID_RATIO);
        if (ratio > 1 || ratio < 0) {
          flags.error("--" + DIPLOID_RATIO + " should be between 0 and 1");
        }
      }
      if (!CommonFlags.validateStartEnd(flags, CommonFlags.START_READ_ID, CommonFlags.END_READ_ID)) {
        return false;
      }
      if (flags.isSet(CommonFlags.START_READ_ID) || flags.isSet(CommonFlags.END_READ_ID)) {
        if (files.size() != 1) {
          flags.error("Can only specify read range with a single input set of reads");
          return false;
        }
      }
      return true;
    }
  }

  protected static void initCommonFlags(CFlags flags) {
    flags.registerRequired('o', CommonFlags.OUTPUT_FLAG, File.class, "DIR", RESOURCE.getString("OUTPUT_DESC")).setCategory(INPUT_OUTPUT);
    flags.registerRequired('k', KMER_SIZE, Integer.class, "int", "kmer length to build graph nodes from").setCategory(SENSITIVITY_TUNING);
    flags.registerOptional('c', MIN_HASH_FREQUENCY, Integer.class, "int", "set minimum kmer frequency to retain, or -1 for automatic threshold", -1).setCategory(SENSITIVITY_TUNING);
    flags.registerOptional(DIPLOID_RATIO, Double.class, "float", "avoid merging bubbles where the ratio of kmers on the branches is below this", 0.0).setCategory(SENSITIVITY_TUNING);
    CommonFlags.initReadRange(flags);
  }

  protected static void initLocalFlags(CFlags flags) {
    flags.registerExtendedHelp();
    flags.setValidator(new DeBruijnValidator());
    initCommonFlags(flags);
    flags.registerOptional('s', XSTRING_KMER, "use string based kmers").setCategory(SENSITIVITY_TUNING);
    CommonFlagCategories.setCategories(flags);
    final Flag inFlag = flags.registerRequired(File.class, "file", "SDF directories containing sequences to assemble");
    inFlag.setCategory(INPUT_OUTPUT);
    inFlag.setMinCount(0);
    inFlag.setMaxCount(Integer.MAX_VALUE);
    final Flag listFlag = flags.registerOptional('I', CommonFlags.INPUT_LIST_FLAG, File.class, "FILE", "file containing a list of SDF directories (1 per line) containing sequences to assemble").setCategory(INPUT_OUTPUT);
    flags.addRequiredSet(inFlag);
    flags.addRequiredSet(listFlag);
  }

  @Override
  public String moduleName() {
    return MODULE_NAME;
  }

  /**
   * Noddy main.
   * @param args see usage
   */
  public static void main(final String[] args) {
    new DeBruijnAssemblerCli().mainInit(args, System.out, System.err);
  }
}
