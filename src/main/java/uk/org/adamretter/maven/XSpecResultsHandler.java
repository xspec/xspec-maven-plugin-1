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

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.ext.DefaultHandler2;

/**
 * Extracts test results from the SAX Stream
 *
 * @author <a href="mailto:adam.retter@googlemail.com">Adam Retter</a>
 */
public class XSpecResultsHandler extends DefaultHandler2 {

    private final static String XSPEC_NS = "http://www.jenitennison.com/xslt/xspec";

    private int tests = 0;
    private int passed = 0;
    private int failed = 0;
    private int pending = 0;
    private final LogProvider logProvider;

    public XSpecResultsHandler(LogProvider logProvider) {
        super();
        this.logProvider=logProvider;
    }
    
    

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        super.startElement(uri, localName, qName, attributes);
        if(uri != null && uri.equals(XSPEC_NS) && localName.equals("test")) {
            tests++;
            final String successful = attributes.getValue("successful");
            final String sPending = attributes.getValue("pending");
            if(successful != null && successful.equals("true")) {
                passed++;
            } else if(sPending!=null && sPending.length()>0) {
                this.pending++;
            } else {
                failed++;
            }
        }
    }

    /**
     * Get the total number of executed tests
     *
     * @return The number of tests
     */
    public int getTests() {
        return tests;
    }

    /**
     * Get the total number of executed tests which passed
     *
     * @return The number of tests which passed
     */
    public int getPassed() {
        return passed;
    }

    /**
     * Get the total number of executed tests which failed
     *
     * @return The number of tests which failed
     */
    public int getFailed() {
        return failed;
    }

    public int getPending() {
	return pending;
    }
}
