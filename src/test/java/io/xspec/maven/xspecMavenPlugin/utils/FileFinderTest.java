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
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.logging.SystemStreamLog;
import static org.junit.Assert.*;

import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.junit.Test;

/**
 *
 * @author cmarchand
 */
public class FileFinderTest {
    private final Log log = new SystemStreamLog();
    
    @Test
    public void given_5_files_in_a_dir_tree_including_all_excluding_none_filefinder_should_find_5() throws Exception {
        // Given
        File rootDir = new File(getProjectDirectory(), "src/test/resources/FileFinder");
        FileFinder finder = new FileFinder(rootDir, "**/*", null, log);
        // When
        List<Path> actual = finder.search();
        // Then
        Assertions.assertThat(actual.size()).isEqualTo(5);
    }

    @Test
    public void given_5_files_in_a_dir_tree_including_all_excluding_bak_filefinder_should_find_4() throws Exception {
        // Given
        File rootDir = new File(getProjectDirectory(), "src/test/resources/FileFinder");
        FileFinder finder = new FileFinder(rootDir, "**/*", Arrays.asList("**/*bak*"), log);
        // When
        List<Path> actual = finder.search();
        // Then
        Assertions.assertThat(actual.size()).isEqualTo(4);
    }

    @Test
    public void given_5_files_in_a_dir_tree_including_all_excluding_en_subdir_filefinder_should_find_4() throws Exception {
        // Given
        File rootDir = new File(getProjectDirectory(), "src/test/resources/FileFinder");
        FileFinder finder = new FileFinder(rootDir, "**/*", Arrays.asList("**/en/**"), log);
        // When
        List<Path> actual = finder.search();
        // Then
        Assertions.assertThat(actual.size()).isEqualTo(4);
    }

    @Test
    public void given_5_files_in_a_dir_tree_including_all_excluding_en_subdir_and_bak_filefinder_should_find_3() throws Exception {
        // Given
        File rootDir = new File(getProjectDirectory(), "src/test/resources/FileFinder");
        FileFinder finder = new FileFinder(rootDir, "**/*", Arrays.asList("**/en/**", "**/*bak*"), log);
        // When
        List<Path> actual = finder.search();
        // Then
        Assertions.assertThat(actual.size()).isEqualTo(3);
    }
    
    @Test
    public void given_a_dir_tree_with_1_xspec_file_should_return_1() throws Exception {
        // Given
        File rootDir = new File(getProjectDirectory(), "src/test/resources/filesToTest/xsltTestCase");
        FileFinder finder = new FileFinder(rootDir, "**/*.xspec", new ArrayList<>(), log);
        // When
        List<Path> actual = finder.search();
        // Then
        SoftAssertions softAssertions = new SoftAssertions();
        softAssertions.assertThat(actual.size()).isEqualTo(1);
        softAssertions.assertThat(actual.get(0).toString()).endsWith("xsl1.xspec");
        softAssertions.assertAll();
    }
    
    @Test
    public void given_exclusion_without_wildChar_should_return_4() throws Exception {
        // Given
        File rootDir = new File(getProjectDirectory(), "src/test/resources/filesToTest");
        List<String> excludes = Arrays.asList("imported.xspec", "schematron2.xspec");
        FileFinder finder = new FileFinder(rootDir, "**/*.xspec", excludes, log);
        // When
        List<Path> actual = finder.search();
        // Then
        Assertions.assertThat(actual.size()).isEqualTo(4);
    }

    @Test
    public void given_exclusion_with_wildChar_should_return_1() throws Exception {
        // Given
        File rootDir = new File(getProjectDirectory(), "src/test/resources/filesToTest");
        List<String> excludes = Arrays.asList("**/imported.xspec", "**/schematron2.xspec", "**/*.control.xspec");
        FileFinder finder = new FileFinder(rootDir, "**/*.xspec", excludes, log);
        // When
        List<Path> actual = finder.search();
        // Then
        SoftAssertions softAssertions = new SoftAssertions();
        softAssertions.assertThat(actual.size()).isEqualTo(1);
        softAssertions.assertThat(actual.get(0).toString()).endsWith("xsl1.xspec");
        softAssertions.assertAll();
    }
    
    
    private static File getProjectDirectory() throws URISyntaxException {
        File pos = new File(FileFinderTest.class.getClassLoader().getResource("").toURI());
        // this should be target/classes or target/test-classes
        return pos.getParentFile().getParentFile();
    }
    
}
