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
package io.xspec.maven.xspecMavenPlugin.resources;

/**
 * This class holds XSpec implementation resources
 * @author cmarchand
 */
public interface XSpecImplResources {
    String XSPEC_PREFIX = "cp:/";
    String XSPEC_PACKAGE = "io/xspec/xspec/impl/";

    /**
     * Usually, generate-xspec-tests.xsl
     * @return XSpec compiler for XSL URI
     */
    public String getXSpecXslCompilerUri();
    
    /**
     * Usually generate-query-tests.xsl
     * @return XSpec compiler for XQuery URI
     */
    public String getXSpecXQueryCompilerUri();
    
    /**
     * Usually schut-to-xspec.xsl
     * @return XSpec-for-Schematron converter URI
     */
    public String getSchematronSchutConverterUri();
    
    /**
     * Usually format-xspec-report.xsl
     * @param useFolding Well, really need a description ?
     * @return XSpec reporter URI
     */
    public String getXSpecReporterUri(boolean useFolding);
    
    /**
     * Usually coverage-report.xsl
     * @return XSpec Code Coverage reporter URI
     */
    public String getXSpecCoverageReporterUri();
    
    /**
     * Usually test-report.css
     * @return test report css uri
     */
    public String getXSpecCssReportUri();

  String getSchematronCompilerUri();
}
