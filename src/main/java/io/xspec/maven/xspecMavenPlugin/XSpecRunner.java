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

import io.xspec.maven.xspecMavenPlugin.resolver.Resolver;
import io.xspec.maven.xspecMavenPlugin.resources.SchematronImplResources;
import io.xspec.maven.xspecMavenPlugin.resources.XSpecImplResources;
import io.xspec.maven.xspecMavenPlugin.resources.XSpecPluginResources;
import io.xspec.maven.xspecMavenPlugin.utils.CatalogWriter;
import io.xspec.maven.xspecMavenPlugin.utils.FileFinder;
import io.xspec.maven.xspecMavenPlugin.utils.ProcessedFile;
import io.xspec.maven.xspecMavenPlugin.utils.RunnerOptions;
import io.xspec.maven.xspecMavenPlugin.utils.XSpecCompiler;
import io.xspec.maven.xspecMavenPlugin.utils.XSpecPluginException;
import io.xspec.maven.xspecMavenPlugin.utils.XmlStuff;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamSource;
import net.sf.saxon.Configuration;
import net.sf.saxon.s9api.Destination;
import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SAXDestination;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.Serializer;
import net.sf.saxon.s9api.TeeDestination;
import net.sf.saxon.s9api.XPathSelector;
import net.sf.saxon.s9api.XQueryEvaluator;
import net.sf.saxon.s9api.XQueryExecutable;
import net.sf.saxon.s9api.XdmAtomicValue;
import net.sf.saxon.s9api.XdmItem;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.XdmNodeKind;
import net.sf.saxon.s9api.XdmSequenceIterator;
import net.sf.saxon.s9api.XdmValue;
import net.sf.saxon.s9api.XsltExecutable;
import net.sf.saxon.s9api.XsltTransformer;
import org.apache.commons.io.output.NullOutputStream;
import org.apache.maven.plugin.logging.Log;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import top.marchand.maven.saxon.utils.SaxonOptions;
import io.xspec.maven.xspecMavenPlugin.utils.CompiledXSpec;
import io.xspec.maven.xspecMavenPlugin.utils.IndexGenerator;
import io.xspec.maven.xspecMavenPlugin.utils.LogProvider;
import io.xspec.maven.xspecMavenPlugin.utils.XSpecFailureException;
import io.xspec.maven.xspecMavenPlugin.utils.XSpecResultsHandler;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.net.URL;
import io.xspec.maven.xspecMavenPlugin.utils.XSpecType;
import io.xspec.maven.xspecMavenPlugin.utils.extenders.CatalogWriterExtender;

/**
 * This class implements the logic of Mojo
 * @author cmarchand
 */
public class XSpecRunner implements LogProvider {
    // technical
    private final Log log;
    private final Configuration saxonConfiguration;
    
    // environment
    private final File baseDirectory;
    
    // resources
    private XSpecImplResources xspecResources;
    private SchematronImplResources schResources;
    private XSpecPluginResources pluginResources;
    private XmlStuff xmlStuff;
    private final Properties executionProperties;
    private RunnerOptions options;
    
    // internal state management
    private boolean initDone;
    private List<ProcessedFile> processedFiles;
    private XSpecCompiler xspecCompiler;
    private CatalogWriterExtender catalogWriterExtender;

    public static final QName INITIAL_TEMPLATE_NAME=QName.fromClarkName("{http://www.jenitennison.com/xslt/xspec}main");
    private static final QName QN_REPORT_CSS_URI = new QName("report-css-uri");
    private static final String COVERAGE_ERROR_MESSAGE = "Coverage report is only available with Saxon-PE or Saxon-EE";
    private boolean failed;
    
    public XSpecRunner(final Log log, final File baseDirectory) {
        super();
        this.log = log;
        this.baseDirectory = baseDirectory;
        saxonConfiguration = getSaxonConfiguration();
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
        System.err.println("Creating XmlStuff");
        xmlStuff = new XmlStuff(
                new Processor(saxonConfiguration),
                saxonOptions,
                getLog(), 
                xspecResources,
                pluginResources,
                schResources,
                baseDirectory,
                options,
                executionProperties,
                catalogWriterExtender
        );
//        if(saxonOptions!=null) {
//            try {
//                SaxonUtils.prepareSaxonConfiguration(xmlStuff.getProcessor(), saxonOptions);
//            } catch(XPathException ex) {
//                getLog().error(ex);
//                throw new XSpecPluginException("Illegal value in Saxon configuration property", ex);
//            }
//        }
        if(options==null) options = new RunnerOptions(baseDirectory);
        xspecCompiler = new XSpecCompiler(xmlStuff, options, log);
        return this;
    }
    
    public void execute() throws XSpecPluginException, XSpecFailureException {
        getLog().debug("Looking for XSpecs in: " + options.testDir);
        final List<File> xspecs = findAllXSpecs();
        getLog().info("Found " + xspecs.size() + " XSpecs...");
        failed = false;
        processedFiles= new ArrayList<>(xspecs.size());
        for (final File xspec : xspecs) {
            try {
                if (!processXSpec(xspec)) {
                    failed = true;
                }
            } catch(IOException | TransformerException | SaxonApiException ex) {
                failed = true;
                throw new XSpecFailureException("while processing "+xspec.getAbsolutePath(), ex);
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
        XSpecType type = getXSpecType(xspecDocument);
        switch(type) {
            case XQ: return processXQueryXSpec(xspecDocument);
            case SCH: {
                XdmNode compiledSchXSpec = xspecCompiler.prepareSchematronDocument(xspecDocument);
                // it will have a problem in report with filename.
                return processXsltXSpec(compiledSchXSpec);
            }
            default: return processXsltXSpec(xspecDocument);
            
        }
    }
    
    /**
     * Process an XSpec on XQuery Test
     * @param xspec The path to the XSpec test file
     * @return true if all tests in XSpec pass, false otherwise
     */
    final boolean processXQueryXSpec(XdmNode xspec) throws SaxonApiException, FileNotFoundException, IOException {
        File sourceFile = new File(xspec.getBaseURI());
        /* compile the test stylesheet */
        final CompiledXSpec compiledXSpec = xspecCompiler.compileXSpecForXQuery(sourceFile);
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
                        // setBaseOutputURI not required, surefire-reporter.xsl 
                        // does xsl:result-document with absolute @href
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
        final CompiledXSpec compiledXSpec = xspecCompiler.compileXSpecForXslt(actualSourceFile);
        if (compiledXSpec == null) {
            return false;
        } else {
            /* execute the test stylesheet */
            final XSpecResultsHandler resultsHandler = new XSpecResultsHandler(this);
            try {
                final XsltExecutable xeXSpec = xmlStuff.compileXsl(
                        new StreamSource(compiledXSpec.getCompiledStylesheet()));
                final XsltTransformer xtXSpec = xeXSpec.load();
                // no code coverage at this point
                File tempCoverageFile = xspecCompiler.getCoverageTempPath(options.reportDir, sourceFile);
//                if(options.coverage) {
//                    try {
//                        TraceListener tl =  new XSLTCoverageTraceListener(new PrintStream(tempCoverageFile)) {
//                            @Override
//                            public boolean isUtilsStylesheet(String systemId) {
//                                return xspecResources.generateXspecUtilsUri.equals(systemId) || schLocationCompareUri.equals(systemId);
//                            }
//                            @Override
//                            public boolean isXSpecStylesheet(String systemId) {
//                                return xspecStylesheetUri.equals(systemId);
//                            }
//                        };
//                        xtXSpec.setTraceListener(tl);
//                    } catch(Exception ex) {
//                        getLog().error("while instanciating XSLTCoverageTraceListener", ex);
//                    }
//                    final String xspecStylesheetUri = compiledXSpec.getCompiledStylesheet().toURI().toString();
//                    listener.setGenerateTestsUtilsName(generateXspecUtilsUri);
//                    getLog().info("Trace listener is active");
//                    xtXSpec.setTraceListener(listener);
//                }
                xtXSpec.setInitialTemplate(INITIAL_TEMPLATE_NAME);

                getLog().info("Executing XSpec: " + compiledXSpec.getCompiledStylesheet().getName());

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
                        xt.setParameter(new QName("outputDir"), new XdmAtomicValue(options.reportDir.toURI().toURL().toExternalForm()));
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

                // JUnit report - no more JUnit report
//                XsltTransformer juReporter = xmlStuff.getJUnitReporter().load();
//                File junitFile = xspecCompiler.getJUnitReportPath(options.reportDir, sourceFile);
//                Serializer junitDest = xmlStuff.newSerializer(new FileOutputStream(junitFile));
//                junitDest.setOutputProperty(Serializer.Property.INDENT, "yes");
//                juReporter.setDestination(junitDest);
//                junitFiles.add(junitFile);
                ProcessedFile pf = new ProcessedFile(options.testDir, sourceFile, options.reportDir, xspecHtmlResult);
                processedFiles.add(pf);
                processedFileAdded = true;
                String relativeCssPath = 
                        (pf.getRelativeCssPath().length()>0 ? pf.getRelativeCssPath()+"/" : "") + XmlStuff.RESOURCES_TEST_REPORT_CSS;
//                reporter.setParameter(new QName("report-css-uri"), new XdmAtomicValue(relativeCssPath));

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
                XMLReader reader = xmlStuff.PARSER_FACTORY.newSAXParser().getXMLReader();
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
                if(options.coverage && (sourceFile.equals(actualSourceFile))) {
                    // coverage
                    if(xmlStuff.getCoverageReporter()!=null) {
                        XsltTransformer coverage = xmlStuff.getCoverageReporter().load();
                        File coverageReportFile = xspecCompiler.getCoverageFinalPath(options.reportDir, sourceFile);
                        coverage.setDestination(xmlStuff.getProcessor().newSerializer(coverageReportFile));
                        coverage.setSource(new StreamSource(tempCoverageFile));
                        getLog().info("coverage pwd: "+options.testDir.toURI().toString());
                        coverage.setParameter(new QName("pwd"),XdmAtomicValue.makeAtomicValue(options.testDir.toURI().toString()));
                        Path relative = options.testDir.toPath().relativize(sourceFile.toPath());
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
    }
    /**
     * Returns the XSpec tested file kind
     * @param doc
     * @return XSpecType
     * @throws SaxonApiException If file is not a XSpec one
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

    /**
     * Set the implementation resources to use. <b>Must be call before {@link #init() }</b>.
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
        IndexGenerator generator = new IndexGenerator(options, processedFiles);
        generator.generateIndex();
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
//    private URIResolver buildUriResolver(final URIResolver saxonUriResolver) throws IOException, XSpecPluginException {
//        CatalogWriter cw = new CatalogWriter(this.getClass().getClassLoader(), catalogWriterExtender);
//        File catalog = cw.writeCatalog(options.catalogFile, executionProperties, options.keepGeneratedCatalog);
//        if(options.keepGeneratedCatalog) {
//            getLog().info("keeping generated catalog: "+catalog.toURI().toURL().toExternalForm());
//        }
//        return new Resolver(saxonUriResolver, catalog, getLog());
//    }
    
    /**
     * We want to be sure that external-functions are allowed
     * @return 
     */
    private static Configuration getSaxonConfiguration() {
        Configuration ret = Configuration.newConfiguration();
        ret.setConfigurationProperty("http://saxon.sf.net/feature/allow-external-functions", Boolean.TRUE);
        return ret;
    }
    
    private List<File> findAllXSpecs() throws XSpecPluginException {
        FileFinder finder = new FileFinder(options.testDir, "**/*.xspec", options.excludes);
        final Path testPath = options.testDir.toPath();
        try {
            List<Path> found = finder.search();
            List<File> ret = new ArrayList<>(found.size());
            found.stream().forEach((p) -> ret.add(p.resolve(testPath).toFile()));
            return ret;
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
        } catch(TransformerException ex) {
            getLog().error("while extracting CSS: ",ex);
        }
    }
    
    // for UT only
    XmlStuff getXmlStuff() { return xmlStuff; }

    public CatalogWriterExtender getCatalogWriterExtender() {
        return catalogWriterExtender;
    }

    public void setCatalogWriterExtender(CatalogWriterExtender catalogWriterExtender) {
        this.catalogWriterExtender = catalogWriterExtender;
    }
    
}
