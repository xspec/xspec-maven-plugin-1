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
import net.sf.saxon.Configuration;
import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.XsltExecutable;
import net.sf.saxon.s9api.XsltTransformer;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import top.marchand.maven.saxon.utils.SaxonOptions;

import java.io.File;
import java.net.URISyntaxException;
import java.util.Properties;

import static io.xspec.maven.xspecMavenPlugin.TestUtils.getBaseDirectory;
import static io.xspec.maven.xspecMavenPlugin.TestUtils.getProjectDirectory;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
 *
 * @author cmarchand
 */
public class XmlStuffTest {
    private final RunnerOptions runnerOptions;
    private final SaxonOptions saxonOptions;
    private final DefaultSchematronImplResources schematronResources;
    private final DefaultXSpecImplResources xspecResources;
    private final DefaultXSpecPluginResources pluginResources;
    private final File baseDirectory;
    private Log log = new SystemStreamLog();

    public XmlStuffTest() throws URISyntaxException {
        super();
        runnerOptions = new RunnerOptions(getBaseDirectory());
        runnerOptions.reportDir = new File(getBaseDirectory(), "target/xspec-reports");
        runnerOptions.testDir = new File(getProjectDirectory(), "src/test/resources/filesToTest/");
        saxonOptions = new SaxonOptions();
        schematronResources = new DefaultSchematronImplResources();
        xspecResources = new DefaultXSpecImplResources();
        pluginResources = new DefaultXSpecPluginResources();
        baseDirectory = TestUtils.getBaseDirectory();
    }

    @Test
    public void xmlStuff_should_be_instanciated_without_exception() throws XSpecPluginException {
      Assertions.assertThat(new XmlStuff(
          saxonOptions,
          getLog(),
          xspecResources,
          pluginResources,
          schematronResources,
          baseDirectory,
          runnerOptions,
          new Properties())
      ).isNotNull();
    }
    
    @Test(expected = NullPointerException.class)
    public void compileXslTest() throws Exception {
        XmlStuff stuff = new XmlStuff(
                saxonOptions,
                getLog(),
                xspecResources, 
                pluginResources, 
                schematronResources,
                baseDirectory,
                runnerOptions,
                new Properties());
        XsltExecutable ret = stuff.compileXsl(null);
        fail("An exception should have been thrown");
    }

    private Log getLog() {
        return log;
    }

    @Test
    public void getXtReporterTest() throws Exception {
        XmlStuff stuff = new XmlStuff(
                saxonOptions,
                getLog(),
                xspecResources,
                pluginResources,
                schematronResources,
                baseDirectory,
                runnerOptions,
                new Properties());
        XsltTransformer ret = stuff.getXtReporter();
        assertNotNull(ret);
    }

}
