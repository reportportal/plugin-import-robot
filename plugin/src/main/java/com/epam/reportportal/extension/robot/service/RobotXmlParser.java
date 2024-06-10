package com.epam.reportportal.extension.robot.service;

import static com.epam.reportportal.extension.robot.service.RobotReportTag.ARG;
import static com.epam.reportportal.extension.robot.service.RobotReportTag.ATTR_END_TIME;
import static com.epam.reportportal.extension.robot.service.RobotReportTag.ATTR_GENERATED;
import static com.epam.reportportal.extension.robot.service.RobotReportTag.ATTR_HTML;
import static com.epam.reportportal.extension.robot.service.RobotReportTag.ATTR_LEVEL;
import static com.epam.reportportal.extension.robot.service.RobotReportTag.ATTR_LIBRARY;
import static com.epam.reportportal.extension.robot.service.RobotReportTag.ATTR_LINE;
import static com.epam.reportportal.extension.robot.service.RobotReportTag.ATTR_NAME;
import static com.epam.reportportal.extension.robot.service.RobotReportTag.ATTR_SOURCE;
import static com.epam.reportportal.extension.robot.service.RobotReportTag.ATTR_START_TIME;
import static com.epam.reportportal.extension.robot.service.RobotReportTag.ATTR_STATUS;
import static com.epam.reportportal.extension.robot.service.RobotReportTag.ATTR_TIMESTAMP;
import static com.epam.reportportal.extension.robot.service.RobotReportTag.ATTR_TYPE;
import static com.epam.reportportal.extension.robot.service.RobotReportTag.DOC;
import static com.epam.reportportal.extension.robot.service.RobotReportTag.KEYWORD;
import static com.epam.reportportal.extension.robot.service.RobotReportTag.MESSAGE;
import static com.epam.reportportal.extension.robot.service.RobotReportTag.ROBOT;
import static com.epam.reportportal.extension.robot.service.RobotReportTag.SUITE;
import static com.epam.reportportal.extension.robot.service.RobotReportTag.TAG;
import static com.epam.reportportal.extension.robot.service.RobotReportTag.TEST;
import static com.epam.reportportal.extension.robot.service.RobotReportTag.fromString;
import static java.lang.Boolean.TRUE;
import static org.springframework.http.MediaType.IMAGE_GIF_VALUE;
import static org.springframework.http.MediaType.IMAGE_JPEG_VALUE;
import static org.springframework.http.MediaType.IMAGE_PNG_VALUE;

import com.epam.reportportal.events.FinishItemRqEvent;
import com.epam.reportportal.events.SaveLogRqEvent;
import com.epam.reportportal.events.StartChildItemRqEvent;
import com.epam.reportportal.events.StartRootItemRqEvent;
import com.epam.reportportal.extension.robot.model.ItemInfo;
import com.epam.reportportal.extension.robot.utils.DateUtils;
import com.epam.reportportal.extension.robot.utils.DocumentBuilderInitializer;
import com.epam.reportportal.extension.robot.utils.RobotMapper;
import com.epam.reportportal.rules.exception.ErrorType;
import com.epam.reportportal.rules.exception.ReportPortalException;
import com.epam.ta.reportportal.entity.enums.LogLevel;
import com.epam.ta.reportportal.entity.enums.StatusEnum;
import com.epam.ta.reportportal.entity.enums.TestItemTypeEnum;
import com.epam.ta.reportportal.ws.reporting.FinishTestItemRQ;
import com.epam.ta.reportportal.ws.reporting.ItemAttributesRQ;
import com.epam.ta.reportportal.ws.reporting.SaveLogRQ;
import com.epam.ta.reportportal.ws.reporting.StartTestItemRQ;
import com.google.api.client.util.Lists;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Enumeration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import javax.annotation.Nullable;
import javax.xml.parsers.DocumentBuilder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.fileupload.disk.DiskFileItem;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.MediaTypeFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.commons.CommonsMultipartFile;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

@Slf4j
@Getter
public class RobotXmlParser {

  private static final Pattern IMG_REGEX = Pattern.compile("<img\\s+src=\"([^\"]*)\"");
  private static final List<String> SUPPORTED_IMAGE_CONTENT_TYPES = List.of(IMAGE_GIF_VALUE,
      IMAGE_JPEG_VALUE, IMAGE_PNG_VALUE);
  private static final List<String> SUPPORTED_XML_ELEMENTS = List.of(ROBOT.val(), SUITE.val(),
      TEST.val(),
      KEYWORD.val(), MESSAGE.val());

  private ZipFile zipFile;
  private final ApplicationEventPublisher eventPublisher;
  private final String launchUuid;
  private final String projectName;
  private Instant lowestTime;
  private Instant highestTime;
  private final Deque<ItemInfo> items = new ArrayDeque<>();

  public RobotXmlParser(ApplicationEventPublisher eventPublisher, String launchUuid,
      String projectName) {
    this.eventPublisher = eventPublisher;
    this.launchUuid = launchUuid;
    this.projectName = projectName;
    this.lowestTime = Instant.now();
    this.highestTime = Instant.EPOCH;
  }

  public RobotXmlParser(ApplicationEventPublisher eventPublisher, String launchUuid,
      String projectName, ZipFile rootZipFile) {
    this.eventPublisher = eventPublisher;
    this.launchUuid = launchUuid;
    this.projectName = projectName;
    this.lowestTime = Instant.now();
    this.highestTime = Instant.EPOCH;
    this.zipFile = rootZipFile;
  }

  public void parse(InputStream inputStream) {
    DocumentBuilder documentBuilder = DocumentBuilderInitializer.get();
    try {
      Document document = documentBuilder.parse(inputStream);
      Element root = document.getDocumentElement();
      Instant generatedTime = DateUtils.parseDateAttribute(
          root.getAttribute(ATTR_GENERATED.val()));
      if (generatedTime.isBefore(lowestTime)) {
        lowestTime = generatedTime;
      }

      traverseNodes(root);

    } catch (IOException | SAXException e) {
      throw new ReportPortalException(ErrorType.UNCLASSIFIED_REPORT_PORTAL_ERROR, e.getMessage());
    }
  }

  private void traverseNodes(Node node) {
    if (!SUPPORTED_XML_ELEMENTS.contains(node.getNodeName())) {
      return;
    }
    processStartNode(node);
    NodeList nodeList = node.getChildNodes();
    for (int i = 0; i < nodeList.getLength(); i++) {
      Node childNode = nodeList.item(i);
      traverseNodes(childNode);
      processFinishNode(childNode);
    }
  }

  private void processStartNode(Node node) {
    if (node.getNodeType() == Node.ELEMENT_NODE) {
      final Element element = (Element) node;
      switch (fromString(element.getNodeName())) {
        case SUITE:
          items.push(handleSuiteElement(element));
          break;
        case KEYWORD:
          items.push(handleKeywordElement(element));
          break;
        case TEST:
          items.push(handleTestElement(element));
          break;
        case MESSAGE:
          handleMsgElement(element);
          break;
        default:
          break;
      }
    }
  }

  private void processFinishNode(Node node) {
    if (node.getNodeType() == Node.ELEMENT_NODE) {
      final Element element = (Element) node;
      switch (fromString(element.getNodeName())) {
        case SUITE:
        case KEYWORD:
        case TEST:
          finishTestItem();
          break;
        default:
          break;
      }
    }
  }

  private ItemInfo handleSuiteElement(Element element) {
    ItemInfo itemInfo = new ItemInfo();
    String sourceAttribute = element.getAttribute(ATTR_SOURCE.val());
    itemInfo.setSource(sourceAttribute.substring(sourceAttribute.lastIndexOf("/")));
    itemInfo.setName(Optional.ofNullable(element.getAttribute(ATTR_NAME.val())).orElse("no_name"));
    itemInfo.setType(TestItemTypeEnum.SUITE);
    updateWithStatusInfo(element, itemInfo);
    updateWithDescription(element, itemInfo);
    String uuid = items.peek() == null ? startRootItem(itemInfo) : startTestItem(itemInfo);
    itemInfo.setUuid(uuid);
    return itemInfo;
  }

  private ItemInfo handleKeywordElement(Element element) {
    ItemInfo itemInfo = new ItemInfo();
    itemInfo.setType(resolveKeywordType(element));
    itemInfo.setName(resolveKeywordName(element));
    updateWithDescription(element, itemInfo);
    updateWithStatusInfo(element, itemInfo);
    String uuid = startTestItem(itemInfo);
    itemInfo.setUuid(uuid);
    return itemInfo;
  }

  private ItemInfo handleTestElement(Element element) {
    ItemInfo itemInfo = new ItemInfo();
    List<String> sources = new ArrayList<>(items.size());
    items.descendingIterator().forEachRemaining(item -> sources.add(item.getSource()));
    String sourcePath = String.join("", sources);
    itemInfo.setName(element.getAttribute(ATTR_NAME.val()));
    itemInfo.setTestCaseId(sourcePath + ":" + itemInfo.getName());
    itemInfo.setCodeReference(sourcePath + ":" + element.getAttribute(ATTR_LINE.val()));
    itemInfo.setType(TestItemTypeEnum.STEP);
    updateWithStatusInfo(element, itemInfo);
    updateWithDescription(element, itemInfo);
    updateWithTags(element, itemInfo);
    String uuid = startTestItem(itemInfo);
    itemInfo.setUuid(uuid);
    return itemInfo;
  }

  private void handleMsgElement(Element element) {
    Instant logTime = DateUtils.parseDateAttribute(
        element.getAttribute(ATTR_TIMESTAMP.val()));
    String msg = element.getTextContent();
    LogLevel level = RobotMapper.mapLogLevel(element.getAttribute(ATTR_LEVEL.val()));

    SaveLogRQ saveLogRQ = new SaveLogRQ();
    saveLogRQ.setLevel(level.name());
    saveLogRQ.setLogTime(logTime);
    saveLogRQ.setMessage(msg.trim());
    saveLogRQ.setItemUuid(items.peek().getUuid());

    MultipartFile multipartFile = null;
    if (Objects.equals(element.getAttribute(ATTR_HTML.val()), TRUE.toString())) {
      multipartFile = getImageMultipartFile(msg);
    }
    eventPublisher.publishEvent(new SaveLogRqEvent(this, projectName, saveLogRQ, multipartFile));

  }

  private String startRootItem(ItemInfo suite) {
    StartTestItemRQ rq = buildStartItemRq(suite);
    eventPublisher.publishEvent(new StartRootItemRqEvent(this, projectName, rq));
    return rq.getUuid();
  }

  private String startTestItem(ItemInfo itemInfo) {
    StartTestItemRQ rq = buildStartItemRq(itemInfo);
    eventPublisher.publishEvent(
        new StartChildItemRqEvent(this, projectName, items.peek().getUuid(), rq));
    return rq.getUuid();
  }

  private StartTestItemRQ buildStartItemRq(ItemInfo itemInfo) {
    StartTestItemRQ rq = new StartTestItemRQ();
    rq.setUuid(UUID.randomUUID().toString());
    rq.setLaunchUuid(launchUuid);
    rq.setStartTime(itemInfo.getStartTime());
    rq.setType(itemInfo.getType().name());
    rq.setDescription(itemInfo.getDescription());
    rq.setName(itemInfo.getName());
    rq.setAttributes(itemInfo.getItemAttributes());
    rq.setCodeRef(itemInfo.getCodeReference());
    rq.setTestCaseId(itemInfo.getTestCaseId());
    return rq;
  }

  private void finishTestItem() {
    ItemInfo itemInfo = items.poll();
    if (itemInfo != null) {
      final FinishTestItemRQ rq = new FinishTestItemRQ();
      rq.setStatus(itemInfo.getStatus().name());
      rq.setEndTime(itemInfo.getEndTime());
      eventPublisher.publishEvent(new FinishItemRqEvent(this, projectName, itemInfo.getUuid(), rq));
      if (itemInfo.getEndTime().isAfter(highestTime)) {
        highestTime = itemInfo.getEndTime();
      }
    }
  }

  private void updateWithStatusInfo(Element element, ItemInfo itemInfo) {
    findChildNodeByName(element, ATTR_STATUS.val()).ifPresent(status -> {
      Element statusElement = (Element) status;
      StatusEnum rpStatus = RobotMapper.mapStatus(statusElement.getAttribute(ATTR_STATUS.val()));
      Instant startTime = DateUtils.parseDateAttribute(
          statusElement.getAttribute(ATTR_START_TIME.val()));
      Instant endTime = DateUtils.parseDateAttribute(
          statusElement.getAttribute(ATTR_END_TIME.val()));
      itemInfo.setStatus(rpStatus);
      itemInfo.setStartTime(startTime);
      itemInfo.setEndTime(endTime);
    });
  }

  private void updateWithDescription(Element element, ItemInfo itemInfo) {
    findChildNodeByName(element, DOC.val()).ifPresent(
        doc -> itemInfo.setDescription(doc.getTextContent()));
  }

  private void updateWithTags(Element element, ItemInfo itemInfo) {
    List<Node> tags = findChildNodes(element, TAG.val());
    Set<ItemAttributesRQ> attributes = tags.stream()
        .map(it -> new ItemAttributesRQ(it.getTextContent())).collect(Collectors.toSet());
    itemInfo.setItemAttributes(attributes);
  }

  private String resolveKeywordName(Element element) {
    StringBuilder name = new StringBuilder();
    if (StringUtils.hasText(element.getAttribute(ATTR_LIBRARY.val()))) {
      name.append(element.getAttribute(ATTR_LIBRARY.val())).append(".");
    }
    name.append(element.getAttribute(ATTR_NAME.val()));
    List<Node> args = findChildNodes(element, ARG.val());
    name.append(" (");
    name.append(args.stream().map(Node::getTextContent).collect(Collectors.joining(", ")));
    name.append(")");
    return name.toString();
  }

  private TestItemTypeEnum resolveKeywordType(Element element) {
    String type = element.getAttribute(ATTR_TYPE.val());
    if (type == null || Objects.equals(element.getParentNode().getNodeName(), TEST.val())) {
      return TestItemTypeEnum.STEP;
    }
    if (Objects.equals(type, "SETUP")) {
      return TestItemTypeEnum.BEFORE_SUITE;
    }
    if (Objects.equals(type, "TEARDOWN")) {
      return TestItemTypeEnum.AFTER_SUITE;
    }
    return TestItemTypeEnum.STEP;
  }

  private Optional<Node> findChildNodeByName(Element element, String name) {
    NodeList childNodes = element.getChildNodes();
    if (childNodes.getLength() > 0) {
      for (int i = 0; i < childNodes.getLength(); i++) {
        if (Objects.equals(childNodes.item(i).getNodeName(), name)) {
          return Optional.of(childNodes.item(i));
        }
      }
    }
    return Optional.empty();
  }

  private List<Node> findChildNodes(Element element, String name) {
    List<Node> result = Lists.newArrayList();
    NodeList childNodes = element.getChildNodes();
    if (childNodes.getLength() > 0) {
      for (int i = 0; i < childNodes.getLength(); i++) {
        if (Objects.equals(childNodes.item(i).getNodeName(), name)) {
          result.add(childNodes.item(i));
        }
      }
    }
    return result;
  }

  @Nullable
  private MultipartFile getImageMultipartFile(String msg) {
    Matcher matcher = IMG_REGEX.matcher(msg);
    if (matcher.find()) {
      String imgName = matcher.group(1);
      return findScreenshot(imgName).map(screen -> {
        DiskFileItem fileItem = new DiskFileItem("file", screen.getContentType(), false,
            screen.getName(),
            screen.getContent().length, null);
        try (OutputStream os = fileItem.getOutputStream()) {
          os.write(screen.getContent());
        } catch (IOException e) {
          log.error(e.getMessage());
        }
        return fileItem;
      }).map(CommonsMultipartFile::new).orElse(null);
    }
    return null;
  }

  private Optional<SaveLogRQ.File> findScreenshot(String imgName) {
    Enumeration<? extends ZipEntry> entries = zipFile.entries();
    while (entries.hasMoreElements()) {
      ZipEntry entry = entries.nextElement();
      if (!entry.isDirectory() &&
          Objects.equals(entry.getName(), imgName) &&
          MediaTypeFactory.getMediaType(entry.getName()).isPresent() &&
          SUPPORTED_IMAGE_CONTENT_TYPES.contains(
              MediaTypeFactory.getMediaType(entry.getName()).get().toString())) {
        try (InputStream inputStream = zipFile.getInputStream(entry)) {
          final SaveLogRQ.File file = new SaveLogRQ.File();
          file.setName(entry.getName());
          file.setContentType(MediaTypeFactory.getMediaType(entry.getName()).get().toString());
          file.setContent(inputStream.readAllBytes());
          return Optional.of(file);
        } catch (IOException e) {
          log.error(e.getMessage());
        }
      }
    }
    return Optional.empty();
  }

}
