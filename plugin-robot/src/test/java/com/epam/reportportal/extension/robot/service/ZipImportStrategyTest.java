package com.epam.reportportal.extension.robot.service;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.epam.reportportal.events.FinishLaunchRqEvent;
import com.epam.reportportal.events.StartLaunchRqEvent;
import com.epam.reportportal.extension.robot.model.LaunchImportRQ;
import com.epam.ta.reportportal.dao.LaunchRepository;
import com.epam.ta.reportportal.entity.launch.Launch;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ZipImportStrategyTest {

  private static final String PROJECT_NAME = "project-name";
  private static final String EXISTING_LAUNCH_UUID = "existing-launch-uuid";
  private static final String ROBOT_REPORT =
      "<robot generator=\"Swift XML generator\" generated=\"20240101 10:00:00.000\">"
          + "<suite id=\"s1\" name=\"Acceptance Tests\" source=\"/AcceptanceTests.swift\">"
          + "<test id=\"s1-t1\" name=\"Login Test\" line=\"10\">"
          + "<status status=\"PASS\" start=\"2024-01-01T10:00:00\" elapsed=\"1.5\"/>"
          + "</test>"
          + "<status status=\"PASS\" start=\"2024-01-01T10:00:00\" elapsed=\"1.5\"/>"
          + "</suite>"
          + "</robot>";

  @Mock
  ApplicationEventPublisher eventPublisher;

  @Mock
  LaunchRepository launchRepository;

  @Test
  void importLaunchStartsAndFinishesNewLaunch() throws IOException {
    when(launchRepository.findByUuid(any())).thenReturn(Optional.of(new Launch(1L)));

    ZipImportStrategy importStrategy = new ZipImportStrategy(eventPublisher, launchRepository);
    importStrategy.importLaunch(zipFile(), PROJECT_NAME, new LaunchImportRQ());

    FinishLaunchRqEvent finishLaunchRqEvent = publishedEvents().stream()
        .filter(FinishLaunchRqEvent.class::isInstance)
        .map(FinishLaunchRqEvent.class::cast)
        .findFirst()
        .orElseThrow();

    verify(launchRepository, times(1)).save(any());
    assertNotEquals(Instant.EPOCH, finishLaunchRqEvent.getFinishExecutionRQ().getEndTime());
  }

  @Test
  void importLaunchIntoExistingLaunchDoesNotUpdateLaunch() throws IOException {
    when(launchRepository.findByUuid(EXISTING_LAUNCH_UUID)).thenReturn(
        Optional.of(existingLaunch()));
    ZipImportStrategy importStrategy = new ZipImportStrategy(eventPublisher, launchRepository);

    String launchUuid = importStrategy.importLaunch(zipFile(), PROJECT_NAME, existingLaunchRq());

    assertEquals(EXISTING_LAUNCH_UUID, launchUuid);
    assertFalse(publishedEvents().stream().anyMatch(StartLaunchRqEvent.class::isInstance));
    assertFalse(publishedEvents().stream().anyMatch(FinishLaunchRqEvent.class::isInstance));
    verify(launchRepository, times(1)).findByUuid(EXISTING_LAUNCH_UUID);
    verify(launchRepository, never()).save(any());
  }

  private MockMultipartFile zipFile() throws IOException {
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    try (ZipOutputStream zipOutputStream = new ZipOutputStream(output)) {
      zipOutputStream.putNextEntry(new ZipEntry("report.xml"));
      zipOutputStream.write(ROBOT_REPORT.getBytes(UTF_8));
      zipOutputStream.closeEntry();
    }
    return new MockMultipartFile("file", "report.zip", "application/zip", output.toByteArray());
  }

  private LaunchImportRQ existingLaunchRq() {
    LaunchImportRQ rq = new LaunchImportRQ();
    ReflectionTestUtils.setField(rq, "launchUuid", EXISTING_LAUNCH_UUID);
    return rq;
  }

  private Launch existingLaunch() {
    Launch launch = new Launch(1L);
    launch.setStartTime(Instant.EPOCH);
    return launch;
  }

  private List<Object> publishedEvents() {
    return mockingDetails(eventPublisher).getInvocations().stream()
        .filter(invocation -> "publishEvent".equals(invocation.getMethod().getName()))
        .map(invocation -> invocation.getArgument(0))
        .collect(Collectors.toList());
  }
}
