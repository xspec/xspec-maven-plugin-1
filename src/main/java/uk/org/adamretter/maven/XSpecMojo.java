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

import io.xspec.maven.xspecMavenPlugin.utils.LogProvider;
import io.xspec.maven.xspecMavenPlugin.utils.ProcessedFile;
import io.xspec.maven.xspecMavenPlugin.utils.XmlStuff;
import net.sf.saxon.s9api.*;
import io.xspec.maven.xspecMavenPlugin.XSpecRunner;
import io.xspec.maven.xspecMavenPlugin.resources.SchematronImplResources;
import io.xspec.maven.xspecMavenPlugin.resources.XSpecImplResources;
import io.xspec.maven.xspecMavenPlugin.resources.XSpecPluginResources;
import io.xspec.maven.xspecMavenPlugin.resources.impl.DefaultSchematronImplResources;
import io.xspec.maven.xspecMavenPlugin.resources.impl.DefaultXSpecImplResources;
import io.xspec.maven.xspecMavenPlugin.resources.impl.DefaultXSpecPluginResources;
import io.xspec.maven.xspecMavenPlugin.utils.LogProvider;
import io.xspec.maven.xspecMavenPlugin.utils.ProcessedFile;
import io.xspec.maven.xspecMavenPlugin.utils.RunnerOptions;
import io.xspec.maven.xspecMavenPlugin.utils.RunnerOptions;
import io.xspec.maven.xspecMavenPlugin.utils.XSpecFailureException;
import io.xspec.maven.xspecMavenPlugin.utils.XSpecFailureException;
import io.xspec.maven.xspecMavenPlugin.utils.XSpecPluginException;
import io.xspec.maven.xspecMavenPlugin.utils.XSpecPluginException;
import io.xspec.maven.xspecMavenPlugin.utils.XmlStuff;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import javax.xml.transform.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.project.MavenProject;
import top.marchand.maven.saxon.utils.SaxonOptions;

/**
 * xspec-maven-plugin is a plugin that run all xspec unit tests at test phase, and produces reports.
 *
 * It relies on XSpec 1.0, available at http://github.com/xspec/xspec
 * 
 * If one unit test fails, the plugin execution fails, and the build fails.
 * 
 * <strong>Saxon implementation</strong>
 * You must define the Saxon implementation (http://www.saxonica.com), as plugin do not embed any Saxon implementation. Just declare a dependency in plugin :
 * 
 * <pre>
 *   &lt;plugin>
 *     &lt;groupId>io.xspec.maven&lt;/groupId>
 *     &lt;artifactId>xspec-maven-plugin&lt;/artifactId>
 *     &lt;dependencies>
 *       &lt;dependency>
 *         &lt;groupId>net.sf.saxon&lt;/groupId>
 *         &lt;artifactId>Saxon-HE&lt;/artifactId>
 *         &lt;version>9.8.0-5&lt;/version>
 *       &lt;/dependency>
 *     &lt;/dependencies>
 *   &lt<;/plugin>
 * </pre>
 *                 
 * Saxon version must be at least 9.8.0-5. Saxon-PE or Saxon-EE can be used, but you'll have to deploy them to your local or enterprise repository, as they are not available in Maven Central. Don't forget to add a dependency to your Saxon license. The license file may be packaged in a .jar file and deployed to your local or enterprise repository.
 * 
 * <ul>
 * <li>xspec-maven-plugin expects XSpec files in src/test/xspec/</li>
 * <li>xspec-maven-plugin produces XSpec reports in target/xspec-reports/</li>
 * <li>xspec-maven-plugin produces Junit reports in target/surefire-reports/</li>
 * </ul>
 * 
 * xspec-maven-plugin respects Maven unit tests convention and supports skipTests system property. See http://maven.apache.org/surefire/maven-surefire-plugin/test-mojo.html#skipTests
 * 
 * xspec-maven-plugin supports testFailureIgnore configuration parameter. See http://maven.apache.org/surefire/maven-surefire-plugin/test-mojo.html#testFailureIgnore
 *             
 * <strong>XPath extension functions</strong>
 * 
 * Saxon allows to create XPath extension functions in Java. See https://www.saxonica.com/documentation/index.html#!extensibility/functions. gaulois-pipe has defined a common way to automatically install extension functions in Saxon. xspec-maven-plugin supports the same mecanism.
 * It looks in classpath for META-INF/services/top.marchand.xml.gaulois.xml resources.
 * Each file declares extension functions in this format :
 * 
 * <pre>       
 *   &lt;gaulois-services>
 *     &lt;saxon>
 *       &lt;extensions>
 *         &lt;function>top.marchand.xml.extfunctions.basex.BaseXQuery&lt;/function>
 *       &lt;/extensions>
 *     &lt;/saxon>
 *   &lt;/gaulois-services>
 * </pre>
 *             
 * At least two function libraries are available in xspec-maven-plugin : 
 * <ul>
 * <li>https://github.com/cmarchand/xpath-basex-ext/</li>
 * <li>https://github.com/AxelCourt/saxon-marklogic-ext</li>
 * </ul>
 * 
 * If you want want to add your own extension functions to XSpec engine, create a maven project with function implementation, a service file, and add it as a dependency to xspec-maven-plugin delaration :
 * 
 * <pre>
 *   &lt;plugin>
 *     &lt;groupId>io.xspec.maven&lt;/groupId>
 *     &lt;artifactId>xspec-maven-plugin&lt;/artifactId>
 *     &lt;dependencies>
 *       &lt;dependency>
 *         &lt;groupId>your.enterprise.groupId&lt;/groupId>
 *         &lt;artifactId>XPath-extension-functions&lt;/artifactId>
 *       &lt;/dependency>
 *     &lt;/dependencies>
 *   &lt;/plugin>
 * </pre>
 *             
 * All extension functions found will create a log in console when installed in Saxon.
 *
 * @author <a href="mailto:adam.retter@googlemail.com">Adam Retter</a>
 * @author <a href="mailto:christophe@marchand.top">Christophe Marchand</a>
 */
@Mojo(name = "run-xspec", defaultPhase = LifecyclePhase.TEST, requiresDependencyResolution = ResolutionScope.TEST)
public class XSpecMojo extends AbstractMojo implements LogProvider {
    public static final transient String XSPEC_PREFIX = "dependency://io.xspec+xspec/";
    public static final transient String XML_UTILITIES_PREFIX = "dependency://org.mricaud+xml-utilities/";
    public static final transient String CATALOG_NS = "urn:oasis:names:tc:entity:xmlns:xml:catalog";
    public static final transient String XSPEC_NS = "http://www.jenitennison.com/xslt/xspec";
    public static final transient String LOCAL_PREFIX = "dependency://io.xspec.maven+xspec-maven-plugin/";

    @Component
    private MavenSession session;
    
    @Parameter( defaultValue = "${project}", readonly = true, required = true )
    public MavenProject project;

    /**
     * Defines if XSpec unit tests should be run or skipped.
     * It's a bad practise to set this option, and this NEVER be done.
     */
    @Parameter(property = "skipTests", defaultValue = "false")
    public boolean skipTests;

    /**
     * Path to compiler/generate-xspec-tests.xsl XSpec implementation file.
     * This parameter is only available for developement purposes, and should never be overriden.
     */
    @Parameter()
    public String xspecXslCompiler;

    /**
     * Path to compiler/generate-query-tests.xsl.
     * This parameter is only available for developement purposes, and should never be overriden.
     */
    @Parameter()
    public String xspecXQueryCompiler;
    
    /**
     * Path to schematron/iso-schematron/iso_dsdl_include.xsl.
     * This parameter is only available for developement purposes, and should never be overriden.
     */
    @Parameter()
    public String schIsoDsdlInclude;
    
    /**
     * Path to schematron/iso-schematron/iso_abstract_expand.xsl.
     * This parameter is only available for developement purposes, and should never be overriden.
     */
    @Parameter()
    public String schIsoAbstractExpand;
    
    /**
     * Path to schematron/iso-schematron/iso_svrl_for_xslt2.xsl.
     * This parameter is only available for developement purposes, and should never be overriden.
     */
    @Parameter()
    public String schIsoSvrlForXslt2;

    /**
     * Path to schematron/schut-to-xspec.xsl.
     * This parameter is only available for developement purposes, and should never be overriden.
     */
    @Parameter()
    public String schSchut;
    /**
     * Path to reporter/format-xspec-report.xsl.
     * This parameter is only available for developement purposes, and should never be overriden.
     */
    @Parameter()
    public String xspecReporter;
    
    /**
     * Path to reporter/junit-report.xsl.
     * This parameter is only available for developement purposes, and should never be overriden.
     */
    @Parameter()
    public String junitReporter;
    
    /**
     * Path to reporter/coverage-report.xsl.
     * This parameter is only available for developement purposes, and should never be overriden.
     */
    @Parameter()
    public String coverageReporter;

    /**
     * Path to io/xspec/maven/xspec-maven-plugin/junit-aggregator.xsl.
     * This parameter is only available for developement purposes, and should never be overriden.
     */
    @Parameter()
    public String junitAggregator;
    
    /**
     * Path to org/mricaud/xml-utilities/get-xml-file-static-dependency-tree.xsl.
     * This parameter is only available for developement purposes, and should never be overriden.
     */
    @Parameter()
    public String dependencyScanner;

    /**
     * Directory where XSpec files are search
     */
    @Parameter(defaultValue = "${project.basedir}/src/test/xspec", required = true)
    public File testDir;
    
    /**
     * The global Saxon options. 
     * See https://github.com/cmarchand/saxonOptions-mvn-plug-utils/wiki for full documentation.
     * It allows to configure Saxon as it'll be used by plugin to run XSpecs. 
     * The main option that might be configured is xi, to activate or not XInclude.
     * <pre>
     * &lt;configuration>
     *   &lt;saxonOptions>
     *     &lt;xi>on&lt;/xi>
     *   &lt;/saxonOptions>
     * &lt;/configuration>
     * </pre>
     */
    @Parameter(name = "saxonOptions")
    public SaxonOptions saxonOptions;

    /**
     * Patterns fo files to exclude
     * Each found file that ends with an excluded value will be skipped.
     * <pre>
     *  &lt;configuration>
     *    &lt;excludes>
     *      &lt;exclude>-TI.xspec&lt;/exclude>
     *    &lt;/excludes>
     *  &lt;/configuration>
     * </pre>
     * Each file that ends with -TI.xspec will be skipped.
     */
    @Parameter(alias = "excludes")
    public List<String> excludes;
    
    /**
     * Defines if a test failure should fail the build, or not.
     * This option should NEVER be used.
     */
    @Parameter(defaultValue = "${maven.test.failure.ignore}")
    public boolean testFailureIgnore;

    /**
     * The directory where report files will be created
     */
    @Parameter(defaultValue = "${project.build.directory}/xspec-reports", required = true)
    public File reportDir;
    
    /**
     * The directory where JUnit final report will be created.
     * xspec-maven-plugin produces on junit report file per XSpec file, in 
     * <tt>reportDir</tt> directory, and creates a merged report, in <tt>junitReportDir</tt>, 
     * named <tt>TEST-xspec&lt;suffix>.xml</tt>.
     * suffix depends on execution id
     */
    @Parameter(defaultValue = "${project.build.directory}/surefire-reports", required = true)
    public File junitReportDir;

    /**
     * The catalog file to use.
     * It must conform to OASIS catalog specification. 
     * See https://www.oasis-open.org/committees/entity/spec-2001-08-06.html. 
     * If defined, this catalog must be provided, or generated before xspec-maven-plugin execution.
     * It can be an absolute or relative path. All relative pathes are relative to ${project.basedir}.
     */
    @Parameter(defaultValue = "${catalog.filename}")
    public String catalogFile;

    /**
     * The directory where surefire report will be created
     */
    @Parameter(defaultValue = "${project.build.directory}/surefire-reports", required = true)
    public File surefireReportDir;

    /**
     * Defines if a surefire report must be generated
     */
    @Parameter(defaultValue = "false")
    public Boolean generateSurefireReport;
    
    /**
     * Defines if generated catalog should be kept or not.
     * xspec-maven-plugin generates its own catalog to access its own resources, 
     * and if <tt>catalogFile</tt> is defined, adds a <tt>&lt;next-catalog /></tt>
     * entry in this generated catalog.
     * Only usefull to debug plugin.
     */
    @Parameter(defaultValue = "false")
    public Boolean keepGeneratedCatalog;
    
    @Parameter(defaultValue = "false")
    private boolean coverage;
    
    @Parameter(defaultValue = "${mojoExecution}", readonly = true)
    public MojoExecution execution;
    
    private String generateXspecUtilsUri = null;


    // package private for tests
    XmlStuff xmlStuff;
    
    // package private for test purpose
    boolean uriResolverSet = false;
    private List<ProcessedFile> processedFiles;
    private static final List<ProcessedFile> PROCESS_FILES = new ArrayList<>();
    private static URIResolver initialUriResolver;
    public static final QName QN_NAME = new QName("name");
    public static final QName QN_SELECT = new QName("select");
//    private List<File> filesToDelete, junitFiles;
    private String schLocationCompareUri;
    
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (isSkipTests()) {
            getLog().info("'skipTests' is set... skipping XSpec tests!");
            return;
        }
        XSpecImplResources xspecResources = getXSpecResources();
        SchematronImplResources schResources = getSchematronResources();
        XSpecPluginResources pluginResources = getXSpecPluginResources();
        RunnerOptions options = new RunnerOptions(
                project.getBasedir(), 
                keepGeneratedCatalog, 
                catalogFile, 
                excludes, 
                testDir,
                reportDir,
                execution==null ? null : execution.getExecutionId(),
                surefireReportDir,
                coverage);
        Properties environment = new Properties();
        environment.putAll(session.getUserProperties());
        environment.putAll(session.getSystemProperties());
        XSpecRunner runner = new XSpecRunner(getLog(), project.getBasedir());
        runner.setResources(xspecResources, schResources, pluginResources);
        runner.setEnvironment(environment, options);

        try {
            runner.init(saxonOptions);
            runner.execute();
        } catch(XSpecFailureException ex) {
            if(!testFailureIgnore) {
                throw new MojoFailureException("Some XSpec tests failed or were missed!");
            } else {
                getLog().warn("Some XSpec tests failed or were missed, but build will not fail!");
            }
        } catch (final XSpecPluginException ex) {
            throw new MojoExecutionException("While running XSpecs", ex);
        } finally {
            try {
                runner.generateIndex();
            } catch(XSpecPluginException ex2) { }
        }
    }
    

    protected boolean isSkipTests() {
        return skipTests;
    }
    
    static final String XSPEC_MOJO_PFX = "[xspec-mojo] ";
    
    private XSpecImplResources getXSpecResources() {
        DefaultXSpecImplResources ret = new DefaultXSpecImplResources();
        if(xspecXslCompiler!=null && !xspecXslCompiler.isEmpty()) {
            ret.setXSpecXslCompilerUri(xspecXslCompiler);
        }
        if(xspecXQueryCompiler!=null && !xspecXQueryCompiler.isEmpty()) {
            ret.setXSpecXQueryCompiler(xspecXQueryCompiler);
        }
        if(schSchut!=null &&!schSchut.isEmpty()) {
            ret.setSchematronSchutConverter(schSchut);
        }
        if(xspecReporter!=null && !xspecReporter.isEmpty()) {
            ret.setXSpecReporter(xspecReporter);
        }
        if(junitReporter!=null && !junitReporter.isEmpty()) {
            ret.setJUnitReporter(junitReporter);
        }
        if(coverageReporter!=null && !coverageReporter.isEmpty()) {
            ret.setXSpecCoverageReporter(coverageReporter);
        }
        return ret;
    }

    private SchematronImplResources getSchematronResources() {
        DefaultSchematronImplResources ret = new DefaultSchematronImplResources();
        if(schIsoDsdlInclude!=null && !schIsoDsdlInclude.isEmpty()) {
            ret.setSchIsoDsdlInclude(schIsoDsdlInclude);
        }
        if(schIsoAbstractExpand!=null && !schIsoAbstractExpand.isEmpty()) {
            ret.setSchIsoAbstractExpand(schIsoAbstractExpand);
        }
        if(schIsoSvrlForXslt2!=null&&!schIsoSvrlForXslt2.isEmpty()) {
            ret.setSchIsoSvrlForXslt2(schIsoSvrlForXslt2);
        }
        return ret;
    }

    private XSpecPluginResources getXSpecPluginResources() {
        DefaultXSpecPluginResources ret = new DefaultXSpecPluginResources();
        if(junitAggregator!=null && !junitAggregator.isEmpty()) {
            ret.setJunitAggregator(junitAggregator);
        }
        if(dependencyScanner!=null && !dependencyScanner.isEmpty()) {
            ret.setDependencyScanner(dependencyScanner);
        }
        return ret;
    }

}