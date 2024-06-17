package com.epam.reportportal.extension.robot.service;

import static com.epam.reportportal.extension.robot.service.FileExtensionConstant.XML_EXTENSION;
import static com.epam.reportportal.extension.robot.service.FileExtensionConstant.ZIP_EXTENSION;

import com.epam.reportportal.rules.exception.ErrorType;
import com.epam.reportportal.rules.exception.ReportPortalException;
import com.epam.ta.reportportal.dao.LaunchRepository;
import com.google.common.collect.ImmutableMap;
import java.util.Map;
import java.util.Optional;
import org.apache.commons.io.FilenameUtils;
import org.springframework.context.ApplicationEventPublisher;

public class ImportStrategyFactory {

  private final Map<String, ImportStrategy> STRATEGY_MAPPING;

  public ImportStrategyFactory(ApplicationEventPublisher eventPublisher,
      LaunchRepository launchRepository) {
    STRATEGY_MAPPING = ImmutableMap.<String, ImportStrategy>builder()
        .put(ZIP_EXTENSION,
            new ZipImportStrategy(eventPublisher, launchRepository))
        .put(XML_EXTENSION,
            new XmlImportStrategy(eventPublisher, launchRepository)).build();
  }

  public ImportStrategy getImportStrategy(String filename) {
    final String extension = FilenameUtils.getExtension(filename);
    return Optional.ofNullable(STRATEGY_MAPPING.get(extension))
        .orElseThrow(() -> new ReportPortalException(ErrorType.BAD_REQUEST_ERROR,
            "Incorrect file extension."));
  }

}
