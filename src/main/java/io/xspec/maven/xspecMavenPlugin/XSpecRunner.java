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
package io.xspec.maven.xspecMavenPlugin;

import com.jenitennison.xslt.tests.XSLTCoverageTraceListener;
import io.xspec.maven.xspecMavenPlugin.resources.SchematronImplResources;
import io.xspec.maven.xspecMavenPlugin.resources.XSpecImplResources;
import io.xspec.maven.xspecMavenPlugin.resources.XSpecPluginResources;
import io.xspec.maven.xspecMavenPlugin.utils.*;
import net.sf.saxon.Configuration;
import net.sf.saxon.lib.TraceListener;
import net.sf.saxon.s9api.*;
import net.sf.saxon.trans.UncheckedXPathException;
import org.apache.commons.io.output.NullOutputStream;
import org.apache.maven.plugin.logging.Log;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import top.marchand.maven.saxon.utils.SaxonOptions;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.ErrorListener;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamSource;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * This class implements the logic of Mojo
 * @author cmarchand
 */
public class XSpecRunner implements LogProvider {
    public final static String XSPEC_NS = "http://www.jenitennison.com/xslt/xspec";
    public static final String TRACE_SYS_PROP_IGNORE_DIR = "xspec.coverage.ignore";
    public static final String TRACE_SYS_PROP_COVERAGE_FILE = "xspec.coverage.xml";
    public static final String TRACE_SYS_PROP_XSPEC_FILE = "xspec.xspecfile";
    // technical
    private final Log log;

    // environment
    /**
     * project directory, in a maven use case
     */
    private final File baseDirectory;
    
    // resources
    private XSpecImplResources xspecResources;
    private SchematronImplResources schResources;
    private XSpecPluginResources pluginResources;
    XmlStuff xmlStuff;
    private final Properties executionProperties;
    private RunnerOptions options;
    
    // internal state management
    private boolean initDone;
    private List<ProcessedFile> processedFiles;
    private XSpecCompiler xspecCompiler;

    public static final QName INITIAL_TEMPLATE_NAME=new QName(XSPEC_NS, "main");
    public static final QName INLINE_CSS = new QName("inline-css");
    private static final String COVERAGE_ERROR_MESSAGE = "Coverage report is only available with Saxon-PE or Saxon-EE";
    private boolean failed;
    
    public XSpecRunner(final Log log, final File baseDirectory) {
        super();
        this.log = log;
        this.baseDirectory = baseDirectory;
        executionProperties = new Properties();
    }
    
    /**
     * Initalizes the runner. This method must be call before {@link #execute() }
     * @param saxonOptions The saxonOptions to use
     * @return This instance, to chain calls
     * @throws IllegalStateException If {@link #setResources(XSpecImplResources, SchematronImplResources, XSpecPluginResources) }
     * has not been call before.
     * @throws XSpecPluginException If a error occurs during initialization
     */
    public XSpecRunner init(SaxonOptions saxonOptions) throws IllegalStateException, XSpecPluginException {
        if(initDone) {
            throw new IllegalStateException("init(SaxonOptions) has already been call");
        }
        
        if(xspecResources==null || schResources==null || pluginResources==null) {
            throw new IllegalStateException(
                    "setResources(XSpecImplResources,SchematronImplResources,XSpecPluginResources) " +
                    "must be call before init()");
        }
        getLog().debug("Creating XmlStuff...");
        if(options==null) {
            getLog().debug("options was null, creating a new one.");
            options = new RunnerOptions(baseDirectory);
        }
        try {
            xmlStuff = new XmlStuff(
                    saxonOptions,
                    getLog(), 
                    xspecResources,
                    pluginResources,
                    schResources,
                    baseDirectory,
                    options,
                    executionProperties
            );
        } catch(XSpecPluginException ex) {
            getLog().error("Exception while creating XmlStuff", ex);
            throw ex;
        }
        xspecCompiler = new XSpecCompiler(xmlStuff, options, log);
        initDone = true;
        return this;
    }
    
    public void execute() throws XSpecPluginException {
        getLog().debug("Looking for XSpecs in: " + options.testDir);
        final List<File> xspecs = findAllXSpecs();
        getLog().info("Found " + xspecs.size() + " XSpecs...");
        failed = false;
        initProcessedFiles(xspecs.size());
        for (final File xspec : xspecs) {
            try {
                if (!processXSpec(xspec)) {
                    failed = true;
                }
            } catch(IOException | TransformerException | SaxonApiException | UncheckedXPathException ex) {
                failed = true;
                getLog().error("while processing "+xspec.getAbsolutePath(), ex);
            }
        }
        
        try {
            extractCssResource();
        } catch(IOException ex) {
            throw new XSpecPluginException("while extracting CSS", ex);
        }
        if (failed) {
            throw new XSpecPluginException("Some XSpec tests failed or were missed!");
        }
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
        XSpecType type = xmlStuff.getXSpecType(xspecDocument);
        getLog().debug(xspec.getName()+" is a "+type.name()+" XSpec file");
        switch(type) {
            case XQ: return processXQueryXSpec(xspecDocument);
            case SCH: return processSchematronXSpec(xspecDocument);
            case XSL: return processXsltXSpec(xspecDocument);
        }
        getLog().error("Unsupported XSpec type: "+type.name());
        return false;
    }

    private boolean processSchematronXSpec(XdmNode xspecDocument) throws SaxonApiException, TransformerException, IOException {
        XdmNode compiledSchXSpec = xspecCompiler.prepareSchematronDocument(xspecDocument);
        // it will have a problem in report with filename.
        return processXsltXSpec(compiledSchXSpec);
    }

    /**
     * Process an XSpec on XQuery Test
     * @param xspec The path to the XSpec test file
     * @return true if all tests in XSpec pass, false otherwise
     */
    private boolean processXQueryXSpec(XdmNode xspec) throws IOException {
        File sourceFile = new File(xspec.getBaseURI());
        /* compile the test stylesheet */
        final CompiledXSpec compiledXSpec = xspecCompiler.compileXSpecForXQuery(sourceFile);
        if (compiledXSpec == null) {
            getLog().error("unable to compile "+sourceFile.getAbsolutePath());
            return false;
        } else {
            getLog().debug("XQuery compiled XSpec is at "+compiledXSpec.getCompiledStylesheet().getAbsolutePath());
            /* execute the test stylesheet */
            final XSpecResultsHandler resultsHandler = new XSpecResultsHandler();
            boolean processedFileAdded = false;
            try {
                final XQueryExecutable xeXSpec = xmlStuff.getXqueryCompiler().compile(new FileInputStream(compiledXSpec.getCompiledStylesheet()));
                final XQueryEvaluator xtXSpec = xeXSpec.load();

                getLog().info("Executing XQuery XSpec: " + compiledXSpec.getCompiledStylesheet().getName());

                //setup xml report output
                final File xspecXmlResult = xspecCompiler.getXSpecXmlResultPath(options.reportDir, sourceFile);
                final Serializer xmlSerializer = xmlStuff.getProcessor().newSerializer();
                xmlSerializer.setOutputProperty(Serializer.Property.METHOD, "xml");
                xmlSerializer.setOutputProperty(Serializer.Property.INDENT, "yes");
                xmlSerializer.setOutputFile(xspecXmlResult);

                //setup html report output
                final File xspecHtmlResult = xspecCompiler.getXSpecHtmlResultPath(options.reportDir, sourceFile);
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
                        xt.setParameter(new QName("baseDir"), new XdmAtomicValue(baseDirectory.toURI().toURL().toExternalForm()));
                        xt.setParameter(new QName("outputDir"), new XdmAtomicValue(options.surefireReportDir.toURI().toURL().toExternalForm()));
                        xt.setParameter(new QName("reportFileName"), new XdmAtomicValue(xspecXmlResult.getName()));
                        xt.setDestination(xmlStuff.newSerializer(new NullOutputStream()));
                        xtSurefire = xt;
                    } catch(MalformedURLException ex) {
                        getLog().warn("Unable to generate surefire report", ex);
                    }
                } else {
                    xtSurefire = xmlStuff.newSerializer(new NullOutputStream());
                }
                ProcessedFile pf = new ProcessedFile(options.testDir, sourceFile, options.reportDir, xspecHtmlResult);
                processedFiles.add(pf);
                processedFileAdded = true;
                String relativeCssPath = 
                        (pf.getRelativeCssPath().length()>0 ? pf.getRelativeCssPath()+"/" : "") + XmlStuff.RESOURCES_TEST_REPORT_CSS;
                reporter.setParameter(XmlStuff.QN_REPORT_CSS, new XdmAtomicValue(relativeCssPath));
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
                    ProcessedFile pf = new ProcessedFile(options.testDir, sourceFile, options.reportDir, xspecCompiler.getXSpecHtmlResultPath(options.reportDir, sourceFile));
                    processedFiles.add(pf);
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
     * Process an XSpec on XSLT Test
     * @param xspec The path to the XSpec test file
     * @return true if all tests in XSpec pass, false otherwise
     */
    final boolean processXsltXSpec(XdmNode xspec) throws SaxonApiException, FileNotFoundException {
        File actualSourceFile = new File(xspec.getBaseURI());
        // Try to determine where was the original XSpec file, in case of XSpec on schematron
        File sourceFile = actualSourceFile;
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
        
        boolean wasItAnXSpecOnSchematron = !sourceFile.equals(actualSourceFile);
        /* compile the test stylesheet */
        final CompiledXSpec compiledXSpec = xspecCompiler.compileXSpecForXslt(actualSourceFile);
        if (compiledXSpec == null) {
            return false;
        } else {
            getLog().info("XSpec has been compiled");
            /* execute the test stylesheet */
            try {
                final ErrorListener errorListener = new OwnErrorListener(getLog());
                final XsltExecutable xeXSpec = xmlStuff.compileXsl(
                        new StreamSource(compiledXSpec.getCompiledStylesheet()));
                final XsltTransformer xtXSpec = xeXSpec.load();
                xtXSpec.setErrorListener(errorListener);
                
                if(wasItAnXSpecOnSchematron || !options.coverage) {
                    getLog().info("coverage not activated for "+sourceFile.getName());
                    getLog().debug("wasItAnXSpecOnSchematron: "+wasItAnXSpecOnSchematron);
                    getLog().debug("options.coverage: "+options.coverage);
                    return runXsltXspecWithoutCoverage(sourceFile, actualSourceFile, xtXSpec, compiledXSpec, errorListener);
                } else {
                    getLog().info("coverage activated for "+sourceFile.getName());
//                    File coverageFile = xspecCompiler.getCoverageTempPath(options.reportDir, sourceFile);
                    return runXsltXspecWithCoverage(sourceFile, actualSourceFile, xtXSpec, compiledXSpec, errorListener);
                }
            } finally {
                // ben, rien !
            }
        }
    }
    
    private boolean runXsltXspecWithoutCoverage(
            File sourceFile,
            File actualSourceFile,
            XsltTransformer xtXSpec,
            CompiledXSpec compiledXSpec,
            ErrorListener errorListener) {
        boolean processedFileAdded = false;
        final XSpecResultsHandler resultsHandler = new XSpecResultsHandler();
        try {
            xtXSpec.setInitialTemplate(INITIAL_TEMPLATE_NAME);

            getLog().info("Executing XSpec: " + compiledXSpec.getCompiledStylesheet().getName());

            //setup xml report output
            final File xspecXmlResult = xspecCompiler.getXSpecXmlResultPath(options.reportDir, sourceFile);
            final Serializer xmlSerializer = xmlStuff.getProcessor().newSerializer();
            xmlSerializer.setOutputProperty(Serializer.Property.METHOD, "xml");
            xmlSerializer.setOutputProperty(Serializer.Property.INDENT, "yes");
            xmlSerializer.setOutputFile(xspecXmlResult);
            getLog().debug("\txml report output set");

            //setup html report output
            final File xspecHtmlResult = xspecCompiler.getXSpecHtmlResultPath(options.reportDir, sourceFile);
            final Serializer htmlSerializer = xmlStuff.getProcessor().newSerializer();
            htmlSerializer.setOutputProperty(Serializer.Property.METHOD, "html");
            htmlSerializer.setOutputProperty(Serializer.Property.INDENT, "yes");
            htmlSerializer.setOutputFile(xspecHtmlResult);
            XsltTransformer reporter = xmlStuff.getReporter().load();
            reporter.setErrorListener(errorListener);
            reporter.setBaseOutputURI(xspecHtmlResult.toURI().toString());
            reporter.setDestination(htmlSerializer);
            getLog().debug("\thtml report output set");


            // setup surefire report output
            Destination xtSurefire = null;
            if(xmlStuff.getXeSurefire()!=null) {
                XsltTransformer xt = xmlStuff.getXeSurefire().load();
                xt.setErrorListener(errorListener);
                try {
                    xt.setParameter(new QName("baseDir"), new XdmAtomicValue(baseDirectory.toURI().toURL().toExternalForm()));
                    // issue #40
                    xt.setParameter(new QName("outputDir"), new XdmAtomicValue(options.surefireReportDir.toURI().toURL().toExternalForm()));
                    xt.setParameter(new QName("reportFileName"), new XdmAtomicValue(xspecXmlResult.getName()));
                    xt.setDestination(xmlStuff.newSerializer(new NullOutputStream()));
                    xtSurefire = xt;
                } catch(MalformedURLException ex) {
                    getLog().warn("Unable to generate surefire report", ex);
                }
            } else {
                xtSurefire = xmlStuff.newSerializer(new NullOutputStream());
            }
            getLog().debug("\tsurefire report output set");

            getLog().debug("\tcreating PF");
            ProcessedFile pf = new ProcessedFile(options.testDir, sourceFile, options.reportDir, xspecHtmlResult);
            getLog().debug("\tadding PF to list");
            processedFiles.add(pf);
            processedFileAdded = true;
            getLog().debug("\tprocessedFile processed");
            String relativeCssPath = 
                    (pf.getRelativeCssPath().length()>0 ? pf.getRelativeCssPath()+"/" : "") + XmlStuff.RESOURCES_TEST_REPORT_CSS;
            getLog().debug("\trelativeCssPath: "+relativeCssPath);
            // issue #36
            reporter.setParameter(XmlStuff.QN_REPORT_CSS, new XdmAtomicValue(relativeCssPath));

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
            getLog().debug("\tdestination tree constructed");

            XMLReader reader = xmlStuff.PARSER_FACTORY.newSAXParser().getXMLReader();
            reader.setEntityResolver((EntityResolver)xmlStuff.getUriResolver());
            Source xspecSource = new SAXSource(reader, new InputSource(new FileInputStream(sourceFile)));
            xspecSource.setSystemId(sourceFile.toURI().toString());
            xtXSpec.setSource(xspecSource);
            xtXSpec.setURIResolver(xmlStuff.getUriResolver());
            xtXSpec.setDestination(destination);
            xtXSpec.setBaseOutputURI(xspecXmlResult.toURI().toString());
            getLog().debug("\tlaunching transform");
            xtXSpec.transform();

            getLog().debug("XSpec run");
        } catch (final SaxonApiException te) {
            getLog().error(te.getMessage());
            getLog().debug(te);
            if(!processedFileAdded) {
                ProcessedFile pf = new ProcessedFile(options.testDir, sourceFile, options.reportDir, xspecCompiler.getXSpecHtmlResultPath(options.reportDir, sourceFile));
                processedFiles.add(pf);
            }
        } catch (final FileNotFoundException | ParserConfigurationException | SAXException te) {
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
    private boolean runXsltXspecWithCoverage(
            File sourceFile,
            File actualSourceFile,
            XsltTransformer xtXSpec,
            CompiledXSpec compiledXSpec,
            ErrorListener errorListener) {
        boolean processedFileAdded = false;
        final XSpecResultsHandler resultsHandler = new XSpecResultsHandler();
        try {
            File coverageFile = xspecCompiler.getCoverageTempPath(options.reportDir, sourceFile);
            getLog().debug("coverage File: "+coverageFile.getAbsolutePath());
            // WARNING : as System properties are used, a multi-threaded system can not be used !
            System.setProperty(TRACE_SYS_PROP_IGNORE_DIR, compiledXSpec.getCompiledStylesheet().getParentFile().getAbsolutePath());
            System.setProperty(TRACE_SYS_PROP_XSPEC_FILE, sourceFile.getAbsolutePath());
            System.setProperty(TRACE_SYS_PROP_COVERAGE_FILE, coverageFile.getAbsolutePath());
//            File tempCoverageFile = xspecCompiler.getCoverageTempPath(options.reportDir, sourceFile);
            try {
                // need to set system properties
                TraceListener tl =  new XSLTCoverageTraceListener();
                xtXSpec.setTraceListener(tl);
                getLog().info("Trace listener is active");
            } catch(Exception ex) {
                getLog().error("while instanciating XSLTCoverageTraceListener", ex);
            }
            xtXSpec.setInitialTemplate(INITIAL_TEMPLATE_NAME);

            getLog().info("Executing XSpec: " + compiledXSpec.getCompiledStylesheet().getName());
            final File xspecXmlResult = xspecCompiler.getXSpecXmlResultPath(options.reportDir, sourceFile);

            XMLReader reader = xmlStuff.PARSER_FACTORY.newSAXParser().getXMLReader();
            reader.setEntityResolver((EntityResolver)xmlStuff.getUriResolver());
            Source xspecSource = new SAXSource(reader, new InputSource(new FileInputStream(sourceFile)));
            xspecSource.setSystemId(sourceFile.toURI().toString());
            xtXSpec.setSource(xspecSource);
            xtXSpec.setURIResolver(xmlStuff.getUriResolver());
            XdmDestination xspecResult = new XdmDestination();
            xtXSpec.setDestination(xspecResult);
            xtXSpec.setBaseOutputURI(xspecXmlResult.toURI().toString());
            getLog().debug("\tlaunching transform");
            xtXSpec.transform();
            getLog().debug("XSpec run");


            //setup xml report output
            final Serializer xmlSerializer = xmlStuff.getProcessor().newSerializer();
            xmlSerializer.setOutputProperty(Serializer.Property.METHOD, "xml");
            xmlSerializer.setOutputProperty(Serializer.Property.INDENT, "yes");
            xmlSerializer.setOutputFile(xspecXmlResult);
            getLog().debug("\txml report output set");

            //setup html report output
            final File xspecHtmlResult = xspecCompiler.getXSpecHtmlResultPath(options.reportDir, sourceFile);
            final Serializer htmlSerializer = xmlStuff.getProcessor().newSerializer();
            htmlSerializer.setOutputProperty(Serializer.Property.METHOD, "html");
            htmlSerializer.setOutputProperty(Serializer.Property.INDENT, "yes");
            htmlSerializer.setOutputFile(xspecHtmlResult);
            XsltTransformer reporter = xmlStuff.getReporter().load();
            reporter.setErrorListener(errorListener);
            reporter.setBaseOutputURI(xspecHtmlResult.toURI().toString());
            reporter.setDestination(htmlSerializer);
            getLog().debug("\thtml report output set");


            // setup surefire report output
            Destination xtSurefire = null;
            if(xmlStuff.getXeSurefire()!=null) {
                XsltTransformer xt = xmlStuff.getXeSurefire().load();
                xt.setErrorListener(errorListener);
                try {
                    xt.setParameter(new QName("baseDir"), new XdmAtomicValue(baseDirectory.toURI().toURL().toExternalForm()));
                    // issue #40
                    xt.setParameter(new QName("outputDir"), new XdmAtomicValue(options.surefireReportDir.toURI().toURL().toExternalForm()));
                    xt.setParameter(new QName("reportFileName"), new XdmAtomicValue(xspecXmlResult.getName()));
                    xt.setDestination(xmlStuff.newSerializer(new NullOutputStream()));
                    xtSurefire = xt;
                } catch(MalformedURLException ex) {
                    getLog().warn("Unable to generate surefire report", ex);
                }
            } else {
                xtSurefire = xmlStuff.newSerializer(new NullOutputStream());
            }
            getLog().debug("\tsurefire report output set");

            getLog().debug("\tcreating PF");
            ProcessedFile pf = new ProcessedFile(options.testDir, sourceFile, options.reportDir, xspecHtmlResult);
            getLog().debug("\tadding PF to list");
            processedFiles.add(pf);
            processedFileAdded = true;
            getLog().debug("\tprocessedFile processed");
            String relativeCssPath = 
                    (pf.getRelativeCssPath().length()>0 ? pf.getRelativeCssPath()+"/" : "") + XmlStuff.RESOURCES_TEST_REPORT_CSS;
            getLog().debug("\trelativeCssPath: "+relativeCssPath);
            reporter.setParameter(XmlStuff.QN_REPORT_CSS, new XdmAtomicValue(relativeCssPath));

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
            getLog().debug("\tdestination tree constructed");

            // here, we process the XSpec result through all the destinations
            xmlStuff.getProcessor().writeXdmValue(xspecResult.getXdmNode(), destination);

            // coverage
            if(xmlStuff.getCoverageReporter()!=null) {
                XsltTransformer coverage = xmlStuff.getCoverageReporter().load();
                coverage.setErrorListener(errorListener);
                File coverageReportFile = xspecCompiler.getCoverageFinalPath(options.reportDir, sourceFile);
                pf.setCoverageFile(coverageReportFile.toPath());
                coverage.setDestination(xmlStuff.getProcessor().newSerializer(coverageReportFile));
//                coverage.setInitialContextNode(xspecResult.getXdmNode());
                coverage.setSource(new StreamSource(coverageFile));
//                getLog().info("coverage pwd: "+options.testDir.toURI().toString());
//                coverage.setParameter(new QName("pwd"),XdmAtomicValue.makeAtomicValue(options.testDir.toURI().toString()));
//                Path relative = options.testDir.toPath().relativize(sourceFile.toPath());
//                getLog().info("coverage tests: "+relative.toString());
//                coverage.setParameter(new QName("tests"), XdmAtomicValue.makeAtomicValue(relative.toString()));
                coverage.setParameter(INLINE_CSS, XdmAtomicValue.makeAtomicValue("false"));
                coverage.setParameter(XmlStuff.QN_REPORT_CSS, new XdmAtomicValue(relativeCssPath));
                coverage.transform();
            } else {
                getLog().warn(COVERAGE_ERROR_MESSAGE);
            }

        } catch (final SaxonApiException te) {
            getLog().error(te.getMessage());
            getLog().debug(te);
            if(!processedFileAdded) {
                ProcessedFile pf = new ProcessedFile(options.testDir, sourceFile, options.reportDir, xspecCompiler.getXSpecHtmlResultPath(options.reportDir, sourceFile));
                processedFiles.add(pf);
            }
        } catch (final FileNotFoundException | ParserConfigurationException | SAXException te) {
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
            getLog().debug("\tXSpec terminated, return false");
            return false;
        } else {
            getLog().info(msg);
            getLog().debug("\tXSpec terminated, return true");
            return true;
        }
    }

    /**
     * Set the implementation resources to use. <b>Must be call before {@link #init(SaxonOptions) }</b>.
     * @param xspecResources XSpec implementation resources
     * @param schResources Schematron implementation resource
     * @param pluginResources Plugin-specific implementation resources
     * @return This instance, to chain calls
     */
    public XSpecRunner setResources(
            XSpecImplResources xspecResources,
            SchematronImplResources schResources,
            XSpecPluginResources pluginResources) {
        this.xspecResources = xspecResources;
        this.schResources = schResources;
        this.pluginResources = pluginResources;
        return this;
    }
    
    /**
     * Defines environment. Mainly, <tt>session.getUserProperties()</tt>
     * and <tt>session.getSystemProperties()</tt>
     * @param executionProperties The properties provided by maven execution.
     * @param options Execution options (usually, all Mojo parameters)
     * @return 
     */
    public XSpecRunner setEnvironment(Properties executionProperties, RunnerOptions options) {
        this.executionProperties.putAll(executionProperties);
        this.options = options;
        return this;
    }
    
    /**
     * Generates general index. May be overriden if required
     * @throws XSpecPluginException 
     */
    public void generateIndex() throws XSpecPluginException {
        if(processedFiles==null) {
            throw new IllegalStateException("no execution has been done. processedFiles is null");
        }
        getLog().debug("processedFiles is "+processedFiles.size()+" length");
        IndexGenerator generator = new IndexGenerator(options, processedFiles, xmlStuff);
        generator.generateIndex();
    }
        

    /**
     * Package private to allow unit tests
     */
    List<File> findAllXSpecs() throws XSpecPluginException {
        if (!options.testDir.exists()) {
            return Collections.emptyList();
        }
        // FIXME : why **/*.xspec is defined here ???
        FileFinder finder = new FileFinder(options.testDir, "**/*.xspec", options.excludes, getLog());
        final Path testPath = options.testDir.toPath();
        try {
            return finder.search().stream()
                    .map(p -> testPath.resolve(p).toFile())
                    .collect(Collectors.toList());
        } catch(IOException ex) {
            throw new XSpecPluginException(ex);
        }
    }
    
    @Override
    public Log getLog() { return log; }
    
    protected void extractCssResource() throws MalformedURLException, IOException {
        File cssFile = new File(options.reportDir, XmlStuff.RESOURCES_TEST_REPORT_CSS);
        cssFile.getParentFile().mkdirs();
        try {
            Source cssSource = xmlStuff.getUriResolver().resolve(xspecResources.getXSpecCssReportUri(), baseDirectory.toURI().toURL().toExternalForm());
            BufferedInputStream is = new BufferedInputStream(new URL(cssSource.getSystemId()).openStream());
            BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(cssFile));
            byte[] buffer = new byte[1024];
            int read = is.read(buffer);
            while(read>0) {
                bos.write(buffer, 0, read);
                read = is.read(buffer);
            }
            is.close();
            bos.close();
        } catch(TransformerException ex) {
            getLog().error("while extracting CSS: ",ex);
        }
    }
    
    // for UT only
    XmlStuff getXmlStuff() { return xmlStuff; }

    /**
     * expose this to package to let unit tests initialize PF, 
     * when running outside of {@link #execute()} method.
     */
    void initProcessedFiles(int size) {
        processedFiles= new ArrayList<>(size);
    }
    
}
