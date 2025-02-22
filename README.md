## XSpec Plugin for Maven

A very simple plugin for Maven that will execute your XSpec tests as part of the *test* phase of your Maven build, reports are generated and if any tests fail the build will be failed.
The XSpec Maven plugin is licensed under the [BSD license](http://opensource.org/licenses/BSD-3-Clause). The plugin bundles aspects of the XSpec processor implementaion (written in XSLT) from https://github.com/xspec/xspec which is released under the [MIT license](http://opensource.org/licenses/MIT). 

***Note*** that tests for XQuery are not supported yet. It should not be too difficult to add support for XQuery as well for a future release, but there is no implementation today.

By default the plugin expects to find your tests in `src/test/xspec` and both XML and HTML reports will be generated into `target/xspec-reports`. In addition the XSLT compiled version of your XSpecs will be placed in `target/xspec-reports/xslt` for reference if you need to debug your tests. No file is created is source directory.


### Goals

The plugin binds to the *verify* phase by default and there is only one goal: `run-xspec`.
The plugin has been published to [Maven Central](http://search.maven.org/#search%7Cga%7C1%7Ca%3A%22xspec-maven-plugin%22)

__Plugin declaration__
```xml
<build>
  <plugins>
    <plugin>
      <groupId>io.xspec.maven</groupId>
      <artifactId>xspec-maven-plugin</artifactId>
      <version>3.1.3</version>
      <dependencies>
        <!-- if you have a license, feel free to add Saxon-PE
           or Saxon-EE instead of Saxon-HE -->
        <dependency>
          <groupId>net.sf.saxon</groupId>
          <artifactId>Saxon-HE</artifactId>
          <!-- Saxon 12.4 is required since 3.1.3 -->
          <version>12.5</version>
        </dependency>
        <dependency>
          <groupId>io.xspec</groupId>
          <artifactId>xspec</artifactId>
          <!-- XSpec 3.1.3 is required since 3.1.3 -->
          <version>3.1.3</version>
        </dependency>
      </dependencies>
      <configuration>
        <catalogFile>catalog.xml</catalogFile>
        <generateSurefireReport>true</generateSurefireReport>
        <saxonOptions>See https://github.com/xspec/xspec-maven-plugin-1/wiki</saxonOptions>
      </configuration>
      <executions>
        <execution>
          <id>xspec-tests</id>
          <phase>test</phase>
          <goals>
            <goal>run-xspec</goal>
          </goals>
        </execution>
      </executions>
    </plugin>
  </plugins>
</build>
```

### Configuration

There are several configuration options that you may specify:

* xspecCompiler
This is the path to the XSpec Compiler XSLT i.e. the `compiler/generate-xspec-tests.xsl` as provided by the de-facto XSpec processor implementation.
By default the XSpec compiler bundled in the XSpec Plugin in used, however this configuration option allows you to specify a custom version.

* xspecReporter
This is the path to the XSpec Reporter XSLT i.e. the `reporter/format-xspec-report.xsl` as provided by the de-facto XSpec processor implementation.
By default the XSpec reporter bundled in the XSpec Plugin in used, however this configuration option allows you to specify a custom version.

* testDir
This is the path to a folder containing your XSpec tests. The tests are expected to be named such that they end with the file extension `.xspec`.
By default the folder `src/test/xspec` is used.

* excludes
You may specify one or more filenames (or partial filenames), which when matched against XSpec paths in *testDir* are excluded from being executed.

* reportDir
This is the path to a folder where the XSpec tests reports will be stored.
By default the folder `target/xspec-reports` is used.

* catalogFile
This is the path to a catalog file, as defined in https://www.oasis-open.org/committees/entity/spec-2001-08-06.html. There is no default value, and is ignored if empty or if catalog file does not exist.

* surefireReportDir
This is the path where to write surefire reports, if '${generateSurefireReports} is 'true'. Default value is '${project.build.directory}/surefire-report'.

* generateSurefireReport
If set to true, generates a surefire report in '${surefireReportDir}'.

* saxonOptions
Allows to specify saxon configuration options. See [Wiki](https://github.com/xspec/xspec-maven-plugin-1/wiki) for more details.

### FAQ
* Where should I put my XSLT?

You can put it anywhere you like, although within `src/` would make the most sense! We would suggest keeping your XSLT files in `src/main/xsl/`. If you do that, then to reference the XSLT from your XSpec, you should set the `@stylesheet` attribute to a relative path to that folder. For example, given `src/main/xsl/some.xslt` and `src/test/xspec/some.xspec`, your `some.xspec` would reference `some.xslt` like so:

```xml
<x:description xmlns:x="http://www.jenitennison.com/xslt/xspec"
  stylesheet="../../main/xsl/some.xslt">
  
  ...
```

* How can I skip the XSpec tests?

XSpec will adhere to the Maven option `-DskipTests`.
If you are doing this in a forked execution such as that used by the Maven Release plugin you may also have to use the Maven option `-Darguments="-DskipTests"`.

* Must I define the Saxon dependency ?

**Yes, you must**. This is to allow to choose between Saxon-HE, Saxon-PE or Saxon-EE, if you have licences. As Maven doesn't provide a mechanism for a default dependency, you must specify it. You can also choose another releases of Saxon ; 12.4 is required since 3.1.3.

* Must I define the XSpec dependency ?

The plugin bundles - and requires - XSpec 3.1.3. So you don't need to specify XSpec release. But if newer releases of XSpec are available, you can specify your own release of XSpec. For this, excludes `io.xspec:xspec-maven-plugin` from plugin, and add your own dependency (scope `test`) of XSpec.

* How are surefire reports generated ?

Surefire report is generated from the XSpec report, via a XSL. At this time, transformation should be improved, to have a good report in Jenkins. Any help will be appreciated.
