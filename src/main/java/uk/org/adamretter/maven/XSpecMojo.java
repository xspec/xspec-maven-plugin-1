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

import io.xspec.maven.xspecMavenPlugin.resolver.Resolver;
import io.xspec.maven.xspecMavenPlugin.utils.ProcessedFile;
import io.xspec.maven.xspecMavenPlugin.utils.XmlStuff;
import net.sf.saxon.s9api.*;
import com.jenitennison.xslt.tests.XSLTCoverageTraceListener;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.*;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamSource;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import javanet.staxutils.IndentingXMLStreamWriter;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import net.sf.saxon.Configuration;
import net.sf.saxon.lib.TraceListener;
import net.sf.saxon.trans.XPathException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.input.ReaderInputStream;
import org.apache.commons.io.output.NullOutputStream;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.project.MavenProject;
import org.xml.sax.EntityResolver;
import top.marchand.maven.saxon.utils.SaxonOptions;
import top.marchand.maven.saxon.utils.SaxonUtils;

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
    public static final transient String XSLT_NS = "http://www.w3.org/1999/XSL/Transform";
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
//    @Parameter(defaultValue = XSPEC_PREFIX+"compiler/generate-xspec-tests.xsl", required = true)
    @Parameter(defaultValue = LOCAL_PREFIX+"io/xspec/maven/xspec-maven-plugin/compiler/generate-xspec-tests.xsl", required = true)
    public String xspecXslCompiler;

    // issue #12
    /**
     * Path to compiler/generate-query-tests.xsl.
     * This parameter is only available for developement purposes, and should never be overriden.
     */
//    @Parameter(defaultValue = XSPEC_PREFIX+"compiler/generate-query-tests.xsl", required = true)
    @Parameter(defaultValue = LOCAL_PREFIX+"io/xspec/maven/xspec-maven-plugin/compiler/generate-query-tests.xsl", required = true)
    public String xspecXQueryCompiler;
    
    /**
     * Path to schematron/iso-schematron/iso_dsdl_include.xsl.
     * This parameter is only available for developement purposes, and should never be overriden.
     */
    @Parameter(defaultValue = XSPEC_PREFIX+"schematron/iso-schematron/iso_dsdl_include.xsl", required = true)
    public String schIsoDsdlInclude;
    
    /**
     * Path to schematron/iso-schematron/iso_abstract_expand.xsl.
     * This parameter is only available for developement purposes, and should never be overriden.
     */
    @Parameter(defaultValue = XSPEC_PREFIX+"schematron/iso-schematron/iso_abstract_expand.xsl", required = true)
    public String schIsoAbstractExpand;
    
    /**
     * Path to schematron/iso-schematron/iso_svrl_for_xslt2.xsl.
     * This parameter is only available for developement purposes, and should never be overriden.
     */
    @Parameter(defaultValue = XSPEC_PREFIX+"schematron/iso-schematron/iso_svrl_for_xslt2.xsl", required = true)
    public String schIsoSvrlForXslt2;

    /**
     * Path to schematron/schut-to-xspec.xsl.
     * This parameter is only available for developement purposes, and should never be overriden.
     */
    @Parameter(defaultValue = LOCAL_PREFIX+"schematron/schut-to-xspec.xsl", required = true)
    public String schSchut;
    /**
     * Path to reporter/format-xspec-report.xsl.
     * This parameter is only available for developement purposes, and should never be overriden.
     */
    @Parameter(defaultValue = XSPEC_PREFIX+"reporter/format-xspec-report.xsl", required = true)
    public String xspecReporter;
    
    /**
     * Path to reporter/junit-report.xsl.
     * This parameter is only available for developement purposes, and should never be overriden.
     */
    @Parameter(defaultValue = XSPEC_PREFIX+"reporter/junit-report.xsl", required = true)
    public String junitReporter;
    
    /**
     * Path to reporter/coverage-report.xsl.
     * This parameter is only available for developement purposes, and should never be overriden.
     */
    @Parameter(defaultValue = XSPEC_PREFIX+"reporter/coverage-report.xsl", required = true)
    public String coverageReporter;

    /**
     * Path to io/xspec/maven/xspec-maven-plugin/junit-aggregator.xsl.
     * This parameter is only available for developement purposes, and should never be overriden.
     */
    @Parameter(defaultValue = LOCAL_PREFIX+"io/xspec/maven/xspec-maven-plugin/junit-aggregator.xsl")
    public String junitAggregator;
    
    /**
     * Path to org/mricaud/xml-utilities/get-xml-file-static-dependency-tree.xsl.
     * This parameter is only available for developement purposes, and should never be overriden.
     */
    @Parameter(defaultValue = XML_UTILITIES_PREFIX+"org/mricaud/xml-utilities/get-xml-file-static-dependency-tree.xsl")
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

    public static final SAXParserFactory PARSER_FACTORY = SAXParserFactory.newInstance();
    public static final Configuration SAXON_CONFIGURATION = getSaxonConfiguration();

    // package private for tests
    XmlStuff xmlStuff;
    
    // package private for test purpose
    boolean uriResolverSet = false;
    private List<ProcessedFile> processedFiles;
    private static final List<ProcessedFile> PROCESS_FILES = new ArrayList<>();
    public static final QName INITIAL_TEMPLATE_NAME=QName.fromClarkName("{http://www.jenitennison.com/xslt/xspec}main");
    private static URIResolver initialUriResolver;
    public static final QName QN_NAME = new QName("name");
    public static final QName QN_SELECT = new QName("select");
    public static final QName QN_STYLESHEET = new QName("stylesheet");
    public static final QName QN_TEST_DIR = new QName("test_dir");
    public static final QName QN_URI = new QName("uri");
    private static final QName QN_REPORT_CSS_URI = new QName("report-css-uri");
    private List<File> filesToDelete, junitFiles;
    private static final String COVERAGE_ERROR_MESSAGE = "Coverage report is only available with Saxon-PE or Saxon-EE";
    private String schLocationCompareUri;
    
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        
        if (isSkipTests()) {
            getLog().info("'skipTests' is set... skipping XSpec tests!");
            return;
        }
        filesToDelete = new ArrayList<>();
        junitFiles = new ArrayList<>();
        try {
            prepareXmlUtilities();
            getLog().debug("Looking for XSpecs in: " + getTestDir());
            final List<File> xspecs = findAllXSpecs(getTestDir());
            getLog().info("Found " + xspecs.size() + " XSpecs...");

            boolean failed = false;
            processedFiles= new ArrayList<>(xspecs.size());
            for (final File xspec : xspecs) {
                if (shouldExclude(xspec)) {
                    getLog().warn("Skipping excluded XSpec: " + xspec.getAbsolutePath());
                } else {
                    if (!processXSpec(xspec)) {
                        failed = true;
                    }
                }
            }
            
            // extract css
            File cssFile = new File(getReportDir(),XmlStuff.RESOURCES_TEST_REPORT_CSS);
            cssFile.getParentFile().mkdirs();
            try {
                Source cssSource = xmlStuff.getUriResolver().resolve(XSPEC_PREFIX+"reporter/test-report.css", project.getBasedir().toURI().toURL().toExternalForm());
                BufferedInputStream is = new BufferedInputStream(new URL(cssSource.getSystemId()).openStream());
                BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(cssFile));
                byte[] buffer = new byte[1024];
                int read = is.read(buffer);
                while(read>0) {
                    bos.write(buffer, 0, read);
                    read = is.read(buffer);
                }
            } catch(TransformerException ex) {
                getLog().error("while extracting CSS: ",ex);
            }

            createJunitReport();

            // issue #16
            if (failed) {
                if(!testFailureIgnore) {
                    throw new MojoFailureException("Some XSpec tests failed or were missed!");
                } else {
                    getLog().warn("Some XSpec tests failed or were missed, but build will not fail!");
                }
            }
        } catch (final SaxonApiException | TransformerException | IOException sae) {
            throw new MojoExecutionException(sae.getMessage(), sae);
        } finally {
            if(processedFiles==null) {
                // C'est qu'on a eu un gros problème, mais on ne sais pas lequel !
                processedFiles = new ArrayList<>();
            }
            PROCESS_FILES.addAll(processedFiles);
            // if there are many executions, index file is generated each time, but results are appended...
            generateIndex();
        }
    }
    
    /**
     * Prepares all XML stuff : XPathCompiler, XsltCompiler, compiled stylesheets,
     * Saxon configuration, and so on...
     * @throws MojoExecutionException
     * @throws MojoFailureException
     * @throws SaxonApiException
     * @throws MalformedURLException
     * @throws TransformerException 
     */
    protected void prepareXmlUtilities() throws MojoExecutionException, MojoFailureException, SaxonApiException, MalformedURLException, TransformerException {
        xmlStuff = new XmlStuff(new Processor(SAXON_CONFIGURATION), getLog());
        if(saxonOptions!=null) {
            try {
                SaxonUtils.prepareSaxonConfiguration(xmlStuff.getProcessor(), saxonOptions);
            } catch(XPathException ex) {
                getLog().error(ex);
                throw new MojoExecutionException("Illegal value in Saxon configuration property", ex);
            }
        }
        if(initialUriResolver==null) {
            initialUriResolver = xmlStuff.getUriResolver();
        }
        try {
            xmlStuff.doAdditionalConfiguration(saxonOptions);
        } catch(XPathException ex) {
            getLog().error(ex);
            throw new MojoExecutionException("Illegal value in Saxon configuration property", ex);
        }
        if (!uriResolverSet) {
            try {
                xmlStuff.setUriResolver(buildUriResolver(initialUriResolver));
                uriResolverSet = true;
            } catch(DependencyResolutionRequiredException | IOException | XMLStreamException ex) {
                throw new MojoExecutionException("while creating URI resolver", ex);
            }
        }
        // trace activation
        xmlStuff.getXsltCompiler().setCompileWithTracing(true);
        xmlStuff.getXqueryCompiler().setCompileWithTracing(true);
        xmlStuff.setXpExecGetXSpecType(xmlStuff.getXPathCompiler().compile("/*/@*"));
        xmlStuff.setXpSchGetXSpecFile(xmlStuff.getXPathCompiler().compile(
                "iri-to-uri("
                        + "concat("
                        +   "replace(document-uri(/), '(.*)/.*$', '$1'), "
                        +   "'/', "
                        +   "/*[local-name() = 'description']/@schematron))"));
        xmlStuff.setXpFileSearcher(xmlStuff.getXPathCompiler().compile("//file[@dependency-type!='x:description']"));
        getLog().debug("Using XSpec Xslt Compiler: " + getXspecXslCompiler());
        getLog().debug("Using XSpec Xquery Compiler: " + getXspecXQueryCompiler());
        getLog().debug("Using XSpec Reporter: " + getXspecReporter());
        getLog().debug("Using JUnit Reporter: "+ getJUnitReporter());
        getLog().debug("Using Coverage Reporter: "+ getCoverageReporter());
        getLog().debug("Using Schematron Dsdl include: "+getSchematronIsoDsdl());
        getLog().debug("Using Schematron expander: "+getSchematronExpand());
        getLog().debug("Using Schematrong Svrl: "+getSchematronSvrForXslt());
        getLog().debug("Using Schematron schut: "+getSchematronSchut());
        getLog().debug("Using XML dependency scanner: "+getXmlDependencyScanner());

        String baseUri = project!=null ? project.getBasedir().toURI().toURL().toExternalForm() : null;

        Source srcXsltCompiler = resolveSrc(getXspecXslCompiler(), baseUri, "XSpec XSL Compiler");
        getLog().debug(getXspecXslCompiler()+" -> "+srcXsltCompiler.getSystemId());
        Source srcXqueryCompiler = resolveSrc(getXspecXQueryCompiler(), baseUri, "XSpec XQuery Compiler");
        getLog().debug(getXspecXQueryCompiler()+" -> "+srcXqueryCompiler.getSystemId());
        Source srcReporter = resolveSrc(getXspecReporter(), baseUri, "XSpec Reporter");
        getLog().debug(getXspecReporter()+" -> "+srcReporter.getSystemId());
        Source srcJUnitReporter = resolveSrc(getJUnitReporter(), baseUri, "JUnit Reporter");
        getLog().debug(getJUnitReporter()+" -> "+srcJUnitReporter.getSystemId());
        Source srcCoverageReporter = resolveSrc(getCoverageReporter(), baseUri, "Coverage Reporter");
        getLog().debug(getCoverageReporter()+" -> "+srcCoverageReporter.getSystemId());
        Source srcSchIsoDsdl = resolveSrc(getSchematronIsoDsdl(), baseUri, "Schematron Dsdl");
        getLog().debug(getSchematronIsoDsdl()+" -> "+srcSchIsoDsdl.getSystemId());
        Source srcSchExpand = resolveSrc(getSchematronExpand(), baseUri, "Schematron expander");
        getLog().debug(getSchematronExpand()+" -> "+srcSchExpand.getSystemId());
        Source srcSchSvrl = resolveSrc(getSchematronSvrForXslt(), baseUri, "Schematron Svrl");
        getLog().debug(getSchematronSvrForXslt()+" -> "+srcSchSvrl.getSystemId());
        Source srcSchSchut = resolveSrc(getSchematronSchut(), baseUri, "Schematron Schut");
        getLog().debug(getSchematronSchut()+" -> "+srcSchSchut.getSystemId());
        Source xmlDependencyScanner = resolveSrc(getXmlDependencyScanner(), baseUri, "Xml dependency scanner");
        getLog().debug(getXmlDependencyScanner()+" -> "+xmlDependencyScanner.getSystemId());
        
        // for code coverage
        generateXspecUtilsUri = resolveSrc("generate-tests-utils.xsl", srcXsltCompiler.getSystemId(), "generate-tests-utils.xsl").getSystemId();
        schLocationCompareUri = resolveSrc("../schematron/sch-location-compare.xsl", srcXsltCompiler.getSystemId(), "../schematron/sch-location-compare.xsl").getSystemId();

        xmlStuff.setXspec4xsltCompiler(xmlStuff.compileXsl(srcXsltCompiler));
        xmlStuff.setXspec4xqueryCompiler(xmlStuff.compileXsl(srcXqueryCompiler));
        xmlStuff.setReporter(xmlStuff.compileXsl(srcReporter));
        xmlStuff.setJUnitReporter(xmlStuff.compileXsl(srcJUnitReporter));
        if(isSaxonPEorEE()) {
            xmlStuff.setCoverageReporter(xmlStuff.compileXsl(srcCoverageReporter));
        }
        xmlStuff.setSchematronDsdl(xmlStuff.compileXsl(srcSchIsoDsdl));
        xmlStuff.setSchematronExpand(xmlStuff.compileXsl(srcSchExpand));
        xmlStuff.setSchematronSvrl(xmlStuff.compileXsl(srcSchSvrl));
        xmlStuff.setSchematronSchut(xmlStuff.compileXsl(srcSchSchut));
        xmlStuff.setXmlDependencyScanner(xmlStuff.compileXsl(xmlDependencyScanner));

        if (generateSurefireReport) {
            xmlStuff.setXeSurefire(xmlStuff.compileXsl(new StreamSource(getClass().getResourceAsStream("/surefire-reporter.xsl"))));
        }

    }
    /**
     * Utility method to resolve a URI, using URIResolver. If resource can not be
     * located, uses <tt>desc</tt> to construct an error message, thrown in
     * <tt>MojoExecutionExcecution</tt>
     * @param source
     * @param baseUri
     * @param desc
     * @return
     * @throws MojoExecutionException
     * @throws TransformerException 
     */
    private Source resolveSrc(String source, String baseUri, String desc) throws MojoExecutionException, TransformerException {
        Source ret = xmlStuff.getUriResolver().resolve(source, baseUri);
        if(ret == null) {
            throw new MojoExecutionException("Could not find "+desc+" stylesheet in: "+source);
        }
        return ret;
    }
    /**
     * Generates the index.html file that point all XSpec reports.
     * All required infos are in {@link #PROCESS_FILES}
     */
    protected void generateIndex() {
        File index = new File(reportDir, "index.html");
        if(!reportDir.exists()) reportDir.mkdirs();
        try (BufferedWriter fos = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(index), Charset.forName("UTF-8")))) {
            fos.write("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\" \"http://www.w3.org/TR/html4/loose.dtd\">\n");
            fos.write("<html>");
            fos.write("<head><meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\">\n");
            fos.write("<style>\n\ttable {border: solid black 1px; border-collapse: collapse; }\n");
            fos.write("\ttr.error {background-color: red; color: white; }\n");
            fos.write("\ttr.error td a { color: white;}\n");
            fos.write("\ttr.title {background-color: lightgrey; }\n");
            fos.write("\ttd,th {border: solid black 1px; }\n");
            fos.write("\ttd:not(:first-child) {text-align: right; }\n");
            fos.write("</style>\n");
            fos.write("<title>XSpec results</title><meta name=\"date\" content=\"");
            fos.write(new SimpleDateFormat("yyyy-MM-dd").format(new Date()));
            fos.write("\"></head>\n");
            fos.write("<body><h1>XSpec results</h1>");
            fos.write("<table>\n");
            fos.write("<colgroup><col/><col class=\"successful\"/><col class=\"pending\"/><col class=\"failed\"/><col class=\"missed\"/><col/></colgroup>\n");
            fos.write("<thead><tr><th>XSpec file</th><th>Passed</th><th>Pending</th><th>Failed</th><th>Missed</th><th>Total</th></tr></thead>\n");
            fos.write("<tbody>");
            String lastRootDir="";
            for(ProcessedFile pf:PROCESS_FILES) {
                String rootDir = pf.getRootSourceDir().toString();
                if(!lastRootDir.equals(rootDir)) {
                    fos.write("<tr class=\"title\"><td colspan=\"6\">");
                    fos.write(rootDir);
                    fos.write("</td></tr>\n");
                    lastRootDir = rootDir;
                }
                int errorCount = pf.getFailed()+pf.getMissed();
                if(errorCount==0) {
                    fos.write("<tr>");
                } else {
                    fos.write("<tr class=\"error\">");
                }
                fos.write("<td><a href=\"");
                fos.write(pf.getReportFile().toUri().toString());
                fos.write("\">"+pf.getRelativeSourcePath()+"</a></td>");
                fos.write("<td");
                if(pf.getPassed()==0) fos.write(" class=\"zero\"");
                fos.write(">"+pf.getPassed()+"</td>");
                fos.write("<td");
                if(pf.getPending()==0) fos.write(" class=\"zero\"");
                fos.write(">"+pf.getPending()+"</td>");
                fos.write("<td");
                if(pf.getFailed()==0) fos.write(" class=\"zero\"");
                fos.write(">"+pf.getFailed()+"</td>");
                fos.write("<td");
                if(pf.getMissed()==0) fos.write(" class=\"zero\"");
                fos.write(">"+pf.getMissed()+"</td>");
                fos.write("<td>"+pf.getTotal()+"</td>");
                fos.write("</tr>\n");
            }
            fos.write("</tbody></table>");
            fos.write("</body></html>");
            fos.flush();
        } catch (IOException ex) {
            getLog().warn("while writing XSpec index file", ex);
        }
    }
    

    /**
     * Checks whether an XSpec should be excluded from processing
     * The comparison is performed on the filename of the xspec
     * @param xspec The filepath of the XSpec
     * @return true if the XSpec should be excluded, false otherwise
     */
    private boolean shouldExclude(final File xspec) {
        final List<String> excludePatterns = getExcludes();
        if (excludePatterns != null) {
            for (final String excludePattern : excludePatterns) {
                if (xspec.getAbsolutePath().replaceAll("\\\\","/").endsWith(excludePattern)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Process a XSpec file
     * @param xspec
     * @return <tt>true</tt> if XSpec succeed, <tt>false</tt> otherwise.
     * @throws SaxonApiException
     * @throws TransformerException
     * @throws IOException 
     */
    final boolean processXSpec(final File xspec) throws SaxonApiException, TransformerException, IOException {
        getLog().info("Processing XSpec: " + xspec.getAbsolutePath());

        XdmNode xspecDocument = xmlStuff.getDocumentBuilder().build(xspec);
        XSpecType type = getXSpecType(xspecDocument);
        switch(type) {
            case XQ: return processXQueryXSpec(xspecDocument);
            case SCH: {
            	XdmNode compiledSchXSpec = prepareSchematronDocument(xspecDocument);
                // it will have a problem in report with filename.
                return processXsltXSpec(compiledSchXSpec);
            }
            default: return processXsltXSpec(xspecDocument);
            
        }
    }
    /**
     * Process an XSpec on XSLT Test
     * @param xspec The path to the XSpec test file
     * @return true if all tests in XSpec pass, false otherwise
     */
    final boolean processXsltXSpec(XdmNode xspec) throws SaxonApiException, FileNotFoundException {
        File actualSourceFile = new File(xspec.getBaseURI());
        // Try to determine where was the original XSpec file, in case of XSpec on schematron
        File sourceFile = actualSourceFile;
        boolean processedFileAdded = false;
        XPathSelector xps = xmlStuff.getXPathCompiler().compile("/x:description/@xspec-original-location").load();
        xps.setContextItem(xspec);
        XdmItem item = xps.evaluateSingle();
        if(item!=null) {
            String value = item.getStringValue();
            if(!value.isEmpty()) {
                try {
                    sourceFile = new File(new URI(value));
                } catch (URISyntaxException ex) {
                    getLog().error("This should never be possible ! Check /x:description/@xspec-original-location", ex);
                }
            }
        }
        getLog().debug("sourceFile is "+sourceFile.getAbsolutePath());
        /* compile the test stylesheet */
        final CompiledXSpec compiledXSpec = compileXSpecForXslt(actualSourceFile, sourceFile);
        if (compiledXSpec == null) {
            return false;
        } else {
            /* execute the test stylesheet */
            final XSpecResultsHandler resultsHandler = new XSpecResultsHandler(this);
            try {
                final XsltExecutable xeXSpec = xmlStuff.compileXsl(
                        new StreamSource(compiledXSpec.getCompiledStylesheet()));
                final XsltTransformer xtXSpec = xeXSpec.load();
                File tempCoverageFile = getCoverageTempPath(getReportDir(), sourceFile);
                if(isCoverageRequired()) {
                    try {
                        TraceListener tl =  new XSLTCoverageTraceListener(new PrintStream(tempCoverageFile));
                        xtXSpec.setTraceListener(tl);
                    } catch(Exception ex) {
                        getLog().error("while instanciating XSLTCoverageTraceListener", ex);
                    }
                    final String xspecStylesheetUri = compiledXSpec.getCompiledStylesheet().toURI().toString();
                    XSLTCoverageTraceListener listener = new XSLTCoverageTraceListener(new PrintStream(tempCoverageFile)) {
                        @Override
                        public boolean isUtilsStylesheet(String systemId) {
                            // we probably need this : https://github.com/xspec/xspec/issues/191
                            return generateXspecUtilsUri.equals(systemId) || schLocationCompareUri.equals(systemId);
                        }
                        @Override
                        public boolean isXSpecStylesheet(String systemId) {
                            return xspecStylesheetUri.equals(systemId);
                        }
                    };
                    listener.setGenerateTestsUtilsName(generateXspecUtilsUri);
                    getLog().info("Trace listener is active");
                    xtXSpec.setTraceListener(listener);
                }
                xtXSpec.setInitialTemplate(INITIAL_TEMPLATE_NAME);

                getLog().info("Executing XSpec: " + compiledXSpec.getCompiledStylesheet().getName());

                //setup xml report output
                final File xspecXmlResult = getXSpecXmlResultPath(getReportDir(), sourceFile);
                final Serializer xmlSerializer = xmlStuff.getProcessor().newSerializer();
                xmlSerializer.setOutputProperty(Serializer.Property.METHOD, "xml");
                xmlSerializer.setOutputProperty(Serializer.Property.INDENT, "yes");
                xmlSerializer.setOutputFile(xspecXmlResult);

                //setup html report output
                final File xspecHtmlResult = getXSpecHtmlResultPath(getReportDir(), sourceFile);
                final Serializer htmlSerializer = xmlStuff.getProcessor().newSerializer();
                htmlSerializer.setOutputProperty(Serializer.Property.METHOD, "html");
                htmlSerializer.setOutputProperty(Serializer.Property.INDENT, "yes");
                htmlSerializer.setOutputFile(xspecHtmlResult);
                XsltTransformer reporter = xmlStuff.getReporter().load();
                reporter.setBaseOutputURI(xspecHtmlResult.toURI().toString());
                reporter.setDestination(htmlSerializer);


                // setup surefire report output
                Destination xtSurefire = null;
                if(xmlStuff.getXeSurefire()!=null) {
                    XsltTransformer xt = xmlStuff.getXeSurefire().load();
                    try {
                        xt.setParameter(new QName("baseDir"), new XdmAtomicValue(project.getBasedir().toURI().toURL().toExternalForm()));
                        xt.setParameter(new QName("outputDir"), new XdmAtomicValue(surefireReportDir.toURI().toURL().toExternalForm()));
                        xt.setParameter(new QName("reportFileName"), new XdmAtomicValue(xspecXmlResult.getName()));
                        xt.setDestination(xmlStuff.newSerializer(new NullOutputStream()));
                        // setBaseOutputURI not required, surefire-reporter.xsl 
                        // does xsl:result-document with absolute @href
                        xtSurefire = xt;
                    } catch(MalformedURLException ex) {
                        getLog().warn("Unable to generate surefire report", ex);
                    }
                } else {
                    xtSurefire = xmlStuff.newSerializer(new NullOutputStream());
                }
//                // JUnit report
//                XsltTransformer juReporter = xmlStuff.getJUnitReporter().load();
//                File junitFile = getJUnitReportPath(getReportDir(), sourceFile);
//                Serializer junitDest = xmlStuff.newSerializer(new FileOutputStream(junitFile));
//                junitDest.setOutputProperty(Serializer.Property.INDENT, "yes");
//                juReporter.setDestination(junitDest);
//                junitFiles.add(junitFile);
//                ProcessedFile pf = new ProcessedFile(testDir, sourceFile, reportDir, xspecHtmlResult);
//                processedFiles.add(pf);
//                String relativeCssPath = 
//                        (pf.getRelativeCssPath().length()>0 ? pf.getRelativeCssPath()+"/" : "") + XmlStuff.RESOURCES_TEST_REPORT_CSS;
//                reporter.setParameter(QN_REPORT_CSS_URI, new XdmAtomicValue(relativeCssPath));
//                //execute
//                final Destination destination = 
//                        new TeeDestination(
//                                new TeeDestination(
//                                        new SAXDestination(resultsHandler), 
//                                        new TeeDestination(
//                                                xmlSerializer,
//                                                xtSurefire)
//                                        ), 
//                                new TeeDestination(reporter, juReporter));
//                xtXSpec.setDestination(destination);
//                xtXSpec.setBaseOutputURI(xspecXmlResult.toURI().toString());
//            Source xspecSource = new StreamSource(sourceFile);
// utilisation du resolver dans le document source
//                XMLReader reader = PARSER_FACTORY.newSAXParser().getXMLReader();
//                reader.setEntityResolver((EntityResolver)xmlStuff.getUriResolver());
//                Source xspecSource = new SAXSource(reader, new InputSource(new FileInputStream(sourceFile)));
//                xspecSource.setSystemId(sourceFile.toURI().toString());
//                xtXSpec.setSource(xspecSource);
//                xtXSpec.setURIResolver(xmlStuff.getUriResolver());
//                xtXSpec.transform();

                // JUnit report
                XsltTransformer juReporter = xmlStuff.getJUnitReporter().load();
                File junitFile = getJUnitReportPath(getReportDir(), sourceFile);
                Serializer junitDest = xmlStuff.newSerializer(new FileOutputStream(junitFile));
                junitDest.setOutputProperty(Serializer.Property.INDENT, "yes");
                juReporter.setDestination(junitDest);
                junitFiles.add(junitFile);
                ProcessedFile pf = new ProcessedFile(testDir, sourceFile, reportDir, xspecHtmlResult);
                processedFiles.add(pf);
                processedFileAdded = true;
                String relativeCssPath = 
                        (pf.getRelativeCssPath().length()>0 ? pf.getRelativeCssPath()+"/" : "") + XmlStuff.RESOURCES_TEST_REPORT_CSS;
                reporter.setParameter(new QName("report-css-uri"), new XdmAtomicValue(relativeCssPath));

                //execute
                final Destination destination = 
                        new TeeDestination(
                                new TeeDestination(
                                        new SAXDestination(resultsHandler), 
                                        new TeeDestination(
                                                xmlSerializer,
                                                xtSurefire)
                                        ), 
                                new TeeDestination(reporter, juReporter));
                XMLReader reader = PARSER_FACTORY.newSAXParser().getXMLReader();
                reader.setEntityResolver((EntityResolver)xmlStuff.getUriResolver());
                Source xspecSource = new SAXSource(reader, new InputSource(new FileInputStream(sourceFile)));
                xspecSource.setSystemId(sourceFile.toURI().toString());
                xtXSpec.setSource(xspecSource);
                xtXSpec.setURIResolver(xmlStuff.getUriResolver());
                xtXSpec.setDestination(destination);
                xtXSpec.setBaseOutputURI(xspecXmlResult.toURI().toString());
                xtXSpec.transform();

                // limit to pure XSLT, exclude schematron
                // https://github.com/xspec/xspec/issues/191
                if(isCoverageRequired() && (sourceFile.equals(actualSourceFile))) {
                    // coverage
                    if(xmlStuff.getCoverageReporter()!=null) {
                        XsltTransformer coverage = xmlStuff.getCoverageReporter().load();
                        File coverageReportFile = getCoverageFinalPath(reportDir, sourceFile);
                        coverage.setDestination(xmlStuff.getProcessor().newSerializer(coverageReportFile));
                        coverage.setSource(new StreamSource(tempCoverageFile));
                        getLog().info("coverage pwd: "+testDir.toURI().toString());
                        coverage.setParameter(new QName("pwd"),XdmAtomicValue.makeAtomicValue(testDir.toURI().toString()));
                        Path relative = testDir.toPath().relativize(sourceFile.toPath());
    //                    Path relative = testDir.toPath().relativize(actualSourceFile.toPath());
                        getLog().info("coverage tests: "+relative.toString());
                        coverage.setParameter(new QName("tests"), XdmAtomicValue.makeAtomicValue(relative.toString()));
                        coverage.setParameter(QN_REPORT_CSS_URI, new XdmAtomicValue(relativeCssPath));
                        coverage.transform();
                    } else {
                        getLog().warn(COVERAGE_ERROR_MESSAGE);
                    }
                }

            } catch (final SaxonApiException te) {
                getLog().error(te.getMessage());
                getLog().debug(te);
                if(!processedFileAdded) {
                    ProcessedFile pf = new ProcessedFile(testDir, sourceFile, reportDir, getXSpecHtmlResultPath(getReportDir(), sourceFile));
                    processedFiles.add(pf);
                    processedFileAdded = true;
                }
            } catch (final Exception te) {
                getLog().error(te.getMessage());
                getLog().debug(te);
            }

            //missed tests come about when the XSLT processor aborts processing the XSpec due to an XSLT error
            final int missed = compiledXSpec.getTests() - resultsHandler.getTests();

            //report results
            final String msg = String.format("%s results [Passed/Pending/Failed/Missed/Total] = [%d/%d/%d/%d/%d]", 
                    sourceFile.getName(), 
                    resultsHandler.getPassed(), 
                    resultsHandler.getPending(),
                    resultsHandler.getFailed(), 
                    missed, 
                    compiledXSpec.getTests());
            if(processedFiles.size()>0) {
                processedFiles.get(processedFiles.size()-1).setResults(
                        resultsHandler.getPassed(), 
                        resultsHandler.getPending(), 
                        resultsHandler.getFailed(), 
                        missed, 
                        compiledXSpec.getTests());
            }
            if (resultsHandler.getFailed() + missed > 0) {
                getLog().error(msg);
                return false;
            } else {
                getLog().info(msg);
                return true;
            }
        }
    }
    
    private boolean isSaxonPEorEE() {
        String configurationClassName = SAXON_CONFIGURATION.getClass().getName();
        return "com.saxonica.config.ProfessionalConfiguration".equals(configurationClassName) ||
                "com.saxonica.config.EnterpriseConfiguration".equals(configurationClassName);
    }

    /**
     * Process an XSpec on XQuery Test
     * @param xspec The path to the XSpec test file
     * @return true if all tests in XSpec pass, false otherwise
     */
    final boolean processXQueryXSpec(XdmNode xspec) throws SaxonApiException, FileNotFoundException, IOException {
        File sourceFile = new File(xspec.getBaseURI());
//        boolean processedFileAdded = false;
        /* compile the test stylesheet */
        final CompiledXSpec compiledXSpec = compileXSpecForXQuery(sourceFile);
        if (compiledXSpec == null) {
            getLog().error("unable to compile "+sourceFile.getAbsolutePath());
            return false;
        } else {
            getLog().debug("XQuery compiled XSpec is at "+compiledXSpec.getCompiledStylesheet().getAbsolutePath());
            /* execute the test stylesheet */
            final XSpecResultsHandler resultsHandler = new XSpecResultsHandler(this);
            boolean processedFileAdded = false;
            try {
                final XQueryExecutable xeXSpec = xmlStuff.getXqueryCompiler().compile(new FileInputStream(compiledXSpec.getCompiledStylesheet()));
                final XQueryEvaluator xtXSpec = xeXSpec.load();

                getLog().info("Executing XQuery XSpec: " + compiledXSpec.getCompiledStylesheet().getName());

                //setup xml report output
                final File xspecXmlResult = getXSpecXmlResultPath(getReportDir(), sourceFile);
                final Serializer xmlSerializer = xmlStuff.getProcessor().newSerializer();
                xmlSerializer.setOutputProperty(Serializer.Property.METHOD, "xml");
                xmlSerializer.setOutputProperty(Serializer.Property.INDENT, "yes");
                xmlSerializer.setOutputFile(xspecXmlResult);

                //setup html report output
                final File xspecHtmlResult = getXSpecHtmlResultPath(getReportDir(), sourceFile);
                final Serializer htmlSerializer = xmlStuff.getProcessor().newSerializer();
                htmlSerializer.setOutputProperty(Serializer.Property.METHOD, "html");
                htmlSerializer.setOutputProperty(Serializer.Property.INDENT, "yes");
                htmlSerializer.setOutputFile(xspecHtmlResult);
                XsltTransformer reporter = xmlStuff.getReporter().load();
                reporter.setBaseOutputURI(xspecHtmlResult.toURI().toString());
                reporter.setDestination(htmlSerializer);


                // setup surefire report output
                Destination xtSurefire = null;
                if(xmlStuff.getXeSurefire()!=null) {
                    XsltTransformer xt = xmlStuff.getXeSurefire().load();
                    try {
                        xt.setParameter(new QName("baseDir"), new XdmAtomicValue(project.getBasedir().toURI().toURL().toExternalForm()));
                        xt.setParameter(new QName("outputDir"), new XdmAtomicValue(surefireReportDir.toURI().toURL().toExternalForm()));
                        xt.setParameter(new QName("reportFileName"), new XdmAtomicValue(xspecXmlResult.getName()));
                        xt.setDestination(xmlStuff.newSerializer(new NullOutputStream()));
                        // setBaseOutputURI not required, surefire-reporter.xsl 
                        // does xsl:result-document with absolute @href
                        xtSurefire = xt;
                    } catch(MalformedURLException ex) {
                        getLog().warn("Unable to generate surefire report", ex);
                    }
                } else {
                    xtSurefire = xmlStuff.newSerializer(new NullOutputStream());
                }
                ProcessedFile pf = new ProcessedFile(testDir, sourceFile, reportDir, xspecHtmlResult);
                processedFiles.add(pf);
                processedFileAdded = true;
                String relativeCssPath = 
                        (pf.getRelativeCssPath().length()>0 ? pf.getRelativeCssPath()+"/" : "") + XmlStuff.RESOURCES_TEST_REPORT_CSS;
                reporter.setParameter(new QName("report-css-uri"), new XdmAtomicValue(relativeCssPath));
                //execute
                // JUnit report
                XsltTransformer juReporter = xmlStuff.getJUnitReporter().load();
                File junitFile = getJUnitReportPath(getReportDir(), sourceFile);
                Serializer junitDest = xmlStuff.newSerializer(new FileOutputStream(junitFile));
                junitDest.setOutputProperty(Serializer.Property.INDENT, "yes");
                juReporter.setDestination(junitDest);
                junitFiles.add(junitFile);
                
                // Serializer nullOutput1 = xmlStuff.newSerializer(new NullOutputStream());
                final Destination destination = 
                        new TeeDestination(
                                new TeeDestination(
                                        new SAXDestination(resultsHandler), 
                                        new TeeDestination(
                                                xmlSerializer,
                                                xtSurefire)
                                        ),
                                new TeeDestination(reporter, juReporter));
                // xtXSpec.setDestination(destination);
                // ??? par quoi remplacer cela ???
//                xtXSpec.setBaseOutputURI(xspecXmlResult.toURI().toString());
                Source xspecSource = new StreamSource(sourceFile);
                xtXSpec.setSource(xspecSource);
                xtXSpec.setURIResolver(xmlStuff.getUriResolver());
                XdmValue result = xtXSpec.evaluate();
                if(result==null) {
                    getLog().debug("processXQueryXSpec result is null");
                } else {
                    getLog().debug("processXQueryXSpec result : "+result.toString());
                    xmlStuff.getProcessor().writeXdmValue(result, destination);
                }
            } catch (final SaxonApiException te) {
                getLog().error(te.getMessage());
                getLog().debug(te);
                if(!processedFileAdded) {
                    ProcessedFile pf = new ProcessedFile(testDir, sourceFile, reportDir, getXSpecHtmlResultPath(getReportDir(), sourceFile));
                    processedFiles.add(pf);
                    processedFileAdded = true;
                }
            }
            
            //missed tests come about when the XSLT processor aborts processing the XSpec due to an XSLT error
            final int missed = compiledXSpec.getTests() - resultsHandler.getTests();

            //report results
            final String msg = String.format("%s results [Passed/Pending/Failed/Missed/Total] = [%d/%d/%d/%d/%d]", 
                    sourceFile.getName(), 
                    resultsHandler.getPassed(), 
		    resultsHandler.getPending(),
                    resultsHandler.getFailed(), 
                    missed, 
                    compiledXSpec.getTests());
            if(processedFiles.size()>0) {
                processedFiles.get(processedFiles.size()-1).setResults(
                        resultsHandler.getPassed(), 
                        resultsHandler.getPending(), 
                        resultsHandler.getFailed(), 
                        missed, 
                        compiledXSpec.getTests());
            }
            if (resultsHandler.getFailed() + missed > 0) {
                getLog().error(msg);
                return false;
            } else {
                getLog().info(msg);
                return true;
            }
        }
    }
    /**
     * Compiles a XSpec file that test a XQuery
     * @param sourceFile The XSpec file to compile
     * @return The compiled XSpec informations
     */
    final CompiledXSpec compileXSpecForXQuery(final File sourceFile) {
        return compileXSpec(sourceFile, xmlStuff.getXspec4xqueryCompiler());
    }
    
    /**
     * Compiles a XSpec file that test a XSLT
     * @param sourceFile The XSpec file to compile
     * @return The compiled XSpec informations
     */
    final CompiledXSpec compileXSpecForXslt(final File sourceFile) {
        return compileXSpecForXslt(sourceFile,sourceFile);
    }
    /**
     * Compiles a XSpec file that test a XSLT
     * @param sourceFile The XSpec file to compile (that may be a compiled schematron)
     * @param initialSourceFile The initial XSpec file (in the case of Schematron
     * @return The compiled XSpec informations
     */
    final CompiledXSpec compileXSpecForXslt(final File sourceFile, File initialSourceFile) {
        return compileXSpec(sourceFile, initialSourceFile,  xmlStuff.getXspec4xsltCompiler());
    }
    
    /**
     * Compiles an XSpec using the provided XSLT XSpec compiler
     * @param xspec The XSpec test to compile (Should not be used whith the result of a schematron compilation)
     * @param compilerExec The XSpec XSLT compiler
     *
     * @return Details of the Compiled XSpec or null if the XSpec could not be
     * compiled
     */
    final CompiledXSpec compileXSpec(final File xspec, XsltExecutable compilerExec) {
    	return compileXSpec(xspec,xspec,compilerExec);
    }

    /**
     * Compiles an XSpec using the provided XSLT XSpec compiler
     * @param xspec The XSpec test to compile (That may result from a schematron compilation)
     * @param initialXspecFile The actual Xpec file
     * @param compilerExec The XSpec XSLT compiler
     *
     * @return Details of the Compiled XSpec or null if the XSpec could not be
     * compiled
     */
    final CompiledXSpec compileXSpec(final File xspec, File initialXspecFile, XsltExecutable compilerExec) {
        XsltTransformer compiler = compilerExec.load();
        InputStream isXSpec = null;
        try {
            final File compiledXSpec = getCompiledXSpecPath(getReportDir(), initialXspecFile);
            getLog().info("Compiling XSpec to XSLT: " + compiledXSpec);

            isXSpec = new FileInputStream(xspec);

            final SAXParser parser = PARSER_FACTORY.newSAXParser();
            final XMLReader reader = parser.getXMLReader();
            final XSpecTestFilter xspecTestFilter = new XSpecTestFilter(
                    reader, 
                    // Bug under Windows
                    xspec.toURI().toString(),
                    xmlStuff.getUriResolver(), 
                    this, 
                    false);

            final InputSource inXSpec = new InputSource(isXSpec);
            inXSpec.setSystemId(xspec.getAbsolutePath());

            compiler.setSource(new SAXSource(xspecTestFilter, inXSpec));

            final Serializer serializer = xmlStuff.getProcessor().newSerializer();
            serializer.setOutputFile(compiledXSpec);
            compiler.setDestination(serializer);

            compiler.transform();

            return new CompiledXSpec(xspecTestFilter.getTests(), xspecTestFilter.getPendingTests(), compiledXSpec);

        } catch (final SaxonApiException sae) {
            getLog().error(sae.getMessage());
            getLog().debug(sae);
        } catch (final ParserConfigurationException | FileNotFoundException pce) {
            getLog().error(pce);
        } catch (SAXException saxe) {
            getLog().error(saxe.getMessage());
            getLog().debug(saxe);
        } finally {
            if (isXSpec != null) {
                try {
                    isXSpec.close();
                } catch (final IOException ioe) {
                    getLog().warn(ioe);
                }
            }
        }

        return null;
    }

    /**
     * Prepare a Schematron XSpec test.
     * There two phases : 
     * <ul>
     * <li>compile schematron to a XSLT</li>
     * <li>compile the XSpec (that points to the schematron) into a XSpec that 
     * points to the XSLT (the compiled at phase 1)</li>
     * </ul>
     * 
     * @param xspecDocument
     * @return
     * @throws SaxonApiException
     * @throws TransformerException
     * @throws IOException 
     */
    protected XdmNode prepareSchematronDocument(XdmNode xspecDocument) throws SaxonApiException, TransformerException, IOException {

        XsltTransformer step1 = xmlStuff.getSchematronDsdl().load();
        XsltTransformer step2 = xmlStuff.getSchematronExpand().load();
        XsltTransformer step3 = xmlStuff.getSchematronSvrl().load();
        
        
        XsltExecutable makeAbsolute = xmlStuff.getMakeAbsolute();
        XsltTransformer step3BisMakeAbsolute = makeAbsolute.load();

        XPathSelector xp = xmlStuff.getXPathCompiler().compile("/x:description/x:param[@name='phase'][1]/text()").load();
        xp.setContextItem(xspecDocument);
        XdmItem phaseItem = xp.evaluateSingle();
        if(phaseItem!=null) {
            String phase = phaseItem.getStringValue();
            getLog().debug("Evaluating phase: "+phase);
            if(phase!=null && !phase.isEmpty()) {
                step3.setParameter(new QName("phase"), new XdmAtomicValue(phase));
            }
        }
        step3.setParameter(new QName("allow-foreign"), new XdmAtomicValue("true"));
        
        File sourceFile = new File(xspecDocument.getBaseURI());
        // compiling schematron
        step1.setDestination(step2);
        step2.setDestination(step3);
        File compiledSchematronDest = getCompiledSchematronPath(getReportDir(), sourceFile);
        Serializer serializer = xmlStuff.newSerializer(new FileOutputStream(compiledSchematronDest));
        
        step3.setDestination(step3BisMakeAbsolute);
        step3BisMakeAbsolute.setDestination(serializer);
                
        // getting from XSpec the schematron location
        XPathSelector xpSchemaPath = xmlStuff.getXPathCompiler().compile("/*/@schematron").load();
        xpSchemaPath.setContextItem(xspecDocument);
        String schematronPath = xpSchemaPath.evaluateSingle().getStringValue();
        Source source = xmlStuff.getUriResolver().resolve(schematronPath, xspecDocument.getBaseURI().toString());
        
        xmlStuff.setMakeAbsoluteParams(step3BisMakeAbsolute, source.getSystemId(), XSLT_NS);

        step1.setInitialContextNode(xmlStuff.getDocumentBuilder().build(source));
        step1.transform();
        getLog().debug("Schematron compiled ! "+compiledSchematronDest.getAbsolutePath());
        
        // modifying xspec to point to compiled schematron
        XsltTransformer schut = xmlStuff.getSchematronSchut().load();
        schut.setParameter(QN_STYLESHEET, new XdmAtomicValue(compiledSchematronDest.toURI().toString()));
        schut.setParameter(QN_TEST_DIR, new XdmAtomicValue(testDir.toURI().toString()));
        schut.setInitialContextNode(xspecDocument);
        File resultFile = getCompiledXspecSchematronPath(getReportDir(), sourceFile);
        // WARNING : we can't use a XdmDestination, the XdmNode generated does not have 
        // an absolute systemId, which is used in processXsltXSpec(XdmNode xspec)
        
        XsltTransformer compiledXSpecMakeAbsolute = makeAbsolute.load();
        xmlStuff.setMakeAbsoluteParams(compiledXSpecMakeAbsolute, xspecDocument.getBaseURI().toString(), XSPEC_NS);
        
        schut.setDestination(compiledXSpecMakeAbsolute);
        compiledXSpecMakeAbsolute.setDestination(xmlStuff.newSerializer(new FileOutputStream(resultFile)));
        
        schut.transform();
        
        getLog().debug("XSpec for schematron compiled: "+resultFile.getAbsolutePath());
        XdmNode result = xmlStuff.getDocumentBuilder().build(resultFile);
        if(!resultFile.exists()) {
            getLog().error(resultFile.getAbsolutePath()+" has not be written");
        }
       
        return result;
    }
    
    
    /**
     * Copies <tt>referencedFile</tt> located relative to <tt>baseUri</tt> to
     * <tt>resutBase</tt>/<tt>referencedFile</tt>.
     * @param baseUri
     * @param referencedFile
     * @param resultBase
     * @throws IOException
     * @throws URISyntaxException 
     */
    protected void copyFile(String baseUri, String referencedFile, File resultBase) throws IOException, URISyntaxException {
        getLog().debug("copyFile("+baseUri+", "+referencedFile+", "+resultBase.getAbsolutePath()+")");
        Path basePath = new File(new URI(baseUri)).getParentFile().toPath();
        File source = basePath.resolve(referencedFile).toFile();
        File dest = resultBase.getParentFile().toPath().resolve(referencedFile).toFile();
        getLog().info("Copying "+source.getAbsolutePath()+" to "+dest.getAbsolutePath());
        FileUtils.copyFile(source, dest);
    }
    /**
     * Get location for Compiled XSpecs
     * @param xspecReportDir The directory to place XSpec reports in
     * @param xspec The XSpec that will be compiled eventually
     * @return A filepath to place the compiled XSpec in
     */
    final File getCompiledXSpecPath(final File xspecReportDir, final File xspec) {
        return getCompiledPath(xspecReportDir, xspec, "xslt", ".xslt");
    }
    /**
     * Get location for Compiled XSpecs
     * @param xspecReportDir The directory to place XSpec reports in
     * @param schematron The Schematron that will be compiled eventually
     *
     * @return A filepath to place the compiled Schematron (a XSLT) in
     */
    final File getCompiledSchematronPath(final File xspecReportDir, final File schematron) {
        File ret = getCompiledPath(xspecReportDir, schematron, "schematron", ".xslt");
        return ret;
    }
    final File getCompiledXspecSchematronPath(final File xspecReportDir, final File xspec) {
        File ret = getCompiledPath(xspecReportDir, xspec, FilenameUtils.getBaseName(xspec.getName()),"-compiled.xspec");
        filesToDelete.add(ret);
        return ret;
    }
    /**
     * Computes the compiled XSpec location
     * @param xspecReportDir
     * @param xspec
     * @param name
     * @param extension
     * @return 
     */
    private File getCompiledPath(final File xspecReportDir, final File xspec, final String name, final String extension) {
        if (!xspecReportDir.exists()) {
            xspecReportDir.mkdirs();
        }
        Path relativeSource = testDir.toPath().relativize(xspec.toPath());
        File executionReportDir = (
                execution!=null && execution.getExecutionId()!=null && !"default".equals(execution.getExecutionId()) ? 
                new File(xspecReportDir,execution.getExecutionId()) :
                xspecReportDir);
        executionReportDir.mkdirs();
        File outputDir = executionReportDir.toPath().resolve(relativeSource).toFile();
        final File fCompiledDir = new File(outputDir, name);
        if (!fCompiledDir.exists()) {
            fCompiledDir.mkdirs();
        }
        return new File(fCompiledDir, xspec.getName() + extension);
    }

    /**
     * Get location for XSpec test report (XML Format)
     *
     * @param xspecReportDir The directory to place XSpec reports in
     * @param xspec The XSpec that will be compiled eventually
     *
     * @return A filepath for the report
     */
    final File getXSpecXmlResultPath(final File xspecReportDir, final File xspec) {
        return getXSpecResultPath(xspecReportDir, xspec, "xml");
    }

    /**
     * Get location for XSpec test report (HTML Format)
     *
     * @param xspecReportDir The directory to place XSpec reports in
     * @param xspec The XSpec that will be compiled eventually
     *
     * @return A filepath for the report
     */
    final File getXSpecHtmlResultPath(final File xspecReportDir, final File xspec) {
        return getXSpecResultPath(xspecReportDir, xspec, "html");
    }

    /**
     * Get location for XSpec test report
     *
     * @param xspecReportDir The directory to place XSpec reports in
     * @param xspec The XSpec that will be compiled eventually
     * @param extension Filename extension for the report (excluding the '.'
     *
     * @return A filepath for the report
     */
    final File getXSpecResultPath(final File xspecReportDir, final File xspec, final String extension) {
        if (!xspecReportDir.exists()) {
            xspecReportDir.mkdirs();
        }
        Path relativeSource = testDir.toPath().relativize(xspec.toPath());
        getLog().debug("executionId="+execution.getExecutionId());
        getLog().debug("relativeSource="+relativeSource.toString());
        File executionReportDir = (
                execution!=null && execution.getExecutionId()!=null && !"default".equals(execution.getExecutionId()) ? 
                new File(xspecReportDir,execution.getExecutionId()) :
                xspecReportDir);
        executionReportDir.mkdirs();
        getLog().debug("executionReportDir="+executionReportDir.getAbsolutePath());
        File outputDir = executionReportDir.toPath().resolve(relativeSource).toFile();
        getLog().debug("outputDir="+outputDir.getAbsolutePath());
        return new File(outputDir, xspec.getName().replace(".xspec", "") + "." + extension);
    }
    /**
     * Computes Junit report location
     * @param xspecReportDir
     * @param xspec
     * @return 
     */
    final File getJUnitReportPath(final File xspecReportDir, final File xspec) {
        if (!xspecReportDir.exists()) {
            xspecReportDir.mkdirs();
        }
        Path relativeSource = testDir.toPath().relativize(xspec.toPath());
        File executionReportDir = (
                execution!=null && execution.getExecutionId()!=null && !"default".equals(execution.getExecutionId()) ? 
                new File(xspecReportDir,execution.getExecutionId()) :
                xspecReportDir);
        executionReportDir.mkdirs();
        File outputDir = executionReportDir.toPath().resolve(relativeSource).toFile();
        return new File(outputDir, "junit-"+xspec.getName().replace(".xspec", "") + ".xml");
    }
    final File getCoverageTempPath(final File xspecReportDir, final File xspec) {
        if (!xspecReportDir.exists()) {
            xspecReportDir.mkdirs();
        }
        Path relativeSource = testDir.toPath().relativize(xspec.toPath());
        File executionReportDir = (
                execution!=null && execution.getExecutionId()!=null && !"default".equals(execution.getExecutionId()) ? 
                new File(xspecReportDir,execution.getExecutionId()) :
                xspecReportDir);
        executionReportDir.mkdirs();
        File outputDir = executionReportDir.toPath().resolve(relativeSource).toFile();
        return new File(outputDir, "coverage-"+xspec.getName().replace(".xspec","") + ".xml");
    }
    final File getCoverageFinalPath(final File xspecReportDir, final File xspec) {
        if (!xspecReportDir.exists()) {
            xspecReportDir.mkdirs();
        }
        Path relativeSource = testDir.toPath().relativize(xspec.toPath());
        File executionReportDir = (
                execution!=null && execution.getExecutionId()!=null && !"default".equals(execution.getExecutionId()) ? 
                new File(xspecReportDir,execution.getExecutionId()) :
                xspecReportDir);
        executionReportDir.mkdirs();
        File outputDir = executionReportDir.toPath().resolve(relativeSource).toFile();
        return new File(outputDir, xspec.getName().replace(".xspec","-coverage") + ".html");
    }

    /**
     * Recursively find any files whoose name ends '.xspec' under the directory
     * xspecTestDir
     *
     * @param xspecTestDir The directory to search for XSpec files
     *
     * @return List of XSpec files
     */
    private List<File> findAllXSpecs(final File xspecTestDir) {

        final List<File> specs = new ArrayList<>();

        if (xspecTestDir.exists()) {
            final File[] specFiles = xspecTestDir.listFiles(new FileFilter() {
                @Override
                public boolean accept(final File file) {
                    return file.isFile() && file.getName().endsWith(".xspec");
                }
            });
            specs.addAll(Arrays.asList(specFiles));

            for (final File subDir : xspecTestDir.listFiles(new FileFilter() {
                @Override
                public boolean accept(final File file) {
                    return file.isDirectory();
                }
            })) {
                specs.addAll(findAllXSpecs(subDir));
            }
        }

        return specs;
    }

    protected boolean isSkipTests() {
        return skipTests;
    }

    protected String getXspecXslCompiler() {
        return xspecXslCompiler;
    }

    protected String getXspecXQueryCompiler() {
        return xspecXQueryCompiler;
    }

    protected String getXspecReporter() {
        return xspecReporter;
    }
    
    protected String getJUnitReporter() {
        return junitReporter;
    }
    
    protected String getCoverageReporter() {
        return coverageReporter;
    }
    
    protected String getSchematronIsoDsdl() {
        return schIsoDsdlInclude;
    }
    
    protected String getSchematronExpand() {
        return schIsoAbstractExpand;
    }
    
    protected String getSchematronSvrForXslt() {
        return schIsoSvrlForXslt2;
    }
    
    protected String getSchematronSchut() {
        return schSchut;
    }
    protected String getXmlDependencyScanner() {
        return dependencyScanner;
    }

    protected File getReportDir() {
        return reportDir;
    }

    protected File getTestDir() {
        return testDir;
    }

    protected List<String> getExcludes() {
        return excludes;
    }
    
    protected Boolean isCoverageRequired() {
        return coverage;
    }
    
    /**
     * Computes the location of the jar identified by <tt>(groupId, artifactId)</tt>.
     * A dependency on this artifact must have been decalred in plugin.
     * @param groupId
     * @param artifactId
     * @return The jar URI
     * @throws IOException
     * @throws MojoFailureException 
     */
    private String getJarUri(final String groupId, final String artifactId) throws IOException, MojoFailureException {
        String thisJar = null;
        String marker = createMarker(groupId, artifactId);
        getLog().debug("marker="+marker);
        for(String s:getClassPathElements()) {
            if(s.contains(marker)) {
                thisJar = s;
                break;
            }
        }
        if(thisJar==null) {
            throw new MojoFailureException("Unable to locate xspec jar file from classpath-");
        }
        String jarUri = makeJarUri(thisJar);
        return jarUri;
    }

    /**
     * Creates the URI Resolver. It also generates the catalog with all
     * dependencies
     * @param saxonUriResolver
     * @return
     * @throws DependencyResolutionRequiredException
     * @throws IOException
     * @throws XMLStreamException
     * @throws MojoFailureException 
     */
    private URIResolver buildUriResolver(final URIResolver saxonUriResolver) throws DependencyResolutionRequiredException, IOException, XMLStreamException, MojoFailureException {
        File tmpCatalog = File.createTempFile("tmp", "-catalog.xml");
        try (OutputStreamWriter osw = new OutputStreamWriter(new FileOutputStream(tmpCatalog), Charset.forName("UTF-8"))) {
            XMLStreamWriter xmlWriter = XMLOutputFactory.newFactory().createXMLStreamWriter(osw);
            xmlWriter = new IndentingXMLStreamWriter(xmlWriter);
            xmlWriter.writeStartDocument("UTF-8", "1.0");
            xmlWriter.writeStartElement("catalog");
            xmlWriter.setDefaultNamespace(CATALOG_NS);
            xmlWriter.writeNamespace("", CATALOG_NS);
            // io.spec / xspec
            String jarUri = getJarUri("io.xspec", "xspec");
            writeCatalogEntry(xmlWriter, jarUri, XSPEC_PREFIX);
            // io.xspec / xspec-maven-plugin
            // TODO : change this when Matthieu will publish https://github.com/mricaud/xml-utilities as an artifact
            jarUri = getJarUri("io.xspec.maven", "xspec-maven-plugin");
            writeCatalogEntry(xmlWriter, jarUri, XML_UTILITIES_PREFIX);
            // io.xspec.maven / xspec-maven-plugin
            jarUri = getJarUri("io.xspec.maven", "xspec-maven-plugin");
            writeCatalogEntry(xmlWriter, jarUri, LOCAL_PREFIX);
            if(catalogFile!=null) {
                xmlWriter.writeEmptyElement("nextCatalog");
                Properties props = new Properties();
                props.putAll(session.getUserProperties());
                props.putAll(session.getSystemProperties());
                String catalogFilename = org.codehaus.plexus.util.StringUtils.interpolate(catalogFile, props);
                try {
                    URI uri = new URI(catalogFilename);
                    if(uri.isAbsolute()) {
                        xmlWriter.writeAttribute("catalog", uri.toString());
                    } else {
                        xmlWriter.writeAttribute("catalog", new File(catalogFilename).toURI().toURL().toExternalForm());
                    }
                } catch(Exception ex) {
                    xmlWriter.writeAttribute("catalog", new File(catalogFilename).toURI().toURL().toExternalForm());
                }
            }
            xmlWriter.writeEndElement();
            xmlWriter.writeEndDocument();
            osw.flush();
        }
        if(!keepGeneratedCatalog) tmpCatalog.deleteOnExit();
        else getLog().info("keeping generated catalog: "+tmpCatalog.toURI().toURL().toExternalForm());
        return new Resolver(saxonUriResolver, tmpCatalog, getLog());
    }
    
    /**
     * Writes a catalog entries for a jar and a URI prefix
     * @param xmlWriter
     * @param jarUri
     * @param prefix
     * @throws XMLStreamException 
     */
    private void writeCatalogEntry(final XMLStreamWriter xmlWriter, final String jarUri, String prefix) throws XMLStreamException {
        xmlWriter.writeEmptyElement("rewriteURI");
        xmlWriter.writeAttribute("uriStartString", prefix);
        xmlWriter.writeAttribute("rewritePrefix", jarUri);
        xmlWriter.writeEmptyElement("rewriteSystem");
        xmlWriter.writeAttribute("uriStartString", prefix);
        xmlWriter.writeAttribute("rewritePrefix", jarUri);
    }
    
    /**
     * Transform a jar file to a URI, as to must be declared in catalog entries
     * @param jarFile
     * @return
     * @throws MalformedURLException 
     */
    private String makeJarUri(String jarFile) throws MalformedURLException {
        getLog().debug(String.format("makeJarUri(%s)", jarFile));
        return "jar:" + jarFile +"!/";
    }
    /**
     * Returns classpath entries
     * @return
     * @throws MojoFailureException 
     */
    private List<String> getClassPathElements() throws MojoFailureException {
        ClassLoader cl = getClass().getClassLoader();
        if(cl instanceof URLClassLoader) {
            URLClassLoader ucl = (URLClassLoader)cl;
            List<String> ret = new ArrayList(ucl.getURLs().length);
            for(URL u:ucl.getURLs()) {
                ret.add(u.toExternalForm());
            }
            return ret;
        } else {
            throw new MojoFailureException("classloader is not a URL classloader : "+cl.getClass().getName());
        }
    }
    /**
     * Creates a marker of a jar file. This marker is used to identify the jar
     * associated to a <tt>(groupId, artifactId)</tt>
     * @param groupId
     * @param artifactId
     * @return
     * @throws IOException 
     */
    private String createMarker(String groupId, String artifactId) throws IOException {
        Properties props = new Properties();
        props.load(getClass().getResourceAsStream("/META-INF/maven/"+groupId+"/"+artifactId+"/pom.properties"));
        return String.format("%s-%s", props.getProperty("artifactId"), props.getProperty("version"));
    }

    static final String XSPEC_MOJO_PFX = "[xspec-mojo] ";
    
    /**
     * Creates the unified JUnit report, which groups all JUnit reports from
     * each XSpec file into a common one
     * @throws MalformedURLException
     * @throws TransformerException
     * @throws SaxonApiException 
     */
    private void createJunitReport() throws MalformedURLException, TransformerException, SaxonApiException {
        String baseUri = project!=null ? project.getBasedir().toURI().toURL().toExternalForm() : null;
        XsltTransformer xsl = xmlStuff.compileXsl(xmlStuff.getUriResolver().resolve(junitAggregator, baseUri)).load();
        xsl.setParameter(new QName("baseDir"), new XdmAtomicValue(testDir.toURI().toString()));
        StringBuilder sb = new StringBuilder("<files>");
        for(File f: junitFiles) {
            sb.append("<file>").append(f.toURI().toString()).append("</file>");
        }
        sb.append("</files>");
        xsl.setSource(new StreamSource(new ReaderInputStream(new StringReader(sb.toString()))));
        String fileName = execution!=null && execution.getExecutionId()!=null ? "TEST-xspec-"+execution.getExecutionId()+".xml" : "TEST-xspec.xml";
        Serializer ser = xmlStuff.getProcessor().newSerializer(new File(junitReportDir, fileName));
        ser.setOutputProperty(Serializer.Property.INDENT, "yes");
        xsl.setDestination(ser);
        xsl.setMessageListener(new MessageListener() {
            @Override
            public void message(XdmNode xn, boolean bln, SourceLocator sl) {
                getLog().debug(xn.getStringValue());
            }
        });
        xsl.transform();
    }
        
    private static Configuration getSaxonConfiguration() {
        Configuration ret = Configuration.newConfiguration();
        ret.setConfigurationProperty("http://saxon.sf.net/feature/allow-external-functions", Boolean.TRUE);
        return ret;
    }
    /**
     * Returns the nature of the XSpec tested file
     * @param doc
     * @return
     * @throws SaxonApiException 
     */
    XSpecType getXSpecType(XdmNode doc) throws SaxonApiException {
        XPathSelector xps = xmlStuff.getXpExecGetXSpecType().load();
        xps.setContextItem(doc);
        XdmValue values = xps.evaluate();
        for(XdmSequenceIterator it=values.iterator();it.hasNext();) {
            XdmNode item=(XdmNode)(it.next());
            if(item.getNodeKind().equals(XdmNodeKind.ATTRIBUTE)) {
                String nodeName = item.getNodeName().getLocalName();
                switch(nodeName) {
                    case "query": 
                    case "query-at": {
                        return XSpecType.XQ;
                    }
                    case "schematron": {
                        return XSpecType.SCH;
                    }
                    case "stylesheet": {
                        return XSpecType.XSL;
                    }
                }
            }
        }
        throw new SaxonApiException("This file does not seem to be a valid XSpec file: "+doc.getBaseURI().toString());
    }

    public enum XSpecType {
        XSL, SCH, XQ;
    }
}