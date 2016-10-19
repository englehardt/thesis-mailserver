package itdelatrisu.mailserver;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Properties;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Classifier for incoming mail.
 */
public class MailClassifier {
	private static final Logger logger = LoggerFactory.getLogger(MailClassifier.class);

	public MailClassifier() {}

	/** Handles the message. */
	public void handleMessage(String from, String recipient, String data) {
		// TODO
		MimeMessage message = null;
		try {
			message = toMimeMessage(data);
		} catch (MessagingException e) {
			logger.error("Failed to parse message.", e);
		}
	}

	/** Parses mail data into a MimeMessage. */
	private MimeMessage toMimeMessage(String content) throws MessagingException {
		Session s = Session.getDefaultInstance(new Properties());
		InputStream is = new ByteArrayInputStream(content.getBytes());
		return new MimeMessage(s, is);
	}
}
