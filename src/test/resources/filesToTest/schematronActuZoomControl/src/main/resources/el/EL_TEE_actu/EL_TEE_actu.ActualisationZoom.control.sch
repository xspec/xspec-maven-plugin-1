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
  
  <title>Schematron for EL el_actu TEE</title>

  
  
  <ns prefix="xsl" uri="http://www.w3.org/1999/XSL/Transform"/>
  <ns prefix="xs" uri="http://www.w3.org/2001/XMLSchema"/>
  <ns prefix="saxon" uri="http://saxon.sf.net/"/>
  <ns prefix="cxml" uri="http://www.lefebvre-sarrut.eu/ns/el/fichesGB_FP"/>
  <ns prefix="xf" uri="http://www.lefebvre-sarrut.eu/ns/xmlfirst"/>
  
  <include href="EL_TEE_actu.commons.control.sch#el-actu-common"/>
  
  <pattern id="test">
    <rule context="xf:editorialEntity/xf:metadata/xf:meta[@code='EL_META_actuCategorieActualite']" role="error">
      <assert id="meta-actu-categorie" test="xf:value/xf:ref/@idRef = 'ZOOM'">La catégorie d'un Zoom doit obligatoirement être renseignée à "Zoom".</assert>
    </rule>
  </pattern>



</schema>