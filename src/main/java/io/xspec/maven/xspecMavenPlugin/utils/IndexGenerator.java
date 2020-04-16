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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.stream.StreamSource;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.Serializer;
import net.sf.saxon.s9api.XdmNode;

/**
 * Generates the general index
 * @author cmarchand
 */
public class IndexGenerator {
    
    protected final RunnerOptions options;
    protected final List<ProcessedFile> processedFiles;
    protected final XmlStuff stuff;
    
    public IndexGenerator(RunnerOptions options, List<ProcessedFile> processedFiles, final XmlStuff stuff) {
        super();
        this.options = options;
        this.processedFiles = processedFiles;
        this.stuff = stuff;
    }
    
    /**
     * Generates the general index. It's an HTML file, located in reportDir.
     * @throws XSpecPluginException 
     */
    public void generateIndex() throws XSpecPluginException {
        File index = new File(options.reportDir, "index.html");
        try {
            if(!options.reportDir.exists()) options.reportDir.mkdirs();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            XMLStreamWriter sw = XMLOutputFactory.newInstance().createXMLStreamWriter(baos);
            sw.writeStartDocument("UTF-8", "1.0");
            sw.writeStartElement("html");
              sw.writeStartElement("head");
                sw.writeStartElement("style");
                  sw.writeAttribute("type", "text/css");
                  sw.writeCharacters("\n\ttable {border: solid black 1px; border-collapse: collapse; }\n");
                  sw.writeCharacters("\ttr.error {background-color: red; color: white; }\n");
                  sw.writeCharacters("\ttr.error td a { color: white;}\n");
                  sw.writeCharacters("\ttr.title {background-color: lightgrey; }\n");
                  sw.writeCharacters("\ttd,th {border: solid black 1px; }\n");
                  sw.writeCharacters("\ttd:not(:first-child) {text-align: right; }\n");
                sw.writeEndElement();
                sw.writeStartElement("title");
                  sw.writeCharacters("XSpec results");
                sw.writeEndElement();
                sw.writeStartElement("meta");
                  sw.writeAttribute("name", "date");
                  sw.writeAttribute("content", new SimpleDateFormat("yyyy-MM-dd").format(new Date()));
                sw.writeEndElement();
              sw.writeEndElement(); // head
              sw.writeStartElement("body");
                sw.writeStartElement("h1");
                  sw.writeCharacters("XSpec results");
                sw.writeEndElement();
                writeTable(sw);
              sw.writeEndElement(); // body
            sw.writeEndElement();   // html
            sw.writeEndDocument();
            sw.flush();
            baos.flush();
            baos.close();
            InputStream is = new ByteArrayInputStream(baos.toByteArray());
            StreamSource source = new StreamSource(is);
            XdmNode tree = stuff.getDocumentBuilder().build(source);
            Serializer ser = stuff.newSerializer(new FileOutputStream(index));
            ser.setOutputProperty(Serializer.Property.INDENT, "true");
            ser.setOutputProperty(Serializer.Property.METHOD, "html");
            ser.setOutputProperty(Serializer.Property.VERSION, "5.0");
            ser.setOutputProperty(Serializer.Property.ENCODING, "UTF-8");
            stuff.getProcessor().writeXdmValue(tree, ser);
        } catch(IllegalArgumentException | XMLStreamException | SaxonApiException | IOException ex) {
            throw new XSpecPluginException("while generating index: "+index.getAbsolutePath(), ex);
        }        
    }
    
    /**
     * Writes the table
     * @param sw
     * @throws XMLStreamException 
     */
    private void writeTable(XMLStreamWriter sw) throws XMLStreamException {
        sw.writeStartElement("table");
          sw.writeStartElement("colgroup");
            sw.writeEmptyElement("col");
            sw.writeEmptyElement("col");
              sw.writeAttribute("class", "successful");
            sw.writeEmptyElement("col");
              sw.writeAttribute("class", "pending");
            sw.writeEmptyElement("col");
              sw.writeAttribute("class", "failed");
            sw.writeEmptyElement("col");
              sw.writeAttribute("class", "missed");
            if(options.coverage) {
                sw.writeEmptyElement("col");
            }
            sw.writeEmptyElement("col");
          sw.writeEndElement(); // colgroup
          sw.writeStartElement("thead");
            sw.writeStartElement("tr");
              writeCell(sw, "th", "XSpec file");
              writeCell(sw, "th", "Passed");
              writeCell(sw, "th", "Pending");
              writeCell(sw, "th", "Failed");
              writeCell(sw, "th", "Missed");
              if(options.coverage) {
                  writeCell(sw, "th", "Coverage");
              }
              writeCell(sw, "th", "Total");
            sw.writeEndElement();
          sw.writeEndElement(); // thead
          sw.writeStartElement("tbody");
            String lastRootDir = "";
            for(ProcessedFile pf: processedFiles) {
                String rootDir = pf.getRootSourceDir().toString();
                if(!lastRootDir.equals(rootDir)) {
                      sw.writeStartElement("tr");
                        sw.writeAttribute("class", "title");
                        sw.writeStartElement("td");
                          sw.writeAttribute("colspan", options.coverage ? "7" : "6");
                          sw.writeCharacters(rootDir);
                        sw.writeEndElement();
                      sw.writeEndElement();
                      lastRootDir = rootDir;
                }
                int errorCount = pf.getFailed()+pf.getMissed();
                sw.writeStartElement("tr");
                if(errorCount!=0) {
                    sw.writeAttribute("class", "error");
                }
                  sw.writeStartElement("td");
                    sw.writeStartElement("a");
                      sw.writeAttribute("href", pf.getReportFile().toUri().toString());
                      sw.writeCharacters(pf.getRelativeSourcePath());
                    sw.writeEndElement();
                  sw.writeEndElement(); //td
                  writeTd(sw, pf.getPassed());
                  writeTd(sw, pf.getPending());
                  writeTd(sw, pf.getFailed());
                  writeTd(sw, pf.getMissed());
                  if(options.coverage) {
                      if(pf.getCoverageFile()!=null) {
                          sw.writeStartElement("td");
                            sw.writeStartElement("a");
                              sw.writeAttribute("href", options.reportDir.toPath().relativize(pf.getCoverageFile()).toString());
                              sw.writeCharacters("Coverage report");
                            sw.writeEndElement();
                          sw.writeEndElement();
                      } else {
                          sw.writeEmptyElement("td");
                      }
                  }
                  sw.writeStartElement("td");
                    sw.writeCharacters(Integer.toString(pf.getTotal()));
                  sw.writeEndElement();
                sw.writeEndElement();   // tr
            }
          sw.writeEndElement(); // tbody
        sw.writeEndElement();   // table
    }
    
    /**
     * Writes a td that contains an integer value. If the value is 0,
     * adds a class named <tt>zero</tt>
     * @param sw
     * @param count
     * @throws XMLStreamException 
     */
    private void writeTd(XMLStreamWriter sw, int count) throws XMLStreamException {
        sw.writeStartElement("td");
        if(count==0) {
            sw.writeAttribute("class", "zero");
        }
        sw.writeCharacters(Integer.toString(count));
        sw.writeEndElement();
    }
    /**
     * Writes a cell thant contains only text
     * @param sw
     * @param cellName
     * @param value
     * @throws XMLStreamException 
     */
    private void writeCell(XMLStreamWriter sw, String cellName, String value) throws XMLStreamException {
        sw.writeStartElement(cellName);
          sw.writeCharacters(value);
        sw.writeEndElement();
    }

    
}
