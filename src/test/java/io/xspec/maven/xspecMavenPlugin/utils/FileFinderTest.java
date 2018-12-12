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
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 *
 * @author cmarchand
 */
public class FileFinderTest {
    @Test
    public void testAllFiles() throws URISyntaxException, IOException {
        File rootDir = new File(getProjectDirectory(), "src/test/resources/FileFinder");
        System.out.println("Searching in "+rootDir.getAbsolutePath());
        FileFinder finder = new FileFinder(rootDir, "**/*", null);
        List<Path> ret = finder.search();
        assertEquals("we expect 5 regular files", 5, ret.size());
    }
    @Test
    public void testAllButBak() throws URISyntaxException, IOException {
        File rootDir = new File(getProjectDirectory(), "src/test/resources/FileFinder");
        System.out.println("Searching in "+rootDir.getAbsolutePath());
        FileFinder finder = new FileFinder(rootDir, "**/*", Arrays.asList("**/*bak*"));
        List<Path> ret = finder.search();
        assertEquals("we expect 4 regular files", 4, ret.size());
    }
    @Test
    public void testAllButEn() throws URISyntaxException, IOException {
        File rootDir = new File(getProjectDirectory(), "src/test/resources/FileFinder");
        System.out.println("Searching in "+rootDir.getAbsolutePath());
        FileFinder finder = new FileFinder(rootDir, "**/*", Arrays.asList("**/en/**"));
        List<Path> ret = finder.search();
        assertEquals("we expect 4 regular files", 4, ret.size());
    }
    @Test
    public void testAllButEnAndBak() throws URISyntaxException, IOException {
        File rootDir = new File(getProjectDirectory(), "src/test/resources/FileFinder");
        System.out.println("Searching in "+rootDir.getAbsolutePath());
        FileFinder finder = new FileFinder(rootDir, "**/*", Arrays.asList("**/en/**", "**/*bak*"));
        List<Path> ret = finder.search();
        assertEquals("we expect 3 regular files", 3, ret.size());
    }
    
    @Test
    public void testXSpecFiles() throws URISyntaxException, IOException {
        File rootDir = new File(getProjectDirectory(), "src/test/resources/filesToTest/xsltTestCase");
        System.out.println("searching in "+rootDir.getAbsolutePath());
        FileFinder finder = new FileFinder(rootDir, "**/*.xspec", new ArrayList<>());
        List<Path> ret = finder.search();
        assertEquals(1, ret.size());
        assertTrue(ret.get(0).toString().endsWith("xsl1.xspec"));
    }
    
    
    private static File getProjectDirectory() throws URISyntaxException {
        File pos = new File(FileFinderTest.class.getClassLoader().getResource("").toURI());
        // this should be target/classes or target/test-classes
        return pos.getParentFile().getParentFile();
    }
    
}
