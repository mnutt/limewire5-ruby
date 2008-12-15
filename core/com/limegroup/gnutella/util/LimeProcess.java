package com.limegroup.gnutella.util;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.util.StringUtils;



/**
 * Thin wrapper class to execute a command. Stores the command, the arguments
 * and the executed process.
 */
public class LimeProcess {

	private final static Log LOG = LogFactory.getLog(LimeProcess.class);

	private final String[] command;

	private Process process;

	private LimeProcess(String[] command) {
		this.command = command;
	}

	private Process exec() throws SecurityException, LaunchException {
		if (LOG.isInfoEnabled()) {
			LOG.info("Running command: '" + StringUtils.explode(command, " ") + "'");
		}
		
		try {
			process = Runtime.getRuntime().exec(command);
		} catch (IOException e) {
			throw new LaunchException(e, command);
		}
		return process;
	}

	/**
	 * Returns the command and arguments.
	 */
	public String[] getCommand() {
		return command;
	}

	/**
	 * Returns the process.
	 */
	public Process getProcess() {
		return process;
	}

	/**
	 * Executes the specified command and arguments in <tt>cmdarray</tt>. 
	 * 
	 * @param cmdarray command and arguments
	 * @return a wrapper object for the spawned process
	 * @throws SecurityException If execution of the command is not allowed
	 * @throws LaunchException If an {@link IOException} occurs
	 * @see Runtime#exec(String[])
	 */
	public static LimeProcess exec(String[] cmdarray) throws SecurityException,
			LaunchException {
		LimeProcess p = new LimeProcess(cmdarray);
		p.exec();
		return p;
	}

}
