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
import static org.junit.Assert.*;
import org.junit.Test;

/**
 * Tests {@link DefaultXSpecImplResources}
 * @author cmarchand
 */
public class DefaultXSpecImplResourcesTest {
    
    @Test
    public void testClassImplementsInterface() {
        DefaultXSpecImplResources impl = new DefaultXSpecImplResources();
        assertTrue("Class does not implements interface", impl instanceof XSpecImplResources);
    }
    
    @Test
    public void testDefaultValuesPrefixes() {
        DefaultXSpecImplResources impl = new DefaultXSpecImplResources();
        assertTrue("junitReporter is not a XSpec resource", impl.getJUnitReporterUri().startsWith(DefaultXSpecImplResources.XSPEC_PREFIX));
        assertTrue("jschutConverter is not a XSpec resource", impl.getSchematronSchutConverterUri().startsWith(DefaultXSpecImplResources.XSPEC_PREFIX));
        assertTrue("coverageReporter is not a XSpec resource", impl.getXSpecCoverageReporterUri().startsWith(DefaultXSpecImplResources.XSPEC_PREFIX));
        assertTrue("xspecReporter is not a XSpec resource", impl.getXSpecReporterUri().startsWith(DefaultXSpecImplResources.XSPEC_PREFIX));
        assertTrue("xqueryCompiler is not a XSpec resource", impl.getXSpecXQueryCompilerUri().startsWith(DefaultXSpecImplResources.XSPEC_PREFIX));
        assertTrue("xsltCompiler is not a XSpec resource", impl.getXSpecXslCompilerUri().startsWith(DefaultXSpecImplResources.XSPEC_PREFIX));
    }
    
    @Test
    public void settersAreEfficients() {
        DefaultXSpecImplResources impl = new DefaultXSpecImplResources();
        impl.setJUnitReporter("");
        impl.setSchematronSchutConverter("");
        impl.setXSpecCoverageReporter("");
        impl.setXSpecReporter("");
        impl.setXSpecXQueryCompiler("");
        impl.setXSpecXslCompilerUri("");
        assertEquals("setJUnitReporter is not efficient", "", impl.getJUnitReporterUri());
        assertEquals("setSchematronSchutConverter is not efficient", "", impl.getXSpecCoverageReporterUri());
        assertEquals("setXSpecCoverageReporter is not efficient", "", impl.getXSpecCoverageReporterUri());
        assertEquals("setXSpecReporter is not efficient", "", impl.getXSpecReporterUri());
        assertEquals("setXSpecXQueryCompiler is not efficient", "", impl.getXSpecXQueryCompilerUri());
        assertEquals("setXSpecXslCompilerUri is not efficient", "", impl.getXSpecXslCompilerUri());
    }
}
