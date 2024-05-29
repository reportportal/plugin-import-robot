package com.epam.reportportal.extension.template.event.handler;

/**
 * @author Andrei Piankouski
 */
public interface EventHandler<T> {

	void handle(T event);
}
