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

import net.sf.saxon.TransformerFactoryImpl;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;

import javax.xml.transform.*;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Goal which runs any XSpec tests
 *
 * @author Adam Retter
 *
 * @requiresDependencyResolution test
 * @goal run-xspec
 * @phase verify
 */
public class XSpecMojo extends AbstractMojo {

    /** @parameter expression="${skipTests}" default-value="false" */
    private boolean skipTests;

    /**
     * Location of the outputDirectory
     * @parameter expression="${project.build.directory}"
     * @required
     */
    private File outputDirectory;

    /**
     * Location of the XSpec tests
     * @parameter expression="${xspecReportDir}" default-value="${basedir}/src/test/resources/xspec"
     */
    private File xspecTestDir;

    /**
     * Location of the XSpec reports
     * @parameter expression="${xspecReportDir}" default-value="${project.build.directory}/xspec-reports"
     */
    private File xspecReportDir;

    public void execute() throws MojoExecutionException {

        final TransformerFactoryImpl factory = new TransformerFactoryImpl();
        InputStream isCompiler = null;

        try {
            isCompiler = getClass().getClassLoader().getResourceAsStream("xspec/src/compiler/generate-xspec-tests.xsl");
            final Source srcCompiler = new StreamSource(isCompiler);

            final Templates tCompiler = factory.newTemplates(srcCompiler);

            getLog().debug("Looking for XSpecs in: " + xspecTestDir);
            final List<File> xspecs = findAllXSpecs(xspecTestDir);
            getLog().info("Found " + xspecs.size() + "XSpecs...");

            for(final File xspec : xspecs) {
                getLog().info("Processing XSpec: " + xspec.getAbsolutePath());

                /* create the test stylesheet */
                getLog().info("Creating test stylesheet...");

                final Result result = new StreamResult(new File("/tmp/adam-" + System.currentTimeMillis() + ".xml"));


                final TransformerHandler hCompiler = factory.newTransformerHandler(tCompiler);
                final Transformer transformer = hCompiler.getTransformer();
                transformer.transform(new StreamSource(xspec), result);

            }
        } catch(final TransformerException te) {
            throw new MojoExecutionException(te.getMessage(), te);
        } finally {
            if(isCompiler != null) {
                try { isCompiler.close(); } catch(final IOException ioe) { getLog().warn(ioe); };
            }
        }
    }

    /**
     * Recursively find any files whoose name ends '.xspec'
     * under the directory xspecTestDir
     *
     * @param xspecTestDir The directory to search for XSpec files
     *
     * @return List of XSpec files
     */
    private List<File> findAllXSpecs(final File xspecTestDir) {

        final List<File> specs = new ArrayList<File>();

        final File[] specFiles = xspecTestDir.listFiles(new FileFilter() {
            @Override
            public boolean accept(final File file) {
                return file.isFile() && file.getName().endsWith(".xspec");
            }
        });
        specs.addAll(Arrays.asList(specFiles));

        for(final File subDir : xspecTestDir.listFiles(new FileFilter(){
            @Override
            public boolean accept(final File file) {
                return file.isDirectory();
            }
        })){
            specs.addAll(findAllXSpecs(subDir));
        }

        return specs;
    }
}
