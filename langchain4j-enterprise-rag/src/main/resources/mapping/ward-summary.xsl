<?xml version="1.0" encoding="UTF-8"?>
<!--
  The summary mapping: HL7v2 in its standard XML encoding (urn:hl7-org:v2xml) in, one
  readable clinical summary out. This text is what gets embedded; an embedding model can
  do nothing useful with raw ER7 pipes. The mapping is configuration, not code: adjust
  what the knowledge base says about an event here, without touching a line of Java.
-->
<xsl:stylesheet version="3.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xpath-default-namespace="urn:hl7-org:v2xml">
  <xsl:output method="text"/>

  <xsl:template match="/">
    <xsl:variable name="event" select="concat(//MSH/MSH.9/*[1], '^', //MSH/MSH.9/*[2])"/>
    <xsl:variable name="born" select="string(//PID/PID.7)"/>

    <!-- Patient Marie Dupont (id PAT-123, born 1956-03-12, sex F). -->
    <xsl:text>Patient </xsl:text>
    <xsl:value-of select="//PID/PID.5/*[2]"/>
    <xsl:text> </xsl:text>
    <xsl:value-of select="//PID/PID.5/*[1]"/>
    <xsl:text> (id </xsl:text>
    <xsl:value-of select="//PID/PID.3"/>
    <xsl:text>, born </xsl:text>
    <xsl:value-of select="concat(substring($born, 1, 4), '-', substring($born, 5, 2), '-', substring($born, 7, 2))"/>
    <xsl:text>, sex </xsl:text>
    <xsl:value-of select="//PID/PID.8"/>
    <xsl:text>).&#10;</xsl:text>

    <!-- Event ORU^R01 at 20261002093000. -->
    <xsl:text>Event </xsl:text>
    <xsl:value-of select="$event"/>
    <xsl:text> at </xsl:text>
    <xsl:value-of select="//MSH/MSH.7"/>
    <xsl:text>.&#10;</xsl:text>

    <!-- Admitted to WARD1, reason: Chest pain. -->
    <xsl:if test="starts-with($event, 'ADT')">
      <xsl:text>Admitted</xsl:text>
      <xsl:if test="string(//PV1/PV1.3) != ''">
        <xsl:text> to </xsl:text>
        <xsl:value-of select="//PV1/PV1.3/*[1]"/>
      </xsl:if>
      <xsl:if test="string(//PV2/PV2.3) != ''">
        <xsl:text>, reason: </xsl:text>
        <xsl:value-of select="//PV2/PV2.3/*[2]"/>
      </xsl:if>
      <xsl:text>.&#10;</xsl:text>
    </xsl:if>

    <!-- Lab results:
         - Glucose: 182 mg/dL (reference 70-99), flagged HIGH -->
    <xsl:if test="starts-with($event, 'ORU')">
      <xsl:text>Lab results:&#10;</xsl:text>
      <xsl:for-each select="//OBX">
        <xsl:text>- </xsl:text>
        <xsl:value-of select="OBX.3/*[2]"/>
        <xsl:text>: </xsl:text>
        <xsl:value-of select="OBX.5"/>
        <xsl:if test="string(OBX.6) != ''">
          <xsl:text> </xsl:text>
          <xsl:value-of select="OBX.6/*[1]"/>
        </xsl:if>
        <xsl:if test="string(OBX.7) != ''">
          <xsl:text> (reference </xsl:text>
          <xsl:value-of select="OBX.7"/>
          <xsl:text>)</xsl:text>
        </xsl:if>
        <xsl:if test="string(OBX.8) != ''">
          <xsl:text>, flagged </xsl:text>
          <xsl:choose>
            <xsl:when test="string(OBX.8) = 'H'">HIGH</xsl:when>
            <xsl:when test="string(OBX.8) = 'L'">LOW</xsl:when>
            <xsl:when test="string(OBX.8) = 'HH'">CRITICALLY HIGH</xsl:when>
            <xsl:when test="string(OBX.8) = 'LL'">CRITICALLY LOW</xsl:when>
            <xsl:when test="string(OBX.8) = 'A'">ABNORMAL</xsl:when>
            <xsl:otherwise><xsl:value-of select="OBX.8"/></xsl:otherwise>
          </xsl:choose>
        </xsl:if>
        <xsl:text>&#10;</xsl:text>
      </xsl:for-each>
    </xsl:if>
  </xsl:template>
</xsl:stylesheet>
