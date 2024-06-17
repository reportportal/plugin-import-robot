package com.epam.reportportal.extension.robot.event;

import com.epam.reportportal.extension.robot.event.handler.EventHandler;

/**
 * @author Andrei Piankouski
 */
public interface EventHandlerFactory<T> {

  EventHandler<T> getEventHandler(String key);
}
