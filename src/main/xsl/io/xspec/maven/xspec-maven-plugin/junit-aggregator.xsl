<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xs="http://www.w3.org/2001/XMLSchema"
    xmlns:math="http://www.w3.org/2005/xpath-functions/math"
    xmlns:xd="http://www.oxygenxml.com/ns/doc/xsl"
    exclude-result-prefixes="xs math xd"
    version="2.0">
    <xd:doc scope="stylesheet">
        <xd:desc>
            <xd:p><xd:b>Created on:</xd:b> Mar 12, 2018</xd:p>
            <xd:p><xd:b>Author:</xd:b> cmarchand</xd:p>
            <xd:p></xd:p>
        </xd:desc>
    </xd:doc>
    
    <xsl:param name="baseDir" as="xs:string" required="yes"/>
    
    <xsl:template match="files">
        <testsuites>
            <xsl:apply-templates/>
        </testsuites>
    </xsl:template>
    
    <xsl:template match="file">
        <xsl:variable name="fileUri" select="resolve-uri(text())" as="xs:anyURI"/>
        <xsl:variable name="fileName" as="xs:string" select="if (starts-with(text(), $baseDir)) then substring(text(),string-length($baseDir)) else text()"/>
        <xsl:apply-templates select="doc($fileUri)" mode="agg">
            <xsl:with-param name="fileName" select="$fileName" tunnel="yes"/>
        </xsl:apply-templates>
    </xsl:template>
    
    <xsl:template match="testsuites" mode="agg">
        <xsl:param name="fileName" as="xs:string" tunnel="yes"/>
        <testsuite name="{$fileName}">
            <xsl:apply-templates select="@* | node()" mode="#current"/>
        </testsuite>
    </xsl:template>
    
    <xsl:template match="@* | node()" mode="agg">
        <xsl:copy>
            <xsl:apply-templates select="@* | node()" mode="#current"/>
        </xsl:copy>
    </xsl:template>
</xsl:stylesheet>