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

import io.xspec.maven.xspecMavenPlugin.utils.CatalogWriter;
import io.xspec.maven.xspecMavenPlugin.utils.LogProvider;
import io.xspec.maven.xspecMavenPlugin.utils.extenders.CatalogWriterExtender;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.logging.SystemStreamLog;

/**
 *
 * @author cmarchand
 */
public class TestUtils implements LogProvider {
    
    private static File baseDirectory;
    private static File projectDirectory;
    private static File testDirectory;
    private static final Log LOG = new SystemStreamLog();
    
    public static File getProjectDirectory() throws URISyntaxException {
        if(projectDirectory==null) {
            projectDirectory = new File(XSpecRunner.class.getClassLoader().getResource("").toURI()).getParentFile().getParentFile();
        }
        return projectDirectory;
    }
    public static File getBaseDirectory() throws URISyntaxException {
        if(baseDirectory==null) {
            baseDirectory = new File(getProjectDirectory(), "target/surefire-reports/tests");
            baseDirectory.mkdirs();
        }
        return baseDirectory;
    }
    public static File getTestDirectory() throws URISyntaxException {
        if(testDirectory==null) {
            testDirectory = new File(getProjectDirectory(), "src/test/resources/filesToTest/");
        }
        return testDirectory;
    }

    @Override
    public Log getLog() {
        return LOG;
    }
    
    public CatalogWriterExtender newExtender() throws URISyntaxException, MalformedURLException {
        URL url = CatalogWriter.class.getClassLoader().getResource("xspec-maven-plugin.properties");
        File classesDir = new File(url.toURI()).getParentFile();
        String classesUri = classesDir.toURI().toURL().toExternalForm();
        return new TestCatalogWriterExtender(classesUri);
    }
}
