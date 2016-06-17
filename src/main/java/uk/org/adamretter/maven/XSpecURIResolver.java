/**
 * Copyright © 2013, Adam Retter
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

import net.sf.saxon.lib.StandardURIResolver;
import net.sf.saxon.trans.XPathException;

import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;
import org.apache.xml.resolver.CatalogManager;
import org.apache.xml.resolver.tools.CatalogResolver;

/**
 * If Saxon fails to find a resource by URI
 * we delegate to our Resource Resolver
 * which will also look up the resource in the
 * classpath and filesystem
 *
 * @author <a href="mailto:adam.retter@googlemail.com">Adam Retter</a>
 * @author <a href="mailto:christophe.marchand@contactoffice.net">Christophe Marchand</a>
 */
public class XSpecURIResolver extends StandardURIResolver {

    private final ResourceResolver resourceResolver;
    private final LogProvider logProvider;
    private final URIResolver catalogResolver;

    public XSpecURIResolver(final LogProvider logProvider, final ResourceResolver resourceResolver, File catalogFile) {
        this.logProvider = logProvider;
        this.resourceResolver = resourceResolver;
	if(resourceResolver==null) {
		System.err.println("resourceResolver is null !!!");
	}
        URIResolver tmpCatalogResolver = null;
        if(catalogFile!=null && catalogFile.exists()) {
            CatalogManager cm = new CatalogManager();
            CatalogResolver cr = new CatalogResolver(cm);
            try {
                cr.getCatalog().parseCatalog(catalogFile.toURI().toURL());
                tmpCatalogResolver = cr;
            } catch (IOException ex) {
                getLogProvider().getLog().error(ex.getMessage(), ex);
            }
        }
        catalogResolver = tmpCatalogResolver;
    }

    @Override
    public Source resolve(final String href, final String base) throws XPathException {
	getLogProvider().getLog().debug(String.format("resolve(%s,%s)",href,base));
        final Source saxonSource = super.resolve(href, base);
        if(saxonSource != null && new File(saxonSource.getSystemId()).exists()) {
            getLogProvider().getLog().debug(String.format("Saxon Resolved URI href=%s ,base=%s to %s", href, base, saxonSource));
            return saxonSource;
        } else {
            getLogProvider().getLog().debug(String.format("Saxon failed URIResolution for: %s", saxonSource.getSystemId()));

            final String path = URI.create(saxonSource.getSystemId()).getPath();
            getLogProvider().getLog().debug(String.format("Attempting URIResolution for XSpec from: %s", path));

            final InputStream is = resourceResolver.getResource(path);
            if(is != null) {
                final StreamSource xspecSource = new StreamSource(is);
                xspecSource.setSystemId(path);
                return xspecSource;
            } else {
                if(catalogResolver!=null) {
                    getLogProvider().getLog().debug(String.format("Attempting catalog resolution for %s", path));
                    try {
                        Source ret = catalogResolver.resolve(href, base);
			if(ret==null) {
				getLogProvider().getLog().warn(String.format("Resolution failed for %s %s",href, base));
			}
			return ret;
                    } catch(TransformerException ex) {
                        getLogProvider().getLog().warn("Could not Resolve URI for XSpec!");
                        return null;
                    }
                } else {
                    getLogProvider().getLog().warn("Could not Resolve URI for XSpec!");
                    return null;
                }
            }
        }
    }

    protected final LogProvider getLogProvider() {
        return logProvider;
    }
}
