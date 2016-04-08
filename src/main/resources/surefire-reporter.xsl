<?xml version="1.0" encoding="UTF-8"?>
<!--
 Copyright © 2013, Adam Retter
 All rights reserved.

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions are met:
     * Redistributions of source code must retain the above copyright
       notice, this list of conditions and the following disclaimer.
     * Redistributions in binary form must reproduce the above copyright
       notice, this list of conditions and the following disclaimer in the
       documentation and/or other materials provided with the distribution.
     * Neither the name of the <organization> nor the
       names of its contributors may be used to endorse or promote products
       derived from this software without specific prior written permission.

 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
 DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
-->
        
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xs="http://www.w3.org/2001/XMLSchema"
    xmlns:local="com:oxiane:common:local"
    exclude-result-prefixes="#all"
    xmlns:x="http://www.jenitennison.com/xslt/xspec"
    version="2.0">
    <!-- converts a XSpec report to a surefire report -->
    
    <xsl:output indent="yes"/>
    
    <xsl:param name="baseDir" as="xs:string" required="yes"/>
    <xsl:param name="outputDir" as="xs:string" required="yes"/>
    <xsl:param name="reportFileName" select="tokenize(document-uri(/),'/')[last()]" as="xs:string"/>
    <xsl:variable name="classname" select="replace($reportFileName,'.xml','.xspec')"/>
    
    <xsl:template match="/">
            <xsl:variable name="xslUrl" select="/x:report/@stylesheet" as="xs:string"/>
            <xsl:comment>xslUrl=<xsl:value-of select="$xslUrl"/></xsl:comment>
            <xsl:variable name="relativeUri" select="substring-after($xslUrl, $baseDir)" as="xs:string"/>
            <xsl:comment>relativeUrl=<xsl:value-of select="$relativeUri"/></xsl:comment>
            <xsl:variable name="pathElements" select="tokenize($relativeUri, '/')" as="xs:string*"/>
            <xsl:variable name="startingPos" as="xs:integer">
                <xsl:choose>
                    <xsl:when test="contains($pathElements[1],':')">2</xsl:when>
                    <xsl:otherwise>1</xsl:otherwise>
                </xsl:choose>
            </xsl:variable>
            <xsl:variable name="package" select="string-join($pathElements[position() &gt;= ($startingPos+3) and position() lt last()],'.')"></xsl:variable>
            <xsl:comment>package=<xsl:value-of select="$package"/></xsl:comment>
            
        <xsl:result-document href="{concat($outputDir,'/TEST-',if(string-length($package) gt 0) then concat($package,'.') else '', $pathElements[last()],'.xml')}">
        <testsuites>
            <xsl:apply-templates>
                <xsl:with-param name="package" tunnel="yes" select="$package"/>
            </xsl:apply-templates>
        </testsuites>
        </xsl:result-document>
    </xsl:template>
    
    <xsl:template match="x:report">
        <xsl:apply-templates select="x:scenario"/>
    </xsl:template>
    
    <xsl:template match="x:scenario">
        <xsl:param name="package" tunnel="yes" required="yes" as="xs:string"/>
        <xsl:param name="libelle" required="no" as="xs:string?" select="''"/>
        <xsl:variable name="testsCount" select="count(.//x:test)"/>
        <xsl:variable name="failuresCount" select="count(.//x:test[@successful='false'])"/>
        <xsl:variable name="errorsCount" select="0">
            <!-- it can not have any error, errors are compilation problem -->
        </xsl:variable>
        <xsl:variable name="skippedCount" select="count(.//x:test[@pending])"/>
        <xsl:if test="./x:test">
        <testsuite tests="{$testsCount}" failures="{$failuresCount}" errors="{$errorsCount}" skipped="{$skippedCount}" package="{$package}" name="{concat($libelle,./x:label/text())}">
            <xsl:apply-templates select="x:test"/>
        </testsuite>
        </xsl:if>
        <xsl:apply-templates select="x:scenario">
            <xsl:with-param name="libelle" select="concat($libelle,x:label/text(),': ')"/>
        </xsl:apply-templates>
    </xsl:template>
    <xsl:template match="x:scenario[@pending]">
        <xsl:param name="package" tunnel="yes" required="yes" as="xs:string"/>
        <testsuite tests="0" failures="0" errors="0" skipped="1" package="{$package}"/>
    </xsl:template>
    
    <xsl:template match="x:test[@successful='false']">
        <testcase classname="{$classname}" name="{./x:label/text() | ./@label}" time="0">
            <failure message="{x:label/text() | ./@label}" type="unexpected result">
                <xsl:apply-templates select="x:expect"/>
                <xsl:apply-templates select="x:result"/>
            </failure>
        </testcase>
    </xsl:template>
    
    <xsl:template match="x:test[@succesful='false']/x:label"/>
    
    <xsl:template match="x:test[@successful='true']">
        <testcase classname="{$classname}" name="{./x:label/text() | ./@label}" time="0"/>
    </xsl:template>
    
    <xsl:template match="x:expect | x:result">
        <xsl:text disable-output-escaping="yes">&lt;![CDATA[</xsl:text>&#xA;<xsl:value-of select="local:nomPropre(local-name(.))"/><xsl:if test="x:label"><xsl:value-of select="concat(' ',x:label)"/></xsl:if>&#xA;<xsl:copy-of select="./(node() except x:label)"/><xsl:text disable-output-escaping="yes">]]&gt;</xsl:text>
    </xsl:template>
    
    <xsl:function name="local:nomPropre" as="xs:string">
        <xsl:param name="s" as="xs:string"/>
        <xsl:value-of select="concat(upper-case(substring($s,1,1)),lower-case(substring($s,2)))"/>
    </xsl:function>
    
</xsl:stylesheet>