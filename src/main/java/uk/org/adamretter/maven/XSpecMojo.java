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
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import net.sf.saxon.Configuration;
import net.sf.saxon.trans.XPathException;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.output.NullOutputStream;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.project.MavenProject;
import top.marchand.maven.saxon.utils.SaxonOptions;
import top.marchand.maven.saxon.utils.SaxonUtils;

/**
 * Goal which runs any XSpec tests in src/test/xspec
 *
 * @author <a href="mailto:adam.retter@googlemail.com">Adam Retter</a>
 * @author <a href="mailto:christophe@marchand.top">Christophe Marchand</a>
 */
@Mojo(name = "run-xspec", defaultPhase = LifecyclePhase.VERIFY, requiresDependencyResolution = ResolutionScope.TEST)
public class XSpecMojo extends AbstractMojo implements LogProvider {
    public static final transient String XSPEC_PREFIX = "xspec:/";
    public static final transient String CATALOG_NS = "urn:oasis:names:tc:entity:xmlns:xml:catalog";

    @Parameter( defaultValue = "${project}", readonly = true, required = true )
    private MavenProject project;

    @Parameter(property = "skipTests", defaultValue = "false")
    private boolean skipTests;

    /**
     * Location of the XSpec-for-XSLT Compiler XSLT i.e. generate-xspec-tests.xsl
     */
    @Parameter(defaultValue = XSPEC_PREFIX+"xspec/compiler/generate-xspec-tests.xsl", required = true)
    private String xspecXslCompiler;

    // issue #12
    /**
     * Location of the XSpec-for-XQ Compiler XSLT i.e. generate-xspec-tests.xsl
     */
    @Parameter(defaultValue = XSPEC_PREFIX+"xspec/compiler/generate-query-tests.xsl", required = true)
    private String xspecXQueryCompiler;
    
    /**
     * Location of the Schematron iso_dsdl_include
     */
    @Parameter(defaultValue = XSPEC_PREFIX+"schematron/iso-schematron/iso_dsdl_include.xsl", required = true)
    private String schIsoDsdlInclude;
    
    /**
     * Location of the Schematron iso abstract expand
     */
    @Parameter(defaultValue = XSPEC_PREFIX+"schematron/iso-schematron/iso_abstract_expand.xsl", required = true)
    private String schIsoAbstractExpand;
    
    /**
     * Location of the Schematron iso svrl for xslt2
     */
    @Parameter(defaultValue = XSPEC_PREFIX+"schematron/iso-schematron/iso_svrl_for_xslt2.xsl", required = true)
    private String schIsoSvrlForXslt2;

    /**
     * Location of the XSL that transforms the xspec for schematron in a real xspec
     */
    @Parameter(defaultValue = XSPEC_PREFIX+"schematron/schut-to-xspec.xsl", required = true)
    private String schSchut;
    /**
     * Location of the XSpec Reporter XSLT i.e. format-xspec-report.xsl
     */
    @Parameter(defaultValue = XSPEC_PREFIX+"xspec/reporter/format-xspec-report.xsl", required = true)
    private String xspecReporter;

    /**
     * Location of the XSpec tests
     */
    @Parameter(defaultValue = "${project.basedir}/src/test/xspec", required = true)
    private File testDir;
    
    @Parameter(name = "saxonOptions")
    private SaxonOptions saxonOptions;

    /**
     * *
     * Exclude various XSpec tests
     */
    @Parameter(alias = "excludes")
    private List<String> excludes;

    /**
     * Location of the XSpec reports
     */
    @Parameter(defaultValue = "${project.build.directory}/xspec-reports", required = true)
    private File reportDir;

    @Parameter(defaultValue = "${catalog.filename}")
    private File catalogFile;

    @Parameter(defaultValue = "${project.build.directory}/surefire-reports", required = true)
    private File surefireReportDir;

    @Parameter(defaultValue = "false")
    private Boolean generateSurefireReport;
    
    @Parameter(defaultValue = "false")
    private Boolean keepGeneratedCatalog;
    
    @Parameter(defaultValue = "${mojoExecution}", readonly = true)
    private MojoExecution execution;

    public static final SAXParserFactory PARSER_FACTORY = SAXParserFactory.newInstance();
    private static final Configuration SAXON_CONFIGURATION = getSaxonConfiguration();

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
    private List<File> filesToDelete;
    
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        
        if (isSkipTests()) {
            getLog().info("'skipTests' is set... skipping XSpec tests!");
            return;
        }
        filesToDelete = new ArrayList<>();
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
                Source cssSource = xmlStuff.getUriResolver().resolve(XSPEC_PREFIX+"xspec/reporter/test-report.css", project.getBasedir().toURI().toURL().toExternalForm());
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

            if (failed) {
                throw new MojoFailureException("Some XSpec tests failed or were missed!");
            }
        } catch (final SaxonApiException | TransformerException | IOException sae) {
            throw new MojoExecutionException(sae.getMessage(), sae);
        } finally {
            getLog().debug("PROCESS_FILES is "+(PROCESS_FILES==null?"":"not ")+" null");
            getLog().debug("processedFiles is "+(processedFiles==null?"":"not ")+" null");
            if(processedFiles==null) {
                // C'est qu'on a eu un gros problème, mais on ne sais pas lequel !
                processedFiles = new ArrayList<>();
            }
            PROCESS_FILES.addAll(processedFiles);
            // if there are many executions, index file is generated each time, but results are appended...
            generateIndex();
            for(File file:filesToDelete) file.delete();
        }
    }
    
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
        xmlStuff.setXpExecGetXSpecType(xmlStuff.getXPathCompiler().compile("/*/@*"));
        xmlStuff.setXpSchGetXSpecFile(xmlStuff.getXPathCompiler().compile(
                "iri-to-uri("
                        + "concat("
                        +   "replace(document-uri(/), '(.*)/.*$', '$1'), "
                        +   "'/', "
                        +   "/*[local-name() = 'description']/@schematron))"));
//        xmlStuff.setXpSchGetSchParams(xmlStuff.getXPathCompiler().compile(
//                "declare function local:escape($v) { " +
//                    "let $w := if (matches($v,codepoints-to-string((91,92,115,93)))) then codepoints-to-string(34) else '' " +
//                    "return concat(" +
//                        "$w, "+
//                        "replace($v,codepoints-to-string((40,91,36,92,92,96,93,41)),codepoints-to-string((92,92,36,49)))," +
//                        "$w)" +
//                "}; " +
//                "string-join(" +
//                    "for $p in /*/*[local-name() = 'param'] "+
//                        "return "+
//                            "if ($p/@select) then concat('?',$p/@name,'=',local:escape($p/@select)) "+
//                            "else concat($p/@name,'=',local:escape($p/string())),' ')"));
        getLog().debug("Using XSpec Xslt Compiler: " + getXspecXslCompiler());
        getLog().debug("Using XSpec Xquery Compiler: " + getXspecXQueryCompiler());
        getLog().debug("Using XSpec Reporter: " + getXspecReporter());
        getLog().debug("Using Schematron Dsdl include: "+getSchematronIsoDsdl());
        getLog().debug("Using Schematron expander: "+getSchematronExpand());
        getLog().debug("Using Schematrong Svrl: "+getSchematronSvrForXslt());
        getLog().debug("Using Schematron schut: "+getSchematronSchut());

        String baseUri = project!=null ? project.getBasedir().toURI().toURL().toExternalForm() : null;

        Source srcXsltCompiler = resolveSrc(getXspecXslCompiler(), baseUri, "XSpec Compiler");
        getLog().debug(getXspecXslCompiler()+" -> "+srcXsltCompiler.getSystemId());
        Source srcXqueryCompiler = resolveSrc(getXspecXQueryCompiler(), baseUri, "XSpec Compiler");
        getLog().debug(getXspecXQueryCompiler()+" -> "+srcXqueryCompiler.getSystemId());
        Source srcReporter = resolveSrc(getXspecReporter(), baseUri, "XSpec Reporter");
        getLog().debug(getXspecReporter()+" -> "+srcReporter.getSystemId());
        Source srcSchIsoDsdl = resolveSrc(getSchematronIsoDsdl(), baseUri, "Schematron Dsdl");
        getLog().debug(getSchematronIsoDsdl()+" -> "+srcSchIsoDsdl.getSystemId());
        Source srcSchExpand = resolveSrc(getSchematronExpand(), baseUri, "Schematron expander");
        getLog().debug(getSchematronExpand()+" -> "+srcSchExpand.getSystemId());
        Source srcSchSvrl = resolveSrc(getSchematronSvrForXslt(), baseUri, "Schematron Svrl");
        getLog().debug(getSchematronSvrForXslt()+" -> "+srcSchSvrl.getSystemId());
        Source srcSchSchut = resolveSrc(getSchematronSchut(), baseUri, "Schematron Schut");
        getLog().debug(getSchematronSchut()+" -> "+srcSchSchut.getSystemId());

        xmlStuff.setXspec4xsltCompiler(xmlStuff.compileXsl(srcXsltCompiler));
        xmlStuff.setXspec4xqueryCompiler(xmlStuff.compileXsl(srcXqueryCompiler));
        xmlStuff.setReporter(xmlStuff.compileXsl(srcReporter));
        xmlStuff.setSchematronDsdl(xmlStuff.compileXsl(srcSchIsoDsdl));
        xmlStuff.setSchematronExpand(xmlStuff.compileXsl(srcSchExpand));
        xmlStuff.setSchematronSvrl(xmlStuff.compileXsl(srcSchSvrl));
        xmlStuff.setSchematronSchut(xmlStuff.compileXsl(srcSchSchut));

        if (generateSurefireReport) {
            xmlStuff.setXeSurefire(xmlStuff.compileXsl(new StreamSource(getClass().getResourceAsStream("/surefire-reporter.xsl"))));
        }

    }
    private Source resolveSrc(String source, String baseUri, String desc) throws MojoExecutionException, TransformerException {
        Source ret = xmlStuff.getUriResolver().resolve(source, baseUri);
        if(ret == null) {
            throw new MojoExecutionException("Could not find "+desc+" stylesheet in: "+source);
        }
        return ret;
    }
    private void generateIndex() {
        File index = new File(reportDir, "index.html");
        if(!reportDir.exists()) reportDir.mkdirs();
        try (BufferedWriter fos = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(index), Charset.forName("UTF-8")))) {
            fos.write("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\" \"http://www.w3.org/TR/html4/loose.dtd\">\n");
            fos.write("<html>");
            fos.write("<head><meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\">\n");
            fos.write("<style>\n\ttable {border: solid black 1px; border-collapse: collapse; }\n");
            fos.write("\ttd,th {border: solid black 1px; }\n");
            fos.write("\ttd:not(:first-child) {text-align: right; }\n");
            fos.write("</style>\n");
            fos.write("<title>XSpec results</title><meta name=\"date\" content=\"");
            fos.write(new SimpleDateFormat("yyyy-MM-dd").format(new Date()));
            fos.write("\"></head>\n");
            fos.write("<body><h3>XSpec results</h3>");
            fos.write("<table><thead><tr><th>XSpec file</th><th>Passed</th><th>Pending</th><th>Failed</th><th>Missed</th><th>Total</th></tr></thead>\n");
            fos.write("<tbody>");
            String lastRootDir="";
            for(ProcessedFile pf:PROCESS_FILES) {
                String rootDir = pf.getRootSourceDir().toString();
                if(!lastRootDir.equals(rootDir)) {
                    fos.write("<tr><td colspan=\"6\">");
                    fos.write(rootDir);
                    fos.write("</td></tr>\n");
                    lastRootDir = rootDir;
                }
                fos.write("<tr><td><a href=\"");
                fos.write(pf.getReportFile().toUri().toString());
                fos.write("\">"+pf.getRelativeSourcePath()+"</a></td>");
                fos.write("<td>"+pf.getPassed()+"</td>");
                fos.write("<td>"+pf.getPending()+"</td>");
                fos.write("<td>"+pf.getFailed()+"</td>");
                fos.write("<td>"+pf.getMissed()+"</td>");
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
     *
     * The comparison is performed on the filename of the xspec
     *
     * @param xspec The filepath of the XSpec
     *
     * @return true if the XSpec should be excluded, false otherwise
     */
    private boolean shouldExclude(final File xspec) {
        final List<String> excludePatterns = getExcludes();
        if (excludePatterns != null) {
            for (final String excludePattern : excludePatterns) {
                // FIXME : getAbsolutePath can return some \ in Windows...
                if (xspec.getAbsolutePath().endsWith(excludePattern)) {
                    return true;
                }
            }
        }
        return false;
    }

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
     *
     * @param xspec The path to the XSpec test file
     *
     * @return true if all tests in XSpec pass, false otherwise
     */
    final boolean processXsltXSpec(XdmNode xspec) throws SaxonApiException {
        File sourceFile = new File(xspec.getBaseURI());
        /* compile the test stylesheet */
        final CompiledXSpec compiledXSpec = compileXSpecForXslt(sourceFile);
        if (compiledXSpec == null) {
            return false;
        } else {
            /* execute the test stylesheet */
            final XSpecResultsHandler resultsHandler = new XSpecResultsHandler(this);
            try {
                final XsltExecutable xeXSpec = xmlStuff.compileXsl(
                        new StreamSource(compiledXSpec.getCompiledStylesheet()));
                final XsltTransformer xtXSpec = xeXSpec.load();
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
                ProcessedFile pf = new ProcessedFile(testDir, sourceFile, reportDir, xspecHtmlResult);
                processedFiles.add(pf);
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
                                reporter);
                xtXSpec.setDestination(destination);
                xtXSpec.setBaseOutputURI(xspecXmlResult.toURI().toString());
                Source xspecSource = new StreamSource(sourceFile);
                xtXSpec.setSource(xspecSource);
                xtXSpec.setURIResolver(xmlStuff.getUriResolver());
                xtXSpec.transform();

            } catch (final SaxonApiException te) {
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

    /**
     * Process an XSpec on XQuery Test
     *
     * @param xspec The path to the XSpec test file
     *
     * @return true if all tests in XSpec pass, false otherwise
     */
    final boolean processXQueryXSpec(XdmNode xspec) throws SaxonApiException, FileNotFoundException, IOException {
        File sourceFile = new File(xspec.getBaseURI());
        /* compile the test stylesheet */
        final CompiledXSpec compiledXSpec = compileXSpecForXQuery(sourceFile);
        if (compiledXSpec == null) {
            return false;
        } else {
            /* execute the test stylesheet */
            final XSpecResultsHandler resultsHandler = new XSpecResultsHandler(this);
            try {
                final XQueryExecutable xeXSpec = xmlStuff.getXqueryCompiler().compile(new FileInputStream(compiledXSpec.getCompiledStylesheet()));
                final XQueryEvaluator xtXSpec = xeXSpec.load();

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
                ProcessedFile pf = new ProcessedFile(testDir, sourceFile, reportDir, xspecHtmlResult);
                processedFiles.add(pf);
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
                                reporter);
                xtXSpec.setDestination(destination);
                // ??? par quoi remplacer cela ???
//                xtXSpec.setBaseOutputURI(xspecXmlResult.toURI().toString());
                Source xspecSource = new StreamSource(sourceFile);
                xtXSpec.setSource(xspecSource);
                xtXSpec.setURIResolver(xmlStuff.getUriResolver());
                xtXSpec.evaluate();
            } catch (final SaxonApiException te) {
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
    final CompiledXSpec compileXSpecForXQuery(final File sourceFile) {
        return compileXSpec(sourceFile, xmlStuff.getXspec4xqueryCompiler());
    }
    final CompiledXSpec compileXSpecForXslt(final File sourceFile) {
        return compileXSpec(sourceFile, xmlStuff.getXspec4xsltCompiler());
    }

    /**
     * Compiles an XSpec using the provided XSLT XSpec compiler
     *
     * @param compiler The XSpec XSLT compiler
     * @param xspec The XSpec test to compile
     *
     * @return Details of the Compiled XSpec or null if the XSpec could not be
     * compiled
     */
    final CompiledXSpec compileXSpec(final File sourceFile, XsltExecutable compilerExec) {
        XsltTransformer compiler = compilerExec.load();
        InputStream isXSpec = null;
        try {
            final File compiledXSpec = getCompiledXSpecPath(getReportDir(), sourceFile);
            getLog().info("Compiling XSpec to XSLT: " + compiledXSpec);

            isXSpec = new FileInputStream(sourceFile);

            final SAXParser parser = PARSER_FACTORY.newSAXParser();
            final XMLReader reader = parser.getXMLReader();
            final XSpecTestFilter xspecTestFilter = new XSpecTestFilter(
                    reader, 
                    sourceFile.getAbsolutePath(), 
                    xmlStuff.getUriResolver(), 
                    this, 
                    false);

            final InputSource inXSpec = new InputSource(isXSpec);
            inXSpec.setSystemId(sourceFile.getAbsolutePath());

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

    protected XdmNode prepareSchematronDocument(XdmNode xspecDocument) throws SaxonApiException, TransformerException, FileNotFoundException {
//        XPathSelector xp = xmlStuff.getXpSchGetXSpecFile().load();
//        xp.setContextItem(xspecDocument);
//        String schematronFilePath = xp.evaluateSingle().getStringValue();

        XsltTransformer step1 = xmlStuff.getSchematronDsdl().load();
        XsltTransformer step2 = xmlStuff.getSchematronExpand().load();
        XsltTransformer step3 = xmlStuff.getSchematronSvrl().load();

        XPathSelector xp = xmlStuff.getXPathCompiler().compile("/*/*[local-name() = 'param']").load();
        xp.setContextItem(xspecDocument);
        for(XdmSequenceIterator it = xp.evaluate().iterator();it.hasNext();) {
            XdmNode param = (XdmNode)it.next();
            QName paramName = new QName(param.getAttributeValue(QN_NAME),param);
            String select = param.getAttributeValue(QN_SELECT);
            if(select!=null) {
                // we have to execute the XPath to get the value
                step3.setParameter(paramName, xmlStuff.getXPathCompiler().compile(select).load().evaluate());
            } else {
                step3.setParameter(paramName, new XdmAtomicValue(param.getStringValue()));
            }
        }
        
        File sourceFile = new File(xspecDocument.getBaseURI());
        // compiling schematron
        step1.setDestination(step2);
        step2.setDestination(step3);
        File compiledSchematronDest = getCompiledSchematronPath(getReportDir(), sourceFile);
        Serializer serializer = xmlStuff.newSerializer(new FileOutputStream(compiledSchematronDest));
        step3.setDestination(serializer);
        
        // getting from XSpec the schematron location
        XPathSelector xpSchemaPath = xmlStuff.getXPathCompiler().compile("/*/@schematron").load();
        xpSchemaPath.setContextItem(xspecDocument);
        String schematronPath = xpSchemaPath.evaluateSingle().getStringValue();
        Source source = xmlStuff.getUriResolver().resolve(schematronPath, xspecDocument.getBaseURI().toString());
        step1.setInitialContextNode(xmlStuff.getDocumentBuilder().build(source));
        step1.transform();
        getLog().debug("Scematron compiled !");
        // modifying xspec to point to compiled schematron
        XsltTransformer schut = xmlStuff.getSchematronSchut().load();
        schut.setParameter(QN_STYLESHEET, new XdmAtomicValue(compiledSchematronDest.toURI().toString()));
        // TODO
        schut.setParameter(QN_TEST_DIR, new XdmAtomicValue(testDir.toURI().toString()));
        schut.setInitialContextNode(xspecDocument);
        File resultFile = getCompiledXspecSchematronPath(getReportDir(), sourceFile);
        getLog().debug("schematron compiled XSpec: "+resultFile.getAbsolutePath());
        schut.setDestination(xmlStuff.newSerializer(new FileOutputStream(resultFile)));
        schut.transform();
        XdmNode result = xmlStuff.getDocumentBuilder().build(resultFile);
        getLog().info("XSpec for schematron is now "+resultFile.getAbsolutePath());
        return result;
    }
    /**
     * Get location for Compiled XSpecs
     *
     * @param xspecReportDir The directory to place XSpec reports in
     * @param xspec The XSpec that will be compiled eventually
     *
     * @return A filepath to place the compiled XSpec in
     */
    final File getCompiledXSpecPath(final File xspecReportDir, final File xspec) {
        return getCompiledPath(xspecReportDir, xspec, "xslt", ".xslt");
    }
    /**
     * Get location for Compiled XSpecs
     *
     * @param xspecReportDir The directory to place XSpec reports in
     * @param schematron The Schematron that will be compiled eventually
     *
     * @return A filepath to place the compiled Schematron (a XSLT) in
     */
    final File getCompiledSchematronPath(final File xspecReportDir, final File schematron) {
        File ret = getCompiledPath(xspecReportDir, schematron, "schematron", ".xslt");
        getLog().debug("compiled schematron: "+ret.getAbsolutePath());
        return ret;
    }
    final File getCompiledXspecSchematronPath(final File xspecReportDir, final File xspec) {
//        File dir = xspec.getParentFile();
//        String fileName = FilenameUtils.getBaseName(xspec.getName())+"-compiled.xspec";
//        File ret = new File(dir, fileName);
        File ret = getCompiledPath(xspecReportDir, xspec, FilenameUtils.getBaseName(xspec.getName()),"-compiled.xspec");
        filesToDelete.add(ret);
        return ret;
    }
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

    protected File getReportDir() {
        return reportDir;
    }

    protected File getTestDir() {
        return testDir;
    }

    protected List<String> getExcludes() {
        return excludes;
    }

    private URIResolver buildUriResolver(final URIResolver saxonUriResolver) throws DependencyResolutionRequiredException, IOException, XMLStreamException, MojoFailureException {
        String thisJar = null;
        String marker = createMarker();
        getLog().debug("marker="+marker);
        for(String s:getClassPathElements()) {
            if(s.contains(marker)) {
                thisJar = s;
                break;
            }
        }
        if(thisJar==null) {
            throw new MojoFailureException("Unable to locate plugin jar file from classpath-");
        }
        String jarUri = makeJarUri(thisJar);
        File tmpCatalog = File.createTempFile("tmp", "-catalog.xml");
        try (OutputStreamWriter osw = new OutputStreamWriter(new FileOutputStream(tmpCatalog), Charset.forName("UTF-8"))) {
            XMLStreamWriter xmlWriter = XMLOutputFactory.newFactory().createXMLStreamWriter(osw);
            xmlWriter.writeStartDocument("UTF-8", "1.0");
            xmlWriter.writeStartElement("catalog");
            xmlWriter.setDefaultNamespace(CATALOG_NS);
            xmlWriter.writeNamespace("", CATALOG_NS);
            xmlWriter.writeStartElement("rewriteURI");
            xmlWriter.writeAttribute("uriStartString", XSPEC_PREFIX);
            xmlWriter.writeAttribute("rewritePrefix", jarUri);
            xmlWriter.writeEndElement();
            xmlWriter.writeStartElement("rewriteSystem");
            xmlWriter.writeAttribute("uriStartString", XSPEC_PREFIX);
            xmlWriter.writeAttribute("rewritePrefix", jarUri);
            xmlWriter.writeEndElement();
            if(catalogFile!=null) {
                xmlWriter.writeStartElement("nextCatalog");
                xmlWriter.writeAttribute("catalog", catalogFile.toURI().toURL().toExternalForm());
                xmlWriter.writeEndElement();
            }
            xmlWriter.writeEndElement();
            xmlWriter.writeEndDocument();
            osw.flush();
        }
        if(!keepGeneratedCatalog) tmpCatalog.deleteOnExit();
        else getLog().info("keeping generated catalog: "+tmpCatalog.toURI().toURL().toExternalForm());
        return new Resolver(saxonUriResolver, tmpCatalog, getLog());
    }
    
    private String makeJarUri(String jarFile) throws MalformedURLException {
        getLog().debug(String.format("makeJarUri(%s)", jarFile));
        return "jar:" + jarFile +"!/";
    }
    
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
    
    private String createMarker() throws IOException {
        Properties props = new Properties();
        props.load(getClass().getResourceAsStream("/xspec-maven-plugin.properties"));
        return String.format("%s-%s", props.getProperty("plugin.artifactId"), props.getProperty("plugin.version"));
    }

    static final String XSPEC_MOJO_PFX = "[xspec-mojo] ";
        
    private static Configuration getSaxonConfiguration() {
        Configuration ret = Configuration.newConfiguration();
        ret.setConfigurationProperty("http://saxon.sf.net/feature/allow-external-functions", Boolean.TRUE);
        return ret;
    }
    
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