<?xml version="1.0" encoding="UTF-8"?>
<sch:schema xmlns:sch="http://purl.oclc.org/dsdl/schematron" queryBinding="xslt2">

  <sch:pattern>
    <sch:rule context="my_element">
      <sch:assert test="true()" id="my_element.assert1">Error message.</sch:assert>
      <sch:assert test="false()" id="my_element.assert2">Error message.</sch:assert>
    </sch:rule>
  </sch:pattern>
</sch:schema>
