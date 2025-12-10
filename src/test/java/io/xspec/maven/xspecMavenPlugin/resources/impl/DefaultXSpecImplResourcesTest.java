/**
 * Copyright © 2018, Christophe Marchand, XSpec organization
 * All rights reserved.
 * <p>
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * * Neither the name of the <organization> nor the
 * names of its contributors may be used to endorse or promote products
 * derived from this software without specific prior written permission.
 * <p>
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
import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;

/**
 * Tests {@link DefaultXSpecImplResources}
 *
 * @author cmarchand
 */
public class DefaultXSpecImplResourcesTest {

  @Test
  public void testClassImplementsInterface() {
    DefaultXSpecImplResources impl = new DefaultXSpecImplResources();
    Assertions
        .assertThat(impl)
        .isInstanceOf(XSpecImplResources.class);
  }

  @Test
  public void testDefaultValuesPrefixes() {
    DefaultXSpecImplResources impl = new DefaultXSpecImplResources();
    SoftAssertions softly = new SoftAssertions();
    softly
        .assertThat(impl.getSchematronSchutConverterUri())
        .startsWith(DefaultXSpecImplResources.XSPEC_PREFIX);
    softly
        .assertThat(impl.getXSpecCoverageReporterUri())
        .startsWith(DefaultXSpecImplResources.XSPEC_PREFIX);
    softly
        .assertThat(impl.getXSpecReporterUri(false))
        .startsWith(DefaultXSpecImplResources.XSPEC_PREFIX);
    softly
        .assertThat(impl.getXSpecXQueryCompilerUri())
        .startsWith(DefaultXSpecImplResources.XSPEC_PREFIX);
    softly
        .assertThat(impl.getXSpecXslCompilerUri())
        .startsWith(DefaultXSpecImplResources.XSPEC_PREFIX);
    softly.assertAll();
  }

  @Test
  public void settersAreEfficients() {
    DefaultXSpecImplResources impl = new DefaultXSpecImplResources();
    impl.setSchematronSchutConverter("");
    impl.setXSpecCoverageReporter("");
    impl.setXSpecReporter("");
    impl.setXSpecXQueryCompiler("");
    impl.setXSpecXslCompilerUri("");
    SoftAssertions softly = new SoftAssertions();
    softly.assertThat(impl.getXSpecCoverageReporterUri()).isEqualTo("");
    softly.assertThat(impl.getXSpecCoverageReporterUri()).isEqualTo("");
    softly.assertThat(impl.getXSpecReporterUri(false)).isEqualTo("");
    softly.assertThat(impl.getXSpecXQueryCompilerUri()).isEqualTo("");
    softly.assertThat(impl.getXSpecXslCompilerUri()).isEqualTo("");
    softly.assertAll();
  }
}
