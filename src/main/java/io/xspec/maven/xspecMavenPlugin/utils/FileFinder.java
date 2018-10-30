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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This class find in a directory files that match a pettern, and are not
 * excluded by other patterns
 * Behind the scene, it uses {@link PathMatcher}, so syntax is quite comparable
 * to ant syntax for patterns
 * @author cmarchand
 */
public class FileFinder {

    private final File directoryToSearch;
    private final String matchPattern;
    private final List<String> excludes;

    /**
     * Creates a new FileFinder that will search in <tt>directoryToSearch</tt>,
     * for files that match <tt>matchPattern</tt>, and are not excluded by
     * patterns stored in <tt>excludes</tt>.
     * Search is always recursive.
     * @param directoryToSearch The directory to sarch in
     * @param matchPattern The pattern files must match. If you want to search
     * recursively, you have to use <tt>**\/</tt> pattern
     * @param excludes The patterns selected files must not match
     */
    public FileFinder(
            final File directoryToSearch, 
            final String matchPattern, 
            final List<String> excludes) {
        super();
        this.directoryToSearch = directoryToSearch;
        this.matchPattern = matchPattern;
        this.excludes = (excludes!=null ? excludes : Collections.EMPTY_LIST);
    }
    
    /**
     * Searches for files.
     * Files <b>must</b> match <tt>matchPattern</tt>, and must not match any
     * of <tt>excludes</tt> patterns.
     * @return The found files.
     * @throws IOException If any problem occurs.
     */
    public List<Path> search() throws IOException {
        Path rootPath = directoryToSearch.toPath();
        PathMatcher matcher = rootPath.getFileSystem().getPathMatcher(getSyntaxAndPattern(matchPattern));
        Stream<Path> stream = Files.find(rootPath, Integer.MAX_VALUE, (p, a) -> matcher.matches(p) && !a.isDirectory());
        for(String exclude: excludes) {
            PathMatcher pmex = rootPath.getFileSystem().getPathMatcher(getSyntaxAndPattern(exclude));
            stream = stream.filter(p -> !pmex.matches(p));
        }
        return stream.collect(Collectors.toList());
    }
    
    private String getSyntaxAndPattern(String pattern) {
        String syntaxAndPattern = pattern.contains(":") ? pattern : "glob:"+pattern;
        // fucking windows !
        // TODO: FIXME
        if("\\".equals(File.pathSeparator)) {
            return syntaxAndPattern.replaceAll("/", "\\");
        } else return syntaxAndPattern;
    }
    
}
