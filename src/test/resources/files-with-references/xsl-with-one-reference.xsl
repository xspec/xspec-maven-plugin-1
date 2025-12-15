<xsl:transform 
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:xs="http://www.w3.org/2001/XMLSchema"
  xmlns:urn="io:xspec:tests:imported"
  version="3.0">
  
  <xsl:variable
      name="data"
      as="xs:string*"
      select="unparsed-text-lines(resolve-uri('data.txt'))"/>
  
  <xsl:function name="urn:inner" as="xs:string">
    <xsl:sequence select="$data[1]"/>
  </xsl:function>
  
</xsl:transform>