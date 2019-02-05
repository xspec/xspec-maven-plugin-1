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

import io.xspec.maven.xspecMavenPlugin.TestUtils;
import java.io.File;
import java.net.URISyntaxException;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;
import javax.xml.transform.stream.StreamSource;
import org.apache.maven.plugin.logging.Log;
import static org.junit.Assert.*;
import org.junit.Test;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.ext.Attributes2Impl;
import org.xml.sax.helpers.AttributesImpl;
import org.xmlresolver.Resolver;

/**
 * Tests XSpecCounterCH
 * @author cmarchand
 */
public class XSpecCounterCHTest {
    
    @Test
    public void testNoLog() throws SAXException {
        LogMock log =  new LogMock();
        XSpecCounterCH counter = new XSpecCounterCH(
                "file:/home/fake/pouet.xspec", 
                new Resolver(), 
                createLogProvider(log), 
                false); // false de-activates logs
        counter.startElement("http://www.jenitennison.com/xslt/xspec", "description", "x:description", new Attributes2Impl());
        assertEquals(0, log.getDebugCount());
        assertEquals(0, log.getInfoCount());
        assertEquals(0, log.getWarnCount());
        assertEquals(0, log.getErrorCount());
        assertNull(log.getLastLog());
    }
    
    @Test
    public void testLogExists() throws SAXException {
        LogMock log =  new LogMock();
        XSpecCounterCH counter = new XSpecCounterCH(
                "file:/home/fake/pouet.xspec", 
                new Resolver(), 
                createLogProvider(log), 
                true,    // true activates logs
                "[XSpec] ");
        counter.startElement("http://www.jenitennison.com/xslt/xspec", "description", "x:description", new Attributes2Impl());
        assertEquals(1, log.getDebugCount());
        assertEquals(0, log.getInfoCount());
        assertEquals(0, log.getWarnCount());
        assertEquals(0, log.getErrorCount());
        assertEquals("[XSpec] startElement(http://www.jenitennison.com/xslt/xspec,description,x:description,...)",log.getLastLog());
    }

    @Test
    public void testOneExpect() throws SAXException {
        LogMock log = new LogMock();
        XSpecCounterCH counter = new XSpecCounterCH(
                "file:/home/fake/test.xspec", 
                new Resolver(), 
                createLogProvider(log), 
                true, "[Counter] ");
        Attributes emptyAttrs = new Attributes2Impl();
        counter.startDocument();
        counter.startElement(XSpecCounterCH.XSPEC_NS, "description", "x:description", emptyAttrs);
        counter.startElement(XSpecCounterCH.XSPEC_NS, "scenario", "x:scenario", emptyAttrs);
        counter.startElement(XSpecCounterCH.XSPEC_NS, "expect", "x:expect", emptyAttrs);
        counter.endElement(XSpecCounterCH.XSPEC_NS, "expect", "x:expect");
        counter.endElement(XSpecCounterCH.XSPEC_NS, "scenario", "x:scenario");
        counter.endElement(XSpecCounterCH.XSPEC_NS, "description", "x:description");
        counter.endDocument();
        assertEquals(1, counter.getTests());
        assertEquals(0, counter.getPendingTests());
    }

    @Test
    public void testOneExpectPendingAttr() throws SAXException {
        LogMock log = new LogMock();
        XSpecCounterCH counter = new XSpecCounterCH(
                "file:/home/fake/test.xspec", 
                new Resolver(), 
                createLogProvider(log), 
                true, "[Counter] ");
        Attributes emptyAttrs = new Attributes2Impl();
        AttributesImpl pendingAttrs = new AttributesImpl();
        pendingAttrs.addAttribute(null, "pending", "pending", "pouet", "wainting for implementation");
        counter.startDocument();
        counter.startElement(XSpecCounterCH.XSPEC_NS, "description", "x:description", emptyAttrs);
        counter.startElement(XSpecCounterCH.XSPEC_NS, "scenario", "x:scenario", pendingAttrs);
        counter.startElement(XSpecCounterCH.XSPEC_NS, "expect", "x:expect", emptyAttrs);
        counter.endElement(XSpecCounterCH.XSPEC_NS, "expect", "x:expect");
        counter.endElement(XSpecCounterCH.XSPEC_NS, "scenario", "x:scenario");
        counter.endElement(XSpecCounterCH.XSPEC_NS, "description", "x:description");
        counter.endDocument();
        assertEquals(1, counter.getTests());
        assertEquals(1, counter.getPendingTests());
    }
    @Test
    public void testOneExpectPendingElement() throws SAXException {
        LogMock log = new LogMock();
        XSpecCounterCH counter = new XSpecCounterCH(
                "file:/home/fake/test.xspec", 
                new Resolver(), 
                createLogProvider(log), 
                true, "[Counter] ");
        Attributes emptyAttrs = new Attributes2Impl();
        AttributesImpl pendingAttrs = new AttributesImpl();
        pendingAttrs.addAttribute(null, "pending", "pending", "pouet", "waiting for implementation");
        counter.startDocument();
        counter.startElement(XSpecCounterCH.XSPEC_NS, "description", "x:description", emptyAttrs);
        counter.startElement(XSpecCounterCH.XSPEC_NS, "pending", "x:pending", emptyAttrs);
        counter.startElement(XSpecCounterCH.XSPEC_NS, "scenario", "x:scenario", emptyAttrs);
        counter.startElement(XSpecCounterCH.XSPEC_NS, "expect", "x:expect", emptyAttrs);
        counter.endElement(XSpecCounterCH.XSPEC_NS, "expect", "x:expect");
        counter.endElement(XSpecCounterCH.XSPEC_NS, "scenario", "x:scenario");
        counter.endElement(XSpecCounterCH.XSPEC_NS, "pending", "x:pending");
        counter.endElement(XSpecCounterCH.XSPEC_NS, "description", "x:description");
        counter.endDocument();
        assertEquals(1, counter.getTests());
        assertEquals(1, counter.getPendingTests());
    }

    @Test
    public void testXSpecImport() throws Exception {
        LogMock log = new LogMock();
        URIResolver uriResolver = new LocalUriResolver();
         XSpecCounterCH counter = new XSpecCounterCH(
                "file:/home/fake/test.xspec", 
                uriResolver, 
                createLogProvider(log), 
                true, "[Import] ");
        Attributes emptyAttrs = new Attributes2Impl();
        AttributesImpl hrefAttrs = new AttributesImpl();
        hrefAttrs.addAttribute(null, "href", "href", "pouet", "imported.xspec");
        counter.startDocument();
        counter.startElement(XSpecCounterCH.XSPEC_NS, "description", "x:description", emptyAttrs);
        counter.startElement(XSpecCounterCH.XSPEC_NS, "import", "x:import", hrefAttrs);
        counter.endElement(XSpecCounterCH.XSPEC_NS, "import", "x:import");
        counter.endElement(XSpecCounterCH.XSPEC_NS, "description", "x:description");
        counter.endDocument();
        assertEquals(1, counter.getTests());
    }

    private LogProvider createLogProvider(final Log log) {
        return () -> log;
    }
    
    public class LogMock implements Log {
        private long debugCount = 0;
        private long infoCount = 0;
        private long warnCount = 0;
        private long errorCount = 0;
        private CharSequence lastLog = null;

        @Override
        public boolean isDebugEnabled() { return true; }

        @Override
        public void debug(CharSequence cs) { debugCount++; lastLog = cs; }

        @Override
        public void debug(CharSequence cs, Throwable thrwbl) { debugCount++; lastLog = cs; }

        @Override
        public void debug(Throwable thrwbl) { debugCount++; }

        @Override
        public boolean isInfoEnabled() { return true; }

        @Override
        public void info(CharSequence cs) { infoCount++; lastLog = cs; }

        @Override
        public void info(CharSequence cs, Throwable thrwbl) { infoCount++; lastLog = cs; }

        @Override
        public void info(Throwable thrwbl) { infoCount++; }

        @Override
        public boolean isWarnEnabled() { return true; }

        @Override
        public void warn(CharSequence cs) { warnCount++; lastLog = cs; }

        @Override
        public void warn(CharSequence cs, Throwable thrwbl) { warnCount++; lastLog = cs; }

        @Override
        public void warn(Throwable thrwbl) { warnCount++; }

        @Override
        public boolean isErrorEnabled() { return true; }

        @Override
        public void error(CharSequence cs) { errorCount++; lastLog = cs; }

        @Override
        public void error(CharSequence cs, Throwable thrwbl) { errorCount++; lastLog = cs; }

        @Override
        public void error(Throwable thrwbl) { errorCount++; }

        public long getDebugCount() { return debugCount; }

        public long getInfoCount() { return infoCount; }

        public long getWarnCount() { return warnCount; }

        public long getErrorCount() { return errorCount; }

        public CharSequence getLastLog() { return lastLog; }
        
    }
    
    private class LocalUriResolver implements URIResolver {
        @Override
        public Source resolve(String href, String base) throws TransformerException {
            try {
                File importedXspecFile = new File(new File(new File(TestUtils.getTestDirectory(),"imported"),"xspec"),"imported.xspec");
                return new StreamSource(importedXspecFile);
            } catch(URISyntaxException ex) {
                throw new TransformerException(ex);
            }
        }
    }
}
