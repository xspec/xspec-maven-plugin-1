/**
 * Copyright © 2017, Christophe Marchand
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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;

import io.xspec.maven.xspecMavenPlugin.XSpecRunner;
import org.xml.sax.Attributes;
import org.xml.sax.Parser;
import org.xml.sax.SAXException;
import org.xml.sax.XMLFilter;
import org.xml.sax.XMLReader;
import org.xml.sax.ext.DefaultHandler2;
import org.xml.sax.helpers.ParserAdapter;
import org.xml.sax.helpers.XMLFilterImpl;

/**
 * A ContentHandler, to count and log with parameters...
 * @author cmarchand
 */
public class XSpecCounterContentHandler extends DefaultHandler2 {
    public final static String XSPEC_NS = XSpecRunner.XSPEC_NS;

    private final Counters globalCounters;
    private Counters currentCounters;

    private boolean pendingScenario = true;
    private final LogProvider logProvider;
    private final String systemId;
    private final URIResolver uriResolver;
    private final Map<String, Counters> sharedScenarios;
    
    private final boolean activateLogs;
    private final String LOG_PREFIX;
    private XSpecCounterContentHandler importedTestFilter;
    
    public XSpecCounterContentHandler(final String systemId, final URIResolver uriResolver, final LogProvider logProvider, boolean activateLogs, String... prefix) {
        super();
        this.systemId=systemId;
        this.uriResolver=uriResolver;
        this.logProvider=logProvider;
        this.activateLogs = activateLogs;
        if(prefix.length>0) {
            LOG_PREFIX=prefix[0];
        } else LOG_PREFIX="";
        globalCounters = new Counters();
        currentCounters = globalCounters;
        sharedScenarios = new HashMap<>();
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
        if(activateLogs)
            logProvider.getLog().debug(LOG_PREFIX+"startElement("+uri+","+localName+","+qName+",...)");
        if(XSPEC_NS.equals(uri) && ("pendingTests".equals(localName) || "pending".equals(localName))) {
            currentCounters.pendingWrapper++;
        } else if(XSPEC_NS.equals(uri) && "scenario".equals(localName)) {
            if(isTrue((atts.getValue("shared")))) {
                String scenarioName = atts.getValue("label");
                Counters scenarioCounters = new Counters();
                sharedScenarios.put(scenarioName, scenarioCounters);
                currentCounters = scenarioCounters;
            }
            if((atts.getValue("pendingTests") != null || atts.getValue("pending") != null)) {
                currentCounters.pendingWrapper++;
                pendingScenario = true;
                if (activateLogs) {
                    logProvider.getLog().debug(LOG_PREFIX + "entering pending scenario");
                }
            }
        } else if(XSPEC_NS.equals(uri) && "expect".equals(localName)) {
            if (activateLogs) {
                logProvider.getLog().debug(LOG_PREFIX + "entering expect");
            }
            if (currentCounters.pendingWrapper > 0) {
                currentCounters.pendingTests++;
            }
            currentCounters.tests++;
        } else if(XSPEC_NS.equals(uri) && "like".equals(localName)) {
            String scenarioName = atts.getValue("label");
            Counters scenarioCounters = sharedScenarios.get(scenarioName);
            currentCounters.pendingTests+=scenarioCounters.pendingTests;
            currentCounters.tests+=scenarioCounters.tests;
            currentCounters.pendingWrapper+=scenarioCounters.pendingWrapper;
        } else if(XSPEC_NS.equals(uri) && "import".equals(localName)) {
            if(activateLogs) {
                logProvider.getLog().debug(LOG_PREFIX+"[in "+systemId+"] seeing imported XSpec "+atts.getValue("href"));
            }
            // in this particular case, we must count also in imported xspec
            String importedSystemId = null;
            try {
                Source source = uriResolver.resolve(atts.getValue("href"), systemId);
                importedSystemId = source.getSystemId();
            } catch(TransformerException ex) {
                logProvider.getLog().error("while resolving "+atts.getValue("href")+" to "+systemId, ex);
            }
            if(importedSystemId!=null) {
                // We must create a new parser, a new filter, and so on...
                try {
                    if(activateLogs) {
                        logProvider.getLog().warn(LOG_PREFIX+"[in "+systemId+"] parsing imported XSpec "+importedSystemId);
                    }
                    final Parser parser = XmlStuff.PARSER_FACTORY.newSAXParser().getParser();
                    final XMLReader reader = new ParserAdapter(parser);
                    importedTestFilter = new XSpecCounterContentHandler(importedSystemId, uriResolver, logProvider, activateLogs, XSPEC_NS+importedSystemId+": ");
                    XMLFilter filter = new XMLFilterImpl(reader) {
                        @Override
                        public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
                            super.startElement(uri, localName, qName, atts);
                            importedTestFilter.startElement(uri, localName, qName, atts);
                        }
                        @Override
                        public void endElement(String uri, String localName, String qName) throws SAXException {
                            super.endElement(uri, localName, qName);
                            importedTestFilter.endElement(uri, localName, qName);
                        }
                    };
                    filter.parse(importedSystemId);
                } catch(ParserConfigurationException | SAXException | IOException ex) {
                    logProvider.getLog().error("["+systemId+"] while counting into imported "+importedSystemId, ex);
                }
            }
        }
    }

    @Override
    public void endElement(final String uri, final String localName, final String qName) throws SAXException {
        if(XSPEC_NS.equals(uri) && ("pendingTests".equals(localName) || "pending".equals(localName))) {
            currentCounters.pendingWrapper--;
        } else if(XSPEC_NS.equals(uri) && "scenario".equals(localName)) {
            if(pendingScenario == true) {
                currentCounters.pendingWrapper--;
                pendingScenario = false;
                if (activateLogs)
                    logProvider.getLog().debug(LOG_PREFIX + "exiting pending scenario");
            }
            currentCounters = globalCounters;
        } else if(XSPEC_NS.equals(uri) && "import".equals(localName)) {
            if(activateLogs) {
                logProvider.getLog().debug(LOG_PREFIX+"Adding "+importedTestFilter.getTests()+" tests");
                logProvider.getLog().debug(LOG_PREFIX+"Adding "+importedTestFilter.getPendingTests()+" pending tests");
            }
            this.currentCounters.tests = this.currentCounters.tests + importedTestFilter.getTests();
            this.currentCounters.pendingTests = this.currentCounters.pendingTests + importedTestFilter.getPendingTests();
            importedTestFilter = null;
        }
    }
    private boolean isTrue(String value) {
        if(value==null) return false;
        if("true".equals(value)) return true;
        if("yes".equals(value)) return true;
        if("1".equals(value)) return true;
        return false;
    }
    /**
     * The total number of test expectations in the provided XSpec
     * includes pendingTests tests
     *
     * @return The number of tests
     */
    public int getTests() {
        return globalCounters.tests;
    }

    /**
     * The total number of pendingTests test expectations in the provided XSpec
     *
     * @return The number of pending tests
     */
    public int getPendingTests() {
        return globalCounters.pendingTests;
    }

    public static class Counters {
        private int tests = 0;
        private int pendingTests = 0;
        private int pendingWrapper = 0;

        public Counters() {
        }
    }
}
