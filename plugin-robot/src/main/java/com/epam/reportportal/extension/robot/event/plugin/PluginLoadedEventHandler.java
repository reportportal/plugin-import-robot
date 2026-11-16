package com.epam.reportportal.extension.robot.event.plugin;

import static java.util.Optional.ofNullable;

import com.epam.reportportal.base.core.events.domain.PluginUploadedEvent;
import com.epam.reportportal.extension.robot.RobotFrameworkPluginExtension;
import com.epam.reportportal.base.infrastructure.persistence.dao.IntegrationRepository;
import com.epam.reportportal.base.infrastructure.persistence.dao.IntegrationTypeRepository;
import com.epam.reportportal.base.infrastructure.persistence.entity.integration.Integration;
import com.epam.reportportal.base.infrastructure.persistence.entity.integration.IntegrationParams;
import com.epam.reportportal.base.infrastructure.persistence.entity.integration.IntegrationType;
import com.epam.reportportal.base.infrastructure.rules.exception.ErrorType;
import com.epam.reportportal.base.infrastructure.rules.exception.ReportPortalException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.springframework.context.ApplicationListener;

/**
 * @author Andrei Piankouski
 */
public class PluginLoadedEventHandler implements ApplicationListener<PluginUploadedEvent> {

  private static final String BINARY_DATA = "binaryData";

  private final String pluginId;
  private final String resourcesDir;
  private final IntegrationTypeRepository integrationTypeRepository;
  private final IntegrationRepository integrationRepository;

  public PluginLoadedEventHandler(String pluginId, String resourcesDir,
      IntegrationTypeRepository integrationTypeRepository,
      IntegrationRepository integrationRepository) {
    this.pluginId = pluginId;
    this.resourcesDir = resourcesDir;
    this.integrationTypeRepository = integrationTypeRepository;
    this.integrationRepository = integrationRepository;
  }

  @Override
  public void onApplicationEvent(PluginUploadedEvent event) {
    if (!supports(event)) {
      return;
    }

    String eventPluginId = event.getPluginActivityResource().getName();
    integrationTypeRepository.findByName(eventPluginId).ifPresent(integrationType -> {
      createIntegration(eventPluginId, integrationType);
      loadBinaryDataInfo(integrationType);
    });
  }

  private boolean supports(PluginUploadedEvent event) {
    return pluginId.equals(event.getPluginActivityResource().getName());
  }

  private void createIntegration(String name, IntegrationType integrationType) {
    List<Integration> integrations = integrationRepository.findAllGlobalByType(integrationType);
    if (integrations.isEmpty()) {
      Integration integration = new Integration();
      integration.setName(name);
      integration.setType(integrationType);
      integration.setCreationDate(Instant.now());
      integration.setEnabled(true);
      integration.setCreator("SYSTEM");
      integration.setParams(new IntegrationParams(new HashMap<>()));
      integrationRepository.save(integration);
    }
  }

  private void loadBinaryDataInfo(IntegrationType integrationType) {
    Map<String, Object> details = integrationType.getDetails().getDetails();
    if (ofNullable(details.get(BINARY_DATA)).isEmpty()) {
      try (InputStream propertiesStream = Files.newInputStream(Paths.get(resourcesDir,
          RobotFrameworkPluginExtension.BINARY_DATA_PROPERTIES_FILE_ID
      ))) {
        Properties binaryDataProperties = new Properties();
        binaryDataProperties.load(propertiesStream);
        Map<String, String> binaryDataInfo = binaryDataProperties.entrySet()
            .stream()
            .collect(HashMap::new,
                (map, entry) -> map.put(String.valueOf(entry.getKey()),
                    String.valueOf(entry.getValue())),
                HashMap::putAll
            );
        details.put(BINARY_DATA, binaryDataInfo);
        integrationTypeRepository.save(integrationType);
      } catch (IOException ex) {
        throw new ReportPortalException(ErrorType.UNABLE_TO_LOAD_BINARY_DATA, ex);
      }
    }
  }
}
