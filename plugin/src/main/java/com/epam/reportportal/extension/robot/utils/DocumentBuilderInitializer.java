package com.epam.reportportal.extension.robot.utils;

import com.epam.reportportal.rules.exception.ErrorType;
import com.epam.reportportal.rules.exception.ReportPortalException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

public class DocumentBuilderInitializer {

  public static DocumentBuilder get() {
    try {
      DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
      // Xerces 1 - http://xerces.apache.org/xerces-j/features.html#external-general-entities
      // Xerces 2 - http://xerces.apache.org/xerces2-j/features.html#external-general-entities
      // Xerces 2 only - http://xerces.apache.org/xerces-j/features.html#external-general-entities
      dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
      dbf.setFeature("http://xml.org/sax/features/external-general-entities", false);
      dbf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
      dbf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
      dbf.setXIncludeAware(false);
      return dbf.newDocumentBuilder();
    } catch (ParserConfigurationException e) {
      throw new ReportPortalException(ErrorType.PARSING_XML_ERROR, e.getMessage());
    }
  }

}
