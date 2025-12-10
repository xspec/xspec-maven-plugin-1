<xsl:transform 
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:urn="io:xspec:tests:imported"
  version="3.0">
  
  <xsl:import href="imported.xsl"/>
  
  <xsl:template match="/*">
    <xsl:apply-templates select="." mode="urn:imported"/>
  </xsl:template>
  
</xsl:transform>