package com.epam.reportportal.extension.robot;

import static com.epam.reportportal.extension.robot.command.RobotImportCommand.MAX_FILE_SIZE;
import static com.epam.reportportal.extension.util.CommonConstants.DESCRIPTION_KEY;
import static com.epam.reportportal.extension.util.CommonConstants.IS_INTEGRATIONS_ALLOWED;
import static com.epam.reportportal.extension.util.CommonConstants.METADATA;

import com.epam.reportportal.base.core.events.domain.PluginUploadedEvent;
import com.epam.reportportal.base.infrastructure.persistence.dao.IntegrationRepository;
import com.epam.reportportal.base.infrastructure.persistence.dao.IntegrationTypeRepository;
import com.epam.reportportal.base.infrastructure.persistence.dao.LaunchRepository;
import com.epam.reportportal.base.infrastructure.persistence.dao.ProjectRepository;
import com.epam.reportportal.base.infrastructure.persistence.dao.ProjectUserRepository;
import com.epam.reportportal.base.infrastructure.persistence.dao.organization.OrganizationRepository;
import com.epam.reportportal.base.infrastructure.persistence.dao.organization.OrganizationUserRepository;
import com.epam.reportportal.extension.CommonPluginCommand;
import com.epam.reportportal.extension.IntegrationGroupEnum;
import com.epam.reportportal.extension.PluginCommand;
import com.epam.reportportal.extension.ReportPortalExtensionPoint;
import com.epam.reportportal.extension.command.ExtensionCommand;
import com.epam.reportportal.extension.common.IntegrationTypeProperties;
import com.epam.reportportal.extension.robot.command.RobotImportCommand;
import com.epam.reportportal.extension.robot.event.plugin.PluginLoadedEventHandler;
import com.epam.reportportal.extension.robot.utils.MemoizingSupplier;
import com.epam.reportportal.extension.util.RequestEntityConverter;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import org.pf4j.Extension;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.context.support.AbstractApplicationContext;

/**
 * @author Andrei Piankouski
 */
@Extension
public class RobotFrameworkPluginExtension implements ReportPortalExtensionPoint, DisposableBean {

  public static final String BINARY_DATA_PROPERTIES_FILE_ID = "binary-data.properties";
  private static final String PLUGIN_ID = "RobotFramework";
  private static final String DESCRIPTION = "Reinforce your ReportPortal instance with RobotFramework Import functionality and easily upload your log files right to ReportPortal.";

  private static final String NAME_FIELD = "name";

  private static final String PLUGIN_NAME = "Robot Framework";

  private final Supplier<Map<String, ExtensionCommand<?>>> pluginCommandMapping =
      new MemoizingSupplier<>(this::getIntegrationExtensionCommands);
  private final Supplier<Map<String, ExtensionCommand<?>>> commonPluginCommandMapping =
      new MemoizingSupplier<>(this::getCommonExtensionCommands);

  private final String resourcesDir;
  private final Supplier<ApplicationListener<PluginUploadedEvent>> pluginLoadedListenerSupplier;
  private final Supplier<RequestEntityConverter> requestEntityConverterSupplier = new MemoizingSupplier<>(
      this::createRequestEntityConverter);
  @Autowired
  private IntegrationTypeRepository integrationTypeRepository;
  @Autowired
  private IntegrationRepository integrationRepository;
  @Autowired
  private LaunchRepository launchRepository;
  @Autowired
  private ApplicationEventPublisher eventPublisher;
  @Autowired
  private ApplicationContext applicationContext;
  @Autowired
  private ProjectRepository projectRepository;
  @Autowired
  private OrganizationUserRepository organizationUserRepository;
  @Autowired
  private OrganizationRepository organizationRepository;
  @Autowired
  private ProjectUserRepository projectUserRepository;
  @Autowired
  private ObjectMapper objectMapper;

  public RobotFrameworkPluginExtension(Map<String, Object> initParams) {
    resourcesDir = IntegrationTypeProperties.RESOURCES_DIRECTORY.getValue(initParams)
        .map(String::valueOf)
        .orElse("");

    pluginLoadedListenerSupplier = new MemoizingSupplier<>(
        () -> new PluginLoadedEventHandler(PLUGIN_ID, resourcesDir, integrationTypeRepository,
            integrationRepository));
  }

  private RequestEntityConverter createRequestEntityConverter() {
    return new RequestEntityConverter(objectMapper);
  }

  @PostConstruct
  public void createIntegration() {
    initListeners();
  }

  private void initListeners() {
    ApplicationEventMulticaster applicationEventMulticaster = applicationContext.getBean(
        AbstractApplicationContext.APPLICATION_EVENT_MULTICASTER_BEAN_NAME,
        ApplicationEventMulticaster.class
    );
    applicationEventMulticaster.addApplicationListener(pluginLoadedListenerSupplier.get());
  }

  @Override
  public void destroy() {
    removeListeners();
  }

  private void removeListeners() {
    ApplicationEventMulticaster applicationEventMulticaster = applicationContext.getBean(
        AbstractApplicationContext.APPLICATION_EVENT_MULTICASTER_BEAN_NAME,
        ApplicationEventMulticaster.class
    );
    applicationEventMulticaster.removeApplicationListener(pluginLoadedListenerSupplier.get());
  }

  @Override
  public Map<String, ?> getPluginParams() {
    Map<String, Object> params = new HashMap<>();
    params.put(NAME_FIELD, PLUGIN_NAME);
    params.put(ALLOWED_COMMANDS, new ArrayList<>(pluginCommandMapping.get().keySet()));
    params.put(COMMON_COMMANDS, new ArrayList<>(commonPluginCommandMapping.get().keySet()));
    params.put(DESCRIPTION_KEY, DESCRIPTION);
    params.put(METADATA, Map.of(IS_INTEGRATIONS_ALLOWED, false));
    params.put("maxFileSize", MAX_FILE_SIZE);
    params.put("acceptFileMimeTypes",
        List.of("application/zip", "application/x-zip-compressed", "application/zip-compressed",
            "application/xml", "text/xml"));
    return params;
  }

  @Override
  public CommonPluginCommand getCommonCommand(String commandName) {
    return null;
  }

  @Override
  public PluginCommand getIntegrationCommand(String commandName) {
    return null;
  }

  @Override
  public IntegrationGroupEnum getIntegrationGroup() {
    return IntegrationGroupEnum.IMPORT;
  }

  @Override
  public Map<String, ExtensionCommand<?>> getCommonExtensionCommands() {
    HashMap<String, ExtensionCommand<?>> pluginCommands = new HashMap<>();
    var robotImportCommand = new RobotImportCommand(requestEntityConverterSupplier.get(),
        eventPublisher, launchRepository, projectRepository, organizationUserRepository,
        organizationRepository, projectUserRepository);
    pluginCommands.put(robotImportCommand.getName(), robotImportCommand);
    return pluginCommands;
  }
}
