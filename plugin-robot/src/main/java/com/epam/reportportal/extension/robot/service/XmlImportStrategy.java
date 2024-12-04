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

import com.epam.reportportal.extension.robot.model.LaunchImportRQ;
import com.epam.reportportal.rules.exception.ErrorType;
import com.epam.reportportal.rules.exception.ReportPortalException;
import com.epam.ta.reportportal.dao.LaunchRepository;
import java.io.InputStream;
import java.util.Optional;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.multipart.MultipartFile;

/**
 * @author <a href="mailto:ivan_budayeu@epam.com">Ivan Budayeu</a>
 */
public class XmlImportStrategy extends AbstractImportStrategy {

  public XmlImportStrategy(ApplicationEventPublisher eventPublisher,
      LaunchRepository launchRepository) {
    super(eventPublisher, launchRepository);
  }

  @Override
  public String importLaunch(MultipartFile file, String projectName, LaunchImportRQ rq) {
    String launchUuid = null;
    try (InputStream xmlStream = file.getInputStream()) {
      launchUuid = startLaunch(getLaunchName(file, XML_EXTENSION), projectName, rq);
      RobotXmlParser robotXmlParser = new RobotXmlParser(eventPublisher, launchUuid,
          projectName, isSkippedNotIssue(rq.getAttributes()));
      robotXmlParser.parse(xmlStream);
      finishLaunch(launchUuid, projectName, robotXmlParser.getHighestTime());
      updateStartTime(launchUuid, robotXmlParser.getLowestTime());
      return launchUuid;
    } catch (Exception e) {
      e.printStackTrace();
      Optional.ofNullable(launchUuid).ifPresent(this::updateBrokenLaunch);
      throw new ReportPortalException(ErrorType.IMPORT_FILE_ERROR, cleanMessage(e));
    }
  }
}
