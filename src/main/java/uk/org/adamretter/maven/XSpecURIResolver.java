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
import java.io.InputStream;
import java.net.URI;

/**
 * If Saxon fails to find a resource by URI
 * we delegate to our Resource Resolver
 * which will also look up the resource in the
 * classpath and filesystem
 *
 * @author <a href="mailto:adam.retter@googlemail.com">Adam Retter</a>
 */
public class XSpecURIResolver extends StandardURIResolver {

    private final ResourceResolver resourceResolver;
    private final LogProvider logProvider;

    public XSpecURIResolver(final LogProvider logProvider, final ResourceResolver resourceResolver) {
        this.logProvider = logProvider;
        this.resourceResolver = resourceResolver;
    }

    @Override
    public Source resolve(final String href, final String base) throws XPathException {
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
                getLogProvider().getLog().warn("Could not Resolve URI for XSpec!");
                return null;
            }
        }
    }

    protected LogProvider getLogProvider() {
        return logProvider;
    }
}
