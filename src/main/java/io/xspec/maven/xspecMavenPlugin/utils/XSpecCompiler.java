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
package io.xspec.maven.xspecMavenPlugin.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.sax.SAXSource;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.Serializer;
import net.sf.saxon.s9api.XPathSelector;
import net.sf.saxon.s9api.XdmAtomicValue;
import net.sf.saxon.s9api.XdmDestination;
import net.sf.saxon.s9api.XdmItem;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.XdmValue;
import net.sf.saxon.s9api.XsltExecutable;
import net.sf.saxon.s9api.XsltTransformer;
import org.apache.commons.io.FilenameUtils;
import org.apache.maven.plugin.logging.Log;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

/**
 * A class that compiles XSpec files
 * @author cmarchand
 */
public class XSpecCompiler implements LogProvider {

    private final XmlStuff xmlStuff;
    private final Log log;
    private final RunnerOptions options;
    
    private final HashMap<File,File> executionReportDirs;
    private final List<File> filesToDelete;
    // In XSpec 1.3, this has been renamed to stylesheet-uri
    // https://github.com/xspec/xspec/pull/325
    public static final QName QN_STYLESHEET = new QName("stylesheet-uri");
    // FIXME: In XSpec 1.3, this will be renamed to test-dir-uri
    // https://github.com/xspec/xspec/pull/322
//    public static final QName QN_TEST_DIR = new QName("test_dir");
    public static final QName QN_URI = new QName("uri");
    
    public XSpecCompiler(XmlStuff xmlStuff, RunnerOptions options, Log log) {
        super();
        this.xmlStuff = xmlStuff;
        this.log = log;
        this.options = options;
        executionReportDirs = new HashMap<>();
        filesToDelete = new ArrayList<>();
    }
    /**
     * Compiles a XSpec file that test a XQuery
     * @param sourceFile The XSpec file to compile
     * @return The compiled XSpec informations
     */
    public final CompiledXSpec compileXSpecForXQuery(final File sourceFile) {
        return compileXSpec(sourceFile, xmlStuff.getXspec4xqueryCompiler());
    }
    /**
     * Compiles a XSpec file that test a XSLT
     * @param sourceFile The XSpec file to compile
     * @return The compiled XSpec informations
     */
    public final CompiledXSpec compileXSpecForXslt(final File sourceFile) {
        return compileXSpec(sourceFile, xmlStuff.getXspec4xsltCompiler());
    }

    /**
     * Compiles an XSpec using the provided XSLT XSpec compiler
     * @param compiler The XSpec XSLT compiler
     * @param xspec The XSpec test to compile
     * @return Details of the Compiled XSpec or null if the XSpec could not be
     * compiled
     */
    final CompiledXSpec compileXSpec(final File sourceFile, XsltExecutable compilerExec) {
        XsltTransformer compiler = compilerExec.load();
        InputStream isXSpec = null;
        try {
            final File compiledXSpec = getCompiledXSpecPath(options.reportDir, sourceFile);
            log.info("Compiling XSpec to XSLT: " + compiledXSpec);

            isXSpec = new FileInputStream(sourceFile);

            final SAXParser parser = XmlStuff.PARSER_FACTORY.newSAXParser();
            final XMLReader reader = parser.getXMLReader();
            final XSpecTestFilter xspecTestFilter = new XSpecTestFilter(
                    reader, 
                    // Bug under Windows
                    sourceFile.toURI().toString(),
                    xmlStuff.getUriResolver(), 
                    this, 
                    false);

            final InputSource inXSpec = new InputSource(isXSpec);
            inXSpec.setSystemId(sourceFile.toURI().toString());

            compiler.setSource(new SAXSource(xspecTestFilter, inXSpec));

            final Serializer serializer = xmlStuff.getProcessor().newSerializer();
            serializer.setOutputFile(compiledXSpec);
            compiler.setDestination(serializer);

            compiler.transform();

            return new CompiledXSpec(xspecTestFilter.getTests(), xspecTestFilter.getPendingTests(), compiledXSpec);

        } catch (final SaxonApiException sae) {
            log.error(sae.getMessage());
            log.debug(sae);
        } catch (final ParserConfigurationException | FileNotFoundException pce) {
            log.error(pce);
        } catch (SAXException saxe) {
            log.error(saxe.getMessage());
            log.debug(saxe);
        } finally {
            if (isXSpec != null) {
                try {
                    isXSpec.close();
                } catch (final IOException ioe) {
                    log.warn(ioe);
                }
            }
        }

        return null;
    }
    
    /**
     * Prepare a Schematron XSpec test.
     * There two phases : 
     * <ol>
     * <li>compile schematron to a XSLT</li>
     * <li>compile the XSpec (that points to the schematron) into a XSpec that 
     * points to the XSLT (the compiled at phase 1)</li>
     * </ol>
     * 
     * @param xspecDocument
     * @return
     * @throws SaxonApiException
     * @throws TransformerException
     * @throws IOException 
     */
    public XdmNode prepareSchematronDocument(XdmNode xspecDocument) throws SaxonApiException, TransformerException, IOException {

        XsltTransformer step1 = xmlStuff.getSchematronDsdl().load();
        XsltTransformer step2 = xmlStuff.getSchematronExpand().load();
        XsltTransformer step3 = xmlStuff.getSchematronSvrl().load();

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
        File compiledSchematronDest = getCompiledSchematronPath(options.reportDir, sourceFile);
        Serializer serializer = xmlStuff.newSerializer(new FileOutputStream(compiledSchematronDest));
        step3.setDestination(serializer);
        
        // getting from XSpec the schematron location
        XPathSelector xpSchemaPath = xmlStuff.getXPathCompiler().compile("/*/@schematron").load();
        xpSchemaPath.setContextItem(xspecDocument);
        String schematronPath = xpSchemaPath.evaluateSingle().getStringValue();
        Source source = xmlStuff.getUriResolver().resolve(schematronPath, xspecDocument.getBaseURI().toString());
        step1.setInitialContextNode(xmlStuff.getDocumentBuilder().build(source));
        step1.transform();
        getLog().debug("Schematron compiled ! "+compiledSchematronDest.getAbsolutePath());
        
        // modifying xspec to point to compiled schematron
        XsltTransformer schut = xmlStuff.getSchematronSchut().load();
        schut.setParameter(QN_STYLESHEET, new XdmAtomicValue(compiledSchematronDest.toURI().toString()));
        // plus nécessaire à partir de 1.5.0
//        schut.setParameter(QN_TEST_DIR, new XdmAtomicValue(options.testDir.toURI().toString()));
        schut.setInitialContextNode(xspecDocument);
        File resultFile = getCompiledXspecSchematronPath(options.reportDir, sourceFile);
        // WARNING : we can't use a XdmDestination, the XdmNode generated does not have 
        // an absolute systemId, which is used in processXsltXSpec(XdmNode xspec)
        schut.setDestination(xmlStuff.newSerializer(new FileOutputStream(resultFile)));
        schut.transform();
        getLog().debug("XSpec for schematron compiled: "+resultFile.getAbsolutePath());
        // TODO: refacto. We could use a TeeDestination(serializer, XdmDestination) to avoid parsing result file
        // Hum... not sure
        XdmNode result = xmlStuff.getDocumentBuilder().build(resultFile);
        if(!resultFile.exists()) {
            getLog().error(resultFile.getAbsolutePath()+" has not be written");
        }
        
        // copy resources referenced from XSpec
        getLog().info("Copying resource files referenced from XSpec for Schematron");
        XsltTransformer xslDependencyScanner = xmlStuff.getXmlDependencyScanner().load();
        XdmDestination xdmDest = new XdmDestination();
        xslDependencyScanner.setDestination(xdmDest);
        xslDependencyScanner.setInitialContextNode(xspecDocument);
        xslDependencyScanner.transform();
        XPathSelector xpFile = xmlStuff.getXpFileSearcher().load();
        xpFile.setContextItem(xdmDest.getXdmNode());
        XdmValue ret = xpFile.evaluate();
        for(int i=0;i<ret.size();i++) {
            XdmNode node = (XdmNode)(ret.itemAt(i));
            String uri = node.getAttributeValue(QN_URI);
            try {
                copyFile(xspecDocument.getUnderlyingNode().getSystemId(), uri, resultFile);
            } catch(URISyntaxException ex) {
                // it can not happens, it is always correct as provided by saxon
                throw new SaxonApiException("Saxon has generated an invalid URI : ",ex);
            }
        }
        return result;
    }
    /**
     * Get location for XSpec test report (XML Format)
     *
     * @param xspecReportDir The directory to place XSpec reports in
     * @param xspec The XSpec that will be compiled eventually
     *
     * @return A filepath for the report
     */
    public final File getXSpecXmlResultPath(final File xspecReportDir, final File xspec) {
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
    public final File getXSpecHtmlResultPath(final File xspecReportDir, final File xspec) {
        return getXSpecResultPath(xspecReportDir, xspec, "html");
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
        getLog().debug("Copying "+source.getAbsolutePath()+" to "+dest.getAbsolutePath());
        Files.copy(source.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
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
        checkDirExists(xspecReportDir);
        Path relativeSource = options.testDir.toPath().relativize(xspec.toPath());
        File executionReportDir = getExecutionReportDir(xspecReportDir);
        executionReportDir.mkdirs();
        File outputDir = executionReportDir.toPath().resolve(relativeSource).toFile();
        final File fCompiledDir = new File(outputDir, name);
        if (!fCompiledDir.exists()) {
            fCompiledDir.mkdirs();
        }
        return new File(fCompiledDir, xspec.getName() + extension);
    }

    /**
     * Get location for Compiled XSpecs in case of schematron
     * @param xspecReportDir The directory to place XSpec reports in
     * @param schematron The Schematron that will be compiled eventually
     *
     * @return A filepath to place the compiled Schematron (a XSLT) in
     */
    final File getCompiledSchematronPath(final File xspecReportDir, final File schematron) {
        // with XSpec 1.5.0, it changes to 
        File ret = getCompiledPath(xspecReportDir, schematron, "schematron", ".xslt");
        return ret;
    }
    final File getCompiledXspecSchematronPath(final File xspecReportDir, final File xspec) {
        File ret = getCompiledPath(
                xspecReportDir, 
                xspec, 
                FilenameUtils.getBaseName(xspec.getName()),
                "-compiled.xspec");
        filesToDelete.add(ret);
        return ret;
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
    final public File getXSpecResultPath(final File xspecReportDir, final File xspec, final String extension) {
        checkDirExists(xspecReportDir);
        Path relativeSource = options.testDir.toPath().relativize(xspec.toPath());
        File executionReportDir = getExecutionReportDir(xspecReportDir);
        executionReportDir.mkdirs();
        File outputDir = executionReportDir.toPath().resolve(relativeSource).toFile();
        return new File(outputDir, xspec.getName().replace(".xspec", "") + "." + extension);
    }
    
    final public File getCoverageTempPath(final File xspecReportDir, final File xspec) {
        checkDirExists(xspecReportDir);
        Path relativeSource = options.testDir.toPath().relativize(xspec.toPath());
        File executionReportDir = (
                options.executionId!=null && !"default".equals(options.executionId) ? 
                new File(xspecReportDir,options.executionId) :
                xspecReportDir);
        executionReportDir.mkdirs();
        File outputDir = executionReportDir.toPath().resolve(relativeSource).toFile();
        return new File(outputDir, "coverage-"+xspec.getName().replace(".xspec","") + ".xml");
    }
    final public File getCoverageFinalPath(final File xspecReportDir, final File xspec) {
        checkDirExists(xspecReportDir);
        Path relativeSource = options.testDir.toPath().relativize(xspec.toPath());
        File executionReportDir = (
                options.executionId!=null && !"default".equals(options.executionId) ? 
                new File(xspecReportDir,options.executionId) :
                xspecReportDir);
        executionReportDir.mkdirs();
        File outputDir = executionReportDir.toPath().resolve(relativeSource).toFile();
        return new File(outputDir, xspec.getName().replace(".xspec","-coverage") + ".html");
    }
    private File getExecutionReportDir(File xspecReportDir) {
        File executionReportDir = executionReportDirs.get(xspecReportDir);
        if(executionReportDir==null) {
            executionReportDir = (
                    options.executionId!=null && !"default".equals(options.executionId) ? 
                    new File(xspecReportDir, options.executionId) :
                    xspecReportDir);
            executionReportDir.mkdirs();
            getLog().debug("executionReportDir("+xspecReportDir.getAbsolutePath()+")="+executionReportDir.getAbsolutePath());
            executionReportDirs.put(xspecReportDir, executionReportDir);
        }
        return executionReportDir;
    }

    private void checkDirExists(final File xspecReportDir) {
        if (!xspecReportDir.exists()) {
            xspecReportDir.mkdirs();
        }
    }

    @Override
    public Log getLog() {
        return log;
    }
    
    public List<File> getFilesToDelete() { return filesToDelete; }
}
