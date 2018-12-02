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

import io.xspec.maven.xspecMavenPlugin.TestCatalogWriterExtender;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Properties;
import net.sf.saxon.Configuration;
import net.sf.saxon.s9api.DocumentBuilder;
import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.XPathCompiler;
import net.sf.saxon.s9api.XPathSelector;
import net.sf.saxon.s9api.XdmItem;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.XdmValue;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 * Tests CatalogWriter
 * @author cmarchand
 */
public class CatalogWriterTest {
    
    private static Processor proc;
    private static DocumentBuilder docBuilder;
    
    private CatalogWriter getWriter() throws URISyntaxException, MalformedURLException, XSpecPluginException {
        URL url = CatalogWriter.class.getClassLoader().getResource("xspec-maven-plugin.properties");
        File classesDir = new File(url.toURI()).getParentFile();
        String classesUri = classesDir.toURI().toURL().toExternalForm();
        return new CatalogWriter(this.getClass().getClassLoader(), new TestCatalogWriterExtender(classesUri));
    }
    
    private static Processor getProc() {
        if(proc==null) {
            Configuration config = Configuration.newConfiguration();
            proc = new Processor(config);
        }
        return proc;
    }
    private static DocumentBuilder getDocBuilder() {
        if(docBuilder==null) {
            docBuilder = getProc().newDocumentBuilder();
        }
        return docBuilder;
    }
    
    @Test
    public void testNoNextCatalog() throws Exception {
        CatalogWriter writer = getWriter();
        File ret = writer.writeCatalog(null, null, true);
        XdmNode document = getDocBuilder().build(ret);
        XPathCompiler compiler = getProc().newXPathCompiler();
        compiler.declareNamespace("cat", "urn:oasis:names:tc:entity:xmlns:xml:catalog");
        XPathSelector sel = compiler.compile("/cat:catalog/cat:nextCatalog").load();
        sel.setContextItem(document);
        XdmValue result = sel.evaluate();
        assertTrue("A nextCatalog has been found, it shouldn't", result.size()==0);
    }
    
    @Test
    public void testHasNextCatalog()  throws Exception {
        CatalogWriter writer = getWriter();
        File ret = writer.writeCatalog("http://fake.org/catalog.xml", new Properties(), true);
        XdmNode document = getDocBuilder().build(ret);
        XPathCompiler compiler = getProc().newXPathCompiler();
        compiler.declareNamespace("cat", "urn:oasis:names:tc:entity:xmlns:xml:catalog");
        XPathSelector sel = compiler.compile("/cat:catalog/cat:nextCatalog").load();
        sel.setContextItem(document);
        XdmValue result = sel.evaluate();
        assertTrue("No nextCatalog has been found, it should", result.size()==1);
        XdmItem item = result.itemAt(0);
        assertTrue("nextCatalog found is not a node", item instanceof XdmNode);
        XdmNode node = (XdmNode)item;
        String catalog = node.getAttributeValue(new QName("catalog"));
        assertEquals("nextCatalog has a wrong value", "http://fake.org/catalog.xml", catalog);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testIllegalArgumentException() throws Exception {
        CatalogWriter writer = getWriter();
        File ret = writer.writeCatalog("http://fake.org/catalog.xml", null, true);
        fail("A IllegalArgumentException is expected if userCatalogFilename is not null and environment is null");
    }
    
}
