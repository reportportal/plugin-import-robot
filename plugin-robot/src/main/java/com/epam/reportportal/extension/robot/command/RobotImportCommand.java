package com.epam.reportportal.extension.robot.command;

import static com.epam.reportportal.extension.robot.service.FileExtensionConstant.XML_EXTENSION;
import static com.epam.reportportal.extension.robot.service.FileExtensionConstant.ZIP_EXTENSION;
import static com.epam.reportportal.extension.util.CommandParamUtils.ENTITY_PARAM;
import static com.epam.reportportal.rules.commons.validation.BusinessRule.expect;
import static com.epam.reportportal.rules.exception.ErrorType.BAD_REQUEST_ERROR;
import static com.epam.reportportal.rules.exception.ErrorType.INCORRECT_REQUEST;
import static org.apache.commons.io.FileUtils.ONE_MB;

import com.epam.reportportal.extension.CommonPluginCommand;
import com.epam.reportportal.extension.robot.model.LaunchImportCompletionRS;
import com.epam.reportportal.extension.robot.model.LaunchImportData;
import com.epam.reportportal.extension.robot.model.LaunchImportRQ;
import com.epam.reportportal.extension.robot.service.ImportStrategy;
import com.epam.reportportal.extension.robot.service.ImportStrategyFactory;
import com.epam.reportportal.extension.util.RequestEntityConverter;
import com.epam.reportportal.rules.exception.ErrorType;
import com.epam.reportportal.rules.exception.ReportPortalException;
import com.epam.ta.reportportal.dao.LaunchRepository;
import com.epam.ta.reportportal.ws.reporting.OperationCompletionRS;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.multipart.MultipartFile;

public class RobotImportCommand implements CommonPluginCommand<OperationCompletionRS> {

  public static final long MAX_FILE_SIZE = 32 * ONE_MB;
  private static final String FILE_PARAM = "file";
  private static final String PROJECT_NAME = "projectName";

  private final RequestEntityConverter requestEntityConverter;
  private final ImportStrategyFactory importStrategyFactory;
  private final LaunchRepository launchRepository;

  public RobotImportCommand(RequestEntityConverter requestEntityConverter,
      ApplicationEventPublisher eventPublisher,
      LaunchRepository launchRepository) {
    this.requestEntityConverter = requestEntityConverter;
    this.launchRepository = launchRepository;
    this.importStrategyFactory = new ImportStrategyFactory(eventPublisher, launchRepository);
  }

  @Override
  public OperationCompletionRS executeCommand(Map<String, Object> params) {

    LaunchImportRQ launchImportRQ = Optional.ofNullable(params.get(ENTITY_PARAM))
        .map(it -> requestEntityConverter.getEntity(ENTITY_PARAM, params, LaunchImportRQ.class))
        .orElseGet(LaunchImportRQ::new);

    MultipartFile file = (MultipartFile) Optional.ofNullable(params.get(FILE_PARAM))
        .orElseThrow(() -> new ReportPortalException(
            ErrorType.BAD_REQUEST_ERROR, "File for import wasn't provided"));

    validate(file);

    ImportStrategy importStrategy = importStrategyFactory.getImportStrategy(
        file.getOriginalFilename());

    String projectName = Optional.ofNullable(params.get(PROJECT_NAME)).map(String::valueOf)
        .orElseThrow(
            () -> new ReportPortalException(BAD_REQUEST_ERROR, "Project name wasn't provided"));

    String launchUuid = importStrategy.importLaunch(file, projectName, launchImportRQ);
    return prepareLaunchImportResponse(launchUuid);
  }

  @Override
  public String getName() {
    return "import";
  }

  private void validate(MultipartFile file) {
    expect(file.getOriginalFilename(), Objects::nonNull).verify(INCORRECT_REQUEST,
        "File name should be not empty."
    );
    expect(file.getOriginalFilename(),
        it -> it.endsWith(ZIP_EXTENSION) || it.endsWith(XML_EXTENSION)
    ).verify(INCORRECT_REQUEST,
        "Should be a zip archive or an xml file " + file.getOriginalFilename()
    );
    expect(file.getSize(), size -> size <= MAX_FILE_SIZE).verify(INCORRECT_REQUEST,
        "File size is more than 32 Mb."
    );
  }

  private OperationCompletionRS prepareLaunchImportResponse(String uuid) {
    var data = new LaunchImportData();
    data.setUuid(uuid);

    var response = new LaunchImportCompletionRS();
    response.setResultMessage("Launch with id = " + uuid + " is successfully started to import.");
    response.setData(data);

    return response;
  }
}
