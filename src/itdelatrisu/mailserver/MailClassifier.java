package itdelatrisu.mailserver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Classifier for incoming mail.
 */
public class MailClassifier {
	private static final Logger logger = LoggerFactory.getLogger(MailClassifier.class);

	/** Classification result. */
	public static class ClassificationResult {
		private final String affiliation;
		private final boolean isSpam;
		public ClassificationResult(String affiliation, boolean isSpam) {
			this.affiliation = affiliation;
			this.isSpam = isSpam;
		}

		/** Returns the sender affiliation. */
		public String getAffiliation() { return affiliation; }

		/** Returns whether this is spam. */
		public boolean isSpam() { return isSpam; }
	}

	public MailClassifier() {}

	/** Classifies the message. */
	public ClassificationResult classify(String from, String recipient, String data, MailDB db) {
		// TODO
		// Read `register_site` for recipient, compare to sender
		String affiliation = null;
		boolean isSpam = false;
		return new ClassificationResult(affiliation, isSpam);
	}
}
