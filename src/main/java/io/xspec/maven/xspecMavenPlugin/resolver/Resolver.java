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
import org.apache.maven.plugin.logging.Log;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xmlresolver.ResolverFeature;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;
import javax.xml.transform.stream.StreamSource;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.regex.Pattern;

/**
 * A URI resolver that relies actually on catalogs, but can fall back on saxon.
 * Saxon URI Resolver should never be used.
 * @author cmarchand
 */
public class Resolver implements javax.xml.transform.URIResolver, EntityResolver {
    private final URIResolver saxonResolver;
    private final Log log;
    org.xmlresolver.Resolver cr;
    private static final boolean LOG_ENABLE = false;
    private final Pattern protocolPattern;

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
        protocolPattern = Pattern.compile("^[\\w\\d]+:/.*");
        // issue #11 : Resolver() initializes and uses a static Catalog. We must not do this
        cr = new org.xmlresolver.Resolver();
        cr.getConfiguration().setFeature(
            ResolverFeature.CATALOG_FILES,
            Collections.singletonList(catalog.toURI().toString())
        );
    }

    @Override
    public Source resolve(String href, String base) throws TransformerException {
        getLog().debug(String.format("resolve(%s,%s)", href, base));
        if(isCpProtocol(href, base)) {
            return resolveToClasspath(href, base);
        }
//        getLog().debug("catalogList="+cr.getConfiguration().);
        getLog().debug("Trying catalog");
        try {
            Source source = cr.resolve(href, base);
            getLog().debug("source is "+(source==null ? "" : "not ")+"null");
            if(source!=null && source.getSystemId()!=null) {
                getLog().debug(String.format("resolved from catalog to %s", source.getSystemId()));
                source.setSystemId(normalizeUrl(source.getSystemId()));
                return source;
            } else {
                getLog().debug("Trying saxon");
                source = saxonResolver.resolve(href, base);
                if(source!=null && source.getSystemId()!=null) {
                    getLog().debug(String.format("resolved from saxon to %s", source.getSystemId()));
                    source.setSystemId(normalizeUrl(source.getSystemId()));
                    return source;
                } else {
                    getLog().error(String.format("fail to resolve (%s, %s)", href, base));
                    return null;
                }
            }
        } catch(TransformerException ex) {
            getLog().error("Resolver.resolve("+href+","+base+")", ex);
            throw ex;
        }
    }

    private String normalizeUrl(String systemId) {
        try {
            URL url = new URL(systemId);
            String protocol = url.getProtocol();
            String path = url.getPath();
            String host = url.getHost();
            return new URL(protocol, host, path.replaceAll("\\/\\/", "/")).toExternalForm();
        } catch(MalformedURLException ex) {
            // impossible !
            return systemId;
        }
    }

    private boolean isCpProtocol(String href, String base) {
        return (href!=null && href.startsWith("cp:/")) ||
                (base!=null && base.startsWith("cp:/"));
    }

    private Source resolveToClasspath(String href, String base) {
        String fullUrl = isAbsolute(href) ? href : base+href;
        String path = removeCpPrefix(fullUrl);
        InputStream is = getClass().getResourceAsStream(path);
        if(is==null) {
            return null;
        }
        StreamSource ret = new StreamSource(is);
        String systemId = getClass().getResource(path).toExternalForm();
        log.debug(systemId);
        ret.setSystemId(normalizeUrl(systemId));
        return ret;
    }

    private boolean isAbsolute(String href) {
        return protocolPattern.matcher(href).matches();
    }

    private String removeCpPrefix(String fullUrl) {
        return fullUrl.substring(3);
    }

    private Log getLog() {
        if(LOG_ENABLE) return log;
        return QuietLogger.getLogger();
    }

    @Override
    public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
        return cr.resolveEntity(publicId, systemId);
    }
    
}
