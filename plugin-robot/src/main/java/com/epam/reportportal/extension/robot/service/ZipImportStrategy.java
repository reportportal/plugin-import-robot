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

import static com.epam.reportportal.extension.robot.service.FileExtensionConstant.XML_EXTENSION;
import static com.epam.reportportal.extension.robot.service.FileExtensionConstant.ZIP_EXTENSION;

import com.epam.reportportal.base.infrastructure.persistence.dao.LaunchRepository;
import com.epam.reportportal.base.infrastructure.rules.exception.ErrorType;
import com.epam.reportportal.base.infrastructure.rules.exception.ReportPortalException;
import com.epam.reportportal.extension.robot.model.LaunchImportRQ;
import com.epam.reportportal.extension.robot.utils.UncompressedSizeLimit;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.time.Instant;
import java.util.Collections;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.multipart.MultipartFile;

/**
 * @author <a href="mailto:ivan_budayeu@epam.com">Ivan Budayeu</a>
 */
@Slf4j
public class ZipImportStrategy extends AbstractImportStrategy {

  /**
   * Zip bomb protection: the upload itself is limited to 32 MB, these limits apply to its content (reports and attached
   * screenshots).
   */
  static final int MAX_ENTRIES = 10_000;
  static final long MAX_UNCOMPRESSED_BYTES = 512L * 1024 * 1024;

  private static final Predicate<ZipEntry> isFile = zipEntry -> !zipEntry.isDirectory();
  private static final Predicate<ZipEntry> isXml = zipEntry -> zipEntry.getName()
      .endsWith(XML_EXTENSION);

  private static final Predicate<ZipEntry> isNotSystemDirectory = zipEntry -> !(zipEntry.getName()
      .contains("_MACOSX") || zipEntry.getName()
      .contains(".DS_Store"));

  private final int maxEntries;
  private final long maxUncompressedBytes;

  public ZipImportStrategy(ApplicationEventPublisher eventPublisher,
      LaunchRepository launchRepository) {
    this(eventPublisher, launchRepository, MAX_ENTRIES, MAX_UNCOMPRESSED_BYTES);
  }

  ZipImportStrategy(ApplicationEventPublisher eventPublisher, LaunchRepository launchRepository,
      int maxEntries, long maxUncompressedBytes) {
    super(eventPublisher, launchRepository);
    this.maxEntries = maxEntries;
    this.maxUncompressedBytes = maxUncompressedBytes;
  }


  @Override
  public String importLaunch(MultipartFile file, String projectName, LaunchImportRQ rq) {
    var existingLaunchUuid = getExistingLaunchUuid(rq);
    boolean importIntoExistingLaunch = existingLaunchUuid.isPresent();
    String launchUuid = existingLaunchUuid.orElseGet(() -> UUID.randomUUID().toString());
    Instant existingLaunchStartTime = importIntoExistingLaunch
        ? getExistingLaunchStartTime(launchUuid) : null;
    File zip = transferToTempFile(file);
    boolean launchStarted = false;

    try (ZipFile zipFile = new ZipFile(zip)) {
      // checked before the launch is created, so a rejected archive doesn't leave an empty launch
      if (zipFile.size() > maxEntries) {
        throw new ReportPortalException(ErrorType.IMPORT_FILE_ERROR,
            "Archive contains more than " + maxEntries + " entries");
      }
      // shared by reports and attachments; counts bytes actually read, so fake sizes in zip headers
      // can't bypass it
      UncompressedSizeLimit sizeLimit = new UncompressedSizeLimit(maxUncompressedBytes);
      if (!importIntoExistingLaunch) {
        launchUuid = startLaunch(launchUuid, getLaunchName(file, ZIP_EXTENSION), projectName, rq);
        launchStarted = true;
      }
      RobotXmlParser robotXmlParser = new RobotXmlParser(eventPublisher, launchUuid,
          projectName, zipFile, sizeLimit, isSkippedNotIssue(rq), existingLaunchStartTime);
      for (ZipEntry zipEntry : Collections.list(zipFile.entries())) {
        if (!isFile.and(isXml).and(isNotSystemDirectory).test(zipEntry)) {
          continue;
        }
        try (InputStream entryStream = sizeLimit.wrap(zipFile.getInputStream(zipEntry))) {
          robotXmlParser.parse(entryStream);
        }
      }
      if (!importIntoExistingLaunch) {
        finishLaunch(launchUuid, projectName, robotXmlParser.getHighestTime());
        updateStartTime(launchUuid, robotXmlParser.getLowestTime());
      }
      return launchUuid;
    } catch (Exception e) {
      log.error("Failed to import Robot ZIP file '{}' into launch '{}' for project '{}'",
          file.getOriginalFilename(), launchUuid, projectName, e);
      if (launchStarted) {
        updateBrokenLaunch(launchUuid);
      }
      throw new ReportPortalException(ErrorType.IMPORT_FILE_ERROR, cleanMessage(e));
    } finally {
      try {
        Files.deleteIfExists(zip.getAbsoluteFile().toPath());
      } catch (IOException e) {
        log.warn("Failed to delete temporary ZIP file '{}'", zip.getAbsolutePath(), e);
      }
    }
  }

  private File transferToTempFile(MultipartFile file) {
    try {
      File tmp = File.createTempFile(file.getOriginalFilename(),
          "." + FilenameUtils.getExtension(file.getOriginalFilename())
      );
      file.transferTo(tmp);
      return tmp;
    } catch (IOException e) {
      throw new ReportPortalException("Error during transferring multipart file.", e);
    }
  }
}
