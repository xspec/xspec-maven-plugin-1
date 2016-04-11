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
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLFilterImpl;

/**
 * Extracts test results from the XSpec Reader
 *
 * @author <a href="mailto:adam.retter@googlemail.com">Adam Retter</a>
 */
public class XSpecTestFilter extends XMLFilterImpl {

    private final static String XSPEC_NS = "http://www.jenitennison.com/xslt/xspec";

    private int tests = 0;
    private int pendingTests = 0;

    private int pendingWrapper = 0;
    private boolean pendingScenario = true;

    public XSpecTestFilter(final XMLReader parent) {
        super(parent);
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
        super.startElement(uri, localName, qName, atts);

        if(uri != null && uri.equals(XSPEC_NS) && (localName.equals("pendingTests") || localName.equals("pending"))) {
            pendingWrapper++;
        }

        if(uri != null && uri.equals(XSPEC_NS) && localName.equals("scenario") && (atts.getValue("pendingTests") != null || atts.getValue("pending") != null)) {
            pendingWrapper++;
            pendingScenario = true;
        }

        if(uri != null && uri.equals(XSPEC_NS) && localName.equals("expect")) {
            if(pendingWrapper > 0) {
                pendingTests++;
            }
            tests++;
        }
    }

    @Override
    public void endElement(final String uri, final String localName, final String qName) throws SAXException {
        super.endElement(uri, localName, qName);

        if(uri != null && uri.equals(XSPEC_NS) && (localName.equals("pendingTests") || localName.equals("pending"))) {
            pendingWrapper--;
        }

        if(uri != null && uri.equals(XSPEC_NS) && localName.equals("scenario") && pendingScenario == true) {
            pendingWrapper--;
            pendingScenario = false;
        }
    }

    /**
     * The total number of test expectations in the provided XSpec
     * includes pendingTests tests
     *
     * @return The number of tests
     */
    public int getTests() {
        return tests;
    }

    /**
     * The total number of pendingTests test expectations in the provided XSpec
     *
     * @return The number of pending tests
     */
    public int getPendingTests() {
        return pendingTests;
    }
}
