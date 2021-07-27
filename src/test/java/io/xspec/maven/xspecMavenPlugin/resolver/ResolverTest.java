/**
 * Copyright © 2018, Christophe Marchand
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
package io.xspec.maven.xspecMavenPlugin.resolver;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;
import net.sf.saxon.Configuration;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.junit.After;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;


/**
 * Tests Resolver class
 * @author cmarchand
 */
public class ResolverTest {

    private Configuration configuration;
    private URIResolver saxonResolver;
    private Log log = new SystemStreamLog();
    
    @Before
    public void before() {
        configuration = Configuration.newConfiguration();
        saxonResolver = configuration.getURIResolver();
    }
    
    @After
    public void after() {
        saxonResolver = null;
        configuration = null;
    }
    
    @Test
    public void testSaxonResources() throws TransformerException {
        URL catalogUrl = this.getClass().getResource("/catalogs/empty-catalog.xml");
        assertNotNull("/catalogs/empty-catalog.xml not found in classpath", catalogUrl);
        File catalogFile = new File(catalogUrl.getFile());
        Resolver resolver = new Resolver(saxonResolver, catalogFile, log);
        Source saxonResolution = saxonResolver.resolve("http://www.w3.org/2002/xmlspec/dtd/2.10/xmlspec.dtd", null);
        Source dtd = resolver.resolve("http://www.w3.org/2002/xmlspec/dtd/2.10/xmlspec.dtd", null);
        assertNotNull("xmlspec.dtd not resolved", dtd);
        assertEquals("resolver resolution is not saxon resolution", saxonResolution.getSystemId(), dtd.getSystemId());
    }
    
    @Test
    public void given_a_catalog_resolve_should_use_this_catalog() throws TransformerException {
        // Given
        URL catalogUrl = this.getClass().getResource("/catalogs/rewriteUri-catalog.xml");
        URL log4jUrl = this.getClass().getResource("/log4j.properties");
        String log4jUrlWithoutProtocol = log4jUrl.toExternalForm().substring("file:".length());
        File catalogFile = new File(catalogUrl.getFile());
        Resolver resolver = new Resolver(saxonResolver, catalogFile, log);
        // When
        Source log4j = resolver.resolve("dependency:/fake+toto/log4j.properties", null);
        // Then
        SoftAssertions softAssert = new SoftAssertions();
        softAssert.assertThat(log4j.getSystemId()).startsWith("file:/");
        softAssert.assertThat(log4j.getSystemId()).endsWith(log4jUrlWithoutProtocol);
        softAssert.assertAll();
    }

    @Test
    public void given_a_cp_url_resolver_should_resolve_from_classpath() throws IOException, TransformerException {
        // Given
        String toResolver = "cp:/catalogs/rewriteUri-catalog.xml";
        File projectDir = new File(".").getCanonicalFile();
        File testClassesDir = new File(projectDir, "target/test-classes");
        File catalogsDir = new File(testClassesDir, "catalogs");
        File targetFile = new File(catalogsDir, "rewriteUri-catalog.xml");
        URL expected = targetFile.toURI().toURL();
        log.warn(expected.toString());
        URL catalogUrl = this.getClass().getResource("/catalogs/empty-catalog.xml");
        File catalogFile = new File(catalogUrl.getFile());
        Resolver resolver = new Resolver(saxonResolver, catalogFile, log);
        // When
        Source actual = resolver.resolve(toResolver, null);
        // Then
        SoftAssertions softAssertions = new SoftAssertions();
        softAssertions.assertThat(actual).isNotNull();
        softAssertions.assertThat(actual.getSystemId()).isEqualTo(expected.toString());
        softAssertions.assertAll();
    }
}
