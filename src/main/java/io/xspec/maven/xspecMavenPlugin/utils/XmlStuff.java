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

import io.xspec.maven.xspecMavenPlugin.resources.SchematronImplResources;
import io.xspec.maven.xspecMavenPlugin.resources.XSpecImplResources;
import io.xspec.maven.xspecMavenPlugin.resources.XSpecPluginResources;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;
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
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.XdmSequenceIterator;
import net.sf.saxon.s9api.XsltCompiler;
import net.sf.saxon.s9api.XsltExecutable;
import net.sf.saxon.s9api.XsltTransformer;
import net.sf.saxon.trans.XPathException;
import org.apache.maven.plugin.logging.Log;
import top.marchand.maven.saxon.utils.SaxonOptions;
import top.marchand.maven.saxon.utils.SaxonUtils;
import uk.org.adamretter.maven.XSpecMojo;

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
//    private XPathExecutable xpSchGetParams;
    private XsltExecutable schSchut;
    private final Log log;
    private final XSpecImplResources xspecResources;
    private final XSpecPluginResources pluginResources;
    private final SchematronImplResources schematronResources;
    private final File baseDir;
    private String generateXspecUtilsUri;
    private String schLocationCompareUri;
    public static final SAXParserFactory PARSER_FACTORY = SAXParserFactory.newInstance();

    
    public XmlStuff(
            Processor processor,
            SaxonOptions saxonOptions,
            Log log, 
            XSpecImplResources xspecResources, 
            XSpecPluginResources pluginResources, 
            SchematronImplResources schematronResources,
            File baseDir) throws XSpecPluginException {
        super();
        this.processor = processor;
        this.xspecResources = xspecResources;
        this.pluginResources = pluginResources;
        this.schematronResources = schematronResources;
        this.baseDir = baseDir;
        if(saxonOptions!=null) {
            try {
                SaxonUtils.prepareSaxonConfiguration(this.processor, saxonOptions);
            } catch(XPathException ex) {
                getLog().error(ex);
                throw new XSpecPluginException("Illegal value in Saxon configuration property", ex);
            }
        }

        
        documentBuilder = processor.newDocumentBuilder();
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
        this.log=log;
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
                    XdmSequenceIterator it = selector.evaluate().iterator();
                    while(it.hasNext()) {
                        String className = it.next().getStringValue();
                        try {
                            Class clazz = Class.forName(className);
                            if(extendsClass(clazz, ExtensionFunctionDefinition.class)) {
                                Class<ExtensionFunctionDefinition> cle = (Class<ExtensionFunctionDefinition>)clazz;
                                processor.getUnderlyingConfiguration().registerExtensionFunction(cle.newInstance());
                                log.debug(className+"registered as Saxon extension function");
                            } else {
                                log.warn(className+" does not extends "+ExtensionFunctionDefinition.class.getName());
                            }
                        } catch(ClassNotFoundException | InstantiationException | IllegalAccessException ex) {
                            log.warn("unable to load extension function "+className);
                        }
                    }
                }
            } catch(IOException | SaxonApiException ex) {
                log.error("while looking for resources in /META-INF/services/top.marchand.xml.gaulois/", ex);
            }
        }
        try {
            createXPathExecutables();
            createXsltExecutables();
        } catch(XSpecPluginException | MalformedURLException | SaxonApiException ex) {
            throw new XSpecPluginException(ex);
        }
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
    
    private Log getLog() { return log; }
    public XsltExecutable compileXsl(Source source) throws SaxonApiException {
        try {
            return getXsltCompiler().compile(source);
        } catch(NullPointerException ex) {
            log.error("while compiling XSL "+source.getSystemId());
            return null;
        }
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
//    public void setUriResolver(URIResolver urr) { this.xsltCompiler.setURIResolver(urr); }

    public XsltExecutable getXeSurefire() {
        return xeSurefire;
    }

    private void setXeSurefire(XsltExecutable xeSurefire) {
        this.xeSurefire = xeSurefire;
    }
    
    public Serializer newSerializer(final OutputStream os) { return getProcessor().newSerializer(os); }

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
    public XsltExecutable getSchematronDsdl() { return schDsdl; }
    public XsltExecutable getSchematronExpand() { return schExpand; }
    public XsltExecutable getSchematronSvrl() { return schSvrl; }
    private void setXpSchGetXSpecFile(XPathExecutable xe) { xpSchGetXSpec = xe; }
    public XPathExecutable getXpSchGetXSpecFile() { return xpSchGetXSpec; }
//    public void setXpSchGetSchParams(XPathExecutable xe) { xpSchGetParams = xe; }
//    public XPathExecutable getXpSchGetSchParams() { return xpSchGetParams; }
    private void setSchematronSchut(XsltExecutable xe) { schSchut = xe; }
    public XsltExecutable getSchematronSchut() { return schSchut; }

    public XsltExecutable getXmlDependencyScanner() { return xmlDependencyScanner; }
    private void setXmlDependencyScanner(XsltExecutable xmlDependencyScanner) { this.xmlDependencyScanner = xmlDependencyScanner; }

    public XPathExecutable getXpFileSearcher() { return xpFileSearcher; }
    private void setXpFileSearcher(XPathExecutable xpFileSearcher) { this.xpFileSearcher = xpFileSearcher; }
    
    private boolean extendsClass(Class toCheck, Class inheritor) {
        if(toCheck.equals(inheritor)) return true;
        if(toCheck.equals(Object.class)) return false;
        return extendsClass(toCheck.getSuperclass(), inheritor);
    }
    private void setJUnitReporter(XsltExecutable xe) { junitReporter = xe ; }
    public XsltExecutable getJUnitReporter() { return junitReporter; }
    private void setCoverageReporter(XsltExecutable xe) { coverageReporter = xe; }
    public XsltExecutable getCoverageReporter() { return coverageReporter; }
}