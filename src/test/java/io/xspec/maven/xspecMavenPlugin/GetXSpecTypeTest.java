/*
 * Copyright © 2017, Christophe Marchand
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
package io.xspec.maven.xspecMavenPlugin;

import io.xspec.maven.xspecMavenPlugin.resources.impl.DefaultSchematronImplResources;
import io.xspec.maven.xspecMavenPlugin.resources.impl.DefaultXSpecImplResources;
import io.xspec.maven.xspecMavenPlugin.resources.impl.DefaultXSpecPluginResources;
import io.xspec.maven.xspecMavenPlugin.utils.RunnerOptions;
import io.xspec.maven.xspecMavenPlugin.utils.XSpecType;
import io.xspec.maven.xspecMavenPlugin.utils.XmlStuff;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmNode;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.assertj.core.api.Assertions;
import org.junit.BeforeClass;
import org.junit.Test;
import top.marchand.maven.saxon.utils.SaxonOptions;

import javax.xml.transform.stream.StreamSource;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.Properties;

/**
 * Tests the getXSpecType from XSpecMojo
 * @author cmarchand
 */
public class GetXSpecTypeTest {

    private static XmlStuff xmlStuff;
    private static SystemStreamLog log;

    private static Log getLog() {
        if (log == null) {
            log = new SystemStreamLog();
        }
        return log;
    }

    @BeforeClass
    public static void beforeClass() throws Exception {
        File baseDir = new File(".");
        RunnerOptions options = new RunnerOptions(baseDir);
        options.keepGeneratedCatalog = true;
        xmlStuff = new XmlStuff(
                new SaxonOptions(),
                getLog(),
                new DefaultXSpecImplResources(),
                new DefaultXSpecPluginResources(),
                new DefaultSchematronImplResources(),
                baseDir,
                options,
                new Properties()
        );
    }

    @Test
    public void given_a_xspec_on_schematron_document_should_return_SCH() throws SaxonApiException {
        // Given
        String document = "<x:description xmlns:x='http://www.jenitennison.com/xslt/xspec' " +
                "schematron='pouet.sch'><x:content/></x:description>";
        XdmNode doc = parseDocument(document);
        // when
        XSpecType actual = xmlStuff.getXSpecType(doc);
        // then
        Assertions.assertThat(actual).isEqualTo(XSpecType.SCH);
    }

    @Test
    public void given_a_xspec_on_xquery_at_should_return_XQ() throws SaxonApiException {
        // Given
        String document = "<x:description xmlns:x='http://www.jenitennison.com/xslt/xspec' " +
                "query-at='top:marchand:xml:xspec:xquery'><x:content/></x:description>";
        XdmNode doc = parseDocument(document);
        // When
        XSpecType actual = xmlStuff.getXSpecType(doc);
        // Then
        Assertions.assertThat(actual).isEqualTo(XSpecType.XQ);
    }

    @Test
    public void given_a_xspec_on_xsquery_should_return_XQ() throws SaxonApiException {
        // Given
        String document = "<x:description xmlns:x='http://www.jenitennison.com/xslt/xspec' " +
                "query='pouet.xq'><x:content/></x:description>";
        XdmNode doc = parseDocument(document);
        // When
        XSpecType actual = xmlStuff.getXSpecType(doc);
        // Then
        Assertions.assertThat(actual).isEqualTo(XSpecType.XQ);
    }

    @Test
    public void given_a_xspec_on_xslt_should_return_XSL() throws SaxonApiException {
        // Given
        String document = "<x:description xmlns:x='http://www.jenitennison.com/xslt/xspec' " +
                "stylesheet='pouet.xslt'><x:content/></x:description>";
        XdmNode doc = parseDocument(document);
        // When
        XSpecType actual = xmlStuff.getXSpecType(doc);
        // Then
        Assertions.assertThat(actual).isEqualTo(XSpecType.XSL);
    }

    private XdmNode parseDocument(String document) throws SaxonApiException {
        return xmlStuff.getDocumentBuilder().build(new StreamSource(new ByteArrayInputStream(document.getBytes())));
    }
}
