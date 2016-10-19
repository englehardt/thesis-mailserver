package itdelatrisu.mailserver;

/**
 * Handler for incoming mail.
 */
public class MailHandler {
	private final MailClassifier classifier;
	private final MailStorage storage;

	/** Creates the mail handler. */
	public MailHandler() {
		storage = new MailStorage();
		classifier = new MailClassifier();
	}

	/** Handles the message. */
	public void handleMessage(String from, String recipient, String data) {
		storage.handleMessage(from, recipient, data);
		classifier.handleMessage(from, recipient, data);
	}
}
