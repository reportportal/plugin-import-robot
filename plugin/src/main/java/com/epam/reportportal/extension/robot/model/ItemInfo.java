package com.epam.reportportal.extension.robot.model;

import com.epam.ta.reportportal.entity.enums.StatusEnum;
import com.epam.ta.reportportal.entity.enums.TestItemTypeEnum;
import com.epam.ta.reportportal.ws.reporting.ItemAttributesRQ;
import java.time.Instant;
import java.util.Set;
import lombok.Data;

@Data
public class ItemInfo {

  private String uuid;
  private String name;
  private String description;
  private Instant startTime;
  private Instant endTime;
  private TestItemTypeEnum type;
  private String source;
  private StatusEnum status;
  private Set<ItemAttributesRQ> itemAttributes;
  private String testCaseId;
  private String codeReference;

}
