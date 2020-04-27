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
package io.xspec.maven.xspecMavenPlugin.resolver;

import io.xspec.maven.xspecMavenPlugin.utils.QuietLogger;
import io.xspec.maven.xspecMavenPlugin.utils.XMP_XMLReader;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;
import javax.xml.transform.sax.SAXSource;
import org.apache.maven.plugin.logging.Log;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xmlresolver.Catalog;
import org.xmlresolver.CatalogSource;

/**
 * A URI resolver that relies actually on catalogs, but can fall back on saxon.
 * Saxon URI Resolver should never be used.
 * @author cmarchand
 */
public class Resolver implements javax.xml.transform.URIResolver, EntityResolver {
    private final URIResolver saxonResolver;
    private final Log log;
    org.xmlresolver.Resolver cr;
    private static final boolean LOG_ENABLE = true;
    
    /**
     * Creates a new URI resolver, based on a catalog file and a saxon URI Resolver
     * @param saxonResolver
     * @param catalog 
     * @param log 
     */
    public Resolver(final URIResolver saxonResolver, final File catalog, final Log log) {
        super();
        this.saxonResolver=saxonResolver;
        this.log=log;
        // issue #11 : Resolver() initializes and uses a static Catalog. We must not do this
        cr = new org.xmlresolver.Resolver(new Catalog());
        cr.getCatalog().addSource(new CatalogSource.UriCatalogSource(catalog.toURI().toString()));
    }

    @Override
    public Source resolve(String href, String base) throws TransformerException {
        getLog().info(String.format("resolve(%s,%s)", href, base));
        getLog().debug("catalogList="+cr.getCatalog().catalogList());
        getLog().debug("Trying catalog");
        try {
            Source source = cr.resolve(href, base);
            getLog().debug("source is "+(source==null ? "" : "not ")+"null");
            if(source!=null && source.getSystemId()!=null) {
                getLog().debug(String.format("resolved from catalog to %s", source.getSystemId()));
                getLog().debug(String.format("Source is a %s", source.getClass().getName()));
  //              return source;
            } else {
                getLog().debug("Trying saxon");
                source = saxonResolver.resolve(href, base);
                if(source!=null && source.getSystemId()!=null) {
                    getLog().debug(String.format("resolved from saxon to %s", source.getSystemId()));
//                    return source;
                } else {
                    getLog().error(String.format("fail to resolve (%s, %s)", href, base));
                    return null;
                }
            }
            if(source instanceof SAXSource) {
                String systemId = source.getSystemId();
                source = new SAXSource(new XMP_XMLReader(), new InputSource(new URL(systemId).openStream()));
                getLog().warn("comparing resolvers: "+(XMP_XMLReader.commonResolver == cr));
                source.setSystemId(systemId);
            }
            return source;
        } catch(TransformerException ex) {
            getLog().error("Resolver.resolve("+href+","+base+")", ex);
            throw ex;
        } catch(ParserConfigurationException| SAXException | IOException ex) {
            getLog().error("Resolver.resolve("+href+","+base+")", ex);
            throw new TransformerException(ex);
        }
    }
    
    private Log getLog() {
        if(LOG_ENABLE) return log;
        return QuietLogger.getLogger();
    }

    @Override
    public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
        InputSource ret = cr.resolveEntity(publicId, systemId);
        getLog().debug(String.format("resolveEntity(%s,%s)->%s", publicId, systemId, ret.getSystemId()));
        return ret;
    }
    
    /**
     * Returns underlying org.xmlresolver.Resolver.
     * @return 
     */
    public org.xmlresolver.Resolver getCr() {
        return cr;
    }
    
}
