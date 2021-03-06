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
package com.rtg.variant.avr;

import static com.rtg.variant.avr.AbstractModelBuilder.MODEL_AVR_VERSION;
import static com.rtg.variant.avr.AbstractModelBuilder.MODEL_PROPERTY_COMMAND_LINE;
import static com.rtg.variant.avr.AbstractModelBuilder.MODEL_PROPERTY_DATE;
import static com.rtg.variant.avr.AbstractModelBuilder.MODEL_PROPERTY_DERIVED_ANNOTATIONS;
import static com.rtg.variant.avr.AbstractModelBuilder.MODEL_PROPERTY_FORMAT_ANNOTATIONS;
import static com.rtg.variant.avr.AbstractModelBuilder.MODEL_PROPERTY_INFO_ANNOTATIONS;
import static com.rtg.variant.avr.AbstractModelBuilder.MODEL_PROPERTY_MODEL_ID;
import static com.rtg.variant.avr.AbstractModelBuilder.MODEL_PROPERTY_QUAL_ANNOTATION;
import static com.rtg.variant.avr.AbstractModelBuilder.MODEL_PROPERTY_TYPE;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Properties;

import com.rtg.launcher.AbstractCli;
import com.rtg.launcher.CommonFlags;
import com.rtg.util.Pair;
import com.rtg.util.StringUtils;
import com.rtg.util.cli.CommonFlagCategories;
import com.rtg.util.diagnostic.NoTalkbackSlimException;
import com.rtg.util.io.LineWriter;
import com.rtg.util.memory.MemoryUsage;

/**
 */
public class AvrStatsCli extends AbstractCli {

  private static final String DUMP_MODEL_FLAG = "Xdump-model";
  private static final String DUMP_PROPERTIES_FLAG = "Xdump-properties";
  private static final String DUMP_MEMORY_FLAG = "Xdump-memory";
  private static final String UPGRADE_MODEL_FLAG = "Xupgrade-model";

  @Override
  public String moduleName() {
    return "avrstats";
  }

  @Override
  public String description() {
    return "print statistics about an AVR model";
  }

  @Override
  protected void initFlags() {
    CommonFlagCategories.setCategories(mFlags);
    mFlags.registerExtendedHelp();
    mFlags.setDescription("Print statistics that describe an AVR model.");
    mFlags.registerOptional(DUMP_PROPERTIES_FLAG, "if set, output the raw model properties").setCategory(CommonFlagCategories.UTILITY);
    mFlags.registerOptional(DUMP_MODEL_FLAG, "if set, output a verbose representation of the model").setCategory(CommonFlagCategories.UTILITY);
    mFlags.registerOptional(DUMP_MEMORY_FLAG, "if set, output model memory usage").setCategory(CommonFlagCategories.UTILITY);
    mFlags.registerOptional(UPGRADE_MODEL_FLAG, File.class, CommonFlags.FILE, "if set, re-save the model (to upgrade model version)").setCategory(CommonFlagCategories.UTILITY);

    AvrUtils.initAvrModel(mFlags, true);
  }

  private final ArrayList<Pair<String, String>> mProperties = new ArrayList<>();
  {
    mProperties.add(new Pair<>("Date built", MODEL_PROPERTY_DATE));
    mProperties.add(new Pair<>("AVR Version", MODEL_AVR_VERSION));
    mProperties.add(new Pair<>("AVR-ID", MODEL_PROPERTY_MODEL_ID));
    mProperties.add(new Pair<>("Type", MODEL_PROPERTY_TYPE));
    mProperties.add(new Pair<>("QUAL used", MODEL_PROPERTY_QUAL_ANNOTATION));
    mProperties.add(new Pair<>("INFO fields", MODEL_PROPERTY_INFO_ANNOTATIONS));
    mProperties.add(new Pair<>("FORMAT fields", MODEL_PROPERTY_FORMAT_ANNOTATIONS));
    mProperties.add(new Pair<>("Derived fields", MODEL_PROPERTY_DERIVED_ANNOTATIONS));
    mProperties.add(new Pair<>("Parameters", MODEL_PROPERTY_COMMAND_LINE));
  }

  @Override
  protected int mainExec(OutputStream out, PrintStream err) throws IOException {
    final File modelFile;
    modelFile = AvrUtils.getAvrModel(mFlags, true);
    if (modelFile == null) {
      throw new NoTalkbackSlimException("No model file specified and no default model available.");
    }
    try (LineWriter lw = new LineWriter(new OutputStreamWriter(out))) {
      final ModelFactory fact = new ModelFactory(modelFile);
      final Properties props = fact.getModelProperties();
      int maxLen = "Location".length();
      for (Pair<String, String> field : mProperties) {
        maxLen = Math.max(maxLen, field.getA().length());
      }
      lw.writeln("Location" + StringUtils.spaces(maxLen - 8) + ": " + modelFile.toString());
      for (Pair<String, String> field : mProperties) {
        final String label = field.getA();
        final String value = props.getProperty(field.getB());
        if (value != null && value.length() > 0) {
          lw.writeln(field.getA() + StringUtils.spaces(maxLen - label.length()) + ": " + value);
        }
      }
      lw.writeln();

      if (mFlags.isSet(DUMP_PROPERTIES_FLAG)) {
        lw.writeln("Properties:");
        for (String property : props.stringPropertyNames()) {
          lw.writeln(property + "=" + props.getProperty(property));
        }
        lw.writeln();
      }

      if (mFlags.isSet(DUMP_MODEL_FLAG)) {
        lw.writeln("Full Model:");
        lw.writeln(fact.getModel().toString());
        lw.writeln();
      }

      if (mFlags.isSet(DUMP_MEMORY_FLAG)) {
        lw.writeln(new MemoryUsage(fact.getModel()).toString());
      }

      if (mFlags.isSet(UPGRADE_MODEL_FLAG)) {
        final File outFile = (File) mFlags.getValue(UPGRADE_MODEL_FLAG);
        lw.writeln("Writing Model To: " + outFile);
        AbstractModelBuilder.save(outFile, fact.getModelProperties(), fact.getModel());
      }
    }
    return 0;
  }
}
