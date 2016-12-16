/*
 * Copyright (c) 2016. Real Time Genomics Limited.
 *
 * Use of this source code is bound by the Real Time Genomics Limited Software Licence Agreement
 * for Academic Non-commercial Research Purposes only.
 *
 * If you did not receive a license accompanying this file, a copy must first be obtained by email
 * from support@realtimegenomics.com.  On downloading, using and/or continuing to use this source
 * code you accept the terms of that license agreement and any amendments to those terms that may
 * be made from time to time by Real Time Genomics Limited.
 */

package com.rtg.variant.cnv.segment;

import static com.rtg.vcf.VcfUtils.FORMAT_GENOTYPE;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.rtg.mode.DnaUtils;
import com.rtg.reader.PrereadNamesInterface;
import com.rtg.reader.SequencesReader;
import com.rtg.util.MathUtils;
import com.rtg.util.Utils;
import com.rtg.variant.format.VcfFormatField;
import com.rtg.vcf.VcfRecord;
import com.rtg.vcf.header.AltField;
import com.rtg.vcf.header.MetaType;
import com.rtg.vcf.header.VcfHeader;
import com.rtg.vcf.header.VcfNumber;

/**
 * Formats segment output as VCF
 */
public class SegmentVcfOutputFormatter {

  // Defined in the VCF spec
  static final String ALT_DEL = "DEL";
  static final String ALT_DUP = "DUP";

  static final String INFO_END = "END";
  static final String INFO_CIPOS = "CIPOS";
  static final String INFO_CIEND = "CIEND";
  static final String INFO_SVTYPE = "SVTYPE";
  static final String INFO_IMPRECISE = "IMPRECISE";

  // Our own fields
  private static final String INFO_BC = "BC";

  static final String FORMAT_RD = "RD";
  static final String FORMAT_RDR = "RDR";
  static final String FORMAT_LOGR = "LR";
  /** The FORMAT field we use to store an overall quality score */
  public static final String FORMAT_SQS = "SQS";

  private static final String ALT_DEL_BR = "<" + ALT_DEL + ">";
  private static final String ALT_DUP_BR = "<" + ALT_DUP + ">";


  private final SequencesReader mTemplate;
  private final double mThreshold;
  private final Map<String, Integer> mSequenceMap = new HashMap<>();
  private final int mMinBins;


  private String mCurrentSequenceName;
  private int mCurrentSequenceId;
  private int mCurrentSequenceLength;

  /**
   * Create a new object
   * @param genomeSequences reader for template
   * @param threshold applied to log ratio beyond which a copy number alteration is output
   * @param minBins minimum number of bins before a copy number alteration is called
   * @throws IOException if error
   */
  public SegmentVcfOutputFormatter(SequencesReader genomeSequences, double threshold, int minBins) throws IOException {
    mTemplate = genomeSequences;
    mThreshold = threshold;
    mMinBins = minBins;
    final PrereadNamesInterface pni = genomeSequences.names();
    for (long i = 0; i < pni.length(); ++i) {
      mSequenceMap.put(genomeSequences.names().name(i), (int) i);
    }
    assert mThreshold >= 0;
  }

  VcfHeader header() throws IOException {
    final VcfHeader header = new VcfHeader();
    header.addCommonHeader();
    header.addReference(mTemplate);
    header.addContigFields(mTemplate);
    header.addAltField(new AltField(ALT_DEL, "Deletion"));
    header.addAltField(new AltField(ALT_DUP, "Duplication"));

    header.addInfoField(INFO_END, MetaType.INTEGER, VcfNumber.ONE, "End position of the variant described in this record");
    header.addInfoField(INFO_IMPRECISE, MetaType.FLAG, new VcfNumber("0"), "Imprecise structural variation");
    header.addInfoField(INFO_SVTYPE, MetaType.STRING, VcfNumber.ONE, "Type of structural variant");
    header.addInfoField(INFO_BC, MetaType.INTEGER, VcfNumber.ONE, "Number of bins contained within the region");
    header.addInfoField(INFO_CIPOS, MetaType.INTEGER, new VcfNumber("2"), "Confidence interval around POS for imprecise variants");
    header.addInfoField(INFO_CIEND, MetaType.INTEGER, new VcfNumber("2"), "Confidence interval around END for imprecise variants");
    //Region/gene names contained within each segment?
    //header.addInfoField(INFO_GENE, MetaType.STRING, VcfNumber.ONE, "Bin names");

    VcfFormatField.GT.updateHeader(header);
    header.addFormatField(FORMAT_SQS, MetaType.FLOAT, VcfNumber.ONE, "Seqment Quality Score");
    header.addFormatField(FORMAT_RDR, MetaType.FLOAT, VcfNumber.ONE, "Mean Normalized RD Ratio with respect to control");
    header.addFormatField(FORMAT_LOGR, MetaType.FLOAT, VcfNumber.ONE, "Log2 of RD Ratio with respect to control");

    // XXX additional fields require more control over the input than we currently have:
    //header.addFormatField(FORMAT_RD, MetaType.FLOAT, VcfNumber.ONE, "Mean Normalized Read Depth");
    //header.addFormatField(FORMAT_LEVEL, MetaType.FLOAT, VcfNumber.ONE, "Amplification/Deletion level (e.g. RDR * ploidy)");

    header.addSampleName("SAMPLE"); // XXX support multiple samples and a control?

    return header;
  }

  VcfRecord vcfRecord(String seqName, Segment s, Segment next) throws IOException {
    // Spec says SV records are given the position BEFORE the SV.
    final int refPosition = Math.max(s.getStart() - 1, 0);
    final String ref = getRefBase(seqName, refPosition);
    final VcfRecord rec = new VcfRecord(seqName, refPosition, ref);

    // Classify the segment as gain or loss
    boolean altered = false;
    if (s.bins() >= mMinBins) {
      if (s.mean() >= mThreshold) {
        altered = true;
        rec.addAltCall(ALT_DUP_BR);
        rec.setInfo(INFO_SVTYPE, ALT_DUP);
      } else if (s.mean() <= -mThreshold) {
        altered = true;
        rec.addAltCall(ALT_DEL_BR);
        rec.setInfo(INFO_SVTYPE, ALT_DEL);
      }
    }

    rec.setInfo(INFO_END, "" + (s.getEnd() + 1));
    rec.setInfo(INFO_IMPRECISE);
    final String cipos = "" + MathUtils.round(s.distanceToPrevious() == 0 ? (double) -s.getStart() : -s.distanceToPrevious() - s.firstBinLength() / 2.0)
      + ","
      + (s.firstBinLength() / 2);
    rec.setInfo(INFO_CIPOS, cipos);

    final String ciend = "" + MathUtils.round((double) -s.lastBinLength() / 2.0)
      + ","
      + MathUtils.round(next == null ? (double) mCurrentSequenceLength - s.getEnd() : next.distanceToPrevious() - s.firstBinLength() / 2.0);
    rec.setInfo(INFO_CIEND, ciend);

    rec.setInfo(INFO_BC, "" + s.bins());

    rec.setNumberOfSamples(1);
    rec.addFormatAndSample(FORMAT_GENOTYPE, altered ? "1" : "0");
    rec.addFormatAndSample(FORMAT_LOGR, Utils.realFormat(s.mean(), 4));
    rec.addFormatAndSample(FORMAT_RDR, Utils.realFormat(Math.pow(2, s.mean()), 4));
    rec.addFormatAndSample(FORMAT_SQS, Utils.realFormat(Math.abs(s.mean()), 4)); // For now, just use abs of LogR as proxy for quality

    return rec;
  }

  private String getRefBase(String refName, int pos) throws IOException {
    if (!refName.equals(mCurrentSequenceName)) {
      final Integer id = mSequenceMap.get(refName);
      if (id == null) {
        throw new RuntimeException("Reference sequence '" + refName + "' was not contained in supplied SDF");
      }
      mCurrentSequenceName = refName;
      mCurrentSequenceId = id;
      mCurrentSequenceLength = mTemplate.length(id);
    }
    assert pos < mCurrentSequenceLength : "pos=" + pos + " currentLen=" + mCurrentSequenceLength;
    if (pos < 0 || (pos > mCurrentSequenceLength - 1)) {
      return "N";
    }
    final byte[] temp = new byte[1];
    mTemplate.read(mCurrentSequenceId, temp, pos, 1);
    return DnaUtils.bytesToSequenceIncCG(temp);
  }

}
