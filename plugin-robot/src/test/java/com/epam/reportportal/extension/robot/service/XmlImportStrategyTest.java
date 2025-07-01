/*
 * Copyright 2025 EPAM Systems
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.epam.reportportal.extension.robot.model.LaunchImportRQ;
import com.epam.ta.reportportal.dao.LaunchRepository;
import com.epam.ta.reportportal.entity.launch.Launch;
import java.io.IOException;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class XmlImportStrategyTest {


  @Mock
  ApplicationEventPublisher eventPublisher;

  @Mock
  LaunchRepository launchRepository;

  @Test
  void importZeroEmptyXml() throws IOException {
    when(launchRepository.findByUuid(any())).thenReturn(Optional.of(new Launch(1L)));

    var file = new MockMultipartFile("file", "empty.xml", "text/xml",
        getClass().getClassLoader().getResourceAsStream("empty.xml"));

    XmlImportStrategy importStrategy = new XmlImportStrategy(eventPublisher, launchRepository);

    importStrategy.importLaunch(file, "project-name", new LaunchImportRQ());
  }
}

