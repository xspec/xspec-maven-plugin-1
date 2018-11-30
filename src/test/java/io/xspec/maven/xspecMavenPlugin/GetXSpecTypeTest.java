/**
 * Copyright © 2017, Christophe Marchand
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
import io.xspec.maven.xspecMavenPlugin.utils.CatalogWriter;
import io.xspec.maven.xspecMavenPlugin.utils.RunnerOptions;
import java.io.ByteArrayInputStream;
import java.io.File;
import javax.xml.transform.stream.StreamSource;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmNode;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import top.marchand.maven.saxon.utils.SaxonOptions;
import io.xspec.maven.xspecMavenPlugin.utils.XSpecType;
import io.xspec.maven.xspecMavenPlugin.utils.extenders.CatalogWriterExtender;
import java.net.URL;
import java.util.Properties;
import top.marchand.java.classpath.utils.ClasspathException;
import top.marchand.java.classpath.utils.ClasspathUtils;

/**
 * Tests the getXSpecType from XSPecMojo
 * @author cmarchand
 */
public class GetXSpecTypeTest {
    
    private static XSpecRunner runner;
    private static SystemStreamLog log;
    
    private static Log getLog() {
        if(log==null) {
            log = new SystemStreamLog();
        }
        return log;
    }
    
    /**
     * Setting runtime environment. As jar is not in classpath, we can not use 
     * default mecanism to load resources. So we override defalt search mecanism.
     * 
     * <b>Warning</b>, if dependencies change, this code may have to be rewritten.
     * @throws Exception 
     */
    @BeforeClass
    public static void beforeClass() throws Exception {
        File baseDir = new File(".");
        runner = new XSpecRunner(getLog(), baseDir);
        runner.setResources(new DefaultXSpecImplResources(), new DefaultSchematronImplResources(), new DefaultXSpecPluginResources());
        RunnerOptions options = new RunnerOptions(baseDir);
        options.keepGeneratedCatalog = true;
        runner.setEnvironment(new Properties(), options);
        SaxonOptions saxonOptions = new SaxonOptions();
        System.err.println("saxonOptions: "+saxonOptions);
        System.err.println("runner: "+runner);
        URL url = CatalogWriter.class.getClassLoader().getResource("xspec-maven-plugin.properties");
        System.err.println("url="+url!=null?url.toExternalForm():"null");
        File classesDir = new File(url.toURI()).getParentFile();
        String classesUri = classesDir.toURI().toURL().toExternalForm();
        System.err.println("classesUri="+classesUri);
        CatalogWriterExtender extender = new CatalogWriterExtender() {
            @Override
            public void beforeWrite(CatalogWriter writer, ClasspathUtils cu) {
                cu.setCallback((String groupId, String artifactId) -> {
                    System.err.println("callback call with ("+groupId+","+artifactId+")");
                    if("io.xspec.maven".equals(groupId) && "xspec-maven-plugin".equals(artifactId)) {
                        return classesUri;
                    } else if("com.schematron".equals(groupId) && "iso-schematron".equals(artifactId)) {
                        return cu.getArtifactJarUri("io.xspec", "xspec");
                    } else {
                        throw new ClasspathException("No resource found for ("+groupId+","+artifactId+") in callback");
                    }
                });
            }
            @Override
            public void afterWrite(CatalogWriter writer, ClasspathUtils cu) {
                cu.removeCallback();
            }
        };
        runner.setCatalogWriterExtender(extender);
        runner.init(saxonOptions);
    }
    
    @Test
    public void testSchematron() throws SaxonApiException {
        String document = "<x:description xmlns:x='http://www.jenitennison.com/xslt/xspec' "+
                "schematron='pouet.sch'><x:content/></x:description>";
        XdmNode doc = runner.getXmlStuff().getDocumentBuilder().build(new StreamSource(new ByteArrayInputStream(document.getBytes())));
        Assert.assertEquals(XSpecType.SCH, runner.getXSpecType(doc));
    }
    @Test
    public void testXQueryAt() throws SaxonApiException {
        String document = "<x:description xmlns:x='http://www.jenitennison.com/xslt/xspec' "+
                "query-at='top:marchand:xml:xspec:xquery'><x:content/></x:description>";
        XdmNode doc = runner.getXmlStuff().getDocumentBuilder().build(new StreamSource(new ByteArrayInputStream(document.getBytes())));
        Assert.assertEquals(XSpecType.XQ, runner.getXSpecType(doc));
    }
    @Test
    public void testXQuery() throws SaxonApiException {
        String document = "<x:description xmlns:x='http://www.jenitennison.com/xslt/xspec' "+
                "query='pouet.xq'><x:content/></x:description>";
        XdmNode doc = runner.getXmlStuff().getDocumentBuilder().build(new StreamSource(new ByteArrayInputStream(document.getBytes())));
        Assert.assertEquals(XSpecType.XQ, runner.getXSpecType(doc));
    }
    @Test
    public void testXslt() throws SaxonApiException {
        String document = "<x:description xmlns:x='http://www.jenitennison.com/xslt/xspec' "+
                "stylesheet='pouet.xslt'><x:content/></x:description>";
        XdmNode doc = runner.getXmlStuff().getDocumentBuilder().build(new StreamSource(new ByteArrayInputStream(document.getBytes())));
        Assert.assertEquals(XSpecType.XSL, runner.getXSpecType(doc));
    }
    
}
