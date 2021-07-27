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
package io.xspec.maven.xspecMavenPlugin;

import io.xspec.maven.xspecMavenPlugin.utils.CatalogWriter;
import io.xspec.maven.xspecMavenPlugin.utils.extenders.CatalogWriterExtender;
import top.marchand.java.classpath.utils.ClasspathException;
import top.marchand.java.classpath.utils.ClasspathUtils;

/**
 *
 * @author cmarchand
 */
public class TestCatalogWriterExtender implements CatalogWriterExtender {
    
    private final String classesUri;
    
    public TestCatalogWriterExtender(String classesUri) {
        super();
        this.classesUri=classesUri;
    }
    
    @Override
    public void beforeWrite(CatalogWriter writer) {
/*
        cu.setCallback((String groupId, String artifactId) -> {
            System.err.println("callback call with ("+groupId+","+artifactId+")");
            if("io.xspec.maven".equals(groupId) && "xspec-maven-plugin".equals(artifactId)) {
                return classesUri;
            } else if("com.schematron".equals(groupId) && "iso-schematron".equals(artifactId)) {
                return cu.getArtifactJarUri("io.xspec", "xspec");
            } else {
                throw new ClasspathException("No resource found for ("+groupId+","+artifactId+") in callback");
            }
        });
*/
    }
    
    @Override
    public void afterWrite(CatalogWriter writer) {
        //cu.removeCallback();
    }
}
