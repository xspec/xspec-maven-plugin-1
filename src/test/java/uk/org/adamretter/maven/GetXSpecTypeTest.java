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
package uk.org.adamretter.maven;

import java.io.ByteArrayInputStream;
import java.net.MalformedURLException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamSource;
import net.sf.saxon.Configuration;
import net.sf.saxon.s9api.DocumentBuilder;
import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmNode;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Tests the getXSpecType from XSPecMojo
 * @author cmarchand
 */
public class GetXSpecTypeTest {
    
    private static XSpecMojo mojo;
    
    @BeforeClass
    public static void beforeClass() {
        mojo = new XSpecMojo();
        mojo.uriResolverSet = true;
        try {
            mojo.prepareXmlUtilities();
        } catch(NullPointerException | MalformedURLException | TransformerException | SaxonApiException | MojoExecutionException | MojoFailureException ex) {
            // a lot of ignorable, as we do not need them for the test
        }
    }
    
    @Test
    public void testSchematron() throws SaxonApiException {
        String document = "<x:description xmlns:x='http://www.jenitennison.com/xslt/xspec' "+
                "schematron='pouet.sch'><x:content/></x:description>";
        XdmNode doc = mojo.xmlStuff.getDocumentBuilder().build(new StreamSource(new ByteArrayInputStream(document.getBytes())));
        Assert.assertEquals(XSpecMojo.XSpecType.SCH, mojo.getXSpecType(doc));
    }
    @Test
    public void testXQueryAt() throws SaxonApiException {
        String document = "<x:description xmlns:x='http://www.jenitennison.com/xslt/xspec' "+
                "query-at='top:marchand:xml:xspec:xquery'><x:content/></x:description>";
        XdmNode doc = mojo.xmlStuff.getDocumentBuilder().build(new StreamSource(new ByteArrayInputStream(document.getBytes())));
        Assert.assertEquals(XSpecMojo.XSpecType.XQ, mojo.getXSpecType(doc));
    }
    @Test
    public void testXQuery() throws SaxonApiException {
        String document = "<x:description xmlns:x='http://www.jenitennison.com/xslt/xspec' "+
                "query='pouet.xq'><x:content/></x:description>";
        XdmNode doc = mojo.xmlStuff.getDocumentBuilder().build(new StreamSource(new ByteArrayInputStream(document.getBytes())));
        Assert.assertEquals(XSpecMojo.XSpecType.XQ, mojo.getXSpecType(doc));
    }
    @Test
    public void testXslt() throws SaxonApiException {
        String document = "<x:description xmlns:x='http://www.jenitennison.com/xslt/xspec' "+
                "stylesheet='pouet.xslt'><x:content/></x:description>";
        XdmNode doc = mojo.xmlStuff.getDocumentBuilder().build(new StreamSource(new ByteArrayInputStream(document.getBytes())));
        Assert.assertEquals(XSpecMojo.XSpecType.XSL, mojo.getXSpecType(doc));
    }
    
}
