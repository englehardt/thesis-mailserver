package itdelatrisu.mailserver;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.Date;
import java.util.Properties;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handler for incoming mail.
 */
public class MailHandler {
	private static final Logger logger = LoggerFactory.getLogger(MailHandler.class);

	private final MailClassifier classifier;
	private final MailStorage storage;
	private final MailDB db;

	/** Creates the mail handler. */
	public MailHandler() {
		storage = new MailStorage();
		classifier = new MailClassifier();
		db = new MailDB();
	}

	/** Handles the message. */
	public void handleMessage(String from, String recipient, String data) {
		// store mail on disk
		File file = storage.store(from, recipient, data);

		// classify mail
		MailClassifier.ClassificationResult result = classifier.classify(from, recipient, data, db);

		// write mail entry into database
		String subject = null;
		Date sentDate = null;
		try {
			MimeMessage message = toMimeMessage(data);
			subject = message.getSubject();
			sentDate = message.getSentDate();
		} catch (MessagingException e) {
			logger.error("Failed to parse message.", e);
		}
		try {
			db.addMailEntry(recipient, from, sentDate, subject, result.getAffiliation(), result.isSpam(), file.getName());
		} catch (SQLException e) {
			logger.error("Failed to log message to database.", e);
		}
	}

	/** Parses mail data into a MimeMessage. */
	private MimeMessage toMimeMessage(String content) throws MessagingException {
		Session s = Session.getDefaultInstance(new Properties());
		InputStream is = new ByteArrayInputStream(content.getBytes());
		return new MimeMessage(s, is);
	}
}
