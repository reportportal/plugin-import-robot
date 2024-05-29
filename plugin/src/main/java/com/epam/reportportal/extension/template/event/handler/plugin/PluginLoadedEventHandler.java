package com.epam.reportportal.extension.template.event.handler.plugin;

import com.epam.reportportal.extension.event.PluginEvent;
import com.epam.reportportal.extension.template.TemplatePluginExtension;
import com.epam.reportportal.extension.template.event.handler.EventHandler;
import com.epam.reportportal.rules.exception.ErrorType;
import com.epam.reportportal.rules.exception.ReportPortalException;
import com.epam.ta.reportportal.dao.IntegrationRepository;
import com.epam.ta.reportportal.dao.IntegrationTypeRepository;
import com.epam.ta.reportportal.entity.integration.Integration;
import com.epam.ta.reportportal.entity.integration.IntegrationParams;
import com.epam.ta.reportportal.entity.integration.IntegrationType;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static java.util.Optional.ofNullable;

/**
 * @author Andrei Piankouski
 */
public class PluginLoadedEventHandler implements EventHandler<PluginEvent> {

	private static final String BINARY_DATA = "binaryData";

	private final String resourcesDir;
	private final IntegrationTypeRepository integrationTypeRepository;
	private final IntegrationRepository integrationRepository;

	public PluginLoadedEventHandler(String resourcesDir, IntegrationTypeRepository integrationTypeRepository,
			IntegrationRepository integrationRepository) {
		this.resourcesDir = resourcesDir;
		this.integrationTypeRepository = integrationTypeRepository;
		this.integrationRepository = integrationRepository;
	}

	@Override
	public void handle(PluginEvent event) {
		integrationTypeRepository.findByName(event.getPluginId()).ifPresent(integrationType -> {
			createIntegration(event.getPluginId(), integrationType);
			loadBinaryDataInfo(integrationType);
		});
	}

	private void createIntegration(String name, IntegrationType integrationType) {
		List<Integration> integrations = integrationRepository.findAllGlobalByType(integrationType);
		if (integrations.isEmpty()) {
			Integration integration = new Integration();
			integration.setName(name);
			integration.setType(integrationType);
			integration.setCreationDate(LocalDateTime.now());
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
					TemplatePluginExtension.BINARY_DATA_PROPERTIES_FILE_ID
			))) {
				Properties binaryDataProperties = new Properties();
				binaryDataProperties.load(propertiesStream);
				Map<String, String> binaryDataInfo = binaryDataProperties.entrySet()
						.stream()
						.collect(HashMap::new,
								(map, entry) -> map.put(String.valueOf(entry.getKey()), String.valueOf(entry.getValue())),
								HashMap::putAll
						);
				details.put(BINARY_DATA, binaryDataInfo);
				integrationTypeRepository.save(integrationType);
			} catch (IOException ex) {
				throw new ReportPortalException(ErrorType.UNABLE_TO_LOAD_BINARY_DATA, ex.getMessage());
			}
		}
	}
}
