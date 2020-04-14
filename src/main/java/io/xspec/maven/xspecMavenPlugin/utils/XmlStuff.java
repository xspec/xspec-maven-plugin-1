/**
 * Copyright © 2017, Christophe Marchand
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
package io.xspec.maven.xspecMavenPlugin.utils;

import io.xspec.maven.xspecMavenPlugin.resolver.Resolver;
import uk.org.adamretter.maven.XSpecMojo;
import io.xspec.maven.xspecMavenPlugin.resources.SchematronImplResources;
import io.xspec.maven.xspecMavenPlugin.resources.XSpecImplResources;
import io.xspec.maven.xspecMavenPlugin.resources.XSpecPluginResources;
import io.xspec.maven.xspecMavenPlugin.utils.extenders.CatalogWriterExtender;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Properties;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;
import javax.xml.transform.stream.StreamSource;
import net.sf.saxon.lib.ExtensionFunctionDefinition;
import net.sf.saxon.s9api.DocumentBuilder;
import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.Serializer;
import net.sf.saxon.s9api.XPathCompiler;
import net.sf.saxon.s9api.XPathExecutable;
import net.sf.saxon.s9api.XPathSelector;
import net.sf.saxon.s9api.XQueryCompiler;
import net.sf.saxon.s9api.XdmAtomicValue;
import net.sf.saxon.s9api.XdmItem;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.XdmSequenceIterator;
import net.sf.saxon.s9api.XsltCompiler;
import net.sf.saxon.s9api.XsltExecutable;
import net.sf.saxon.s9api.XsltTransformer;
import net.sf.saxon.trans.XPathException;
import org.apache.maven.plugin.logging.Log;
import top.marchand.maven.saxon.utils.SaxonOptions;
import top.marchand.maven.saxon.utils.SaxonUtils;

/**
 * This class holds all utility variables need to process XSPec (XsltCompiler, XPathCompiler, compiled Xslt, and so on...)
 * @author cmarchand
 */
public class XmlStuff {
    private final Processor processor;
    private final DocumentBuilder documentBuilder;
    private final XsltCompiler xsltCompiler;
    private final XQueryCompiler xqueryCompiler;
    private final XPathCompiler xpathCompiler;
    
    private XsltExecutable xspec4xsltCompiler;
    private XsltExecutable xspec4xqueryCompiler;
    private XsltExecutable reporter;
    private XsltExecutable junitReporter;
    private XsltExecutable coverageReporter;
    private XsltExecutable xeSurefire;
    private XsltExecutable schDsdl;
    private XsltExecutable schExpand;
    private XsltExecutable schSvrl;
    private XsltExecutable xmlDependencyScanner;
    private XPathExecutable xpExecGetXSpecType;
    private XPathExecutable xpFileSearcher;

    public final static QName QN_REPORT_CSS = new QName("report-css-uri");
    public static final String RESOURCES_TEST_REPORT_CSS = "resources/test-report.css";
    private XPathExecutable xpSchGetXSpec;
    private XsltExecutable schSchut;
    private final Log log;
    private final XSpecImplResources xspecResources;
    private final XSpecPluginResources pluginResources;
    private final SchematronImplResources schematronResources;
    private final File baseDir;
    private final RunnerOptions options;
    private final Properties executionProperties;
    // for code coverage
    private String generateXspecUtilsUri;
    private String schLocationCompareUri;
    public static final SAXParserFactory PARSER_FACTORY = SAXParserFactory.newInstance();
    private static final Class[] EMPTY_PARAMS = new Class[]{};

    
    public XmlStuff(
            Processor processor,
            SaxonOptions saxonOptions,
            Log log, 
            XSpecImplResources xspecResources, 
            XSpecPluginResources pluginResources, 
            SchematronImplResources schematronResources,
            File baseDir,
            RunnerOptions options,
            Properties executionProperties,
            CatalogWriterExtender extender) throws XSpecPluginException {
        super();
        this.processor = processor;
        this.xspecResources = xspecResources;
        this.pluginResources = pluginResources;
        this.schematronResources = schematronResources;
        this.baseDir = baseDir;
        this.options=options;
        this.executionProperties=executionProperties;
        this.log=log;
        if(saxonOptions!=null) {
            try {
                SaxonUtils.prepareSaxonConfiguration(this.processor, saxonOptions);
            } catch(XPathException ex) {
                getLog().error(ex);
                throw new XSpecPluginException("Illegal value in Saxon configuration property", ex);
            }
        }
        documentBuilder = processor.newDocumentBuilder();
        try {
            xsltCompiler = processor.newXsltCompiler();
            xsltCompiler.setCompileWithTracing(true);
            xpathCompiler = processor.newXPathCompiler();
            xpathCompiler.declareNamespace("x", XSpecMojo.XSPEC_NS);
            xqueryCompiler = processor.newXQueryCompiler();
            xqueryCompiler.setCompileWithTracing(true);
            try {
                doAdditionalConfiguration(saxonOptions);
            } catch(XPathException ex) {
                throw new XSpecPluginException(ex);
            }
            try {
                xsltCompiler.setURIResolver(buildUriResolver(xsltCompiler.getURIResolver(), extender));
            } catch(IOException ex) {
                throw new XSpecPluginException("while constructing URIResolver", ex);
            }
            getLog().info("URI resolver Ok");
            ClassLoader cl = getClass().getClassLoader();
            if(cl instanceof URLClassLoader) {
                URLClassLoader ucl = (URLClassLoader)cl;
                try {
                    for(Enumeration<URL> enumer = ucl.findResources("META-INF/services/top.marchand.xml.gaulois.xml"); enumer.hasMoreElements();) {
                        URL url = enumer.nextElement();
                        log.debug("loading service "+url.toExternalForm());
                        XdmNode document = documentBuilder.build(new StreamSource(url.openStream()));
                        XPathSelector selector = xpathCompiler.compile("/gaulois-services/saxon/extensions/function").load();
                        selector.setContextItem(document);
                        Object o = selector.evaluate().iterator();
                        if(o instanceof XdmSequenceIterator) {
                            // saxon 9.8
                            XdmSequenceIterator it = (XdmSequenceIterator)o;
                            while(it.hasNext()) {
                                String className = it.next().getStringValue();
                                registerSaxonExtension(className);
                            }
                        } else {
                            Iterator<XdmItem> it = (Iterator<XdmItem>)o;
                            while(it.hasNext()) {
                                String className = it.next().getStringValue();
                                registerSaxonExtension(className);
                            }
                        }
                    }
                } catch(IOException | SaxonApiException ex) {
                    log.error("while looking for resources in /META-INF/services/top.marchand.xml.gaulois/", ex);
                }
            }
            // TODO: for next release of XSpec :
            // add extension function io.xspec.xspec.saxon.funcdefs.LineNumber
//            processor.getUnderlyingConfiguration().registerExtensionFunction(
//                    new io.xspec.xspec.saxon.funcdefs.LineNumber());
            try {
                createXPathExecutables();
                getLog().debug("XPath executables created");
                createXsltExecutables();
                getLog().debug("XSLT executables created");
            } catch(XSpecPluginException | MalformedURLException | SaxonApiException ex) {
                throw new XSpecPluginException(ex);
            }
        } catch(RuntimeException ex) {
            throw new XSpecPluginException(ex.getMessage(), ex);
        }
    }
    
    private void registerSaxonExtension(String className) {
        try {
            Class clazz = Class.forName(className);
            if(extendsClass(clazz, ExtensionFunctionDefinition.class)) {
                Class<ExtensionFunctionDefinition> cle = (Class<ExtensionFunctionDefinition>)clazz;
                Constructor<ExtensionFunctionDefinition> cc = cle.getConstructor(EMPTY_PARAMS);
                processor.getUnderlyingConfiguration().registerExtensionFunction(cc.newInstance());
                log.debug(className+"registered as Saxon extension function");
            } else {
                log.warn(className+" does not extends "+ExtensionFunctionDefinition.class.getName());
            }
        } catch(
                ClassNotFoundException | 
                InstantiationException | 
                IllegalAccessException | 
                InvocationTargetException |
                NoSuchMethodException ex) {
            log.warn("unable to load extension function "+className);
        }
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
    private URIResolver buildUriResolver(final URIResolver saxonUriResolver, CatalogWriterExtender extender) throws IOException, XSpecPluginException {
        getLog().debug("buildUriResolver");
        CatalogWriter cw = new CatalogWriter(this.getClass().getClassLoader(), extender);
        getLog().debug("CatalogWriter instanciated");
        File catalog = cw.writeCatalog(options.catalogFile, executionProperties, options.keepGeneratedCatalog);
        if(options.keepGeneratedCatalog) {
            getLog().info("keeping generated catalog: "+catalog.toURI().toURL().toExternalForm());
        }
        return new Resolver(saxonUriResolver, catalog, getLog());
    }
    
    private void createXPathExecutables() throws SaxonApiException {
        setXpExecGetXSpecType(getXPathCompiler().compile("/*/@*"));
        setXpSchGetXSpecFile(getXPathCompiler().compile(
                "iri-to-uri("
                        + "concat("
                        +   "replace(document-uri(/), '(.*)/.*$', '$1'), "
                        +   "'/', "
                        +   "/*[local-name() = 'description']/@schematron))"));
        setXpFileSearcher(getXPathCompiler().compile("//file[@dependency-type!='x:description']"));
    }
    private void createXsltExecutables() throws MalformedURLException, XSpecPluginException, SaxonApiException {
        getLog().debug("Using XSpec Xslt Compiler: " + xspecResources.getXSpecXslCompilerUri());
        getLog().debug("Using XSpec Xquery Compiler: " + xspecResources.getXSpecXQueryCompilerUri());
        getLog().debug("Using XSpec Reporter: " + xspecResources.getXSpecReporterUri());
        getLog().debug("Using JUnit Reporter: " + xspecResources.getJUnitReporterUri());
        getLog().debug("Using Coverage Reporter: " + xspecResources.getXSpecCoverageReporterUri());
        getLog().debug("Using Schematron Dsdl include: " + schematronResources.getSchIsoDsdlIncludeUri());
        getLog().debug("Using Schematron expander: " + schematronResources.getSchIsoAbstractExpandUri());
        getLog().debug("Using Schematrong Svrl: " + schematronResources.getSchIsoSvrlForXslt2Uri());
        getLog().debug("Using Schematron schut: " + xspecResources.getSchematronSchutConverterUri());
        getLog().debug("Using XML dependency scanner: " + pluginResources.getDependencyScannerUri());
        String baseUri = baseDir!=null ? baseDir.toURI().toURL().toExternalForm() : null;

        Source srcXsltCompiler = resolveSrc(xspecResources.getXSpecXslCompilerUri(), baseUri, "XSpec XSL Compiler");
        getLog().debug(xspecResources.getXSpecXslCompilerUri()+" -> "+srcXsltCompiler.getSystemId());
        Source srcXqueryCompiler = resolveSrc(xspecResources.getXSpecXQueryCompilerUri(), baseUri, "XSpec XQuery Compiler");
        getLog().debug(xspecResources.getXSpecXQueryCompilerUri()+" -> "+srcXqueryCompiler.getSystemId());
        Source srcReporter = resolveSrc(xspecResources.getXSpecReporterUri(), baseUri, "XSpec Reporter");
        getLog().debug(xspecResources.getXSpecReporterUri()+" -> "+srcReporter.getSystemId());
        Source srcJUnitReporter = resolveSrc(xspecResources.getJUnitReporterUri(), baseUri, "JUnit Reporter");
        getLog().debug(xspecResources.getJUnitReporterUri()+" -> "+srcJUnitReporter.getSystemId());
        Source srcCoverageReporter = resolveSrc(xspecResources.getXSpecCoverageReporterUri(), baseUri, "Coverage Reporter");
        getLog().debug(xspecResources.getXSpecCoverageReporterUri()+" -> "+srcCoverageReporter.getSystemId());
        Source srcSchIsoDsdl = resolveSrc(schematronResources.getSchIsoDsdlIncludeUri(), baseUri, "Schematron Dsdl");
        getLog().debug(schematronResources.getSchIsoDsdlIncludeUri()+" -> "+srcSchIsoDsdl.getSystemId());
        Source srcSchExpand = resolveSrc(schematronResources.getSchIsoAbstractExpandUri(), baseUri, "Schematron expander");
        getLog().debug(schematronResources.getSchIsoAbstractExpandUri()+" -> "+srcSchExpand.getSystemId());
        Source srcSchSvrl = resolveSrc(schematronResources.getSchIsoSvrlForXslt2Uri(), baseUri, "Schematron Svrl");
        getLog().debug(schematronResources.getSchIsoSvrlForXslt2Uri()+" -> "+srcSchSvrl.getSystemId());
        Source srcSchSchut = resolveSrc(xspecResources.getSchematronSchutConverterUri(), baseUri, "Schematron Schut");
        getLog().debug(xspecResources.getSchematronSchutConverterUri()+" -> "+srcSchSchut.getSystemId());
        Source srcXmlDependencyScanner = resolveSrc(pluginResources.getDependencyScannerUri(), baseUri, "Xml dependency scanner");
        getLog().debug(pluginResources.getDependencyScannerUri()+" -> "+srcXmlDependencyScanner.getSystemId());
        
        // for code coverage
        generateXspecUtilsUri = resolveSrc("generate-tests-utils.xsl", srcXsltCompiler.getSystemId(), "generate-tests-utils.xsl").getSystemId();
        schLocationCompareUri = resolveSrc("../schematron/sch-location-compare.xsl", srcXsltCompiler.getSystemId(), "../schematron/sch-location-compare.xsl").getSystemId();
        setXspec4xsltCompiler(compileXsl(srcXsltCompiler));
        setXspec4xqueryCompiler(compileXsl(srcXqueryCompiler));
        setReporter(compileXsl(srcReporter));
        setJUnitReporter(compileXsl(srcJUnitReporter));
        if(isSaxonPEorEE()) {
            setCoverageReporter(compileXsl(srcCoverageReporter));
        }
        setSchematronDsdl(compileXsl(srcSchIsoDsdl));
        setSchematronExpand(compileXsl(srcSchExpand));
        setSchematronSvrl(compileXsl(srcSchSvrl));
        setSchematronSchut(compileXsl(srcSchSchut));
        setXmlDependencyScanner(compileXsl(srcXmlDependencyScanner));

        setXeSurefire(compileXsl(new StreamSource(getClass().getResourceAsStream("/surefire-reporter.xsl"))));
    }
    public boolean isSaxonPEorEE() {
        String configurationClassName = processor.getUnderlyingConfiguration().getClass().getName();
        return "com.saxonica.config.ProfessionalConfiguration".equals(configurationClassName) ||
                "com.saxonica.config.EnterpriseConfiguration".equals(configurationClassName);
    }
    
    private Log getLog() { 
        return log;
    }
    public XsltExecutable compileXsl(Source source) throws SaxonApiException {
        return getXsltCompiler().compile(source);
    }

    public Processor getProcessor() {
        return processor;
    }

    public DocumentBuilder getDocumentBuilder() {
        return documentBuilder;
    }

    public XsltCompiler getXsltCompiler() {
        return xsltCompiler;
    }

    public XPathCompiler getXPathCompiler() {
        return xpathCompiler;
    }

    public XsltExecutable getXspec4xsltCompiler() {
        return xspec4xsltCompiler;
    }

    private void setXspec4xsltCompiler(XsltExecutable xspec4xsltCompiler) {
        this.xspec4xsltCompiler = xspec4xsltCompiler;
    }

    public XsltExecutable getXspec4xqueryCompiler() {
        return xspec4xqueryCompiler;
    }

    private void setXspec4xqueryCompiler(XsltExecutable xspec4xqueryCompiler) {
        this.xspec4xqueryCompiler = xspec4xqueryCompiler;
    }

    public XsltExecutable getReporter() {
        return reporter;
    }

    private void setReporter(XsltExecutable reporter) {
        this.reporter = reporter;
    }
    
    public XsltTransformer getXtReporter() {
        XsltTransformer ret = getReporter().load();
        ret.setParameter(QN_REPORT_CSS, new XdmAtomicValue(RESOURCES_TEST_REPORT_CSS));
        return ret;
    }

    /**
     * The XPath that allows to know which type of XSpec is the file
     * @return The XPath executable
     */
    public XPathExecutable getXpExecGetXSpecType() {
        return xpExecGetXSpecType;
    }

    private void setXpExecGetXSpecType(XPathExecutable xpExecGetXSpecType) {
        this.xpExecGetXSpecType = xpExecGetXSpecType;
    }
    
    public URIResolver getUriResolver() { return xsltCompiler.getURIResolver(); }

    public XsltExecutable getXeSurefire() {
        return xeSurefire;
    }

    private void setXeSurefire(XsltExecutable xeSurefire) {
        this.xeSurefire = xeSurefire;
    }
    
    public Serializer newSerializer(final OutputStream os) { return getProcessor().newSerializer(os); }
    public Serializer newSerializer() { return getProcessor().newSerializer(); }

    public XQueryCompiler getXqueryCompiler() {
        return xqueryCompiler;
    }
    
    private void doAdditionalConfiguration(SaxonOptions saxonOptions) throws XPathException {
        if(saxonOptions!=null) {
            SaxonUtils.configureXsltCompiler(getXsltCompiler(), saxonOptions);
        }
    }

    /**
     * Utility method to resolve a URI, using URIResolver. If resource can not be
     * located, uses <tt>desc</tt> to construct an error message, thrown in
     * <tt>MojoExecutionExcecution</tt>
     * @param source
     * @param baseUri
     * @param desc
     * @return Source
     * @throws XSpecPluginException
     */
    private Source resolveSrc(String source, String baseUri, String desc) throws XSpecPluginException {
        try {
            Source ret = getUriResolver().resolve(source, baseUri);
            if(ret == null) {
                throw new XSpecPluginException("Could not find "+desc+" stylesheet in: "+source);
            }
            return ret;
        } catch(TransformerException ex) {
            throw new XSpecPluginException("while resolving "+source, ex);
        }
    }
    
    private void setSchematronDsdl(XsltExecutable xe) { schDsdl = xe; }
    private void setSchematronExpand(XsltExecutable xe) { schExpand = xe; }
    private void setSchematronSvrl(XsltExecutable xe) { schSvrl = xe; }
    /**
     * Return XSL for <tt>iso_dsdl_include.xsl</tt>
     * @return iso_dsdl_include.xsl XSL
     */
    public XsltExecutable getSchematronDsdl() { return schDsdl; }
    /**
     * Return XSL for <tt>iso_abstract_expand.xsl</tt>
     * @return iso_abstract_expand.xsl XSL
     */
    public XsltExecutable getSchematronExpand() { return schExpand; }
    /**
     * Return XSL for <tt>iso_svrl_for_xslt2.xsl</tt>
     * @return iso_svrl_for_xslt2.xsl XSL
     */
    public XsltExecutable getSchematronSvrl() { return schSvrl; }
    private void setXpSchGetXSpecFile(XPathExecutable xe) { xpSchGetXSpec = xe; }
    /**
     * Return XPath that get all Schematron files
     * @return all schematron files XPath
     */
    public XPathExecutable getXpSchGetXSpecFile() { return xpSchGetXSpec; }
//    public void setXpSchGetSchParams(XPathExecutable xe) { xpSchGetParams = xe; }
//    public XPathExecutable getXpSchGetSchParams() { return xpSchGetParams; }
    private void setSchematronSchut(XsltExecutable xe) { schSchut = xe; }
    /**
     * Return XSL for <tt>schut-to-xspec.xsl</tt>
     * @return schut-to-xspec.xsl XSL
     */
    public XsltExecutable getSchematronSchut() { return schSchut; }
    /**
     * Returns XSL for dependency scanner
     * @return dependency scanner XSL
     */
    public XsltExecutable getXmlDependencyScanner() { return xmlDependencyScanner; }
    private void setXmlDependencyScanner(XsltExecutable xmlDependencyScanner) { this.xmlDependencyScanner = xmlDependencyScanner; }
    /**
     * Return XPath for file searcher
     * @return file seracher XPath
     */
    public XPathExecutable getXpFileSearcher() { return xpFileSearcher; }
    private void setXpFileSearcher(XPathExecutable xpFileSearcher) { this.xpFileSearcher = xpFileSearcher; }
    
    private boolean extendsClass(Class toCheck, Class inheritor) {
        if(toCheck.equals(inheritor)) {
            return true;
        }
        if(toCheck.equals(Object.class)) {
            return false;
        }
        return extendsClass(toCheck.getSuperclass(), inheritor);
    }
    private void setJUnitReporter(XsltExecutable xe) { junitReporter = xe ; }
    public XsltExecutable getJUnitReporter() { return junitReporter; }
    private void setCoverageReporter(XsltExecutable xe) { coverageReporter = xe; }
    public XsltExecutable getCoverageReporter() { return coverageReporter; }
}