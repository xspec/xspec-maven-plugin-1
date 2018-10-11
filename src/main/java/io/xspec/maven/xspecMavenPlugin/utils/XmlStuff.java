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

import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;
import javax.xml.transform.Source;
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
	private static final String MAKE_ABSOLUTE_XSL = "make-absolute.xsl";
    private XPathExecutable xpSchGetXSpec;
//    private XPathExecutable xpSchGetParams;
    private XsltExecutable schSchut;
    private final Log log;
    
    public XmlStuff(Processor processor, Log log) {
        super();
        this.processor=processor;
        documentBuilder = processor.newDocumentBuilder();
        xsltCompiler = processor.newXsltCompiler();
        xpathCompiler = processor.newXPathCompiler();
        xpathCompiler.declareNamespace("x", XSpecMojo.XSPEC_NS);
        xqueryCompiler = processor.newXQueryCompiler();
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
                                XSpecMojo.SAXON_CONFIGURATION.registerExtensionFunction(cle.newInstance());
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
    }
    
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

    public void setXspec4xsltCompiler(XsltExecutable xspec4xsltCompiler) {
        this.xspec4xsltCompiler = xspec4xsltCompiler;
    }

    public XsltExecutable getXspec4xqueryCompiler() {
        return xspec4xqueryCompiler;
    }

    public void setXspec4xqueryCompiler(XsltExecutable xspec4xqueryCompiler) {
        this.xspec4xqueryCompiler = xspec4xqueryCompiler;
    }

    public XsltExecutable getReporter() {
        return reporter;
    }

    public void setReporter(XsltExecutable reporter) {
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

    public void setXpExecGetXSpecType(XPathExecutable xpExecGetXSpecType) {
        this.xpExecGetXSpecType = xpExecGetXSpecType;
    }
    
    public URIResolver getUriResolver() { return xsltCompiler.getURIResolver(); }
    public void setUriResolver(URIResolver urr) { this.xsltCompiler.setURIResolver(urr); }

    public XsltExecutable getXeSurefire() {
        return xeSurefire;
    }

    public void setXeSurefire(XsltExecutable xeSurefire) {
        this.xeSurefire = xeSurefire;
    }
    
    public Serializer newSerializer(final OutputStream os) { return getProcessor().newSerializer(os); }

    public XQueryCompiler getXqueryCompiler() {
        return xqueryCompiler;
    }
    
    public void doAdditionalConfiguration(SaxonOptions saxonOptions) throws XPathException {
        if(saxonOptions!=null) {
            SaxonUtils.configureXsltCompiler(getXsltCompiler(), saxonOptions);
        }
    }
    
    public XsltExecutable getMakeAbsolute() throws SaxonApiException{
    	return this.compileXsl(new StreamSource(XmlStuff.class.getClassLoader().getResourceAsStream(MAKE_ABSOLUTE_XSL)));
    }
    
    public void setMakeAbsoluteParams(XsltTransformer makeAbsolute, String baseUri, String hrefParentNS){
    	QName baseParameter = new QName("base");
        QName parentNSParameter = new QName("href-parent-namespace");
        
        makeAbsolute.setParameter(baseParameter, new XdmAtomicValue(baseUri));
        makeAbsolute.setParameter(parentNSParameter, new XdmAtomicValue(hrefParentNS));
    }
    
    public void setSchematronDsdl(XsltExecutable xe) { schDsdl = xe; }
    public void setSchematronExpand(XsltExecutable xe) { schExpand = xe; }
    public void setSchematronSvrl(XsltExecutable xe) { schSvrl = xe; }
    public XsltExecutable getSchematronDsdl() { return schDsdl; }
    public XsltExecutable getSchematronExpand() { return schExpand; }
    public XsltExecutable getSchematronSvrl() { return schSvrl; }
    public void setXpSchGetXSpecFile(XPathExecutable xe) { xpSchGetXSpec = xe; }
    public XPathExecutable getXpSchGetXSpecFile() { return xpSchGetXSpec; }
//    public void setXpSchGetSchParams(XPathExecutable xe) { xpSchGetParams = xe; }
//    public XPathExecutable getXpSchGetSchParams() { return xpSchGetParams; }
    public void setSchematronSchut(XsltExecutable xe) { schSchut = xe; }
    public XsltExecutable getSchematronSchut() { return schSchut; }

    public XsltExecutable getXmlDependencyScanner() { return xmlDependencyScanner; }
    public void setXmlDependencyScanner(XsltExecutable xmlDependencyScanner) { this.xmlDependencyScanner = xmlDependencyScanner; }

    public XPathExecutable getXpFileSearcher() { return xpFileSearcher; }
    public void setXpFileSearcher(XPathExecutable xpFileSearcher) { this.xpFileSearcher = xpFileSearcher; }
    
    private boolean extendsClass(Class toCheck, Class inheritor) {
        if(toCheck.equals(inheritor)) return true;
        if(toCheck.equals(Object.class)) return false;
        return extendsClass(toCheck.getSuperclass(), inheritor);
    }
    public void setJUnitReporter(XsltExecutable xe) { junitReporter = xe ; }
    public XsltExecutable getJUnitReporter() { return junitReporter; }
    public void setCoverageReporter(XsltExecutable xe) { coverageReporter = xe; }
    public XsltExecutable getCoverageReporter() { return coverageReporter; }
}
