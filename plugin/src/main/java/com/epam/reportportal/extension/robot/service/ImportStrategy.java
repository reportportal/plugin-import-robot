/*
 * Copyright 2019 EPAM Systems
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.epam.reportportal.extension.robot.service;


import com.epam.reportportal.extension.robot.model.LaunchImportRQ;
import org.springframework.web.multipart.MultipartFile;

/**
 * Handler for processing launch importing.
 *
 * @author Pavel_Bortnik
 */
public interface ImportStrategy {

  /**
   * Processing launch importing.
   *
   * @param file        zip file that contains xml test reports
   * @param projectName project name
   * @param rq          {@link LaunchImportRQ} launch import request
   * @return launch uuid
   */
  String importLaunch(MultipartFile file, String projectName, LaunchImportRQ rq);
}
