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

import io.xspec.maven.xspecMavenPlugin.resources.XSpecImplResources;
import io.xspec.maven.xspecMavenPlugin.resources.XSpecPluginResources;
import io.xspec.maven.xspecMavenPlugin.resources.impl.DefaultSchematronImplResources;
import io.xspec.maven.xspecMavenPlugin.utils.extenders.CatalogWriterExtender;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.Properties;
import javanet.staxutils.IndentingXMLStreamWriter;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import top.marchand.java.classpath.utils.ClasspathException;
import top.marchand.java.classpath.utils.ClasspathUtils;

/**
 * This class writes catalog catalog
 * @author cmarchand
 */
public class CatalogWriter {
    private final ClasspathUtils cu;
    private CatalogWriterExtender catalogWriterExtender;
    
    public CatalogWriter(ClassLoader cl) throws XSpecPluginException {
        super();
        try {
            cu = new ClasspathUtils(cl);
        } catch(ClasspathException ex) {
            throw new XSpecPluginException("while creating catalogBuilder", ex);
        }
    }
    public CatalogWriter(ClassLoader cl, CatalogWriterExtender ext) throws XSpecPluginException {
        this(cl);
        this.catalogWriterExtender = ext;
    }
        
    /**
     * Generates and write a catalog tha t resolves all resources for XSpec,
     * schematron and plugin implementation. If XSpec execution requires a user
     * defined catalog, it may be specified in <tt>userCatalogFileName</tt>.<br/>
     * <tt>userCatalogFileName</tt> may be null ; no <tt>&lt;nextCatalog&gt;</tt> 
     * entry will be added to generated catalog.<br/>
     * If a non null value is provided for <tt>userCatalogFileName</tt>, then
     * a non null value <strong>must</strong> be provided for <tt>environment</tt> ;
     * else, a  IllegalArgumentException will be thrown.
     * @param userCatalogFilename The nextCatalog URI to add to generated catalog.
     * @param environment Environment properties, used to resolve all placeholders
     * in <tt>userCatalogFileName</tt>.
     * @param keepGeneratedCatalog Indicates if generated catalog must be kept after
     * plugin execution. If <tt>false</tt>, generated catalog will be deleted at
     * JVM exit.
     * @return Generated file
     * @throws XSpecPluginException
     * @throws IOException 
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
            // io.xspec / xspec
            if(catalogWriterExtender!=null) {
                catalogWriterExtender.beforeWrite(this, cu);
            }
            String jarUri = cu.getArtifactJarUri("io.xspec", "xspec");
//            System.err.println("io.xspec jarUri="+jarUri);
            writeCatalogEntry(xmlWriter, jarUri, XSpecImplResources.XSPEC_PREFIX);
            // com.schematron / iso-schematron
            writeCatalogEntry(xmlWriter, jarUri, DefaultSchematronImplResources.SCHEMATRON_PREFIX);
            // io.xspec / xspec-maven-plugin
            jarUri = cu.getArtifactJarUri("org.mricaud.xml", "xut");
//            System.err.println("org.mricaud.xml jarUri="+jarUri);
            writeCatalogEntry(xmlWriter, jarUri, XSpecPluginResources.XML_UTILITIES_PREFIX);
            // io.xspec.maven / xspec-maven-plugin
            jarUri = cu.getArtifactJarUri("io.xspec.maven", "xspec-maven-plugin");
//            System.err.println("io.xspec.maven jarUri="+jarUri);
            writeCatalogEntry(xmlWriter, jarUri, XSpecPluginResources.LOCAL_PREFIX);
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
            if(catalogWriterExtender!=null) {
                catalogWriterExtender.afterWrite(this, cu);
            }
            osw.flush();
//            System.err.println("catalog written");
        } catch(XMLStreamException | ClasspathException ex) {
            System.err.println("while creating catalog, exception thrown: "+ex.getClass().getName());
            throw new XSpecPluginException("while creating catalog", ex);
        } catch(NullPointerException ex) {
            System.err.println("while creating catalog, NPE thrown: "+ex.getClass().getName());
            throw new XSpecPluginException("while creating catalog", ex);
        }
        if(!keepGeneratedCatalog) tmpCatalog.deleteOnExit();
        return tmpCatalog;
    }


    /**
     * Writes a catalog entries for a jar and a URI prefix
     * @param xmlWriter
     * @param jarUri
     * @param prefix
     * @throws XMLStreamException 
     */
    private void writeCatalogEntry(final XMLStreamWriter xmlWriter, final String jarUri, String prefix) throws XMLStreamException {
        xmlWriter.writeEmptyElement("rewriteURI");
        xmlWriter.writeAttribute("uriStartString", prefix);
        xmlWriter.writeAttribute("rewritePrefix", jarUri);
        xmlWriter.writeEmptyElement("rewriteSystem");
        xmlWriter.writeAttribute("uriStartString", prefix);
        xmlWriter.writeAttribute("rewritePrefix", jarUri);
    }
}
