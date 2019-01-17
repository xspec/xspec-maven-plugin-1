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
import java.net.URISyntaxException;
import java.util.List;
import java.util.Properties;
import net.sf.saxon.Configuration;
import net.sf.saxon.s9api.Processor;
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
        Processor proc = new Processor(Configuration.newConfiguration());
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
                new Properties(), 
                newExtender());
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
}
