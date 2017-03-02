package itdelatrisu.mailserver;

import java.io.IOException;
import java.net.MalformedURLException;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Analyzer for incoming mail.
 */
public class MailAnalyzer {
	private static final Logger logger = LoggerFactory.getLogger(MailAnalyzer.class);
	private static final int MAX_REQUEST_THREADS = 5;
	private static final int TASK_SCHEDULE_DELAY = 1000;

	private final MailDB db;
	private final ScheduledExecutorService pool;

	/** Task for making requests to a URL. */
	private class RequestTask implements Callable<Request> {
		private final Request req;
		private final String senderDomain, senderAddress, recipientAddress;

		/** Creates a new request task to request the given URL. */
		public RequestTask(String url, String senderDomain, String senderAddress, String recipientAddress)
			throws MalformedURLException {
			this.req = new Request(url);
			this.senderDomain = senderDomain;
			this.senderAddress = senderAddress;
			this.recipientAddress = recipientAddress;
		}

		@Override
		public Request call() throws Exception {
			try {
				// make the request
				req.go();

				// write results into database
				db.addRedirects(req, senderDomain, senderAddress, recipientAddress);

				return req;
			} catch (Exception e) {
				logger.error("Error during request.", e);
				throw e;
			}
		}
	}

	/** Initializes the analyzer module. */
	public MailAnalyzer(MailDB db) {
		this.db = db;
		this.pool = Executors.newScheduledThreadPool(MAX_REQUEST_THREADS);
	}

	/** Analyzes the mail. */
	public void analyze(String from, String recipient, String data) {
		try {
			MimeMessage message = Utils.toMimeMessage(data);
			requestTrackingImages(message, from, recipient);
		} catch (MessagingException e) {
			logger.error("Failed to parse message.", e);
		}
	}

	/** Makes requests for tracking images present in the message. */
	private void requestTrackingImages(MimeMessage message, String from, String recipient) {
		try {
			// extract URLs from the message
			LinkExtractor extractor = new LinkExtractor(message);

			// make requests for:
			// - images explicitly labeled as 1x1
			// - URLs containing the recipient email address
			Set<String> requests = new HashSet<String>();
			for (LinkExtractor.Image img : extractor.getInlineImages()) {
				if (img.width.equals("1") && img.height.equals("1"))
					requests.add(img.url);
				else if (img.url.contains(recipient))
					requests.add(img.url);
			}
			for (String img : extractor.getInlineCssImages()) {
				if (img.contains(recipient))
					requests.add(img);
			}
			if (requests.isEmpty())
				return;

			// find sender domain associated with recipient
			String senderDomain = db.userRegisterDomain(recipient);
			if (senderDomain == null) {
				logger.error("Failed to find sender domain for email '{}'.", recipient);
				return;
			}

			// submit all requests
			for (String url : requests) {
				try {
					RequestTask task = new RequestTask(url, senderDomain, from, recipient);
					pool.schedule(task, TASK_SCHEDULE_DELAY, TimeUnit.MILLISECONDS);
				} catch (MalformedURLException e) {}
			}
		} catch (MessagingException | IOException | SQLException e) {
			logger.error("Failed to request tracking images.", e);
		}
	}

	/** Shuts down the executor service. */
	public void shutdown() { pool.shutdown(); }
}
