/**
 * Copyright © 2017, Christophe Marchand, XSpec organization
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
import java.io.Serializable;
import java.nio.file.Path;

/**
 * This class is used to carry informations on a processed file.
 * It carries : <ul>
 * <li>The root source directory</li>
 * <li>The relative to &lt;source directory&gt; source path</li>
 * <li>The absolute source path</li>
 * <li>The output dir</li>
 * <li>The absolute report file path</li>
 * <li>The relative path from  report file to &lt;output dir&gt;</li>
 * </ul>
 * 
 * @author cmarchand
 */
public class ProcessedFile implements Serializable {
    private final Path rootSourceDir;
    private final Path sourceFile;
    /**
     * The relative path from {@link #rootSourceDir} to {@link #sourceFile}
     */
    private final String relativeSourcePath;
    private final Path outputDir;
    private final Path reportFile;
    private Path coverageFile;
    /**
     * The relative path from {@link #reportFile} to {@link #outputDir}
     */
    private final String relativeReportPath, relativeCssPath;
    
    private int passed, pending, failed, missed, total;
    
    public ProcessedFile(final File rootSourceDir, final File sourceFile, final File outputDir, final File reportFile) {
        super();
        this.rootSourceDir = rootSourceDir.getAbsoluteFile().toPath();
        this.sourceFile = sourceFile.getAbsoluteFile().toPath();
        this.outputDir = outputDir.getAbsoluteFile().toPath();
        this.reportFile = reportFile.getAbsoluteFile().toPath();
        this.relativeSourcePath = this.rootSourceDir.relativize(this.sourceFile).toString();
        this.relativeReportPath = this.outputDir.relativize(this.reportFile).toString();
        this.relativeCssPath = this.reportFile.getParent().relativize(this.outputDir).toString();
    }
    
//    private String normalizeFilePath(final String input) {
//        return input.replaceAll("\\\\", "/");
//    }

    /**
     * Returns the root source dir from where the source file was found
     * @return The absolute path to source dir
     */
    public Path getRootSourceDir() {
        return rootSourceDir;
    }

    /**
     * Returns the absolute path to source file.
     * @return The absolute path to source file
     */
    public Path getSourceFile() {
        return sourceFile;
    }

    /**
     * Returns the relative path from root source directory to source file
     * @return The relative path, in URL form
     */
    public String getRelativeSourcePath() {
        return relativeSourcePath;
    }

    /**
     * Returns the output directory path.
     * @return The absolute output directory
     */
    public Path getOutputDir() {
        return outputDir;
    }

    /**
     * Returns the report file path.
     * @return The absolute report path
     */
    public Path getReportFile() {
        return reportFile;
    }

    /**
     * Returns the relative path from report file to output dir.
     * This is mainly usefull for CSS relative path.
     * @return The relative path, in URL form
     */
    public String getRelativeReportPath() {
        return relativeReportPath;
    }
    
    public String getRelativeCssPath() {
        return relativeCssPath;
    }

    public int getPassed() {
        return passed;
    }

    public int getPending() {
        return pending;
    }

    public int getFailed() {
        return failed;
    }

    public int getMissed() {
        return missed;
    }

    public int getTotal() {
        return total;
    }
    public void setResults(final  int passed, final int pending, final int failed, final int missed, final int total) {
        this.passed=passed;
        this.pending=pending;
        this.failed=failed;
        this.missed=missed;
        this.total=total;
    }

    public Path getCoverageFile() {
        return coverageFile;
    }

    public void setCoverageFile(Path coverageFile) {
        this.coverageFile = coverageFile;
    }
    
}
