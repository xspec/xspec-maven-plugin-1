/**
 * Copyright © 2018, Christophe Marchand, XSpec organization
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the <organization> nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
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
package io.xspec.maven.xspecMavenPlugin;

import io.xspec.maven.xspecMavenPlugin.resources.impl.DefaultSchematronImplResources;
import io.xspec.maven.xspecMavenPlugin.resources.impl.DefaultXSpecImplResources;
import io.xspec.maven.xspecMavenPlugin.resources.impl.DefaultXSpecPluginResources;
import io.xspec.maven.xspecMavenPlugin.utils.RunnerOptions;
import io.xspec.maven.xspecMavenPlugin.utils.XSpecPluginException;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.List;
import java.util.Properties;
import net.sf.saxon.s9api.XdmNode;
import org.junit.Test;
import static org.junit.Assert.*;
import top.marchand.maven.saxon.utils.SaxonOptions;

/**
 *
 * @author cmarchand
 */
public class XSpecRunnerTest extends TestUtils {
    
    
    @Test
    public void extractCssResourceTest() throws Exception {
        XSpecRunner runner = new XSpecRunner(getLog(), getBaseDirectory());
        runner.setResources(
                new DefaultXSpecImplResources(), 
                new DefaultSchematronImplResources(), 
                new DefaultXSpecPluginResources());
        RunnerOptions options = new RunnerOptions(getBaseDirectory());
        runner.setEnvironment(new Properties(), options);

        runner.init(new SaxonOptions());
        runner.extractCssResource();
        File expectedFile = new File(getBaseDirectory(), "target/xspec-reports/resources/test-report.css");
        assertTrue(expectedFile.getAbsolutePath()+" does not exists", expectedFile.exists());
    }
    
    @Test(expected = IllegalStateException.class)
    public void initBeforeSetResources() throws Exception {
        XSpecRunner runner = new XSpecRunner(getLog(), getBaseDirectory());
        runner.init(new SaxonOptions());
        fail("calling init(SaxonOption) before setResources(...) should throw an IllegalStateException");
    }
    
    @Test(expected = IllegalStateException.class)
    public void initTwiceTest() throws Exception {
        SaxonOptions saxonOptions = new SaxonOptions();
        XSpecRunner runner = getNewRunner(saxonOptions);
        getLog().debug("calling runner.init a second time");
        runner.init(saxonOptions);
        fail("init shouldn't be call twice without throwing an IllegalStateException");
    }

    @Test
    public void processXsltXspecTest() throws Exception {
        RunnerOptions options = new RunnerOptions(getBaseDirectory());
        options.testDir = new File(getProjectDirectory(), "src/test/resources/filesToTest/xsltTestCase");
        XSpecRunner runner = getNewRunner(new SaxonOptions(), options);
        File xspecFile = new File(getBaseDirectory().getParentFile().getParentFile().getParentFile(), "src/test/resources/filesToTest/xsltTestCase/xsl1.xspec");
        XdmNode node = runner.getXmlStuff().getDocumentBuilder().build(xspecFile);
        assertNotNull("node is null", node);
        assertNotNull("node baseUri is null", node.getBaseURI());
        runner.initProcessedFiles(1);
        boolean ret = runner.processXsltXSpec(node);
        assertTrue("XSpec failed", ret);
    }
    @Test
    public void generateIndexWithXsltTest() throws Exception {
        RunnerOptions options = new RunnerOptions(getBaseDirectory());
        options.testDir = new File(getProjectDirectory(), "src/test/resources/filesToTest/xsltTestCase");
        XSpecRunner runner = getNewRunner(new SaxonOptions(), options);
        File xspecFile = new File(getBaseDirectory().getParentFile().getParentFile().getParentFile(), "src/test/resources/filesToTest/xsltTestCase/xsl1.xspec");
        XdmNode node = runner.getXmlStuff().getDocumentBuilder().build(xspecFile);
        assertNotNull("node is null", node);
        assertNotNull("node baseUri is null", node.getBaseURI());
        runner.initProcessedFiles(1);
        runner.processXsltXSpec(node);
        runner.generateIndex();
        File indexFile = new File(options.reportDir,"index.html");
        assertTrue("index file "+indexFile.getAbsolutePath()+" does not exist", indexFile.exists());
        assertTrue("index file "+indexFile.getAbsolutePath()+" is not a file", indexFile.isFile());
    }
    
    @Test
    public void executeTest() throws Exception {
        getLog().info("baseDirectory: "+getBaseDirectory().getAbsolutePath());
        RunnerOptions options = new RunnerOptions(getBaseDirectory());
        options.testDir=new File(getProjectDirectory(), "src/test/resources/filesToTest/xsltTestCase");
        getLog().info("reportDirectory: "+options.reportDir.getAbsolutePath());
        XSpecRunner runner = getNewRunner(new SaxonOptions(), options);
        runner.execute();
        File indexFile = new File(options.reportDir,"index.html");
        runner.generateIndex();
        assertTrue("index file "+indexFile.getAbsolutePath()+" does not exist", indexFile.exists());
        assertTrue("index file "+indexFile.getAbsolutePath()+" is not a file", indexFile.isFile());
    }
    
    @Test
    public void findAllXSpecsTests() throws Exception {
        RunnerOptions runnerOptions = new RunnerOptions(getProjectDirectory());
        runnerOptions.testDir = new File(getTestDirectory(), "xsltTestCase");
        XSpecRunner runner = getNewRunner(new SaxonOptions(), runnerOptions);
        List<File> xspecFiles = runner.findAllXSpecs();
        assertEquals("wrong number of XSpecFiles found", 1, xspecFiles.size());
        assertEquals("wrong file found", "xsl1.xspec", xspecFiles.get(0).getName());
    }

    @Test
    public void findAllXSpecsTestsNonExistingDir() throws Exception {
        RunnerOptions runnerOptions = new RunnerOptions(getProjectDirectory());
        runnerOptions.testDir = new File(getTestDirectory(), "unexisting");
        XSpecRunner runner = getNewRunner(new SaxonOptions(), runnerOptions);
        List<File> xspecFiles = runner.findAllXSpecs();
        assertEquals("wrong number of XSpecFiles found", 0, xspecFiles.size());
    }

    @Test
    public void findAllXSpecsTestsDirWithoutXspecFiles() throws Exception {
        RunnerOptions runnerOptions = new RunnerOptions(getProjectDirectory());
        runnerOptions.testDir = new File(getTestDirectory(), "samples");
        XSpecRunner runner = getNewRunner(new SaxonOptions(), runnerOptions);
        List<File> xspecFiles = runner.findAllXSpecs();
        assertEquals("wrong number of XSpecFiles found", 0, xspecFiles.size());
    }

    @Test
    public void verifyFailingXSpecIsReported() throws Exception {
        RunnerOptions runnerOptions = new RunnerOptions(getProjectDirectory());
        runnerOptions.testDir = new File(getTestDirectory(), "failingXslt");
        XSpecRunner runner = getNewRunner(new SaxonOptions(), runnerOptions);
        List<File> xspecFiles = runner.findAllXSpecs();
        assertEquals("wrong number of XSpecFiles found", 2, xspecFiles.size());
        try {
            runner.execute();
            fail("Should have given an error");
        } catch (XSpecPluginException ex) {
            // XSpec test should be missed
        }
        runner.generateIndex();
        File surefireReport = new File(getProjectDirectory(), "target/surefire-reports/TEST-xsl1.xspec.xml");
        assertTrue("Surefire report doesn't exist", surefireReport.exists());
        assertTrue("Surefire report is not a file", surefireReport.isFile());
        assertFalse("Surefire report should report on failing test",
                Files.readString(surefireReport.toPath()).contains("<testsuites/>"));
        File surefireReport2 = new File(getProjectDirectory(), "target/surefire-reports/TEST-xsl2.xspec.xml");
        assertTrue("Surefire report 2 doesn't exist", surefireReport2.exists());
        assertTrue("Surefire report 2 is not a file", surefireReport2.isFile());
        assertFalse("Surefire report 2 should report on failing test",
                Files.readString(surefireReport2.toPath()).contains("<testsuites/>"));
    }

    private XSpecRunner getNewRunner(SaxonOptions saxonOptions, RunnerOptions runnerOptions) throws IllegalStateException, XSpecPluginException, MalformedURLException, URISyntaxException {
        XSpecRunner runner = new XSpecRunner(getLog(), getBaseDirectory());
        runner.setResources(
                new DefaultXSpecImplResources(), 
                new DefaultSchematronImplResources(), 
                new DefaultXSpecPluginResources());
        runner.setEnvironment(new Properties(), runnerOptions);
        runner.init(saxonOptions);
        return runner;
    }
    
    private XSpecRunner getNewRunner(SaxonOptions saxonOptions) throws IllegalStateException, XSpecPluginException, MalformedURLException, URISyntaxException {
       return  getNewRunner(saxonOptions, new RunnerOptions(getBaseDirectory()));
    }
}
