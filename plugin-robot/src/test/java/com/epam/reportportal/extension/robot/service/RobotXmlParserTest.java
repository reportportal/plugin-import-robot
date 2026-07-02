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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

import com.epam.reportportal.base.infrastructure.events.StartChildItemRqEvent;
import com.epam.reportportal.base.reporting.ItemAttributesRQ;
import com.epam.reportportal.base.reporting.StartTestItemRQ;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class RobotXmlParserTest {

  @Mock
  ApplicationEventPublisher eventPublisher;

  private List<StartTestItemRQ> captureStartItemEvents() {
    List<StartTestItemRQ> captured = new ArrayList<>();
    doAnswer(invocation -> {
      Object event = invocation.getArgument(0);
      if (event instanceof StartChildItemRqEvent e) {
        captured.add(e.getStartTestItemRq());
      }
      return null;
    }).when(eventPublisher).publishEvent(any());
    return captured;
  }

  private InputStream xml(String resourceName) {
    return getClass().getClassLoader().getResourceAsStream(resourceName);
  }

  @Test
  void keyValueTagsAreParsedAsKeyValueAttributes() throws IOException {
    List<StartTestItemRQ> started = captureStartItemEvents();

    RobotXmlParser parser = new RobotXmlParser(eventPublisher, "launch-uuid", "project", false);
    parser.parse(xml("report_with_key_value_tags.xml"));

    StartTestItemRQ testItem = started.stream()
        .filter(rq -> "Login Test".equals(rq.getName()))
        .findFirst()
        .orElseThrow(() -> new AssertionError("Test item not found"));

    Set<ItemAttributesRQ> attributes = testItem.getAttributes();
    assertEquals(2, attributes.size());

    Map<String, String> attrMap = attributes.stream()
        .collect(Collectors.toMap(ItemAttributesRQ::getKey, ItemAttributesRQ::getValue, (a, b) -> a));

    assertEquals("team-trust", attrMap.get("scenario_owner"));
    assertEquals("authentication", attrMap.get("component"));
  }

  @Test
  void plainTagsAreBackwardCompatible() throws IOException {
    List<StartTestItemRQ> started = captureStartItemEvents();

    RobotXmlParser parser = new RobotXmlParser(eventPublisher, "launch-uuid", "project", false);
    parser.parse(xml("report_with_plain_tags.xml"));

    StartTestItemRQ testItem = started.stream()
        .filter(rq -> "Login Test".equals(rq.getName()))
        .findFirst()
        .orElseThrow(() -> new AssertionError("Test item not found"));

    Set<ItemAttributesRQ> attributes = testItem.getAttributes();
    assertEquals(2, attributes.size());

    // plain tags have no key — value is the full text content
    attributes.forEach(attr -> assertNull(attr.getKey()));

    Set<String> values = attributes.stream()
        .map(ItemAttributesRQ::getValue)
        .collect(Collectors.toSet());
    assertEquals(Set.of("scenario_owner:team-trust", "component:authentication"), values);
  }
}
