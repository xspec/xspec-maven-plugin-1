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
package io.xspec.maven.xspecMavenPlugin.utils;

import io.xspec.maven.xspecMavenPlugin.TestUtils;
import io.xspec.maven.xspecMavenPlugin.resources.impl.DefaultSchematronImplResources;
import io.xspec.maven.xspecMavenPlugin.resources.impl.DefaultXSpecImplResources;
import io.xspec.maven.xspecMavenPlugin.resources.impl.DefaultXSpecPluginResources;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.List;
import java.util.Properties;
import net.sf.saxon.Configuration;
import net.sf.saxon.s9api.Axis;
import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.XdmItem;
import net.sf.saxon.s9api.XdmNode;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.junit.Test;
import static org.junit.Assert.*;
import top.marchand.maven.saxon.utils.SaxonOptions;

/**
 * Tests XSpecCompiler
 * @author cmarchand
 */
public class XSpecCompilerTest extends TestUtils {
    private final XmlStuff stuff;
    private final RunnerOptions runnerOptions;
    
    public XSpecCompilerTest() throws URISyntaxException, XSpecPluginException, MalformedURLException {
        super();
        Configuration saxonConfiguration = Configuration.newConfiguration();
        Processor proc = new Processor(saxonConfiguration);
        runnerOptions = new RunnerOptions(getBaseDirectory());
        runnerOptions.reportDir = new File(getBaseDirectory(), "target/xspec-reports");
        runnerOptions.testDir = new File(getProjectDirectory(), "src/test/resources/filesToTest/");
        stuff = new XmlStuff(
                proc, 
                new SaxonOptions(), 
                getLog(), 
                new DefaultXSpecImplResources(), 
                new DefaultXSpecPluginResources(), 
                new DefaultSchematronImplResources(), 
                getBaseDirectory(), 
                runnerOptions, 
                new Properties());
    }
    
    @Test
    public void getCompiledSchematronPathTest() throws Exception {
        XSpecCompiler compiler = new XSpecCompiler(stuff,runnerOptions,getLog());
        File schematronFile = new File(runnerOptions.testDir, "schematronTestCase/schematron1.sch");
        File ret = compiler.getCompiledSchematronPath(runnerOptions.reportDir, schematronFile);
        File expected = new File(runnerOptions.reportDir, "schematronTestCase/schematron1.sch/schematron/schematron1.sch.xslt");
        assertEquals(expected.getAbsolutePath(), ret.getAbsolutePath());
    }
    
    @Test
    public void getCompiledXSpecPathTest() throws Exception {
        XSpecCompiler compiler = new XSpecCompiler(stuff,runnerOptions,getLog());
        File xspec = new File(runnerOptions.testDir, "xsltTestCase/xsl1.xspec");
        File ret = compiler.getCompiledXSpecPath(runnerOptions.reportDir, xspec);
        File expected = new File(runnerOptions.reportDir, "xsltTestCase/xsl1.xspec/xslt/xsl1.xspec.xslt");
        assertEquals(expected.getAbsolutePath(), ret.getAbsolutePath());
    }
    
    @Test
    public void getCompiledXspecSchematronPathTest() throws Exception {
        XSpecCompiler compiler = new XSpecCompiler(stuff,runnerOptions,getLog());
        File xspec = new File(runnerOptions.testDir, "schematronTestCase/schematron1.xspec");
        File ret = compiler.getCompiledXspecSchematronPath(runnerOptions.reportDir, xspec);
        File expected = new File(runnerOptions.reportDir, "schematronTestCase/schematron1.xspec/schematron1/schematron1.xspec-compiled.xspec");
        assertEquals(expected.getAbsolutePath(), ret.getAbsolutePath());
    }
    
    @Test
    public void getFilesToDeleteTest() throws Exception {
        XSpecCompiler compiler = new XSpecCompiler(stuff,runnerOptions,getLog());
        List<File> files = compiler.getFilesToDelete();
        assertNotNull(files);
        assertTrue(files.isEmpty());
    }
    
    @Test
    public void getCoverageTempPathTest() throws Exception {
        XSpecCompiler compiler = new XSpecCompiler(stuff,runnerOptions,getLog());
        File xspec = new File(runnerOptions.testDir, "schematronTestCase/schematron1.xspec");
        File ret = compiler.getCoverageTempPath(runnerOptions.reportDir, xspec);
        File expected = new File(runnerOptions.reportDir, "schematronTestCase/schematron1.xspec/coverage-schematron1.xml");
        assertEquals(expected.getAbsolutePath(),ret.getAbsolutePath());
    }
    
    @Test
    public void getCoverageFinalPathTest() throws Exception {
        XSpecCompiler compiler = new XSpecCompiler(stuff,runnerOptions,getLog());
        File xspec = new File(runnerOptions.testDir, "schematronTestCase/schematron1.xspec");
        File ret = compiler.getCoverageFinalPath(runnerOptions.reportDir, xspec);
        File expected = new File(runnerOptions.reportDir, "schematronTestCase/schematron1.xspec/schematron1-coverage.html");
        assertEquals(expected.getAbsolutePath(),ret.getAbsolutePath());
    }
    
    @Test
    public void copyFileTest() throws Exception {
        XSpecCompiler compiler = new XSpecCompiler(stuff,runnerOptions,getLog());
        File xspec = new File(runnerOptions.testDir, "schematronTestCase/schematron1.xspec");
        File resultBase = new File(runnerOptions.reportDir, "schematronTestCase/schematron1.xspec/");
        compiler.copyFile(xspec.toURI().toURL().toExternalForm(), "included.xsl", resultBase);
        File expected = new File(runnerOptions.reportDir, "schematronTestCase/included.xsl");
        assertTrue("File does not exist: "+expected.getAbsolutePath(), expected.exists());
    }
    
    @Test
    public void copyFileWithDotDotTest() throws Exception {
        // This test case is for XSpec that references a expect or a source file that is not in src/test/xspec
        // In this example, we have this :
        // <x:context href="../samples/sample.xml" />
        XSpecCompiler compiler = new XSpecCompiler(stuff, runnerOptions, getLog());
        File xspec = new File(runnerOptions.testDir, "schematronTestCase/schematron1.xspec");
        File resultBase = new File(runnerOptions.reportDir, "schematronTestCase/schematron1.xspec/");
        compiler.copyFile(xspec.toURI().toURL().toExternalForm(), "../samples/sample.xml", resultBase);
        // With File, if path contains ../foo, it is not normalized, and file is declared as not exist, but it does
        File expected = new File(runnerOptions.reportDir, "schematronTestCase/../samples/sample.xml").toPath().normalize().toFile();
        assertTrue("File does not exist: "+expected.getAbsolutePath(), expected.exists());
    }
    
    @Test
    public void prepareSchematronDocumentTest() throws Exception {
        XdmNode xspecDoc = stuff.getDocumentBuilder().build(new File(getTestDirectory(), "schematronTestCase/schematron2.xspec"));
        XSpecCompiler compiler = new XSpecCompiler(stuff, runnerOptions, getLog());
        XdmNode ret = compiler.prepareSchematronDocument(xspecDoc);
        Assertions.assertThat(ret).isNotNull();
        // Vérification des fichiers créés
        File workDir = new File(runnerOptions.reportDir, "schematronTestCase/schematron2.xspec");
        File schCompiledAsXsl = new File(workDir, "schematron/schematron2.xspec.xslt");
        File compiledXspec = new File(workDir, "schematron2/schematron2.xspec-compiled.xspec");
        SoftAssertions softAssertions = new SoftAssertions();
        softAssertions.assertThat(workDir).as(workDir.getAbsolutePath()+" does not exists").exists();
        softAssertions.assertThat(workDir).as(workDir.getAbsolutePath()+" is not a directory").isDirectory();
        softAssertions.assertThat(schCompiledAsXsl).as(schCompiledAsXsl.getAbsolutePath()+" does not exist").exists();
        softAssertions.assertThat(schCompiledAsXsl).as(schCompiledAsXsl.getAbsolutePath()+" is not a regular file").isFile();
        softAssertions.assertThat(compiledXspec).as(compiledXspec.getAbsolutePath()+" does not exist").exists();
        softAssertions.assertThat(compiledXspec).as(compiledXspec.getAbsolutePath()+" is not a regular file").isFile();
        softAssertions.assertAll();
        // now check the generated XSpec points to generated XSL
        XdmNode generatedXspec = stuff.getDocumentBuilder().build(compiledXspec);
        XdmItem rootItem = generatedXspec.axisIterator(Axis.CHILD, new QName(XSpecCounterContentHandler.XSPEC_NS, "description")).next();
        XdmNode rootNode = (XdmNode)rootItem;
        XdmItem attrItem = rootNode.axisIterator(Axis.ATTRIBUTE, new QName("stylesheet")).next();
        String fileUri = attrItem.getStringValue();
        URI uri = new URI(fileUri);
        getLog().debug("uri: "+uri.toString());
        File referencedFile = new File(uri);
        Assertions
                .assertThat(Files.isSameFile(schCompiledAsXsl.toPath(), referencedFile.toPath()))
                .as("generated XSpec does not reference generated XSLT")
                .isTrue();
    }
}
