package io.xspec.maven.xspecMavenPlugin;

import io.xspec.maven.xspecMavenPlugin.resources.impl.DefaultSchematronImplResources;
import io.xspec.maven.xspecMavenPlugin.resources.impl.DefaultXSpecImplResources;
import io.xspec.maven.xspecMavenPlugin.resources.impl.DefaultXSpecPluginResources;
import io.xspec.maven.xspecMavenPlugin.utils.RunnerOptions;
import io.xspec.maven.xspecMavenPlugin.utils.XSpecPluginException;
import org.assertj.core.api.SoftAssertions;
import org.junit.Test;
import top.marchand.maven.saxon.utils.SaxonOptions;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Properties;

public class XSpecRunnerFailingTests extends TestUtils {

  @Test
  public void verifyFailingXSpecIsReportedPreChecks() throws Exception {
    // Given
    RunnerOptions runnerOptions = new RunnerOptions(TestUtils.getProjectDirectory());
    runnerOptions.testDir = new File(TestUtils.getTestDirectory(), "failingXslt");
    XSpecRunner runner = getNewRunner(new SaxonOptions(), runnerOptions);
    SoftAssertions softly = new SoftAssertions();
    // When
    List<File> xspecFiles = runner.findAllXSpecs();
    // Then
    softly.assertThat(xspecFiles)
          .as("wrong number of XSpecFiles found")
          .hasSize(2);
    softly.assertThatThrownBy(runner::execute)
          .isInstanceOf(XSpecPluginException.class);
    softly.assertAll();
  }

  @Test
  public void verifyMessageExitYesXSpecIsReported() throws Exception {
    // Given
    RunnerOptions runnerOptions = new RunnerOptions(getProjectDirectory());
    runnerOptions.testDir = new File(getTestDirectory(), "failingXslt");
    XSpecRunner runner = getNewRunner(new SaxonOptions(), runnerOptions);
    List<File> xspecFiles = runner.findAllXSpecs();
    SoftAssertions softly = new SoftAssertions();
    softly.assertThatThrownBy(runner::execute)
          .isInstanceOf(XSpecPluginException.class);
    // When
    runner.generateIndex();
    String xspecReportFilename = "target/surefire-reports/TEST-xsl1.xspec.xml";
    File surefireReportXSpec = new File(getProjectDirectory(), xspecReportFilename);
    // Then
    softly.assertThat(surefireReportXSpec)
          .exists();
    softly.assertThat(surefireReportXSpec)
          .isFile();
    softly.assertThat(surefireReportXSpec)
          .content()
          .doesNotContain("<testsuites/>");
    softly.assertAll();
  }
  @Test
  public void verifyUncompilableXslIsReported() throws Exception {
    // Given
    RunnerOptions runnerOptions = new RunnerOptions(getProjectDirectory());
    runnerOptions.testDir = new File(getTestDirectory(), "failingXslt");
    XSpecRunner runner = getNewRunner(new SaxonOptions(), runnerOptions);
    List<File> xspecFiles = runner.findAllXSpecs();
    SoftAssertions softly = new SoftAssertions();
    softly.assertThatThrownBy(runner::execute)
          .isInstanceOf(XSpecPluginException.class);
    // When
    runner.generateIndex();
    String xspecReportFilename = "target/surefire-reports/TEST-xsl2.xspec.xml";
    File surefireReportXSpec = new File(getProjectDirectory(), xspecReportFilename);
    // Then
    softly.assertThat(surefireReportXSpec)
          .exists();
    softly.assertThat(surefireReportXSpec)
          .isFile();
    softly.assertThat(surefireReportXSpec)
          .content()
          .doesNotContain("<testsuites/>");
    softly.assertAll();
  }

  private XSpecRunner getNewRunner(SaxonOptions saxonOptions, RunnerOptions runnerOptions) throws IllegalStateException, XSpecPluginException, MalformedURLException, URISyntaxException {
    XSpecRunner runner = new XSpecRunner(getLog(), TestUtils.getBaseDirectory());
    runner.setResources(
        new DefaultXSpecImplResources(),
        new DefaultSchematronImplResources(),
        new DefaultXSpecPluginResources());
    runner.setEnvironment(new Properties(), runnerOptions);
    runner.init(saxonOptions);
    return runner;
  }

}
