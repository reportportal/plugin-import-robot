package com.epam.reportportal.extension.template.event;

import com.epam.reportportal.extension.template.event.handler.EventHandler;

/**
 * @author Andrei Piankouski
 */
public interface EventHandlerFactory<T> {

	EventHandler<T> getEventHandler(String key);
}
