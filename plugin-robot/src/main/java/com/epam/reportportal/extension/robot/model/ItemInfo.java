package com.epam.reportportal.extension.robot.model;

import com.epam.reportportal.base.infrastructure.persistence.entity.enums.StatusEnum;
import com.epam.reportportal.base.infrastructure.persistence.entity.enums.TestItemTypeEnum;
import com.epam.reportportal.base.reporting.ItemAttributesRQ;
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
  private boolean hasStats = true;

}
