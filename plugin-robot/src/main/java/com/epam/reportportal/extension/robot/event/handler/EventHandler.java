package com.epam.reportportal.extension.robot.event.handler;

/**
 * @author Andrei Piankouski
 */
public interface EventHandler<T> {

  void handle(T event);
}
