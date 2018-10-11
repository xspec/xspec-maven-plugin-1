<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xs="http://www.w3.org/2001/XMLSchema"
    exclude-result-prefixes="xs"
    version="2.0">
    
    <xsl:param name="base" as="xs:string" />
    <xsl:param name="href-parent-namespace" as="xs:string+" />
    
    <xsl:template match="@*|node()">
        <xsl:copy>
            <xsl:apply-templates select="@*|node()"/>
        </xsl:copy>
    </xsl:template>
    
    <xsl:template match="*[namespace-uri(.) = $href-parent-namespace]/@href" priority="1">
        <xsl:attribute name="href" select="resolve-uri(.,$base)"></xsl:attribute>
    </xsl:template>
    
</xsl:stylesheet>