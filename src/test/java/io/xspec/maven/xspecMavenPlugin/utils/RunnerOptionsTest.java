/**
 * Copyright © 2019, Christophe Marchand, XSpec organization
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
package io.xspec.maven.xspecMavenPlugin.utils;

import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Arrays;
import java.util.List;

/**
 *
 * @author cmarchand
 */
public class RunnerOptionsTest {

  @Test
  public void defaultConstructorTest() {
    File source = new File(".").getAbsoluteFile();
    File testDir = new File(source, "src/test/xspec");
    File reportDir = new File(source, "target/xspec-reports");
    File surefireReport = new File(source, "target/surefire-reports");

    RunnerOptions options = new RunnerOptions(source);
    SoftAssertions softly = new SoftAssertions();
    softly.assertThat(options.testDir).isEqualTo(testDir);
    softly.assertThat(options.reportDir).isEqualTo(reportDir);
    softly.assertThat(options.surefireReportDir).isEqualTo(surefireReport);
    softly.assertThat(options.keepGeneratedCatalog).isFalse();
    softly.assertThat(options.coverage).isFalse();
    softly.assertThat(options.catalogFile).isNull();
    softly.assertThat(options.executionId).isEqualTo("default");
    softly.assertThat(options.excludes).isEmpty();
    softly.assertThat(options.folding).isFalse();
    softly.assertAll();
  }

  @Test
  public void constructorOverrideKeepGeneratedCatalogTest() {
    File source = new File(".").getAbsoluteFile();
    File testDir = new File(source, "src/test/xspec");
    File reportDir = new File(source, "target/xspec-reports");
    File surefireReport = new File(source, "target/surefire-reports");

    RunnerOptions options = new RunnerOptions(source, Boolean.TRUE, null, null, null, null, null, null, null, null);
    SoftAssertions softly = new SoftAssertions();
    softly.assertThat(options.keepGeneratedCatalog).isTrue();
    // not changed
    softly.assertThat(options.testDir).isEqualTo(testDir);
    softly.assertThat(options.reportDir).isEqualTo(reportDir);
    softly.assertThat(options.surefireReportDir).isEqualTo(surefireReport);
    softly.assertThat(options.coverage).isFalse();
    softly.assertThat(options.catalogFile).isNull();
    softly.assertThat(options.executionId).isEqualTo("default");
    softly.assertThat(options.excludes).isEmpty();
    softly.assertThat(options.folding).isFalse();
    softly.assertAll();
  }

  @Test
  public void constructorOverrideCatalogFileTest() {
    File source = new File(".").getAbsoluteFile();
    File testDir = new File(source, "src/test/xspec");
    File reportDir = new File(source, "target/xspec-reports");
    File surefireReport = new File(source, "target/surefire-reports");

    RunnerOptions options = new RunnerOptions(source, null, "catalog.xml", null, null, null, null, null, null, null);
    SoftAssertions softly = new SoftAssertions();
    softly.assertThat(options.catalogFile).isEqualTo("catalog.xml");
    // not changed
    softly.assertThat(options.testDir).isEqualTo(testDir);
    softly.assertThat(options.reportDir).isEqualTo(reportDir);
    softly.assertThat(options.surefireReportDir).isEqualTo(surefireReport);
    softly.assertThat(options.keepGeneratedCatalog).isFalse();
    softly.assertThat(options.coverage).isFalse();
    softly.assertThat(options.executionId).isEqualTo("default");
    softly.assertThat(options.excludes).isEmpty();
    softly.assertThat(options.folding).isFalse();
    softly.assertAll();
  }

  @Test
  public void constructorOverrideExcludesTest() {
    File source = new File(".").getAbsoluteFile();
    File testDir = new File(source, "src/test/xspec");
    File reportDir = new File(source, "target/xspec-reports");
    File surefireReport = new File(source, "target/surefire-reports");

    List<String> excludes = List.of("*IT.xspec");

    RunnerOptions options = new RunnerOptions(source, null, null, excludes, null, null, null, null, null, null);
    SoftAssertions softly = new SoftAssertions();
    softly.assertThat(options.excludes).hasSize(1);

    // not changed
    softly.assertThat(options.testDir).isEqualTo(testDir);
    softly.assertThat(options.reportDir).isEqualTo(reportDir);
    softly.assertThat(options.surefireReportDir).isEqualTo(surefireReport);
    softly.assertThat(options.keepGeneratedCatalog).isFalse();
    softly.assertThat(options.catalogFile).isNull();
    softly.assertThat(options.coverage).isFalse();
    softly.assertThat(options.executionId).isEqualTo("default");
    softly.assertThat(options.folding).isFalse();
    softly.assertAll();
  }

  @Test
  public void constructorOverrideTestDirTest() {
    File source = new File(".").getAbsoluteFile();
    File testDir = new File(source, "src/test/xspec");
    File reportDir = new File(source, "target/xspec-reports");
    File surefireReport = new File(source, "target/surefire-reports");

    RunnerOptions options = new RunnerOptions(source, null, null, null, source, null, null, null, null, null);
    SoftAssertions softly = new SoftAssertions();
    softly.assertThat(options.testDir).isEqualTo(source);
    // not changed
    softly.assertThat(options.reportDir).isEqualTo(reportDir);
    softly.assertThat(options.surefireReportDir).isEqualTo(surefireReport);
    softly.assertThat(options.keepGeneratedCatalog).isFalse();
    softly.assertThat(options.catalogFile).isNull();
    softly.assertThat(options.coverage).isFalse();
    softly.assertThat(options.executionId).isEqualTo("default");
    softly.assertThat(options.folding).isFalse();
    softly.assertAll();
  }

  @Test
  public void constructorOverrideReportTest() {
    File source = new File(".").getAbsoluteFile();
    File testDir = new File(source, "src/test/xspec");
    File reportDir = new File(source, "target/xspec-reports");
    File surefireReport = new File(source, "target/surefire-reports");

    RunnerOptions options = new RunnerOptions(source, null, null, null, null, source, null, null, null, null);
    SoftAssertions softly = new SoftAssertions();
    softly.assertThat(options.reportDir).isEqualTo(source);
    // not changed
    softly.assertThat(options.testDir).isEqualTo(testDir);
    softly.assertThat(options.surefireReportDir).isEqualTo(surefireReport);
    softly.assertThat(options.keepGeneratedCatalog).isFalse();
    softly.assertThat(options.catalogFile).isNull();
    softly.assertThat(options.coverage).isFalse();
    softly.assertThat(options.executionId).isEqualTo("default");
    softly.assertThat(options.excludes).isEmpty();
    softly.assertThat(options.folding).isFalse();
    softly.assertAll();
  }

  @Test
  public void constructorOverrideExecutionIdTest() {
    File source = new File(".").getAbsoluteFile();
    File testDir = new File(source, "src/test/xspec");
    File reportDir = new File(source, "target/xspec-reports");
    File surefireReport = new File(source, "target/surefire-reports");

    RunnerOptions options = new RunnerOptions(source, null, null, null, null, null, "executionId", null, null, null);
    SoftAssertions softly = new SoftAssertions();
    softly.assertThat(options.executionId).isEqualTo("executionId");

    // not changed
    softly.assertThat(options.testDir).isEqualTo(testDir);
    softly.assertThat(options.reportDir).isEqualTo(reportDir);
    softly.assertThat(options.surefireReportDir).isEqualTo(surefireReport);
    softly.assertThat(options.keepGeneratedCatalog).isFalse();
    softly.assertThat(options.catalogFile).isNull();
    softly.assertThat(options.coverage).isFalse();
    softly.assertThat(options.excludes).isEmpty();
    softly.assertThat(options.folding).isFalse();
    softly.assertAll();
  }

  @Test
  public void constructorOverrideSurefireTest() {
    File source = new File(".").getAbsoluteFile();
    File testDir = new File(source, "src/test/xspec");
    File reportDir = new File(source, "target/xspec-reports");
    File surefireReport = new File(source, "target/surefire-reports");

    RunnerOptions options = new RunnerOptions(source, null, null, null, null, null, null, source, null, null);
    SoftAssertions softly = new SoftAssertions();
    softly.assertThat(options.surefireReportDir).isEqualTo(source);

    // not changed
    softly.assertThat(options.testDir).isEqualTo(testDir);
    softly.assertThat(options.reportDir).isEqualTo(reportDir);
//        assertEquals("surefireReport is not correct", surefireReport, options.surefireReportDir);
    softly.assertThat(options.keepGeneratedCatalog).isFalse();
    softly.assertThat(options.catalogFile).isNull();
    softly.assertThat(options.coverage).isFalse();
    softly.assertThat(options.executionId).isEqualTo("default");
    softly.assertThat(options.excludes).isEmpty();
    softly.assertThat(options.folding).isFalse();
    softly.assertAll();
  }

  @Test
  public void constructorOverrideCoverageTest() {
    File source = new File(".").getAbsoluteFile();
    File testDir = new File(source, "src/test/xspec");
    File reportDir = new File(source, "target/xspec-reports");
    File surefireReport = new File(source, "target/surefire-reports");

    RunnerOptions options = new RunnerOptions(source, null, null, null, null, null, null, null, Boolean.TRUE, false);
    SoftAssertions softly = new SoftAssertions();
    softly.assertThat(options.coverage).isTrue();

    // not changed
    softly.assertThat(options.testDir).isEqualTo(testDir);
    softly.assertThat(options.reportDir).isEqualTo(reportDir);
    softly.assertThat(options.surefireReportDir).isEqualTo(surefireReport);
    softly.assertThat(options.keepGeneratedCatalog).isFalse();
    softly.assertThat(options.catalogFile).isNull();
//        assertFalse("coverage is not false", options.coverage);
    softly.assertThat(options.executionId).isEqualTo("default");
    softly.assertThat(options.excludes).isEmpty();
    softly.assertThat(options.folding).isFalse();
    softly.assertAll();
  }

  @Test
  public void constructorOverrideFoldingTest() {
    File source = new File(".").getAbsoluteFile();
    File testDir = new File(source, "src/test/xspec");
    File reportDir = new File(source, "target/xspec-reports");
    File surefireReport = new File(source, "target/surefire-reports");

    RunnerOptions options = new RunnerOptions(source, null, null, null, null, null, null, null, null, Boolean.TRUE);
    SoftAssertions softly = new SoftAssertions();

    // not changed
    softly.assertThat(options.testDir).isEqualTo(testDir);
    softly.assertThat(options.reportDir).isEqualTo(reportDir);
    softly.assertThat(options.surefireReportDir).isEqualTo(surefireReport);
    softly.assertThat(options.keepGeneratedCatalog).isFalse();
    softly.assertThat(options.catalogFile).isNull();
    softly.assertThat(options.coverage).isFalse();
    softly.assertThat(options.executionId).isEqualTo("default");
    softly.assertThat(options.excludes).isEmpty();
    // must be true !
    softly.assertThat(options.folding).isTrue();
    softly.assertAll();
  }
}
