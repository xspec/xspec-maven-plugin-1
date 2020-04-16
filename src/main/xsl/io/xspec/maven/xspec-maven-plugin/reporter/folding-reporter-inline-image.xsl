<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:xs="http://www.w3.org/2001/XMLSchema"
  xmlns:math="http://www.w3.org/2005/xpath-functions/math"
  xmlns:xd="http://www.oxygenxml.com/ns/doc/xsl"
  xmlns:xhtml="http://www.w3.org/1999/xhtml"
  xmlns:private="top:marchand:xml:private"
  exclude-result-prefixes="#all"
  version="3.0">

  <xsl:param name="imgDown" as="xs:string" select="'down.gif'"/>
  <xsl:param name="imgUp" as="xs:string" select="'up.gif'"/>
  
  <xsl:output method="xml" indent="true"/>
  <xsl:mode on-no-match="shallow-copy"/>
  
  <xsl:template match="xsl:stylesheet">
    <xsl:copy>
      <xsl:apply-templates select="@*"/>
      <!-- required, because generated XSL has a xsl:import with a relative href -->
      <xsl:attribute name="xml:base" select="base-uri()"/>
      <xsl:apply-templates select="node()"/>
    </xsl:copy>
  </xsl:template>
  
  <xsl:template match="xhtml:script">
    <xsl:copy>
      <xsl:apply-templates select="@*"/>
      <xsl:variable name="content" as="xs:string+">
        <xsl:apply-templates select="node()"/>
      </xsl:variable>
      <xsl:value-of select="string-join($content, '')"/>
    </xsl:copy>
  </xsl:template>
  
  <xsl:template match="xhtml:script/text()" as="xs:string">
    <xsl:sequence select="string(.)"/>
  </xsl:template>
  
  <xsl:template match="xhtml:script/xsl:value-of" as="xs:string">
    <xsl:sequence select="private:change-value-of(.)"/>
  </xsl:template>
  
  <xsl:template match="xhtml:img">
    <xsl:copy>
      <xsl:apply-templates select="@* except @src"/>
      <xsl:attribute name="src" expand-text="false">{concat('data:image/gif;base64, ',if ($any-descendant-failure) then '<xsl:sequence select="$imgDown"/>' else '<xsl:sequence select="$imgUp"/>')}</xsl:attribute>
    </xsl:copy>
  </xsl:template>
  
  <xsl:function name="private:change-value-of" as="xs:string">
    <xsl:param name="content" as="element(xsl:value-of)"/>
    <xsl:variable name="expr" as="xs:string" select="$content/@select/string()"/>
    <xsl:variable name="longUri" as="xs:string" select="$expr=>substring-after('resolve-uri(''')=>substring-before(')')=>substring(1)"/>
    <xsl:variable name="uri" as="xs:string" select="$longUri=>substring-before('''')"/>
    <xsl:variable name="image" as="xs:string" select="if($uri=>ends-with('3angle-down.gif')) 
      then $imgDown 
      else $imgUp"/>
    <xsl:sequence select="concat('data:image/gif;base64, ',$image)"/>
  </xsl:function>
</xsl:stylesheet>