package com.epam.reportportal.extension.robot.utils;

import com.epam.ta.reportportal.entity.enums.LogLevel;
import com.epam.ta.reportportal.entity.enums.StatusEnum;

public interface RobotMapper {

  static StatusEnum mapStatus(String robotStatus) {
    switch (robotStatus) {
      case "PASS":
        return StatusEnum.PASSED;
      case "FAIL":
        return StatusEnum.FAILED;
      case "NOT RUN":
      case "SKIP":
        return StatusEnum.SKIPPED;
      default:
        return StatusEnum.INFO;
    }
  }

  static LogLevel mapLogLevel(String logLevel) {
    switch (logLevel) {
      case "INFO":
      case "SKIP":
      case "HTML":
        return LogLevel.INFO;
      case "FAIL":
      case "ERROR":
        return LogLevel.ERROR;
      case "TRACE":
        return LogLevel.TRACE;
      case "DEBUG":
        return LogLevel.DEBUG;
      case "WARN":
        return LogLevel.WARN;
      default:
        return LogLevel.INFO;
    }
  }
}
