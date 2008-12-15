package com.limegroup.gnutella.gui.wizard;

/**
 * This class stores a message and a severity. Used to display user
 * notifications.
 */
public class Status {

	public enum Severity { INFO, WARNING, ERROR };
	
	private String message;
	
	private Severity severity;
	
	public Status(String message, Severity severity) {
		this.message = message;
		this.severity = severity;
	}
	
	public String getMessage() {
		return message;
	}
	
	public Severity getSeverity() {
		return severity;
	}
	
}
