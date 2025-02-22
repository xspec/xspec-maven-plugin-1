/**
 * Copyright © 2018, Christophe Marchand, XSpec organization
 * All rights reserved.
 * <p>
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * * Neither the name of the <organization> nor the
 * names of its contributors may be used to endorse or promote products
 * derived from this software without specific prior written permission.
 * <p>
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package io.xspec.maven.xspecMavenPlugin.resources.impl;

import io.xspec.maven.xspecMavenPlugin.resources.XSpecImplResources;

/**
 * Default implementation of XSpec resources.
 * @author cmarchand
 */
public class DefaultXSpecImplResources implements XSpecImplResources {
  private String xspecXslCompilerUri;
  private String xspecXQueryCompilerUri;
  private String schematronUnitTestConverter;
  private String xspecReporter;
  private final String xspecFoldingReporter;
  private String xspecCoverageReporter;
  private String testReportCss;
  private String schematronCompiler;

  public DefaultXSpecImplResources() {
    super();
    xspecXslCompilerUri = XSPEC_PREFIX + XSPEC_PACKAGE + "src/compiler/compile-xslt-tests.xsl";
    xspecXQueryCompilerUri = XSPEC_PREFIX + XSPEC_PACKAGE + "src/compiler/compile-xquery-tests.xsl";
    schematronUnitTestConverter = XSPEC_PREFIX + XSPEC_PACKAGE + "src/schematron/schut-to-xspec.xsl";
    schematronCompiler = XSPEC_PREFIX + XSPEC_PACKAGE + "src/schematron/schut-to-xslt.xsl";
    xspecReporter = XSPEC_PREFIX + XSPEC_PACKAGE + "src/reporter/format-xspec-report.xsl";
    xspecFoldingReporter = XSPEC_PREFIX + XSPEC_PACKAGE + "src/reporter/format-xspec-report-folding.xsl";
    xspecCoverageReporter = XSPEC_PREFIX + XSPEC_PACKAGE + "src/reporter/coverage-report.xsl";
    testReportCss = XSPEC_PREFIX + XSPEC_PACKAGE + "src/reporter/test-report.css";
  }

  @Override
  public String getXSpecXslCompilerUri() {
    return xspecXslCompilerUri;
  }

  @Override
  public String getXSpecXQueryCompilerUri() {
    return xspecXQueryCompilerUri;
  }

  @Override
  public String getSchematronSchutConverterUri() {
    return schematronUnitTestConverter;
  }

  @Override
  public String getXSpecReporterUri(boolean useFolding) {
    if (useFolding) {
      return xspecFoldingReporter;
    }
    return xspecReporter;
  }

  @Override
  public String getXSpecCoverageReporterUri() {
    return xspecCoverageReporter;
  }

  @Override
  public String getXSpecCssReportUri() {
    return testReportCss;
  }

  @Override
  public String getSchematronCompilerUri() {
    return schematronCompiler;
  }

  /**
   * Defines the XSpec compiler for XSL URI
   * @param xspecXslCompilerUri The xspec xsl compiler URI
   */
  public void setXSpecXslCompilerUri(String xspecXslCompilerUri) {
    this.xspecXslCompilerUri = xspecXslCompilerUri;
  }

  /**
   * Defines the XSpec compiler for XQuery URI
   * @param xspecXQueryCompilerUri The xspec XQuery compiler uri
   */
  public void setXSpecXQueryCompiler(String xspecXQueryCompilerUri) {
    this.xspecXQueryCompilerUri = xspecXQueryCompilerUri;
  }

  /**
   * Defines the XSpec-for-Schematron converter URI
   * @param schematronUnitTestConverter The schematron unit-test converter
   */
  public void setSchematronSchutConverter(String schematronUnitTestConverter) {
    this.schematronUnitTestConverter = schematronUnitTestConverter;
  }

  /**
   * Defines the XSpec reporter URI
   * @param xspecReporter The xspec reporter
   */
  public void setXSpecReporter(String xspecReporter) {
    this.xspecReporter = xspecReporter;
  }

  /**
   * Defines the XSpec Code Coverage reporter URI
   * @param xspecCoverageReporter The xspec coverage reporter
   */
  public void setXSpecCoverageReporter(String xspecCoverageReporter) {
    this.xspecCoverageReporter = xspecCoverageReporter;
  }

  /**
   * Defines the report Css URI
   * @param testReportCss the test report CSS
   */
  public void setTestReportCss(String testReportCss) {
    this.testReportCss = testReportCss;
  }

}
