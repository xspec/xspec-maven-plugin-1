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

import io.xspec.maven.xspecMavenPlugin.XSpecRunner;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * All options for {@link XSpecRunner}
 * @author cmarchand
 */
public class RunnerOptions {
    /**
     * Says if generated catalog must be kept, or deleted at the end of execution
     */
    public Boolean keepGeneratedCatalog = Boolean.FALSE;

    /**
     * The provided catalog to use (to add to generated catalog).
     * It is a String, as it can contains properties that will be resolved
     * with environment properties.
     */
    public String catalogFile;
    
    /**
     * The folder where are located xspec files
     */
    public File testDir;
    
    public List<String> excludes;
    
    public File reportDir;
    
    public String executionId;

    public File surefireReportDir;
    
    public Boolean coverage;
    

    
    /**
     * Construct a new RunnerOptions instance, with all default values,
     * according to maven defaults
     * @param baseDir Project base directory
     */
    public RunnerOptions(File baseDir) {
        super();
        testDir = new File(baseDir, "src/test/xspec");
        reportDir = new File(baseDir, "target/xspec-reports");
        excludes = new ArrayList<>();
        executionId = "default";
        surefireReportDir = new File(baseDir, "target/surefire-reports");
        coverage = Boolean.FALSE;
    }
    
    /**
     * Creates a RunnerOptions, with specified values;
     * @param baseDir Project base directory
     * @param keepGeneratedCatalog keep or not generated catalog
     * @param catalogFile The catalogFile to use
     * @param excludes The exclude patterns
     * @param testDir The directory where XSpec are stored in
     * @param reportDir The directory where to put reports
     * @param executionId ExecutionId to use;
     * @param surefireReportDir Surefire report dir
     * @param coverage Says if coverage should be process or not. At this time, coverage is not yet implemented
     */
    public RunnerOptions(
            File baseDir, 
            Boolean keepGeneratedCatalog, 
            String catalogFile, 
            List<String> excludes, 
            File testDir, 
            File reportDir,
            String executionId,
            File surefireReportDir,
            Boolean coverage) {
        this(baseDir);
        if(keepGeneratedCatalog!=null) this.keepGeneratedCatalog = keepGeneratedCatalog;
        if(catalogFile!=null) this.catalogFile = catalogFile;
        if(excludes!=null) this.excludes.addAll(excludes);
        if(testDir!=null) this.testDir = testDir;
        if(reportDir!=null) this.reportDir = reportDir;
        if(executionId!=null) this.executionId = executionId;
        if(surefireReportDir!=null) this.surefireReportDir = surefireReportDir;
        if(coverage!=null) this.coverage = coverage;
    }
    
}
