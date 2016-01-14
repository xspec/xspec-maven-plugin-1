<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet 
    version="2.0" 
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xs="http://www.w3.org/2001/XMLSchema"
    xmlns="http://www.w3.org/1999/XSL/TransformAlias"
    xmlns:test="http://www.jenitennison.com/xslt/unit-test"
    exclude-result-prefixes="#default test"
    xmlns:x="http://www.jenitennison.com/xslt/xspec"
    xmlns:__x="http://www.w3.org/1999/XSL/TransformAliasAlias"
    xmlns:pkg="http://expath.org/ns/pkg"
    xmlns:impl="urn:x-xspec:compile:xslt:impl">
    
    <xsl:import href="generate-xspec-tests.xsl"/>
    
    <doc xmlns="http://www.oxygenxml.com/ns/doc/xsl">
        <desc>Overridden to allow a version 3.0 stylesheet to be tested.</desc>
    </doc>
    <xsl:template match="x:description" mode="x:generate-tests">
        <!-- The compiled stylesheet element. -->
        <!-- OXYGEN PATCH START -->
        <stylesheet version="{( @xslt-version, '2.0' )[1]}">
            <!-- OXYGEN PATCH END -->
            <xsl:apply-templates select="." mode="x:copy-namespaces" />
            <import href="{$stylesheet-uri}" />
            <import href="{resolve-uri('generate-tests-utils.xsl', static-base-uri())}"/>
            <!-- This namespace alias is used for when the testing process needs to test
         the generation of XSLT! -->
            <namespace-alias stylesheet-prefix="__x" result-prefix="xsl" />
            <variable name="x:stylesheet-uri" as="xs:string" select="'{$stylesheet-uri}'" />
            <output name="x:report" method="xml" indent="yes" />
            <!-- Compile the test suite params (aka global params). -->
            <xsl:call-template name="x:compile-params"/>
            <!-- The main compiled template. -->
            <template name="x:main">
                <message>
                    <text>Testing with </text>
                    <value-of select="system-property('xsl:product-name')" />
                    <text><xsl:text> </xsl:text></text>
                    <value-of select="system-property('xsl:product-version')" />
                </message>
                <result-document format="x:report">
                    <processing-instruction name="xml-stylesheet">
                        <xsl:text>type="text/xsl" href="</xsl:text>
                        <xsl:value-of select="resolve-uri('format-xspec-report.xsl',
                            static-base-uri())" />
                        <xsl:text>"</xsl:text>
                    </processing-instruction>
                    <!-- This bit of jiggery-pokery with the $stylesheet-uri variable is so
	        that the URI appears in the trace report generated from running the
	        test stylesheet, which can then be picked up by stylesheets that
	        process *that* to generate a coverage report -->
                    <x:report stylesheet="{{$x:stylesheet-uri}}" date="{{current-dateTime()}}">
                        <!-- Generate calls to the compiled top-level scenarios. -->
                        <xsl:call-template name="x:call-scenarios"/>
                    </x:report>
                </result-document>
            </template>
            <!-- Compile the top-level scenarios. -->
            <xsl:call-template name="x:compile-scenarios"/>
        </stylesheet>
    </xsl:template>
</xsl:stylesheet>