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
package io.xspec.maven.xspecMavenPlugin.utils;

import io.xspec.maven.xspecMavenPlugin.XSpecRunner;
import static org.junit.Assert.*;
import org.junit.Test;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.AttributesImpl;

/**
 * Tests XSpecResultsHandler
 * @author cmarchand
 */
public class XSpecResultsHandlerTest {
    private static final Attributes EMPTY_ATTRS = new AttributesImpl();
    
    @Test
    public void testNothing() {
        XSpecResultsHandler handler = new XSpecResultsHandler();
        assertEquals(0, handler.getFailed());
        assertEquals(0, handler.getPassed());
        assertEquals(0, handler.getPending());
        assertEquals(0, handler.getTests());
        assertEquals(handler.getTests(), handler.getFailed()+handler.getPassed()+handler.getPending());
    }
    
    @Test   // this should have one failed, because no attribute
    public void testNoAttribute() throws Exception {
        XSpecResultsHandler handler = new XSpecResultsHandler();
        handler.startDocument();
        handler.startElement(XSpecRunner.XSPEC_NS, "test", "x:test", EMPTY_ATTRS);
        handler.endElement(XSpecRunner.XSPEC_NS, "test", "x:test");
        assertEquals(1, handler.getFailed());
        assertEquals(0, handler.getPassed());
        assertEquals(0, handler.getPending());
        assertEquals(1, handler.getTests());
        assertEquals(handler.getTests(), handler.getFailed()+handler.getPassed()+handler.getPending());
    }
    
    @Test   // this should have one success
    public void testSuccess() throws Exception {
        XSpecResultsHandler handler = new XSpecResultsHandler();
        handler.startDocument();
        AttributesImpl attrs = new AttributesImpl();
        attrs.addAttribute(null, "successful", "successful", "string", "true");
        handler.startElement(XSpecRunner.XSPEC_NS, "test", "x:test", attrs);
        handler.endElement(XSpecRunner.XSPEC_NS, "test", "x:test");
        assertEquals(0, handler.getFailed());
        assertEquals(1, handler.getPassed());
        assertEquals(0, handler.getPending());
        assertEquals(1, handler.getTests());
        assertEquals(handler.getTests(), handler.getFailed()+handler.getPassed()+handler.getPending());
    }

    @Test   // this should have one success
    public void testSuccessFalse() throws Exception {
        XSpecResultsHandler handler = new XSpecResultsHandler();
        handler.startDocument();
        AttributesImpl attrs = new AttributesImpl();
        attrs.addAttribute(null, "successful", "successful", "string", "false");
        handler.startElement(XSpecRunner.XSPEC_NS, "test", "x:test", attrs);
        handler.endElement(XSpecRunner.XSPEC_NS, "test", "x:test");
        assertEquals(1, handler.getFailed());
        assertEquals(0, handler.getPassed());
        assertEquals(0, handler.getPending());
        assertEquals(1, handler.getTests());
        assertEquals(handler.getTests(), handler.getFailed()+handler.getPassed()+handler.getPending());
    }

    @Test   // this should have one pending
    public void testPending() throws Exception {
        XSpecResultsHandler handler = new XSpecResultsHandler();
        handler.startDocument();
        AttributesImpl attrs = new AttributesImpl();
        attrs.addAttribute(null, "pending", "pending", "string", "Because not implemented");
        handler.startElement(XSpecRunner.XSPEC_NS, "test", "x:test", attrs);
        handler.endElement(XSpecRunner.XSPEC_NS, "test", "x:test");
        assertEquals(0, handler.getFailed());
        assertEquals(0, handler.getPassed());
        assertEquals(1, handler.getPending());
        assertEquals(1, handler.getTests());
        assertEquals(handler.getTests(), handler.getFailed()+handler.getPassed()+handler.getPending());
    }

    @Test   // this should have one failed
    public void testPendingEmpty() throws Exception {
        XSpecResultsHandler handler = new XSpecResultsHandler();
        handler.startDocument();
        AttributesImpl attrs = new AttributesImpl();
        attrs.addAttribute(null, "pending", "pending", "string", "");
        handler.startElement(XSpecRunner.XSPEC_NS, "test", "x:test", attrs);
        handler.endElement(XSpecRunner.XSPEC_NS, "test", "x:test");
        assertEquals(1, handler.getFailed());
        assertEquals(0, handler.getPassed());
        assertEquals(0, handler.getPending());
        assertEquals(1, handler.getTests());
        assertEquals(handler.getTests(), handler.getFailed()+handler.getPassed()+handler.getPending());
    }
}
