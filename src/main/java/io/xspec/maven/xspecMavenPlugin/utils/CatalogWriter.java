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

import io.xspec.maven.xspecMavenPlugin.resources.XSpecPluginResources;
import javanet.staxutils.IndentingXMLStreamWriter;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.Properties;

/**
 * This class writes a catalog
 * @author cmarchand
 */
public class CatalogWriter {

    public CatalogWriter(ClassLoader cl) throws XSpecPluginException {
        super();
    }

    /**
     * Generates and write a catalog that resolves all resources for XSpec,
     * schematron and plugin implementation. If XSpec execution requires a user
     * defined catalog, it may be specified in {@code userCatalogFileName}.
     *
     * {@code userCatalogFileName} may be null ; no {@code &lt;nextCatalog&gt;} 
     * entry will be added to generated catalog.
     *
     * If a non null value is provided for {@code userCatalogFileName}, then
     * a non null value <strong>must</strong> be provided for {@code environment} ;
     * else, a  IllegalArgumentException will be thrown.
     * @param userCatalogFilename The nextCatalog URI to add to generated catalog.
     * @param environment Environment properties, used to resolve all placeholders
     * in {@code userCatalogFileName}.
     * @param keepGeneratedCatalog Indicates if generated catalog must be kept after
     * plugin execution. If {@code false}, generated catalog will be deleted at
     * JVM exit.
     * @return Generated file
     * @throws XSpecPluginException In case of any error
     * @throws IOException In case of I/O error
     */
    public File writeCatalog(String userCatalogFilename, Properties environment, boolean keepGeneratedCatalog) throws XSpecPluginException, IOException, IllegalArgumentException {
        if(userCatalogFilename!=null && environment==null) {
            throw new IllegalArgumentException("If you specify a userCatalogFilename, you must provide a non null environment.");
        }
        File tmpCatalog = File.createTempFile("tmp", "-catalog.xml");
        try (OutputStreamWriter osw = new OutputStreamWriter(new FileOutputStream(tmpCatalog), Charset.forName("UTF-8"))) {
            XMLStreamWriter xmlWriter = XMLOutputFactory.newFactory().createXMLStreamWriter(osw);
            xmlWriter = new IndentingXMLStreamWriter(xmlWriter);
            xmlWriter.writeStartDocument("UTF-8", "1.0");
            xmlWriter.writeStartElement("catalog");
            xmlWriter.setDefaultNamespace(XSpecPluginResources.CATALOG_NS);
            xmlWriter.writeNamespace("", XSpecPluginResources.CATALOG_NS);
            if(userCatalogFilename!=null) {
                xmlWriter.writeEmptyElement("nextCatalog");
                String catalogFilename = org.codehaus.plexus.util.StringUtils.interpolate(userCatalogFilename, environment);
                try {
                    URI uri = new URI(catalogFilename);
                    if(uri.isAbsolute()) {
                        xmlWriter.writeAttribute("catalog", uri.toString());
                    } else {
                        xmlWriter.writeAttribute("catalog", new File(catalogFilename).toURI().toURL().toExternalForm());
                    }
                } catch(MalformedURLException | URISyntaxException | XMLStreamException ex) {
                    xmlWriter.writeAttribute("catalog", new File(catalogFilename).toURI().toURL().toExternalForm());
                }
            }
            xmlWriter.writeEndElement();
            xmlWriter.writeEndDocument();
            osw.flush();
        } catch(XMLStreamException ex) {
            System.err.println("while creating catalog, exception thrown: "+ex.getClass().getName());
            throw new XSpecPluginException("while creating catalog", ex);
        } catch(NullPointerException ex) {
            System.err.println("while creating catalog, NPE thrown: "+ex.getClass().getName());
            throw new XSpecPluginException("while creating catalog", ex);
        }
        if(!keepGeneratedCatalog) {
            tmpCatalog.deleteOnExit();
        }
        return tmpCatalog;
    }

}
