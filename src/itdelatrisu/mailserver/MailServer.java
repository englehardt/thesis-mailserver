package itdelatrisu.mailserver;

import java.io.IOException;
import java.io.InputStream;
import java.util.Scanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.subethamail.smtp.TooMuchDataException;
import org.subethamail.smtp.helper.SimpleMessageListener;
import org.subethamail.smtp.helper.SimpleMessageListenerAdapter;
import org.subethamail.smtp.server.SMTPServer;

/**
 * Main SMTP server class.
 */
public class MailServer extends SMTPServer {
	private static final Logger logger = LoggerFactory.getLogger(MailServer.class);

	/** SMTP message listener. */
	private static class MessageListener implements SimpleMessageListener {
		private final MailHandler handler;
		public MessageListener() { handler = new MailHandler(); }

		@Override
		public boolean accept(String from, String recipient) {
			logger.info("ACCEPT:\n{}\n{}", from, recipient);
			return true;
		}

		@Override
		public void deliver(String from, String recipient, InputStream data)
			throws TooMuchDataException, IOException {
			logger.info("DELIVER:\n{}\n{}", from, recipient);
			handler.handleMessage(from, recipient, streamToString(data));
		}

		/** Reads the input stream and returns the data as a string. */
		private String streamToString(InputStream is) {
			try (Scanner s = new Scanner(is)) {
				return s.useDelimiter("\\A").hasNext() ? s.next() : "";
			}
		}
	}

	/** Creates the SMTP server. */
	public MailServer() {
		super(new SimpleMessageListenerAdapter(new MessageListener()));
	}
}
