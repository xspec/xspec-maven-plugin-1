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

import io.xspec.maven.xspecMavenPlugin.resources.XSpecImplResources;

/**
 * Default implementation of XSpec resources.
 * @author cmarchand
 */
public class DefaultXSpecImplResources implements XSpecImplResources {

    private String xspecXslCompilerUri;
    private String xspecXQueryCompilerUri;
    private String schematronUnitTestConverter;
    private String xspecReporter;
    private String junitReporter;
    private String xspecCoverageReporter;
    
    public DefaultXSpecImplResources() {
        super();
        xspecXslCompilerUri = XSPEC_PREFIX+"compiler/generate-xspec-tests.xsl";
        xspecXQueryCompilerUri = XSPEC_PREFIX+"compiler/generate-query-tests.xsl";
        schematronUnitTestConverter = XSPEC_PREFIX+"schematron/schut-to-xspec.xsl";
        xspecReporter = XSPEC_PREFIX+"reporter/format-xspec-report.xsl";
        junitReporter = XSPEC_PREFIX+"reporter/junit-report.xsl";
        xspecCoverageReporter = XSPEC_PREFIX+"reporter/coverage-report.xsl";
    }

    @Override
    public String getXSpecXslCompilerUri() { return xspecXslCompilerUri; }

    @Override
    public String getXSpecXQueryCompilerUri() { return xspecXQueryCompilerUri; }

    @Override
    public String getSchematronSchutConverterUri() { return schematronUnitTestConverter; }

    @Override
    public String getXSpecReporterUri() { return xspecReporter; }

    @Override
    public String getJUnitReporterUri() { return junitReporter; }

    @Override
    public String getXSpecCoverageReporterUri() { return xspecCoverageReporter; }

    /**
     * Defines the XSpec compiler for XSL URI
     * @param xspecXslCompilerUri 
     */
    public void setXSpecXslCompilerUri(String xspecXslCompilerUri) {
        this.xspecXslCompilerUri=xspecXslCompilerUri;
    }
    
    /**
     * Defines the XSpec compiler for XQuery URI
     * @param xspecXQueryCompilerUri 
     */
    public void setXSpecXQueryCompiler(String xspecXQueryCompilerUri) {
        this.xspecXQueryCompilerUri = xspecXQueryCompilerUri;
    }
    
    /**
     * Defines the XSpec-for-Schematron converter URI
     * @param schematronUnitTestConverter 
     */
    public void setSchematronSchutConverter(String schematronUnitTestConverter) {
        this.schematronUnitTestConverter = schematronUnitTestConverter;
    }

    /**
     * Defines the XSpec reporter URI
     * @param xspecReporter 
     */
    public void setXSpecReporter(String xspecReporter) {
        this.xspecReporter = xspecReporter;
    }

    /**
     * Defines the JUnit reporter URI
     * @param junitReporter 
     */
    public void setJUnitReporter(String junitReporter) {
        this.junitReporter = junitReporter;
    }

    /**
     * Defines the XSpec Code Coverage reporter URI
     * @param xspecCoverageReporter 
     */
    public void setXSpecCoverageReporter(String xspecCoverageReporter) {
        this.xspecCoverageReporter = xspecCoverageReporter;
    }
}
