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
package io.xspec.maven.xspecMavenPlugin.resources.impl;

import io.xspec.maven.xspecMavenPlugin.resources.XSpecPluginResources;

/**
 * Default implementation
 * @author cmarchand
 */
public class DefaultXSpecPluginResources implements XSpecPluginResources {
    
    private String junitAggregator;
    private String dependencyScanner;
    private final String imageDown;
    private final String imageUp;
    private final String imageChangerXsl;
    
    public DefaultXSpecPluginResources() {
        super();
        this.junitAggregator = LOCAL_PREFIX+"io/xspec/maven/xspec-maven-plugin/junit-aggregator.xsl";
        this.dependencyScanner = XML_UTILITIES_PREFIX+"org/mricaud/xml-utilities/get-xml-file-static-dependency-tree.xsl";
        this.imageDown = LOCAL_PREFIX+"reporter/3angle-down.gif";
        this.imageUp = LOCAL_PREFIX+"reporter/3angle-right.gif";
        this.imageChangerXsl = LOCAL_PREFIX+"io/xspec/maven/xspec-maven-plugin/reporter/folding-reporter-inline-image.xsl";
    }

    @Override
    public String getJunitAggregatorUri() {
        return junitAggregator;
    }

    public void setJunitAggregator(String junitAggregator) {
        this.junitAggregator = junitAggregator;
    }

    @Override
    public String getDependencyScannerUri() {
        return dependencyScanner;
    }

    public void setDependencyScanner(String dependencyScanner) {
        this.dependencyScanner = dependencyScanner;
    }

    @Override
    public String getImageDown() {
        return imageDown;
    }

    @Override
    public String getImageUp() {
        return imageUp;
    }

    @Override
    public String getXsltImageChanger() {
        return imageChangerXsl;
    }

}
