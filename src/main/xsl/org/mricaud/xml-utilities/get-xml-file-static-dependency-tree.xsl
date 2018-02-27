<?xml version= "1.0" encoding="UTF-8"?>
<xsl:stylesheet 
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
  xmlns:xs="http://www.w3.org/2001/XMLSchema"
  xmlns:xd="http://www.oxygenxml.com/ns/doc/xsl"
  xmlns:functx="http://www.functx.com"
  xmlns:xut="https://github.com/mricaud/xml-utilities"
  xmlns:xi="http://www.w3.org/2003/XInclude"
  xmlns:xslt="http://www.w3.org/1999/XSL/Transform"
  xmlns:rng="http://relaxng.org/ns/structure/1.0"
  xmlns:nvdl="http://purl.oclc.org/dsdl/nvdl/ns/structure/1.0"
  xmlns:iso-sch="http://purl.oclc.org/dsdl/schematron"
  xmlns:sch="http://www.ascc.net/xml/schematron"
  xmlns:p="http://www.w3.org/ns/xproc"  
  xmlns:xspec="http://www.jenitennison.com/xslt/xspec"
  version="3.0"
  exclude-result-prefixes="#all">

  <xd:doc scope="stylesheet">
    <xd:desc>
      <xd:p><xd:b>Author:</xd:b> Matthieu Ricaud-Dussarget</xd:p>
      <xd:p>Generate an file which shows all xml dependencies of an "x-file"(xsl, xpl, xsd, etc.)</xd:p>
    </xd:desc>
  </xd:doc>
  
  <xsl:import href="functx.xsl"/>
  
  <xsl:output name="xut:xml" method="xml" indent="yes"/>
  <xsl:output name="xut:xhtml" method="xhtml" indent="yes"/>
  
  <xd:p>param to gather each "x-file" content within the final result</xd:p>
  <xsl:param name="xut:get-xml-file-static-dependency-tree.getContent" select="false()" as="xs:boolean"/>
  
  <xd:p>When a dependency appears several times, only develop the first occurence</xd:p>
  <xsl:param name="xut:get-xml-file-static-dependency-tree.filterDuplicatedDependencies" select="true()" as="xs:boolean"/>
  
  <xd:p>Instead of the default XML output one can generate an HTML file which might be easyer to share</xd:p> 
  <xsl:param name="xut:get-xml-file-static-dependency-tree.outputHtml" select="false()" as="xs:boolean"/>
  
  <!--================================================================================================================-->
  <!--INIT-->
  <!--================================================================================================================-->
  
  <xd:p>Calling this xsl with no specific mode should work, as long as with mode="xut:get-xml-dependency-tree.main"</xd:p>
  <xsl:template match="/">
    <xsl:apply-templates select="." mode="xut:get-xml-dependency-tree.main"/>
  </xsl:template>
  
  <!--================================================================================================================-->
  <!--MAIN-->
  <!--================================================================================================================-->
  
  <xd:p>Main driver</xd:p>
  <xsl:template match="/" mode="xut:get-xml-dependency-tree.main">
    <!--First, get the dependency tree-->
    <xsl:variable name="step" as="document-node()">
      <xsl:document>
        <xsl:apply-templates select="." mode="xut:get-xml-dependency-tree"/>
      </xsl:document>
    </xsl:variable>
    <!--get content if asked-->
    <xsl:variable name="step" as="document-node()">
      <xsl:choose>
        <xsl:when test="$xut:get-xml-file-static-dependency-tree.getContent">
          <xsl:document>
            <xsl:apply-templates select="$step" mode="xut:get-xml-dependency-tree.getContent"/>
          </xsl:document>
        </xsl:when>
        <xsl:otherwise>
          <xsl:sequence select="$step"/>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:variable>
    <!--filter duplicated dependencies if asked-->
    <xsl:variable name="step" as="document-node()">
      <xsl:choose>
        <xsl:when test="$xut:get-xml-file-static-dependency-tree.filterDuplicatedDependencies">
          <xsl:document>
            <xsl:apply-templates select="$step" mode="xut:get-xml-dependency-tree.filterDuplicatedDependencies"/>
          </xsl:document>
        </xsl:when>
        <xsl:otherwise>
          <xsl:sequence select="$step"/>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:variable>
    <!--Final result as XML or HTML-->
    <xsl:choose>
      <xsl:when test="not($xut:get-xml-file-static-dependency-tree.outputHtml)">
        <xsl:result-document format="xut:xml">
          <xsl:sequence select="$step"/>
        </xsl:result-document>
      </xsl:when>
      <xsl:otherwise>
        <xsl:result-document format="xut:xhtml">
          <xsl:apply-templates select="$step" mode="xut:get-xml-dependency-tree.to-html"/>
        </xsl:result-document>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>
  
  <!--================================================================================================================-->
  <!--MODE xut:get-xml-dependency-tree-->
  <!--================================================================================================================-->
  
  <xd:p>Document-node matching (any kind of file)</xd:p>
  <xsl:template match="/" mode="xut:get-xml-dependency-tree">
    <xsl:param name="dependency-type" as="xs:string?"/>
    <xsl:param name="uri" as="xs:string?"/>
    <xsl:variable name="specific-attributes-set" as="attribute()*">
      <xsl:apply-templates select="*" mode="xut:get-xml-dependency-tree.specific-attributes-set"/>
    </xsl:variable>
    <xsl:variable name="abs-uri" select="document-uri(/)" as="xs:anyURI"/>
    <file>
      <xsl:if test="not(empty($dependency-type))">
        <xsl:attribute name="dependency-type" select="$dependency-type"/>
      </xsl:if>
      <xsl:attribute name="name" select="xut:getFileName(string($abs-uri))"/>
      <xsl:if test="not(empty($uri))">
        <xsl:attribute name="uri" select="$uri"/>
      </xsl:if>
      <xsl:if test="not(empty($abs-uri))"> <!--$addAbsoluteUri-->
        <xsl:attribute name="abs-uri" select="$abs-uri"/>
      </xsl:if>
      <xsl:for-each select="$specific-attributes-set">
        <xsl:attribute name="{'_' || local-name(.)}" select="."/>
      </xsl:for-each>
      <xsl:apply-templates mode="#current">
        <xsl:with-param name="caller.uri" select="$abs-uri" as="xs:anyURI" tunnel="yes"/>
      </xsl:apply-templates>
    </file>
  </xsl:template>
  
  <xd:p>Common template to manage dependencies walking behaviour</xd:p>
  <xsl:template name="xut:get-xml-dependency">
    <xsl:param name="caller.uri" as="xs:anyURI" tunnel="yes"/>
    <xsl:param name="res.attribute" required="yes" as="xs:string"/>
    <xsl:param name="dependency-type" select="name()" as="xs:string"/>
    <xsl:variable name="abs-uri" select="xut:getAbsoluteUri(., $res.attribute)" as="xs:anyURI"/>
    <xsl:choose>
      <xsl:when test="$caller.uri != $abs-uri">
        <xsl:choose>
          <xsl:when test="doc-available($abs-uri)">
            <xsl:apply-templates select="doc($abs-uri)" mode="#current">
              <xsl:with-param name="dependency-type" select="$dependency-type" as="xs:string"/>
              <xsl:with-param name="uri" select="$res.attribute" as="xs:string"/>
            </xsl:apply-templates>
            <!--In some special case an inclusion can contains another inclusion (e.g. rng:include with rng:externalRef inside)-->
            <xsl:apply-templates mode="#current"/>
          </xsl:when>
          <xsl:when test="unparsed-text-available($abs-uri)">
            <xsl:variable name="error.msg" as="xs:string">document is not available as XML: sub-dependencies will not be analyzed</xsl:variable>
            <file name="{xut:getFileName(string($abs-uri))}" abs-uri="{$abs-uri}">
              <xsl:if test="not(empty($dependency-type))">
                <xsl:attribute name="dependency-type" select="$dependency-type"/>
              </xsl:if>
              <report role="warning" code="xut:docIsNotAvailableAsXML"><xsl:value-of select="$error.msg"/></report>
              <xsl:message>[WARNING] <xsl:value-of select="$error.msg"/></xsl:message>
              <!--In some special case an inclusion can contains another inclusion (e.g. rng:include with rng:externalRef inside)-->
              <xsl:apply-templates mode="#current"/>
            </file>
          </xsl:when>
          <xsl:otherwise>
            <xsl:variable name="error.msg" as="xs:string*">document is not available: <xsl:value-of select="$abs-uri"/></xsl:variable>
            <file name="{xut:getFileName(string($abs-uri))}">
              <xsl:if test="not(empty($dependency-type))">
                <xsl:attribute name="dependency-type" select="$dependency-type"/>
              </xsl:if>
              <report role="error" code="xut:docIsNotAvailable"><xsl:value-of select="$error.msg"/></report>
              <!--In some special case an inclusion can contains another inclusion (e.g. rng:include with rng:externalRef inside)-->
              <xsl:apply-templates mode="#current"/>
            </file>
            <xsl:message>[ERROR] <xsl:value-of select="$error.msg"/></xsl:message>
          </xsl:otherwise>
        </xsl:choose>
      </xsl:when>
      <xsl:otherwise>
        <xsl:variable name="error.msg" as="xs:string*">Circular ref to <xsl:value-of select="$caller.uri"/></xsl:variable>
        <file name="{xut:getFileName(string($abs-uri))}">
          <report role="warning" code="xut:circularRef"><xsl:value-of select="$error.msg"/></report>
        </file>
        <xsl:message>[WARNING] <xsl:value-of select="$error.msg"/></xsl:message>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>
  
  <!--===========================================-->
  <!-- xi:include depencencies -->
  <!--===========================================-->
  
  <xsl:template match="xi:include" mode="xut:get-xml-dependency-tree">
    <xsl:call-template name="xut:get-xml-dependency">
      <xsl:with-param name="res.attribute" select="@href" as="xs:string"/>
    </xsl:call-template>
  </xsl:template>
  
  <!--===========================================-->
  <!-- XSLT depencencies -->
  <!--===========================================-->
  
  <xsl:template match="xslt:import | xslt:include" mode="xut:get-xml-dependency-tree">
    <xsl:call-template name="xut:get-xml-dependency">
      <xsl:with-param name="res.attribute" select="@href" as="xs:string"/>
    </xsl:call-template>
  </xsl:template>
  
  <!--TODO-->
  <!--<xsl:template match="xsl:*[contains(@,'document(')]">
  </xsl:template>-->
  
  <xsl:template match="/xsl:stylesheet" mode="xut:get-xml-dependency-tree.specific-attributes-set" as="attribute()*">
    <xsl:copy-of select="@version"/>
  </xsl:template>
  
  <!--===========================================-->
  <!-- XPROC depencencies -->
  <!--===========================================-->
  
  <xd:p>This special uri is resolved within calabash</xd:p>
  <xsl:template match="p:import[@href = 'http://xmlcalabash.com/extension/steps/library-1.0.xpl']" priority="1" mode="xut:get-xml-dependency-tree">
    <xsl:copy-of select="."/>
  </xsl:template>
  
  <xsl:template match="p:import[@href] | p:document[@href]" mode="xut:get-xml-dependency-tree">
    <xsl:call-template name="xut:get-xml-dependency">
      <xsl:with-param name="res.attribute" select="@href" as="xs:string"/>
    </xsl:call-template>
  </xsl:template>
  
  <xsl:template match="p:load[@href]" mode="xut:get-xml-dependency-tree">
    <xsl:call-template name="xut:get-xml-dependency">
      <xsl:with-param name="res.attribute" select="@href" as="xs:string"/>
    </xsl:call-template>
  </xsl:template>
  
  <xsl:template match="p:import | p:document | p:load" mode="xut:get-xml-dependency-tree" priority="-1">
    <xsl:message>[WARNING] <xsl:value-of select="name()"/> without @href will not be processed</xsl:message>
  </xsl:template>
  
  <!--===========================================-->
  <!-- XSD depencencies -->
  <!--===========================================-->
  
  <xsl:template match="xs:include | xs:import" mode="xut:get-xml-dependency-tree">
    <xsl:call-template name="xut:get-xml-dependency">
      <xsl:with-param name="res.attribute" select="@schemaLocation" as="xs:string"/>
    </xsl:call-template>
  </xsl:template>
  
  <!--===========================================-->
  <!-- RNG depencencies-->
  <!--===========================================-->
  
  <xsl:template match="rng:include | rng:externalRef" mode="xut:get-xml-dependency-tree">
    <xsl:call-template name="xut:get-xml-dependency">
      <xsl:with-param name="res.attribute" select="@href" as="xs:string"/>
    </xsl:call-template>
  </xsl:template>
  
  <!--===========================================-->
  <!-- SCH depencencies -->
  <!--===========================================-->
  
  <xsl:template match="sch:extends| iso-sch:extends" mode="xut:get-xml-dependency-tree">
    <xsl:call-template name="xut:get-xml-dependency">
      <xsl:with-param name="res.attribute" select="@href" as="xs:string"/>
    </xsl:call-template>
  </xsl:template>
  
  <!--FIXME-->
  <!--<xsl:template match="/iso-sch:schema | /sch:schema" mode="xut:get-xml-dependency-tree">
    <xsl:call-template name="xut:get-xml-dependency">
      <xsl:with-param name="res.attribute" select="@see" as="xs:string"/>
    </xsl:call-template>
  </xsl:template>-->
  
  <xsl:template match="/iso-sch:schema | /sch:schema" mode="xut:get-xml-dependency-tree.specific-attributes-set" as="attribute()*">
    <xsl:copy-of select="@* except (@see | @icon)"/>
  </xsl:template>
  
  <!--===========================================-->
  <!-- NVDL depencencies -->
  <!--===========================================-->
  
  <xsl:template match="nvdl:validate[@schema]" mode="xut:get-xml-dependency-tree">
    <xsl:call-template name="xut:get-xml-dependency">
      <xsl:with-param name="res.attribute" select="@schema" as="xs:string"/>
    </xsl:call-template>
  </xsl:template>
  
  <!--===========================================-->
  <!-- XSpec depencencies -->
  <!--===========================================-->
  
  <xsl:template match="xspec:context[@href] | xspec:expect[@href]" mode="xut:get-xml-dependency-tree">
    <xsl:call-template name="xut:get-xml-dependency">
      <xsl:with-param name="res.attribute" select="@href" as="xs:string"/>
    </xsl:call-template>
  </xsl:template>
  
  <xsl:template match="xspec:description[@stylesheet]" mode="xut:get-xml-dependency-tree">
    <xsl:call-template name="xut:get-xml-dependency">
      <xsl:with-param name="res.attribute" select="@stylesheet" as="xs:string"/>
    </xsl:call-template>
  </xsl:template>
  <xsl:template match="xspec:description[@query-at]" mode="xut:get-xml-dependency-tree">
    <xsl:call-template name="xut:get-xml-dependency">
      <xsl:with-param name="res.attribute" select="@query-at" as="xs:string"/>
    </xsl:call-template>
  </xsl:template>
  <xsl:template match="xspec:description[@schematron]" mode="xut:get-xml-dependency-tree">
    <xsl:call-template name="xut:get-xml-dependency">
      <xsl:with-param name="res.attribute" select="@schematron" as="xs:string"/>
    </xsl:call-template>
  </xsl:template>

  <!--===========================================-->
  <!-- DEFAULT -->
  <!--===========================================-->
  
  <xsl:template match="*" mode="xut:get-xml-dependency-tree" priority="-2">
    <xsl:apply-templates mode="#current"/>
  </xsl:template>
  
  <xsl:template match="text()" mode="xut:get-xml-dependency-tree" priority="-1"/>
  
  <xsl:template match="/*" mode="xut:get-xml-dependency-tree.specific-attributes-set" priority="-1"/>

  <!--================================================================================================================-->
  <!-- MODE xut:get-xml-file-static-dependency-tree.filterDuplicatedDependencies -->
  <!--================================================================================================================-->
  
  <xsl:template match="file[@abs-uri = preceding::file/@abs-uri]" mode="xut:get-xml-dependency-tree.filterDuplicatedDependencies">
    <xsl:copy>
      <xsl:apply-templates select="@*" mode="#current"/>
      <report role="info" code="duplicatedDependency">This file has already been processed</report>
    </xsl:copy>
  </xsl:template>
  
  <xsl:template match="node() | @*" mode="xut:get-xml-dependency-tree.filterDuplicatedDependencies">
    <xsl:copy>
      <xsl:apply-templates select="node() | @*" mode="#current"/>
    </xsl:copy>
  </xsl:template>
  
  <!--================================================================================================================-->
  <!-- MODE xut:get-xml-dependency-tree.getContent -->
  <!--================================================================================================================-->
  
  <xd:p>Copy the content of the file,  except when it has already been done</xd:p>
  <xsl:template match="file" mode="xut:get-xml-dependency-tree.getContent">
    <xsl:copy>
      <xsl:apply-templates select="@* | report" mode="#current"/>
      <xsl:choose>
        <xsl:when test="report[@code != 'xut:docIsNotAvailableAsXML']"/>
        <xsl:otherwise>
          <content>
            <xsl:choose>
              <xsl:when test="@abs-uri = preceding::file/@abs-uri">
                <xsl:text>Content has already been processed</xsl:text>
              </xsl:when>
              <xsl:when test="report[@code = 'xut:docIsNotAvailableAsXML']">
                <xsl:sequence select="unparsed-text(@abs-uri)"/>
              </xsl:when>
              <xsl:otherwise>
                <xsl:copy-of select="doc(@abs-uri)"/>
              </xsl:otherwise>
            </xsl:choose>
          </content>
        </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="node() except report" mode="#current"/>
    </xsl:copy>
  </xsl:template>
  
  <xsl:template match="node() | @*" mode="xut:get-xml-dependency-tree.getContent">
    <xsl:copy>
      <xsl:apply-templates select="node() | @*" mode="#current"/>
    </xsl:copy>
  </xsl:template>
  
  <!--================================================================================================================-->
  <!--MODE xut:get-xml-dependency-tree.to-html-->
  <!--================================================================================================================-->
  
  <xsl:template match="/" mode="xut:get-xml-dependency-tree.to-html">
    <html xmlns="http://www.w3.org/1999/xhtml">
      <head>
        <title><xsl:value-of select="file[1]/@name"/> dependency tree</title>
        <style type="text/css">
          <![CDATA[
            body{font-family:sans-serif;}
            li{list-style-type:square; margin-top:1em;}
            .dependency-type{color:grey;}
            .specific-attributes{color:grey; font-size:0.85em;}
            .content{padding:0.5em;border:1px solid grey;color:grey;overflow: auto;}
            .report.error{color:red;}
            .report.warning{color:orange;}
            .report.info{color:lightblue;}
          ]]>
        </style>
      </head>
      <body>
        <h3><xsl:value-of select="file[1]/@name"/> dependency tree</h3>
        <ul>
          <xsl:apply-templates mode="#current"/>
        </ul>
      </body>
    </html>
  </xsl:template>
  
  <xsl:template match="file" mode="xut:get-xml-dependency-tree.to-html">
    <li xmlns="http://www.w3.org/1999/xhtml">
      <a href="{@abs-uri}">
        <xsl:if test="xut:getFolderPath(@uri) != ''">
          <xsl:sequence select="xut:getFolderPath(@uri) || '/'"/>
        </xsl:if>
        <strong>
          <xsl:value-of select="@name"/>
        </strong>
      </a>
      <xsl:if test="@dependency-type">
        <span class="dependency-type">
          <xsl:sequence select="' ' || @dependency-type"/>
        </span>
      </xsl:if>
      <xsl:if test="@*[starts-with(local-name(.), '_')]">
        <br/>
        <span class="specific-attributes">
          <xsl:text> </xsl:text>
          <xsl:for-each select="@*[starts-with(local-name(.), '_')]">
            <xsl:sequence select="substring-after(local-name(), '_') || '=&quot;' ||  . || '&quot;'"/>
          </xsl:for-each>
        </span>
      </xsl:if>
      <xsl:apply-templates select="* except file" mode="#current"/>
      <xsl:if test="file">
        <ul>
          <xsl:apply-templates select="file | report" mode="#current"/>
        </ul>
      </xsl:if>
    </li>
  </xsl:template>
  
  <xsl:template match="content" mode="xut:get-xml-dependency-tree.to-html">
    <pre xmlns="http://www.w3.org/1999/xhtml" class="content">
      <xsl:value-of select="serialize(node())"/>
    </pre>
  </xsl:template>
  
  <xsl:template match="report" mode="xut:get-xml-dependency-tree.to-html">
    <div xmlns="http://www.w3.org/1999/xhtml" class="report {@role}">
      <strong><xsl:value-of select="upper-case(@role)"/></strong>
      <xsl:text>: </xsl:text>
      <xsl:value-of select="."/>
    </div>
  </xsl:template>
  
  <xsl:variable name="xut:abs-uri.reg" as="xs:string">
    <!--<xsl:text>/^([a-z0-9+.-]+):(?://(?:((?:[a-z0-9-._~!$&amp;'()*+,;=:]|%[0-9A-F]{2})*)@)?((?:[a-z0-9-._~!$&amp;'()*+,;=]|%[0-9A-F]{2})*)(?::(\d*))?(/(?:[a-z0-9-._~!$&amp;'()*+,;=:@/]|%[0-9A-F]{2})*)?|(/?(?:[a-z0-9-._~!$&amp;'()*+,;=:@]|%[0-9A-F]{2})+(?:[a-z0-9-._~!$&amp;'()*+,;=:@/]|%[0-9A-F]{2})*)?)(?:\?((?:[a-z0-9-._~!$&amp;'()*+,;=:/?@]|%[0-9A-F]{2})*))?(?:#((?:[a-z0-9-._~!$&amp;'()*+,;=:/?@]|%[0-9A-F]{2})*))?$</xsl:text>-->
    <!--<xsl:text>^([a-z0-9+.-]+)://?/?.*$</xsl:text>-->
    <xsl:text>^([a-zA-Z0-9+.-]+):/.*$</xsl:text>
  </xsl:variable>
  
  <xd:p>Get the absolute uri of a included file within another</xd:p>
  <xsl:function name="xut:getAbsoluteUri" as="xs:anyURI">
    <xsl:param name="n" as="node()"/>
    <xsl:param name="uri" as="xs:string"/>
    <xsl:choose>
      <!--uri is absolute-->
      <xsl:when test="matches($uri, $xut:abs-uri.reg)">
        <xsl:value-of select="xs:anyURI($uri)"/>
      </xsl:when>
      <!--uri is relativ-->
      <xsl:otherwise>
        <xsl:sequence select="resolve-uri($uri, base-uri($n) )"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:function>
  
  <!--================================================================================================================-->
  <!--FUNCTION TO BE EXTERNALIZED-->
  <!--================================================================================================================-->
  
  <xd:doc>
    <xd:desc>
      <xd:p>Return the file name from an abolute or a relativ path</xd:p>
      <xd:p>If <xd:ref name="filePath" type="parameter">$filePath</xd:ref> is empty it will retunr an empty string (not an empty sequence)</xd:p>
    </xd:desc>
    <xd:param name="filePath">[String] path of the file (typically string(base-uri())</xd:param>
    <xd:param name="withExt">[Boolean] with or without extension</xd:param>
    <xd:return>File name (with or without extension)</xd:return>
  </xd:doc>
  <xsl:function name="xut:getFileName" as="xs:string">
    <xsl:param name="filePath" as="xs:string?"/>
    <xsl:param name="withExt" as="xs:boolean"/>
    <xsl:choose>
      <!-- An empty string would lead an error in the next when (because of a empty regex)-->
      <xsl:when test="normalize-space($filePath) = ''">
        <xsl:value-of select="$filePath"/>
      </xsl:when>
      <xsl:otherwise>
        <xsl:variable name="fileNameWithExt" select="functx:substring-after-last-match($filePath,'/')" as="xs:string?"/>
        <xsl:variable name="fileNameNoExt" select="functx:substring-before-last-match($fileNameWithExt,'\.')" as="xs:string?"/>
        <xsl:variable name="ext" select="functx:substring-after-match($fileNameWithExt,$fileNameNoExt)" as="xs:string?"/>
        <xsl:sequence select="concat('', $fileNameNoExt, if ($withExt) then ($ext) else (''))"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:function>
  
  <xd:doc>1 arg signature of xut:getFileName. Default : extension is on</xd:doc>
  <xsl:function name="xut:getFileName" as="xs:string">
    <xsl:param name="filePath" as="xs:string?"/>
    <xsl:sequence select="xut:getFileName($filePath,true())"/>
  </xsl:function>
  
  <xd:doc>
    <xd:desc>
      <xd:p>Get the extension of a file from it an absolute or relativ path</xd:p>
      <xd:p>If <xd:ref name="filePath" type="parameter">$filePath</xd:ref> is empty, it will return an empty string (not an empty sequence)</xd:p>
    </xd:desc>
    <xd:param name="filePath">[String] path of the file (typically string(base-uri())</xd:param>
    <xd:return>The file extension if it has one</xd:return>
  </xd:doc>
  <xsl:function name="xut:getFileExt" as="xs:string?">
    <xsl:param name="filePath" as="xs:string?"/>
    <xsl:choose>
      <xsl:when test="not(matches(functx:substring-after-last-match($filePath,'/'), '\.'))">
        <!-- return an empty string (not an empty sequence) -->
        <xsl:text/>
      </xsl:when>
      <xsl:otherwise>
        <xsl:sequence select="concat('',functx:substring-after-last-match($filePath,'\.'))"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:function>
  
  <xd:doc>
    <xd:desc>
      <xd:p>Get the folder path of a file path, level can be specified to have the parg</xd:p>
    </xd:desc>
    <xd:param name="filePath">File path as xs:string (use string(base-uri()) if necessary)</xd:param>
    <xd:param name="level">Tree level as integer, min = 1 (1 = full path, 2 = full path except last folder, etc.)</xd:param>
    <xd:return>Folder Path as string</xd:return>
  </xd:doc>
  <xsl:function name="xut:getFolderPath" as="xs:string">
    <xsl:param name="filePath" as="xs:string?"/>
    <xsl:param name="level" as="xs:integer"/>
    <xsl:variable name="level.normalized" as="xs:integer" select="if ($level ge 1) then ($level) else (1)"/>
    <xsl:value-of select="string-join(tokenize($filePath,'/')[position() le (last() - $level.normalized)],'/')"/>
  </xsl:function>
  
  <xd:doc>
    <xd:desc>
      <xd:p>1 arg Signature of xut:getFolderPath(). Default is to get the full folder path to the file (level = 1)</xd:p>
    </xd:desc>
    <xd:param name="filePath">File path as xs:string (use string(base-uri()) if necessary)</xd:param>
    <xd:return>Full folder path of the file path</xd:return>
  </xd:doc>
  <xsl:function name="xut:getFolderPath" as="xs:string">
    <xsl:param name="filePath" as="xs:string?"/>
    <xsl:sequence select="xut:getFolderPath($filePath,1)"/>
  </xsl:function>
  
  <xd:doc>
    <xd:desc>
      <xd:p>Get the folder name of a file path</xd:p>
    </xd:desc>
    <xd:param name="filePath">File path as xs:string (use string(base-uri()) if necessary)</xd:param>
    <xd:param name="level">Tree level as integer, min = 1 (1 = parent folder of the file, 2 = "grand-parent-folderName", etc.)</xd:param>
    <xd:return>The folder name of the nth parent folder of file</xd:return>
  </xd:doc>
  <xsl:function name="xut:getFolderName" as="xs:string">
    <xsl:param name="filePath" as="xs:string?"/>
    <xsl:param name="level" as="xs:integer"/>
    <xsl:variable name="level.normalized" as="xs:integer" select="if ($level ge 1) then ($level) else (1)"/>
    <xsl:value-of select="tokenize($filePath,'/')[last() - $level.normalized]"/>
  </xsl:function>
  
  <xd:doc>
    <xd:desc>
      <xd:p>1 arg signature of xut:getFolderName()</xd:p>
    </xd:desc>
    <xd:param name="filePath">File path as xs:string (use string(base-uri()) if necessary)</xd:param>
    <xd:return>Name of the parent folder of the file</xd:return>
  </xd:doc>
  <xsl:function name="xut:getFolderName" as="xs:string">
    <xsl:param name="filePath" as="xs:string?"/>
    <xsl:value-of select="xut:getFolderName($filePath,1)"/>
  </xsl:function>
  
</xsl:stylesheet>