package com.epam.reportportal.extension.template.command;

import com.epam.reportportal.extension.PluginCommand;
import com.epam.ta.reportportal.entity.integration.Integration;

import java.util.Map;

/**
 * @author Andrei Piankouski
 */
public class TemplateCommand implements PluginCommand<String> {

    @Override
    public String getName() {
        return "TemplateCommand";
    }

    @Override
    public String executeCommand(Integration integration, Map<String, Object> params) {
        return "TemplateCommand";
    }
}
