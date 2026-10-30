<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
  <xsl:output method="html" encoding="UTF-8" indent="yes"/>
  <xsl:template match="/">
    <html lang="ru">
      <head><title>Семейные связи</title></head>
      <body>
        <h1>Семейные связи</h1>
        <xsl:variable name="selected" select="people/person[parents/father and parents/mother and siblings/* and id(parents/*/@ref)/parents/*][position() = 1]"/>
        <xsl:choose>
          <xsl:when test="$selected">
            <p>Выбранный человек: <xsl:for-each select="$selected"><xsl:call-template name="name"/></xsl:for-each></p>
            <xsl:apply-templates select="$selected" mode="card"/>
            <xsl:apply-templates select="id($selected/parents/*/@ref)" mode="card"/>
            <xsl:apply-templates select="id($selected/siblings/*/@ref)" mode="card"/>
          </xsl:when>
          <xsl:otherwise><p>Нет человека с обоими родителями, предком второго поколения и братьями или сёстрами.</p></xsl:otherwise>
        </xsl:choose>
      </body>
    </html>
  </xsl:template>
  <xsl:template match="person" mode="card">
    <section>
      <h2><xsl:call-template name="name"/> (<xsl:value-of select="@id"/>)</h2>
      <p>Пол: <xsl:choose><xsl:when test="gender = 'male'">мужской</xsl:when><xsl:when test="gender = 'female'">женский</xsl:when><xsl:otherwise>неизвестен</xsl:otherwise></xsl:choose></p>
      <dl>
        <dt>Отец</dt><dd><xsl:call-template name="names"><xsl:with-param name="persons" select="id(parents/father/@ref)"/></xsl:call-template></dd>
        <dt>Мать</dt><dd><xsl:call-template name="names"><xsl:with-param name="persons" select="id(parents/mother/@ref)"/></xsl:call-template></dd>
        <dt>Братья</dt><dd><xsl:call-template name="names"><xsl:with-param name="persons" select="id(siblings/brother/@ref)"/></xsl:call-template></dd>
        <dt>Сёстры</dt><dd><xsl:call-template name="names"><xsl:with-param name="persons" select="id(siblings/sister/@ref)"/></xsl:call-template></dd>
        <dt>Сыновья</dt><dd><xsl:call-template name="names"><xsl:with-param name="persons" select="id(children/son/@ref)"/></xsl:call-template></dd>
        <dt>Дочери</dt><dd><xsl:call-template name="names"><xsl:with-param name="persons" select="id(children/daughter/@ref)"/></xsl:call-template></dd>
        <dt>Бабушки</dt><dd><xsl:call-template name="names"><xsl:with-param name="persons" select="id(id(parents/*/@ref)/parents/*/@ref)[gender = 'female']"/></xsl:call-template></dd>
        <dt>Дедушки</dt><dd><xsl:call-template name="names"><xsl:with-param name="persons" select="id(id(parents/*/@ref)/parents/*/@ref)[gender = 'male']"/></xsl:call-template></dd>
        <dt>Дяди</dt><dd><xsl:call-template name="names"><xsl:with-param name="persons" select="id(id(parents/*/@ref)/siblings/*/@ref)[gender = 'male']"/></xsl:call-template></dd>
        <dt>Тёти</dt><dd><xsl:call-template name="names"><xsl:with-param name="persons" select="id(id(parents/*/@ref)/siblings/*/@ref)[gender = 'female']"/></xsl:call-template></dd>
      </dl>
    </section>
  </xsl:template>
  <xsl:template name="name">
    <xsl:value-of select="first-name"/><xsl:text> </xsl:text><xsl:value-of select="last-name"/>
  </xsl:template>
  <xsl:template name="names">
    <xsl:param name="persons"/>
    <xsl:choose>
      <xsl:when test="$persons">
        <xsl:for-each select="$persons">
          <xsl:if test="position() &gt; 1"><xsl:text>, </xsl:text></xsl:if>
          <xsl:call-template name="name"/>
        </xsl:for-each>
      </xsl:when>
      <xsl:otherwise>нет сведений</xsl:otherwise>
    </xsl:choose>
  </xsl:template>
</xsl:stylesheet>
