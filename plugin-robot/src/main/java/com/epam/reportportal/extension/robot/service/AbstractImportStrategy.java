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

import static java.util.Optional.ofNullable;

import com.epam.reportportal.events.FinishLaunchRqEvent;
import com.epam.reportportal.events.StartLaunchRqEvent;
import com.epam.reportportal.rules.exception.ErrorType;
import com.epam.reportportal.rules.exception.ReportPortalException;
import com.epam.reportportal.extension.robot.model.LaunchImportRQ;
import com.epam.ta.reportportal.dao.LaunchRepository;
import com.epam.ta.reportportal.entity.enums.StatusEnum;
import com.epam.ta.reportportal.entity.launch.Launch;
import com.epam.ta.reportportal.ws.reporting.FinishExecutionRQ;
import com.epam.ta.reportportal.ws.reporting.ItemAttributesRQ;
import com.epam.ta.reportportal.ws.reporting.Mode;
import com.epam.ta.reportportal.ws.reporting.StartLaunchRQ;
import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.multipart.MultipartFile;

/**
 * @author <a href="mailto:ivan_budayeu@epam.com">Ivan Budayeu</a>
 */
public abstract class AbstractImportStrategy implements ImportStrategy {

  public static final String SKIPPED_IS_NOT_ISSUE = "skippedIsNotIssue";

  protected final ApplicationEventPublisher eventPublisher;

  protected final LaunchRepository launchRepository;


  public AbstractImportStrategy(ApplicationEventPublisher eventPublisher,
      LaunchRepository launchRepository) {
    this.eventPublisher = eventPublisher;
    this.launchRepository = launchRepository;
  }

  /**
   * Got a cause exception message if it has any.
   *
   * @param e Exception
   * @return Clean exception message
   */
  public static String cleanMessage(Exception e) {
    if (e.getCause() != null) {
      return e.getCause().getMessage();
    }
    return e.getMessage();
  }

  protected String startLaunch(String launchUuid, String launchName, String projectName,
      LaunchImportRQ rq) {
    if (rq == null) {
      rq = new LaunchImportRQ();
    }
    StartLaunchRQ startLaunchRQ = new StartLaunchRQ();
    startLaunchRQ.setUuid(launchUuid);
    startLaunchRQ.setStartTime(ofNullable(rq.getStartTime()).orElse(Instant.EPOCH));
    startLaunchRQ.setName(ofNullable(rq.getName()).orElse(launchName));
    ofNullable(rq.getDescription()).ifPresent(startLaunchRQ::setDescription);
    startLaunchRQ.setMode(ofNullable(rq.getMode()).orElse(Mode.DEFAULT));
    startLaunchRQ.setAttributes(ofNullable(rq.getAttributes()).orElse(new HashSet<>()));
    eventPublisher.publishEvent(new StartLaunchRqEvent(this, projectName, startLaunchRQ));
    return launchUuid;
  }

  protected void finishLaunch(String launchUuid, String projectName, Instant time) {
    FinishExecutionRQ finishExecutionRQ = new FinishExecutionRQ();
    var endTime = time.equals(Instant.EPOCH) ? Instant.now() : time;
    finishExecutionRQ.setEndTime(endTime);
    eventPublisher.publishEvent(
        new FinishLaunchRqEvent(this, projectName, launchUuid, finishExecutionRQ));
  }

  protected Boolean isSkippedNotIssue(Set<ItemAttributesRQ> attributes) {
    return ofNullable(attributes).orElse(Collections.emptySet()).stream().filter(
            attribute -> SKIPPED_IS_NOT_ISSUE.equals(attribute.getKey()) && attribute.isSystem())
        .findAny().filter(itemAttributesRQ -> Boolean.parseBoolean(itemAttributesRQ.getValue()))
        .isPresent();
  }

  /*
   * if the importing results do not contain initial timestamp a launch gets
   * a default date if the launch is broken, time should be updated to not to broke
   * the statistics
   */
  protected void updateBrokenLaunch(String launchUuid) {
    Launch launch = launchRepository.findByUuid(launchUuid)
        .orElseThrow(() -> new ReportPortalException(ErrorType.LAUNCH_NOT_FOUND, launchUuid));
    launch.setStartTime(Instant.now());
    launch.setStatus(StatusEnum.INTERRUPTED);
    launchRepository.save(launch);
  }


  protected void updateStartTime(String launchUuid, Instant startTime) {
    Launch launch = launchRepository.findByUuid(launchUuid)
        .orElseThrow(() -> new ReportPortalException(ErrorType.LAUNCH_NOT_FOUND, launchUuid));
    launch.setStartTime(startTime);
    launchRepository.save(launch);
  }

  protected String getLaunchName(MultipartFile file, String extension) {
    return file.getOriginalFilename()
        .substring(0, file.getOriginalFilename().indexOf("." + extension));
  }

  public Optional<Launch> getLaunch(String launchUuid) {
    int maxRetries = 3;
    int attempt = 0;
    while (attempt < maxRetries) {
      attempt++;
      var launch = launchRepository.findByUuid(launchUuid);
      if (launch.isEmpty()) {
        try {
          Thread.sleep(1000);
        } catch (InterruptedException ie) {
          Thread.currentThread().interrupt();
          throw new RuntimeException("Retry interrupted", ie);
        }
      } else {
        return launch;
      }
    }
    return Optional.empty();
  }
}
