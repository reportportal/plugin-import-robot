package com.epam.reportportal.extension.robot.service;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.epam.reportportal.events.FinishLaunchRqEvent;
import com.epam.reportportal.extension.robot.model.LaunchImportRQ;
import com.epam.ta.reportportal.dao.LaunchRepository;
import com.epam.ta.reportportal.entity.launch.Launch;
import java.io.IOException;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class ZipImportStrategyTest {

  @Mock
  ApplicationEventPublisher eventPublisher;

  @Mock
  LaunchRepository launchRepository;

  @Test
  void importLaunch() throws IOException {
    when(launchRepository.findByUuid(any())).thenReturn(Optional.of(new Launch(1L)));
    ArgumentCaptor<FinishLaunchRqEvent> eventArgumentCaptor = ArgumentCaptor.forClass(FinishLaunchRqEvent.class);

    var file = new MockMultipartFile("file", "report.zip", "application/zip",
        getClass().getClassLoader().getResourceAsStream("report.zip"));

    ZipImportStrategy importStrategy = new ZipImportStrategy(eventPublisher, launchRepository);
    importStrategy.importLaunch(file, "project-name", new LaunchImportRQ());

    verify(eventPublisher, times(1)).publishEvent(eventArgumentCaptor.capture());
    assertNotEquals(Instant.EPOCH, eventArgumentCaptor.getValue().getFinishExecutionRQ().getEndTime());
  }
}
