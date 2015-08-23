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

import java.io.File;

/**
 * Simple class holding the results of compiling an XSpec
 *
 * @author <a href="mailto:adam.retter@googlemail.com">Adam Retter</a>
 */
public class CompiledXSpec {

    private final int tests;
    private final int pendingTests;
    private final File compiledStylesheet;

    public CompiledXSpec(final int tests, final int pendingTests, final File compiledStylesheet) {
        this.tests = tests;
        this.pendingTests = pendingTests;
        this.compiledStylesheet = compiledStylesheet;
    }

    /**
     * Count of the number of tests in the compiled XSpec
     * (including pending tests)
     *
     * @return The number of tests
     */
    public int getTests() {
        return tests;
    }

    /**
     * Count of the number of pending tests in the compiled XSpec
     *
     * @return The number of pending tests
     */
    public int getPendingTests() {
        return pendingTests;
    }

    /**
     * File system path of the compiled XSpec stylesheet
     *
     * @return path to the compiled xspec stylesheet
     */
    public File getCompiledStylesheet() {
        return compiledStylesheet;
    }
}
