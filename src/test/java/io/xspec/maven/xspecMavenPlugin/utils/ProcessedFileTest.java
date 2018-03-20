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
import java.io.IOException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.SystemUtils;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class ProcessedFileTest {
    private static boolean testDirExisted = false;
    private static File projectDir;
    private static File testDir;
    private static File sourceDir;
    private static File reportDir;
    private File sourceFile;
    private File reportFile;
    
    @BeforeClass
    public static void beforeClass() throws IOException {
        // hope this will not become a maven module !
        projectDir = new File(System.getProperty("user.dir"));
        testDir = new File(projectDir, "target/testResources");
        testDirExisted = testDir.exists() && testDir.isDirectory();
        checkDirectory(testDir);
        sourceDir = new File(testDir, "sources");
        reportDir = new File(testDir, "results");
    }
    
    @AfterClass
    public static void afterClass() throws IOException {
        FileUtils.deleteDirectory(sourceDir);
        FileUtils.deleteDirectory(reportDir);
        if(!testDirExisted) FileUtils.deleteDirectory(testDir);
    }
    
    @Before
    public void before() throws IOException {
        sourceFile = new File(sourceDir, "1/1.xspec");
        reportFile = new File(reportDir, "1/1.html");
        FileUtils.touch(sourceFile);
        FileUtils.touch(reportFile);
    }
    
    @After
    public void after() {
        FileUtils.deleteQuietly(sourceFile);
        FileUtils.deleteQuietly(reportFile);
    }
    
    @Test
    public void testSourceRelativePath() {
        ProcessedFile pf = new ProcessedFile(sourceDir, sourceFile, reportDir, reportFile);
        if(SystemUtils.IS_OS_WINDOWS) {
        assertEquals(pf.getRelativeSourcePath(),"1\\1.xspec");
        } else {
            assertEquals(pf.getRelativeSourcePath(),"1/1.xspec");
        }
        assertEquals(pf.getRelativeCssPath(),"..");
        if(SystemUtils.IS_OS_WINDOWS) {
            assertEquals(pf.getRelativeReportPath(), "1\\1.html");
        } else {
            assertEquals(pf.getRelativeReportPath(), "1/1.html");
        }
    }
    
    private static void checkDirectory(final File f) throws IOException {
        if(f.exists() && !f.isDirectory()) {
            throw new IOException(f.getAbsolutePath()+" exists but is not a directory. Can not process tests.");
        }
        if(!(f.exists() && f.isDirectory())) f.mkdirs();
    }
    
    
    
}