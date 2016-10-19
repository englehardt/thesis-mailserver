package itdelatrisu.mailserver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Storage handler for incoming mail.
 */
public class MailStorage {
	private static final Logger logger = LoggerFactory.getLogger(MailStorage.class);

	public MailStorage() {}

	/** Handles the message. */
	public void handleMessage(String from, String recipient, String data) {
		// TODO
		// Write to disk
		// Write metadata to database
	}
}
