/**
 * Copyright © 2017, Christophe Marchand
 * All rights reserved.
 * <p>
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
package io.xspec.maven.xspecMavenPlugin.utils;

import io.xspec.maven.xspecMavenPlugin.resolver.XSpecResourceResolver;
import io.xspec.maven.xspecMavenPlugin.resources.SchematronImplResources;
import io.xspec.maven.xspecMavenPlugin.resources.XSpecImplResources;
import io.xspec.maven.xspecMavenPlugin.resources.XSpecPluginResources;
import net.sf.saxon.Configuration;
import net.sf.saxon.lib.ExtensionFunctionDefinition;
import net.sf.saxon.lib.ResourceRequest;
import net.sf.saxon.lib.ResourceResolver;
import net.sf.saxon.s9api.*;
import net.sf.saxon.trans.XPathException;
import org.apache.maven.plugin.logging.Log;
import top.marchand.maven.saxon.utils.SaxonOptions;
import top.marchand.maven.saxon.utils.SaxonUtils;
import uk.org.adamretter.maven.XSpecMojo;

import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;
import javax.xml.transform.stream.StreamSource;
import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Base64;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Properties;

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
  private final ResourceResolver xspecResourceResolver;
  private XsltExecutable xspec4xsltCompiler;
    private XsltExecutable xspec4xqueryCompiler;
    private XsltExecutable reporter;
    private XsltExecutable junitReporter;
    private XsltExecutable coverageReporter;
    private XsltExecutable xeSurefire;
    private XsltExecutable xmlDependencyScanner;
    private XPathExecutable xpExecGetXSpecType;
    private XPathExecutable xpFileSearcher;

    public final static QName QN_REPORT_CSS = new QName("report-css-uri");
    public static final String RESOURCES_TEST_REPORT_CSS = "resources/test-report.css";
  private XsltExecutable schematronCompiler;
    private XsltExecutable schSchut;
    private final Log log;
    private final XSpecImplResources xspecResources;
    private final XSpecPluginResources pluginResources;
    private final SchematronImplResources schematronResources;
    private final File baseDir;
    private final RunnerOptions options;
    private final Properties executionProperties;
  public static final SAXParserFactory PARSER_FACTORY = SAXParserFactory.newInstance();
    private static final Class[] EMPTY_PARAMS = new Class[]{};


  public XmlStuff(
            SaxonOptions saxonOptions,
            Log log, 
            XSpecImplResources xspecResources, 
            XSpecPluginResources pluginResources, 
            SchematronImplResources schematronResources,
            File baseDir,
            RunnerOptions options,
            Properties executionProperties) throws XSpecPluginException {
        super();
        this.processor = new Processor(getSaxonConfiguration());
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
      File catalogFile = generateCatalog();
      xspecResourceResolver = new XSpecResourceResolver(catalogFile, getLog());
    } catch(IOException ex) {
      throw new XSpecPluginException("while constructing URIResolver", ex);
    }
    getLog().debug("URI resolver Ok");
        try {
            xsltCompiler = processor.newXsltCompiler();
            xsltCompiler.setCompileWithTracing(true);
            xsltCompiler.setResourceResolver(xspecResourceResolver);
            xpathCompiler = processor.newXPathCompiler();
            xpathCompiler.declareNamespace("x", XSpecMojo.XSPEC_NS);
            xqueryCompiler = processor.newXQueryCompiler();
            xqueryCompiler.setCompileWithTracing(true);
            try {
                doAdditionalConfiguration(saxonOptions);
            } catch(XPathException ex) {
                throw new XSpecPluginException(ex);
            }
            loadAllSaxonExtensionFunctions();
            try {
                createXPathExecutables();
                getLog().debug("XPath executables created");
                createXsltExecutables();
                getLog().debug("XSLT executables created");
            } catch(XSpecPluginException | SaxonApiException | URISyntaxException | IOException ex) {
                throw new XSpecPluginException(ex);
            }
        } catch(RuntimeException ex) {
            throw new XSpecPluginException(ex.getMessage(), ex);
        }
    }

    private void loadAllSaxonExtensionFunctions() {
        try {
            for(Enumeration<URL> enumer = getClass().getClassLoader().getResources("META-INF/services/top.marchand.xml.gaulois.xml"); enumer.hasMoreElements();) {
                URL url = enumer.nextElement();
                log.debug("loading service "+url.toExternalForm());
                XdmNode document = documentBuilder.build(new StreamSource(url.openStream()));
                XPathSelector selector = xpathCompiler.compile("/gaulois-services/saxon/extensions/function").load();
                selector.setContextItem(document);
                Iterator<XdmItem> o = selector.evaluate().iterator();
                XdmSequenceIterator it = (XdmSequenceIterator)o;
                while(it.hasNext()) {
                    String className = it.next().getStringValue();
                    registerSaxonExtension(className);
                }
            }
        } catch(IOException | SaxonApiException ex) {
            getLog().error("while looking for resources in /META-INF/services/top.marchand.xml.gaulois/", ex);
        }
        registerSaxonExtension("io.xspec.xspec.saxon.funcdefs.LineNumber");
    }

    @SuppressWarnings("unchecked")
    private void registerSaxonExtension(String className) {
        try {
            Class<?> clazz = Class.forName(className);
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
     * Decorates the XsltCompiler with a custom resolver. It also generates the catalog with all
     * dependencies
     * @throws IOException
     * @throws XSpecPluginException
     */
    private File generateCatalog() throws IOException, XSpecPluginException {
        getLog().debug("buildUriResolver");
        CatalogWriter cw = new CatalogWriter(this.getClass().getClassLoader());
        getLog().debug("CatalogWriter instanciated");
        File catalog = cw.writeCatalog(options.catalogFile, executionProperties, options.keepGeneratedCatalog);
        if(options.keepGeneratedCatalog) {
            getLog().info("keeping generated catalog: "+catalog.toURI().toURL().toExternalForm());
        }
        return catalog;
    }
    
    private void createXPathExecutables() throws SaxonApiException {
        setXpExecGetXSpecType(getXPathCompiler().compile("/*/@*"));
        setXpSchGetXSpecFile(getXPathCompiler().compile(
                "iri-to-uri("
                        + "concat("
                        +   "replace(document-uri(/), '(.*)/.*$', '$1'), "
                        +   "'/', "
                        +   "/*[local-name() = 'description']/@schematron))"));
        setXpFileSearcher(getXPathCompiler().compile("//file[@dependency-type!='x:description'][not(starts-with(@abs-uri,'jar:file:'))]"));
    }
    private void createXsltExecutables() throws XSpecPluginException, SaxonApiException, IOException, URISyntaxException {
        getLog().debug("Using XSpec Xslt Compiler: " + xspecResources.getXSpecXslCompilerUri());
        getLog().debug("Using XSpec Xquery Compiler: " + xspecResources.getXSpecXQueryCompilerUri());
        getLog().debug("Using XSpec Reporter: " + xspecResources.getXSpecReporterUri(options.folding));
        getLog().debug("Using Coverage Reporter: " + xspecResources.getXSpecCoverageReporterUri());
        getLog().debug("Using Schematron step1: " + schematronResources.getSchStep1Uri());
        getLog().debug("Using Schematron step2: " + schematronResources.getSchStep2Uri());
        getLog().debug("Using Schematron step3: " + schematronResources.getSchStep3Uri());
        getLog().debug("Using Schematron schut: " + xspecResources.getSchematronSchutConverterUri());
        getLog().debug("Using XML dependency scanner: " + pluginResources.getDependencyScannerUri());
        String baseUri = baseDir!=null ? baseDir.toURI().toURL().toExternalForm() : null;

        // compilers
        Source srcXsltCompiler = resolveSrc(xspecResources.getXSpecXslCompilerUri(), baseUri, "XSpec XSL Compiler");
        getLog().debug(xspecResources.getXSpecXslCompilerUri()+" -> "+srcXsltCompiler.getSystemId());
        Source srcXqueryCompiler = resolveSrc(xspecResources.getXSpecXQueryCompilerUri(), baseUri, "XSpec XQuery Compiler");
        getLog().debug(xspecResources.getXSpecXQueryCompilerUri()+" -> "+srcXqueryCompiler.getSystemId());
        Source srcReporter = resolveSrc(xspecResources.getXSpecReporterUri(options.folding), baseUri, "XSpec Reporter");
        getLog().debug(xspecResources.getXSpecReporterUri(options.folding)+" -> "+srcReporter.getSystemId());
        Source srcCoverageReporter = resolveSrc(xspecResources.getXSpecCoverageReporterUri(), baseUri, "Coverage Reporter");
        getLog().debug(xspecResources.getXSpecCoverageReporterUri()+" -> "+srcCoverageReporter.getSystemId());

        // Schematron
        Source srcSchematronCompiler = resolveSrc(xspecResources.getSchematronCompilerUri(), baseUri, "Schematron compiler");
        getLog().debug(xspecResources.getSchematronCompilerUri()+" -> "+srcSchematronCompiler.getSystemId());
        Source srcSchSchut = resolveSrc(xspecResources.getSchematronSchutConverterUri(), baseUri, "Schematron unit-test to XSpec converter");
        getLog().debug(xspecResources.getSchematronSchutConverterUri()+" -> "+srcSchSchut.getSystemId());

        // dependency scanner
        Source srcXmlDependencyScanner = resolveSrc(pluginResources.getDependencyScannerUri(), baseUri, "Xml dependency scanner");
        getLog().debug(pluginResources.getDependencyScannerUri()+" -> "+srcXmlDependencyScanner.getSystemId());
        
        // for code coverage
      // for code coverage
      String generateXspecUtilsUri = resolveSrc("generate-tests-utils.xsl", srcXsltCompiler.getSystemId(), "generate-tests-utils.xsl").getSystemId();
      String schLocationCompareUri = resolveSrc("../schematron/sch-location-compare.xsl", srcXsltCompiler.getSystemId(), "../schematron/sch-location-compare.xsl").getSystemId();
        setXspec4xsltCompiler(compileXsl(srcXsltCompiler));
        setXspec4xqueryCompiler(compileXsl(srcXqueryCompiler));
        setReporter(compileReporter(srcReporter));
        if(isSaxonPEorEE()) {
            setCoverageReporter(compileXsl(srcCoverageReporter));
        }
        setSchematronCompiler(compileXsl(srcSchematronCompiler));
        setSchematronSchut(compileXsl(srcSchSchut));
        setXmlDependencyScanner(compileXsl(srcXmlDependencyScanner));

        setXeSurefire(compileXsl(new StreamSource(getClass().getResourceAsStream("/surefire-reporter.xsl"))));
    }

  private void setSchematronCompiler(XsltExecutable xsltExecutable) {
    schematronCompiler = xsltExecutable;
  }

  public XsltExecutable getSchematronCompiler() {
    return schematronCompiler;
  }

  public boolean isSaxonPEorEE() {
        String configurationClassName = processor.getUnderlyingConfiguration().getClass().getName();
        return "com.saxonica.config.ProfessionalConfiguration".equals(configurationClassName) ||
                "com.saxonica.config.EnterpriseConfiguration".equals(configurationClassName);
    }
    private XsltExecutable compileReporter(Source reporterSource) throws SaxonApiException, XSpecPluginException, IOException, URISyntaxException {
        if(reporterSource.getSystemId().contains("fold")) {
            getLog().debug("reporter is folding");
            XdmNode xslSource  = documentBuilder.build(reporterSource);
            XsltExecutable xslChanger = getXsltCompiler().compile(resolveSrc(pluginResources.getXsltImageChanger(), null,null));
            XsltTransformer tr = xslChanger.load();
            tr.setParameter(new QName("imgDown"), XdmValue.makeValue(encodeBase64(pluginResources.getImageDown())));
            tr.setParameter(new QName("imgUp"), XdmValue.makeValue(encodeBase64(pluginResources.getImageUp())));
            XdmDestination dest = new XdmDestination();
            tr.setDestination(dest);
            tr.setInitialContextNode(xslSource);
            tr.setBaseOutputURI(reporterSource.getSystemId());
            tr.transform();
            Source transformed = dest.getXdmNode().getUnderlyingNode();
            return getXsltCompiler().compile(transformed);
        } else {
            getLog().debug("reporter is not folding");
            return compileXsl(reporterSource);
        }
    }
    private String encodeBase64(String uri) throws XSpecPluginException, IOException, URISyntaxException {
        Source source = resolveSrc(uri, null, null);
        URL resolvedUri = new URL(source.getSystemId());
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[2048];
        InputStream is = resolvedUri.openStream();
        int read = is.read(buffer);
        long written = 0;
        while(read>0) {
            baos.write(buffer, 0, read);
            read = is.read(buffer);
            written += read;
        }
        baos.flush();
        getLog().debug("image is "+written+" bytes long");
        Base64.Encoder encoder = Base64.getEncoder();
        return encoder.encodeToString(baos.toByteArray());
    }
    private Log getLog() { 
        return log;
    }
    public XsltExecutable compileXsl(Source source) throws SaxonApiException {
      getLog().info("Compiling "+source.getSystemId());
        return getXsltCompiler().compile(source);
    }

    public Processor getProcessor() {
        return processor;
    }

    public DocumentBuilder getDocumentBuilder() {
        return documentBuilder;
    }

    public XsltCompiler getXsltCompiler() {
      ResourceResolver resourceResolver = xsltCompiler.getResourceResolver();
      if(resourceResolver!=null)
        getLog().debug("compiler's ResourceResolver is "+ resourceResolver.getClass().getName());
      URIResolver uriResolver = xsltCompiler.getURIResolver();
      if(uriResolver!=null)
        getLog().debug("compiler's URIResolver is "+ uriResolver.getClass().getName());
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

    public ResourceResolver getResourceResolver() { return xspecResourceResolver; }

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
          ResourceRequest resourceRequest = new ResourceRequest();
          resourceRequest.baseUri=baseUri;
          resourceRequest.uri=source;
          //Source ret = getUriResolver().resolve(source, baseUri);
          Source ret = getResourceResolver().resolve(resourceRequest);
            if(ret == null) {
                throw new XSpecPluginException("Could not find "+desc+" stylesheet in: "+source);
            }
            return ret;
        } catch(TransformerException ex) {
            throw new XSpecPluginException("while resolving "+source, ex);
        }
    }
    
    private void setXpSchGetXSpecFile(XPathExecutable xe) {
    }

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
    public XsltExecutable getJUnitReporter() { return junitReporter; }
    private void setCoverageReporter(XsltExecutable xe) { coverageReporter = xe; }
    public XsltExecutable getCoverageReporter() { return coverageReporter; }

    /**
     * Returns the XSpec tested file kind
     * @param doc
     * @return XSpecType
     * @throws SaxonApiException If file is not a XSpec one
     */
    public XSpecType getXSpecType(XdmNode doc) throws SaxonApiException {
        XPathSelector xps = getXpExecGetXSpecType().load();
        xps.setContextItem(doc);
        XdmValue values = xps.evaluate();
        Iterator<XdmItem> o = values.iterator();
      for (XdmSequenceIterator it = (XdmSequenceIterator) o; it.hasNext(); ) {
        XdmNode item = (XdmNode) (it.next());
        if (item.getNodeKind().equals(XdmNodeKind.ATTRIBUTE)) {
          String nodeName = item.getNodeName().getLocalName();
          switch (nodeName) {
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
     * We want to be sure that external-functions are allowed
     * @return
     */
    private static Configuration getSaxonConfiguration() {
        Configuration ret = Configuration.newConfiguration();
        ret.setConfigurationProperty("http://saxon.sf.net/feature/allow-external-functions", Boolean.TRUE);
        return ret;
    }

  public SchematronImplResources getSchematronResources() {
    return schematronResources;
  }
}