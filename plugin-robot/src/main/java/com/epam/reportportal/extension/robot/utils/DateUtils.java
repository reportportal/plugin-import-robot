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

import com.epam.reportportal.rules.exception.ReportPortalException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;
import org.springframework.util.StringUtils;

/**
 * @author Pavel Bortnik
 */
public final class DateUtils {

  private static final List<DateTimeFormatter> ROBOT_DATE_FORMATTERS = Arrays.asList(
      DateTimeFormatter.ofPattern("yyyyMMdd HH:mm:ss.SSS"),
      DateTimeFormatter.ISO_LOCAL_DATE_TIME
  );

  private DateUtils() {
    //static only
  }

  public static Instant parseDateAttribute(String attribute) {
    if (StringUtils.hasText(attribute)) {
      for (DateTimeFormatter formatter : ROBOT_DATE_FORMATTERS) {
        try {
          return LocalDateTime.parse(attribute, formatter).toInstant(ZoneOffset.UTC);
        } catch (DateTimeParseException ignored) {
        }
      }
    }
    return Instant.MIN;
  }

}
