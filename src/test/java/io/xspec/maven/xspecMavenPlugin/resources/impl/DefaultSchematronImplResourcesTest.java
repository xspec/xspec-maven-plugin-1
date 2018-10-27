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

import io.xspec.maven.xspecMavenPlugin.resources.SchematronImplResources;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 * Tests {@link DefaultSchematronImplResources}
 * @author cmarchand
 */
public class DefaultSchematronImplResourcesTest {
    
    @Test
    public void testClassImplementsInterface() {
        DefaultSchematronImplResources impl = new DefaultSchematronImplResources();
        assertTrue("Class does not implement interface", (impl instanceof SchematronImplResources));
    }
    
    @Test
    public void testDefaultvaluesPrefixes() {
        DefaultSchematronImplResources impl = new DefaultSchematronImplResources();
        assertTrue("schIsoAbstractExpand is not from Schematron implementation",impl.getSchIsoAbstractExpand().startsWith(DefaultSchematronImplResources.SCHEMATRON_PREFIX));
        assertTrue("schIsoDsdlInclude is not from Schematron implementation",impl.getSchIsoDsdlInclude().startsWith(DefaultSchematronImplResources.SCHEMATRON_PREFIX));
        assertTrue("schIsoSvrlForXslt2 is not from Schematron implementation",impl.getSchIsoSvrlForXslt2().startsWith(DefaultSchematronImplResources.SCHEMATRON_PREFIX));
    }
    
    @Test
    public void testAccessorsEfficients() {
        DefaultSchematronImplResources impl = new DefaultSchematronImplResources();
        impl.setSchIsoAbstractExpand("");
        impl.setSchIsoDsdlInclude("");
        impl.setSchIsoSvrlForXslt2("");
        assertEquals("setSchIsoAbstractExpand is not efficient", "", impl.getSchIsoAbstractExpand());
        assertEquals("setSchIsoDsdlInclude is not efficient", "", impl.getSchIsoDsdlInclude());
        assertEquals("setSchIsoSvrlForXslt2 is not efficient", "", impl.getSchIsoSvrlForXslt2());
    }
    
}
