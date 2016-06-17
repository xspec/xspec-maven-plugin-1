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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

/**
 * Utility class for resolving resources
 *
 * @author <a href="mailto:adam.retter@googlemail.com">Adam Retter</a>
 */
public class ResourceResolver {

    private final LogProvider logProvider;

    public ResourceResolver(final LogProvider logProvider) {
        this.logProvider = logProvider;
    }
    
    /**
     * Attempts to resolve a resource path from the classpath and then the filesystem
     *
     * @param path to a resource
     *
     * @return An InputStream for the resource or null if the resource could not be found
     */
    public InputStream getResource(final String path) {

        getLogProvider().getLog().debug("Attempting to resolve resource: " + path);
	if(path==null) return null;

        final InputStream is = getClass().getResourceAsStream(path);
        if(is != null) {
            getLogProvider().getLog().debug("Found resource from classpath: " + path);
            return is;
        } else {
            getLogProvider().getLog().debug("Could not find resource from classpath: " + path);

            final File f = new File(path);
            try {
                if(f.exists()) {
                    getLogProvider().getLog().debug("Found resource from filesystem: " + f.getAbsolutePath());
                    return new FileInputStream(f);
                } else {
                    getLogProvider().getLog().debug("Could not find resource from filesystem: " + f.getAbsolutePath());
                }
            } catch(final FileNotFoundException fnfe) {
                getLogProvider().getLog().debug("Could not find resource from filesystem: " + f.getAbsolutePath(), fnfe);
            }
        }

        return null;
    }

    protected LogProvider getLogProvider() {
        return logProvider;
    }
}
