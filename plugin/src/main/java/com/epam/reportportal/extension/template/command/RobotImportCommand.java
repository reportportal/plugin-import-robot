package com.epam.reportportal.extension.template.command;

import static org.apache.commons.io.FileUtils.ONE_MB;

import com.epam.reportportal.extension.CommonPluginCommand;
import com.epam.reportportal.extension.util.RequestEntityConverter;
import com.epam.ta.reportportal.dao.LaunchRepository;
import com.epam.ta.reportportal.ws.reporting.OperationCompletionRS;
import java.util.Map;
import org.springframework.context.ApplicationEventPublisher;

public class RobotImportCommand implements CommonPluginCommand<OperationCompletionRS> {

  public static final long MAX_FILE_SIZE = 32 * ONE_MB;
  private static final String FILE_PARAM = "file";
  private static final String PROJECT_NAME = "projectName";

  private final RequestEntityConverter requestEntityConverter;

  //  private final ImportStrategyFactory importStrategyFactory;
  private final LaunchRepository launchRepository;

  public RobotImportCommand(RequestEntityConverter requestEntityConverter,
      ApplicationEventPublisher eventPublisher, LaunchRepository launchRepository) {
    this.requestEntityConverter = requestEntityConverter;
    this.launchRepository = launchRepository;
  }

  @Override
  public OperationCompletionRS executeCommand(Map<String, Object> params) {
    return null;
  }

  @Override
  public String getName() {
    return "import";
  }
}
