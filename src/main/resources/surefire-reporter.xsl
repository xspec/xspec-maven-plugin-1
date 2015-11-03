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
    exclude-result-prefixes="xs"
    xmlns:x="http://www.jenitennison.com/xslt/xspec"
    version="2.0">
    <!-- converts a XSpec report to a surefire report -->
    
    <xsl:output indent="yes"/>
    <xsl:variable name="reportFileName" select="tokenize(document-uri(/),'/')[last()]" as="xs:string"/>
    <xsl:variable name="classname" select="replace($reportFileName,'.xml','.xspec')"/>
    
    <xsl:template match="x:report">
        <xsl:variable name="testsCount" select="count(//x:test)"/>
        <xsl:variable name="failuresCount" select="count(//x:test[@successful='false'])"/>
        <xsl:variable name="errorsCount" select="0">
            <!-- it can not have any error, errors are compilation problem -->
        </xsl:variable>
        <xsl:variable name="skippedCount" select="count(//x:scenario[@pending])"/>
        <testsuite tests="{$testsCount}" failures="{$failuresCount}" error="{$errorsCount}" skipped="{$skippedCount}">
            <xsl:apply-templates select="x:scenario"/>
        </testsuite>
    </xsl:template>
    
    <xsl:template match="x:scenario">
        <testcase classname="{$classname}" name="{./x:label/text()}" time="0">
            <xsl:apply-templates select="x:test"/>
        </testcase>
    </xsl:template>
    
    <xsl:template match="x:test[@successful='false']">
        <failure message="{x:label/text()}">
            <xsl:apply-templates select="x:expect"/>
            <xsl:apply-templates select="x:result"/>
        </failure>
    </xsl:template>
    
    <xsl:template match="x:expect | x:result">
        <xsl:copy-of select="."/>
    </xsl:template>
</xsl:stylesheet>