package com.epam.reportportal.extension.robot.model;

import com.epam.ta.reportportal.ws.reporting.ItemAttributesRQ;
import com.epam.ta.reportportal.ws.reporting.Mode;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.Set;
import lombok.Getter;

@Getter
public class LaunchImportRQ {

  @JsonProperty(value = "name")
  protected String name;

  @JsonProperty(value = "description")
  private String description;

  @JsonProperty("attributes")
  @JsonAlias({"attributes", "tags"})
  private Set<ItemAttributesRQ> attributes;

  @JsonProperty
  @JsonAlias({"startTime", "start_time"})
  private Instant startTime;

  @JsonProperty("mode")
  private Mode mode;

}
