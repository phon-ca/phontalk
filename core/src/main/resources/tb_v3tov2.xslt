<?xml version="1.0" encoding="UTF-8" ?>

<!--
    Stylesheet to convert pho/mod lines from TalkBank v3 to TalkBank v2
-->
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:tb="http://www.talkbank.org/ns/talkbank"
                xmlns:xml="http://www.w3.org/XML/1998/namespace" xmlns:xs="http://www.w3.org/1999/XSL/Transform"
                exclude-result-prefixes="xml">
    <xsl:output method="xml" indent="yes" encoding="UTF-8" />
    <!-- Write everything as is if not matched -->

    <xsl:template match="@*|node()">
        <xsl:copy>
            <xsl:apply-templates select="@*|node()" />
        </xsl:copy>
    </xsl:template>

    <!-- re-write Version attribute of root element, retain all other attributes -->
    <xsl:template match="tb:CHAT">
        <xsl:element name="CHAT">
            <xsl:attribute name="Version">2.20.2</xsl:attribute>
            <xsl:apply-templates select="@*[not(name()='Version')]|node()" />
        </xsl:element>
    </xsl:template>

    <!-- If a w element contains a pho or mod element, wrap the w element in a pg element and convert pho to actual -->
    <xsl:template match="tb:w[tb:pho or tb:mod]">
        <xsl:element name="pg">
            <xsl:element name="w">
                <xsl:apply-templates select="@*|node()[not(self::tb:pho or self::tb:mod)]" />
            </xsl:element>
            <xsl:if test="tb:pho">
                <xsl:element name="actual">
                    <xsl:apply-templates select="tb:pho/node()" />
                </xsl:element>
            </xsl:if>
            <xsl:if test="tb:mod">
                <xsl:element name="model">
                    <xsl:apply-templates select="tb:mod/node()" />
                </xsl:element>
            </xsl:if>
        </xsl:element>
    </xsl:template>

    <xsl:template match="tb:pause[tb:pho or tb:mod]">
        <xsl:element name="pg">
            <xsl:element name="ph">
                <xsl:apply-templates select="@*|node()[not(self::tb:pho or self::tb:mod)]" />
            </xsl:element>
            <xsl:if test="tb:pho">
                <xsl:element name="actual">
                    <xsl:apply-templates select="tb:pho/node()" />
                </xsl:element>
            </xsl:if>
            <xsl:if test="tb:mod">
                <xsl:element name="model">
                    <xsl:apply-templates select="tb:mod/node()" />
                </xsl:element>
            </xsl:if>
        </xsl:element>
    </xsl:template>

    <!-- Match pho element and convert to actual -->
    <xsl:template match="tb:pho">
        <xsl:element name="actual">
            <xsl:apply-templates select="@*|node()" />
        </xsl:element>
    </xsl:template>
    <xsl:template match="tb:mod">
        <xsl:element name="model">
            <xsl:apply-templates select="@*|node()" />
        </xsl:element>
    </xsl:template>

    <!-- turn <ph><base>xxx</base></ph> into <ph>xxx</ph> -->
    <xsl:template match="tb:ph">
        <xsl:element name="ph">
            <xsl:attribute name="id">
                <xsl:text>ph</xsl:text>
                <xsl:number count="tb:ph" level="any" from="tb:pw" format="1" />
            </xsl:attribute>
            <xs:value-of select="concat(tb:prefix, tb:base, tb:combining, tb:phlen, tb:suffix, tb:toneNumber)" />
        </xsl:element>
    </xsl:template>

    <xsl:template match="tb:stress">
        <xsl:element name="ss">
            <xsl:choose>
                <xsl:when test="@type='primary'">
                    <xsl:text>1</xsl:text>
                </xsl:when>
                <xsl:when test="@type='secondary'">
                    <xsl:text>2</xsl:text>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:text>0</xsl:text>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:element>
    </xsl:template>

    <xsl:template match="tb:pp">
        <xsl:element name="ph">
            <xsl:attribute name="id">
                <xsl:text>pp</xsl:text>
                <xsl:number count="tb:ph" level="any" from="tb:pw" format="1" />
            </xsl:attribute>
            <xsl:choose>
                <xsl:when test="@type='syllable break'">
                    <xsl:text>.</xsl:text>
                </xsl:when>
                <xsl:when test="@type='major intonation group'">
                    <xsl:text>‖</xsl:text>
                </xsl:when>
                <xsl:when test="@type='minor intonation group'">
                    <xsl:text>|</xsl:text>
                </xsl:when>
                <xsl:when test="@type='cmp'">
                    <xsl:text>+</xsl:text>
                </xsl:when>
                <xsl:when test="@type='pause'">
                    <xsl:text>^</xsl:text>
                </xsl:when>
                <xsl:when test="@type='blocking'">
                    <xsl:text>^</xsl:text>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:text>*</xsl:text>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:element>
    </xsl:template>

    <xsl:template match="tb:sandhi">
        <xsl:element name="ph">
            <xsl:attribute name="id">
                <xsl:text>sandhi</xsl:text>
                <xsl:number count="tb:ph" level="any" from="tb:pw" format="1" />
            </xsl:attribute>
            <xsl:choose>
                <xsl:when test="@type='contraction'">
                    <xsl:text>⁀</xsl:text>
                </xsl:when>
                <xsl:when test="@type='linker'">
                    <xsl:text>‿</xsl:text>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:text>*</xsl:text>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:element>
    </xsl:template>
    
    <!-- compound phones are recursive elements, we need to combine the text of all children -->
    <xsl:template match="tb:cmph">
        <xsl:element name="ph">
            <xsl:attribute name="id">
                <xsl:text>cmph</xsl:text>
                <xsl:number count="tb:ph" level="any" from="tb:pw" format="1" />
            </xsl:attribute>
            <xsl:apply-templates />
        </xsl:element>
    </xsl:template>

    <xsl:template match="tb:cmph/tb:ph">
        <xsl:value-of select="concat(tb:prefix, tb:base, tb:combining, tb:phlen, tb:suffix, tb:toneNumber)" />
    </xsl:template>

    <xsl:template match="tb:lig">
        <xsl:choose>
            <xsl:when test="@type='breve below'">
                <xsl:text>͜</xsl:text>
            </xsl:when>
            <xsl:when test="@type='breve'">
                <xsl:text>͡</xsl:text>
            </xsl:when>
            <xsl:when test="@type='right arrow below'">
                <xsl:text>͢</xsl:text>
            </xsl:when>
        </xsl:choose>
    </xsl:template>
    
    <xsl:template match="tb:pause">
        <xsl:element name="ph">
            <xsl:attribute name="id">
                <xsl:number count="tb:ph" level="any" from="tb:pw" format="1" />
            </xsl:attribute>
            <xsl:choose>
                <xsl:when test="@symbolic-length='simple'">
                    <xsl:text>(.)</xsl:text>
                </xsl:when>
                <xsl:when test="@symbolic-length='long'">
                    <xsl:text>(..)</xsl:text>
                </xsl:when>
                <xsl:when test="@symbolic-length='very long'">
                    <xsl:text>(...)</xsl:text>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:text>(</xsl:text>
                    <xsl:value-of select="@length" />
                    <xsl:text>)</xsl:text>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:element>
    </xsl:template>

    <!-- Don't output phog elements but do output their children -->
    <xsl:template match="tb:phog">
        <xsl:apply-templates />
    </xsl:template>

</xsl:stylesheet>
