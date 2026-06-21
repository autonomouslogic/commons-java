package com.autonomouslogic.commons.concurrent;

/**
 * Exception thrown when an operation requires a virtual thread but is executed on a platform thread.
 */
public class NotVirtualThreadException extends RuntimeException {
	public NotVirtualThreadException() {
		super("Current thread is not a virtual thread");
	}

	public NotVirtualThreadException(String message) {
		super(message);
	}

	public NotVirtualThreadException(String message, Throwable cause) {
		super(message, cause);
	}

	public NotVirtualThreadException(Throwable cause) {
		super(cause);
	}
}
