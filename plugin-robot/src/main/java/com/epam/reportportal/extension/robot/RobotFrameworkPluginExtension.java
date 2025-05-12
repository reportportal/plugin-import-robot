package com.epam.reportportal.extension.robot;

import static com.epam.reportportal.extension.robot.command.RobotImportCommand.MAX_FILE_SIZE;
import static com.epam.reportportal.extension.util.CommonConstants.DESCRIPTION_KEY;
import static com.epam.reportportal.extension.util.CommonConstants.IS_INTEGRATIONS_ALLOWED;
import static com.epam.reportportal.extension.util.CommonConstants.METADATA;

import com.epam.reportportal.extension.CommonPluginCommand;
import com.epam.reportportal.extension.IntegrationGroupEnum;
import com.epam.reportportal.extension.PluginCommand;
import com.epam.reportportal.extension.ReportPortalExtensionPoint;
import com.epam.reportportal.extension.common.IntegrationTypeProperties;
import com.epam.reportportal.extension.event.PluginEvent;
import com.epam.reportportal.extension.robot.command.RobotImportCommand;
import com.epam.reportportal.extension.robot.event.plugin.PluginEventHandlerFactory;
import com.epam.reportportal.extension.robot.event.plugin.PluginEventListener;
import com.epam.reportportal.extension.robot.utils.MemoizingSupplier;
import com.epam.reportportal.extension.util.RequestEntityConverter;
import com.epam.ta.reportportal.dao.IntegrationRepository;
import com.epam.ta.reportportal.dao.IntegrationTypeRepository;
import com.epam.ta.reportportal.dao.LaunchRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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

  private final Supplier<Map<String, PluginCommand>> pluginCommandMapping = new MemoizingSupplier<>(
      this::getCommands);
  private final String resourcesDir;
  private final Supplier<ApplicationListener<PluginEvent>> pluginLoadedListener;
  private final RequestEntityConverter requestEntityConverter;
  @Autowired
  private IntegrationTypeRepository integrationTypeRepository;
  @Autowired
  private IntegrationRepository integrationRepository;
  @Autowired
  private LaunchRepository launchRepository;
  @Autowired
  private ApplicationEventPublisher eventPublisher;
  private final Supplier<Map<String, CommonPluginCommand<?>>> commonPluginCommandMapping = new MemoizingSupplier<>(
      this::getCommonCommands);
  @Autowired
  private ApplicationContext applicationContext;

  public RobotFrameworkPluginExtension(Map<String, Object> initParams) {
    resourcesDir = IntegrationTypeProperties.RESOURCES_DIRECTORY.getValue(initParams)
        .map(String::valueOf).orElse("");

    pluginLoadedListener = new MemoizingSupplier<>(() -> new PluginEventListener(PLUGIN_ID,
        new PluginEventHandlerFactory(resourcesDir, integrationTypeRepository,
            integrationRepository)
    ));

    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
    objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    requestEntityConverter = new RequestEntityConverter(objectMapper);
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
    applicationEventMulticaster.addApplicationListener(pluginLoadedListener.get());
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
    applicationEventMulticaster.removeApplicationListener(pluginLoadedListener.get());
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
    return commonPluginCommandMapping.get().get(commandName);
  }

  @Override
  public PluginCommand getIntegrationCommand(String commandName) {
    return pluginCommandMapping.get().get(commandName);
  }

  @Override
  public IntegrationGroupEnum getIntegrationGroup() {
    return IntegrationGroupEnum.IMPORT;
  }

  private Map<String, PluginCommand> getCommands() {
    return new HashMap<>();
  }

  private Map<String, CommonPluginCommand<?>> getCommonCommands() {
    HashMap<String, CommonPluginCommand<?>> pluginCommands = new HashMap<>();
    var robotImportCommand = new RobotImportCommand(requestEntityConverter,
        eventPublisher, launchRepository);
    pluginCommands.put(robotImportCommand.getName(), robotImportCommand);
    return pluginCommands;
  }
}
