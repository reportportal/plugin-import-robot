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
package com.epam.reportportal.extension.robot.utils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import org.springframework.util.StringUtils;

/**
 * @author Pavel Bortnik
 */
public final class DateUtils {

  private final static DateTimeFormatter ROBOT_TIME_FORMATTER = DateTimeFormatter.ofPattern(
      "yyyyMMdd HH:mm:ss.SSS");

  private DateUtils() {
    //static only
  }

  /**
   * Converts string representation of seconds to millis
   *
   * @param duration String seconds
   * @return long millis
   */
  public static long toMillis(String duration) {
    if (null != duration) {
      Double value = Double.valueOf(duration) * 1000;
      return value.longValue();
    }
    return 0;
  }

  public static Instant parseDateAttribute(String attribute) {
    if (StringUtils.hasText(attribute)) {
      return LocalDateTime.parse(attribute, ROBOT_TIME_FORMATTER).toInstant(ZoneOffset.UTC);
    }
    return Instant.MIN;
  }

}
