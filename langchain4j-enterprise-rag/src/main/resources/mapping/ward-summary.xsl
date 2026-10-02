<?xml version="1.0" encoding="UTF-8"?>
<!--
  The summary mapping: HL7v2 in its standard XML encoding (urn:hl7-org:v2xml) in, one
  readable clinical summary out. This text is what gets embedded; an embedding model can
  do nothing useful with raw ER7 pipes. The mapping is configuration, not code: adjust
  what the knowledge base says about an event here, without touching a line of Java.

  Values are read with normalize-space(): the encoder pretty-prints composite fields,
  so raw node values carry indentation.
-->
<xsl:stylesheet version="3.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xpath-default-namespace="urn:hl7-org:v2xml">
  <xsl:output method="text"/>

  <xsl:template match="/">
    <xsl:variable name="event"
                  select="concat(normalize-space(//MSH/MSH.9/MSG.1), '^', normalize-space(//MSH/MSH.9/MSG.2))"/>
    <xsl:variable name="born" select="normalize-space(//PID/PID.7/TS.1)"/>

    <!-- Patient Marie Dupont (id PAT-123, born 1956-03-12, sex F). -->
    <xsl:text>Patient </xsl:text>
    <xsl:value-of select="normalize-space(//PID/PID.5/XPN.2)"/>
    <xsl:text> </xsl:text>
    <xsl:value-of select="normalize-space(//PID/PID.5/XPN.1)"/>
    <xsl:text> (id </xsl:text>
    <xsl:value-of select="normalize-space(//PID/PID.3/CX.1)"/>
    <xsl:text>, born </xsl:text>
    <xsl:value-of select="concat(substring($born, 1, 4), '-', substring($born, 5, 2), '-', substring($born, 7, 2))"/>
    <xsl:text>, sex </xsl:text>
    <xsl:value-of select="normalize-space(//PID/PID.8)"/>
    <xsl:text>).&#10;</xsl:text>

    <!-- Event ORU^R01 on 2026-10-02 at 09:30. The raw HL7 timestamp (20261002093000)
         means nothing to an embedding model or an LLM; a readable date is what lets
         "what happened today?" match. -->
    <xsl:variable name="ts" select="normalize-space(//MSH/MSH.7/TS.1)"/>
    <xsl:text>Event </xsl:text>
    <xsl:value-of select="$event"/>
    <xsl:text> on </xsl:text>
    <xsl:value-of select="concat(substring($ts, 1, 4), '-', substring($ts, 5, 2), '-', substring($ts, 7, 2))"/>
    <xsl:if test="string-length($ts) &gt;= 12">
      <xsl:text> at </xsl:text>
      <xsl:value-of select="concat(substring($ts, 9, 2), ':', substring($ts, 11, 2))"/>
    </xsl:if>
    <xsl:text>.&#10;</xsl:text>

    <!-- Admitted to CARD1, reason: Chest pain. -->
    <xsl:if test="starts-with($event, 'ADT')">
      <xsl:text>Admitted</xsl:text>
      <xsl:if test="normalize-space(//PV1/PV1.3/PL.1) != ''">
        <xsl:text> to </xsl:text>
        <xsl:value-of select="normalize-space(//PV1/PV1.3/PL.1)"/>
      </xsl:if>
      <xsl:if test="normalize-space(//PV2/PV2.3/CE.2) != ''">
        <xsl:text>, reason: </xsl:text>
        <xsl:value-of select="normalize-space(//PV2/PV2.3/CE.2)"/>
      </xsl:if>
      <xsl:text>.&#10;</xsl:text>
    </xsl:if>

    <!-- Lab results:
         - Glucose: 182 mg/dL (reference 70-99), flagged HIGH -->
    <xsl:if test="starts-with($event, 'ORU')">
      <xsl:text>Lab results:&#10;</xsl:text>
      <xsl:for-each select="//OBX">
        <xsl:variable name="flag" select="normalize-space(OBX.8)"/>
        <xsl:text>- </xsl:text>
        <xsl:value-of select="normalize-space(OBX.3/CE.2)"/>
        <xsl:text>: </xsl:text>
        <xsl:value-of select="normalize-space(OBX.5)"/>
        <xsl:if test="normalize-space(OBX.6/CE.1) != ''">
          <xsl:text> </xsl:text>
          <xsl:value-of select="normalize-space(OBX.6/CE.1)"/>
        </xsl:if>
        <xsl:if test="normalize-space(OBX.7) != ''">
          <xsl:text> (reference </xsl:text>
          <xsl:value-of select="normalize-space(OBX.7)"/>
          <xsl:text>)</xsl:text>
        </xsl:if>
        <xsl:if test="$flag != ''">
          <xsl:text>, flagged </xsl:text>
          <xsl:choose>
            <xsl:when test="$flag = 'H'">HIGH</xsl:when>
            <xsl:when test="$flag = 'L'">LOW</xsl:when>
            <xsl:when test="$flag = 'HH'">CRITICALLY HIGH</xsl:when>
            <xsl:when test="$flag = 'LL'">CRITICALLY LOW</xsl:when>
            <xsl:when test="$flag = 'A'">ABNORMAL</xsl:when>
            <xsl:otherwise><xsl:value-of select="$flag"/></xsl:otherwise>
          </xsl:choose>
        </xsl:if>
        <xsl:text>&#10;</xsl:text>
      </xsl:for-each>
    </xsl:if>
  </xsl:template>
</xsl:stylesheet>
