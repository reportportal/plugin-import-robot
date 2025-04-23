/*
 * Copyright 2019 EPAM Systems
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.epam.reportportal.extension.robot.service;

import java.util.Arrays;

public enum RobotReportTag {

  ROBOT("robot"),
  SUITE("suite"),
  TEST("test"),
  MESSAGE("msg"),
  KEYWORD("kw"),
  FOR("for"),
  ITER("iter"),
  UNSUPPORTED("unsupported"),
  DOC("doc"),
  ARG("arg"),
  TAG("tag"),
  ATTR_LIBRARY("library"),
  ATTR_NAME("name"),
  ATTR_LINE("line"),
  ATTR_SOURCE("source"),
  ATTR_LEVEL("level"),
  ATTR_HTML("html"),
  ATTR_STATUS("status"),
  ATTR_START_TIME("starttime"),
  ATTR_END_TIME("endtime"),
  ATTR_TIMESTAMP("timestamp"),
  ATTR_GENERATED("generated"),
  ATTR_TYPE("type");

  private final String value;

  RobotReportTag(String value) {
    this.value = value;
  }

  public static RobotReportTag fromString(String type) {
    return Arrays.stream(values()).filter(it -> it.val().equalsIgnoreCase(type)).findAny()
        .orElse(UNSUPPORTED);
  }

  public final String val() {
    return value;
  }
}
