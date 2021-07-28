<?xml version="1.0" encoding="utf-8"?>
<!--<schema xmlns="http://www.ascc.net/xml/schematron">-->
<schema 
  xmlns="http://purl.oclc.org/dsdl/schematron"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:xs="http://www.w3.org/2001/XMLSchema"
  xmlns:saxon="http://saxon.sf.net/"
  xmlns:c="http://www.lefebvre-sarrut.eu/ns/el/el_FICHE_FP"
  queryBinding="xslt2"
  id="validation_editorialEntity">
  
  <title>Schematron for EL el_actu TEE - common controls</title>
  
  <ns prefix="xsl" uri="http://www.w3.org/1999/XSL/Transform"/>
  <ns prefix="xs" uri="http://www.w3.org/2001/XMLSchema"/>
  <ns prefix="saxon" uri="http://saxon.sf.net/"/>
  <ns prefix="cxml" uri="http://www.lefebvre-sarrut.eu/ns/el/fichesGB_FP"/>
  <ns prefix="xf" uri="http://www.lefebvre-sarrut.eu/ns/xmlfirst"/>
  
  <pattern id="el-actu-common">
    <rule context="xf:editorialEntity/xf:metadata/xf:meta[@code='EL_META_actuPJ']/xf:value" id="test-refs-actives-limite" role="error">
      <assert id="meta-number-of-refs" test="count(preceding-sibling::xf:value) lt 15">Le nombre total de références actives ne peut être supérieur à 15.</assert>
    </rule>
    
    <!-- Contrôle des les liens Flash rendu nécéssaire par le partage du schéma -->
    <rule context="xf:editorialEntity[not(@code = ('EL_TEE_actuActualisationArreteExtension','EL_TEE_actuActualisation'))]" id="test-links" role="error">
      <report id="report-forbiden-els-link" test="*:a[tokenize(@class,'\s') = 'els-link']">Les liens Flash ne sont autorisés que sur les Actualisation et Arrêtés d'extension</report>
    </rule>
  </pattern>
  
</schema>