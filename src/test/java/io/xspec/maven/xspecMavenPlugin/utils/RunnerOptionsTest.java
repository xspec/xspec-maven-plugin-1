/**
 * Copyright © 2019, Christophe Marchand, XSpec organization
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
import java.util.Arrays;
import java.util.List;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 *
 * @author cmarchand
 */
public class RunnerOptionsTest {
    
    @Test
    public void defaultConstructorTest() {
        File source = new File(".").getAbsoluteFile();
        File testDir = new File(source, "src/test/xspec");
        File reportDir = new File(source, "target/xspec-reports");
        File surefireReport = new File(source, "target/surefire-reports");
        
        RunnerOptions options = new RunnerOptions(source);
        assertEquals("testDir is not correct", testDir, options.testDir);
        assertEquals("reportDir is not correct", reportDir, options.reportDir);
        assertEquals("surefireReport is not correct", surefireReport, options.surefireReportDir);
        
        assertFalse("keepGeneratedCatalog is not false", options.keepGeneratedCatalog);
        assertFalse("coverage is not false", options.coverage);
        assertNull("catalog file is not null", options.catalogFile);
        assertEquals("executionId is not default", "default", options.executionId);
        assertTrue("excludes is not empty", options.excludes.isEmpty());
        assertFalse("folding is not false", options.folding);
    }
    
    @Test
    public void constructorOverrideKeepGeneratedCatalogTest() {
        File source = new File(".").getAbsoluteFile();
        File testDir = new File(source, "src/test/xspec");
        File reportDir = new File(source, "target/xspec-reports");
        File surefireReport = new File(source, "target/surefire-reports");

        RunnerOptions options = new RunnerOptions(source, Boolean.TRUE, null, null, null, null, null, null, null, null);

        assertTrue("keepGeneratedCatalog is not true", options.keepGeneratedCatalog);
        // not changed
        assertEquals("testDir is not correct", testDir, options.testDir);
        assertEquals("reportDir is not correct", reportDir, options.reportDir);
        assertEquals("surefireReport is not correct", surefireReport, options.surefireReportDir);
        assertFalse("coverage is not false", options.coverage);
        assertNull("catalog file is not null", options.catalogFile);
        assertEquals("executionId is not default", "default", options.executionId);
        assertTrue("excludes is not empty", options.excludes.isEmpty());
        assertFalse("folding is not false", options.folding);
    }
    
    @Test
    public void constructorOverrideCatalogFileTest() {
        File source = new File(".").getAbsoluteFile();
        File testDir = new File(source, "src/test/xspec");
        File reportDir = new File(source, "target/xspec-reports");
        File surefireReport = new File(source, "target/surefire-reports");

        RunnerOptions options = new RunnerOptions(source, null, "catalog.xml", null, null, null, null, null, null, null);
        assertEquals("catalog file is not overwriten", "catalog.xml", options.catalogFile);

        // not changed
        assertEquals("testDir is not correct", testDir, options.testDir);
        assertEquals("reportDir is not correct", reportDir, options.reportDir);
        assertEquals("surefireReport is not correct", surefireReport, options.surefireReportDir);
        assertFalse("keepGeneratedCatalog is not false", options.keepGeneratedCatalog);
        assertFalse("coverage is not false", options.coverage);
        assertEquals("executionId is not default", "default", options.executionId);
        assertTrue("excludes is not empty", options.excludes.isEmpty());
        assertFalse("folding is not false", options.folding);
    }

    @Test
    public void constructorOverrideExcludesTest() {
        File source = new File(".").getAbsoluteFile();
        File testDir = new File(source, "src/test/xspec");
        File reportDir = new File(source, "target/xspec-reports");
        File surefireReport = new File(source, "target/surefire-reports");
        
        List<String> excludes = Arrays.asList("*IT.xspec");

        RunnerOptions options = new RunnerOptions(source, null, null, excludes, null, null, null, null, null, null);
        assertEquals("excludes is empty", 1, options.excludes.size());

        // not changed
        assertEquals("testDir is not correct", testDir, options.testDir);
        assertEquals("reportDir is not correct", reportDir, options.reportDir);
        assertEquals("surefireReport is not correct", surefireReport, options.surefireReportDir);
        assertFalse("keepGeneratedCatalog is not false", options.keepGeneratedCatalog);
        assertNull("catalog file is not null", options.catalogFile);
        assertFalse("coverage is not false", options.coverage);
        assertEquals("executionId is not default", "default", options.executionId);
        assertFalse("folding is not false", options.folding);
    }

    @Test
    public void constructorOverrideTestDirTest() {
        File source = new File(".").getAbsoluteFile();
        File testDir = new File(source, "src/test/xspec");
        File reportDir = new File(source, "target/xspec-reports");
        File surefireReport = new File(source, "target/surefire-reports");
        
        RunnerOptions options = new RunnerOptions(source, null, null, null, source, null, null, null, null, null);
        assertEquals("testDir not overwritten", source, options.testDir);
        // not changed
//        assertEquals("testDir is not correct", testDir, options.testDir);
        assertEquals("reportDir is not correct", reportDir, options.reportDir);
        assertEquals("surefireReport is not correct", surefireReport, options.surefireReportDir);
        assertFalse("keepGeneratedCatalog is not false", options.keepGeneratedCatalog);
        assertNull("catalog file is not null", options.catalogFile);
        assertFalse("coverage is not false", options.coverage);
        assertEquals("executionId is not default", "default", options.executionId);
        assertTrue("excludes is not empty", options.excludes.isEmpty());
        assertFalse("folding is not false", options.folding);
    } 

    @Test
    public void constructorOverrideReportTest() {
        File source = new File(".").getAbsoluteFile();
        File testDir = new File(source, "src/test/xspec");
        File reportDir = new File(source, "target/xspec-reports");
        File surefireReport = new File(source, "target/surefire-reports");
        
        RunnerOptions options = new RunnerOptions(source, null, null, null, null, source, null, null, null, null);
        assertEquals("reportDir not overwritten", source, options.reportDir);
        // not changed
        assertEquals("testDir is not correct", testDir, options.testDir);
//        assertEquals("reportDir is not correct", reportDir, options.reportDir);
        assertEquals("surefireReport is not correct", surefireReport, options.surefireReportDir);
        assertFalse("keepGeneratedCatalog is not false", options.keepGeneratedCatalog);
        assertNull("catalog file is not null", options.catalogFile);
        assertFalse("coverage is not false", options.coverage);
        assertEquals("executionId is not default", "default", options.executionId);
        assertTrue("excludes is not empty", options.excludes.isEmpty());
        assertFalse("folding is not false", options.folding);
     } 

    @Test
    public void constructorOverrideExecutionIdTest() {
        File source = new File(".").getAbsoluteFile();
        File testDir = new File(source, "src/test/xspec");
        File reportDir = new File(source, "target/xspec-reports");
        File surefireReport = new File(source, "target/surefire-reports");
        
        RunnerOptions options = new RunnerOptions(source, null, null, null, null, null, "executionId", null, null, null);
        assertEquals("executionId not overwritten", "executionId", options.executionId);

        // not changed
        assertEquals("testDir is not correct", testDir, options.testDir);
        assertEquals("reportDir is not correct", reportDir, options.reportDir);
        assertEquals("surefireReport is not correct", surefireReport, options.surefireReportDir);
        assertFalse("keepGeneratedCatalog is not false", options.keepGeneratedCatalog);
        assertNull("catalog file is not null", options.catalogFile);
        assertFalse("coverage is not false", options.coverage);
//        assertEquals("executionId is not default", "default", options.executionId);
        assertTrue("excludes is not empty", options.excludes.isEmpty());
        assertFalse("folding is not false", options.folding);
    }

    @Test
    public void constructorOverrideSurefireTest() {
        File source = new File(".").getAbsoluteFile();
        File testDir = new File(source, "src/test/xspec");
        File reportDir = new File(source, "target/xspec-reports");
        File surefireReport = new File(source, "target/surefire-reports");
        
        RunnerOptions options = new RunnerOptions(source, null, null, null, null, null, null, source, null, null);
        assertEquals("surefireReport not overwritten", source, options.surefireReportDir);
        
        // not changed
        assertEquals("testDir is not correct", testDir, options.testDir);
        assertEquals("reportDir is not correct", reportDir, options.reportDir);
//        assertEquals("surefireReport is not correct", surefireReport, options.surefireReportDir);
        assertFalse("keepGeneratedCatalog is not false", options.keepGeneratedCatalog);
        assertNull("catalog file is not null", options.catalogFile);
        assertFalse("coverage is not false", options.coverage);
        assertEquals("executionId is not default", "default", options.executionId);
        assertTrue("excludes is not empty", options.excludes.isEmpty());
        assertFalse("folding is not false", options.folding);
    }

    @Test
    public void constructorOverrideCoverageTest() {
        File source = new File(".").getAbsoluteFile();
        File testDir = new File(source, "src/test/xspec");
        File reportDir = new File(source, "target/xspec-reports");
        File surefireReport = new File(source, "target/surefire-reports");
        
        RunnerOptions options = new RunnerOptions(source, null, null, null, null, null, null, null, Boolean.TRUE, false);
        assertTrue("coverage not overWritten", options.coverage);
        
        // not changed
        assertEquals("testDir is not correct", testDir, options.testDir);
        assertEquals("reportDir is not correct", reportDir, options.reportDir);
        assertEquals("surefireReport is not correct", surefireReport, options.surefireReportDir);
        assertFalse("keepGeneratedCatalog is not false", options.keepGeneratedCatalog);
        assertNull("catalog file is not null", options.catalogFile);
//        assertFalse("coverage is not false", options.coverage);
        assertEquals("executionId is not default", "default", options.executionId);
        assertTrue("excludes is not empty", options.excludes.isEmpty());
        assertFalse("folding is not false", options.folding);
    }

    @Test
    public void constructorOverrideFoldingTest() {
        File source = new File(".").getAbsoluteFile();
        File testDir = new File(source, "src/test/xspec");
        File reportDir = new File(source, "target/xspec-reports");
        File surefireReport = new File(source, "target/surefire-reports");
        
        RunnerOptions options = new RunnerOptions(source, null, null, null, null, null, null, null, null, Boolean.TRUE);
        
        // not changed
        assertEquals("testDir is not correct", testDir, options.testDir);
        assertEquals("reportDir is not correct", reportDir, options.reportDir);
        assertEquals("surefireReport is not correct", surefireReport, options.surefireReportDir);
        assertFalse("keepGeneratedCatalog is not false", options.keepGeneratedCatalog);
        assertNull("catalog file is not null", options.catalogFile);
        assertFalse("coverage is not false", options.coverage);
        assertEquals("executionId is not default", "default", options.executionId);
        assertTrue("excludes is not empty", options.excludes.isEmpty());
        // must be true !
        assertTrue("folding is not true", options.folding);
    }
}
