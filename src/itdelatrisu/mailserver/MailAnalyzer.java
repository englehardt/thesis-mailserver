package itdelatrisu.mailserver;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
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
	private final Random random;

	/** Task for making requests to a URL. */
	private class RequestTask implements Callable<Request> {
		private final Request req;
		private final String senderDomain, senderAddress;
		private final int recipientId;
		private final List<HashChecker.NamedValue<String>> encodings;

		/** Creates a new request task to request the given URL. */
		public RequestTask(
			String url,
			String senderDomain,
			String senderAddress,
			int recipientId,
			List<HashChecker.NamedValue<String>> encodings
		) throws MalformedURLException {
			this.req = new Request(url);
			this.senderDomain = senderDomain;
			this.senderAddress = senderAddress;
			this.recipientId = recipientId;
			this.encodings = encodings;
		}

		@Override
		public Request call() throws Exception {
			try {
				// make the request
				req.go();

				// write results into database
				db.addRedirects(req, senderDomain, senderAddress, recipientId);
				if (!req.getRedirects().isEmpty()) {
					for (URL url : req.getRedirects())
						findLeakedEmailAddress(url.toString(), encodings, true, recipientId, senderDomain, senderAddress);
				}

				return req;
			} catch (Exception e) {
				logger.error(String.format("Error raised during request for [%s].", req.getURL().toString()), e);
				throw e;
			}
		}
	}

	/** Initializes the analyzer module. */
	public MailAnalyzer(MailDB db) {
		this.db = db;
		this.pool = Executors.newScheduledThreadPool(MAX_REQUEST_THREADS);
		this.random = new Random();
	}

	/** Analyzes the mail. */
	public void analyze(String from, String recipient, String data) {
		// extract HTML from the email
		String html;
		try {
			MimeMessage message = Utils.toMimeMessage(data);
			html = Utils.getHtmlFromMessage(message);
		} catch (MessagingException | IOException e) {
			logger.error("Failed to parse message.", e);
			return;
		}
		if (html == null)
			return;

		// extract URLs
		LinkExtractor extractor = new LinkExtractor(html);

		// get recipient's user info
		int recipientId;
		String senderDomain;
		try {
			MailDB.MailUser user = db.getUserInfo(recipient);
			if (user == null) {
				logger.error("No user entry for email '{}'.", recipient);
				return;
			}
			recipientId = user.getId();
			senderDomain = new URL(user.getRegistrationSiteUrl()).getHost();
		} catch (SQLException | MalformedURLException e) {
			logger.error("Failed to get user info for email '{}'.", recipient);
			return;
		}

		// find leaked email addresses
		List<HashChecker.NamedValue<String>> encodings = HashChecker.getEncodings(recipient);
		for (String link : extractor.getAllLinks())
			findLeakedEmailAddress(link, encodings, false, recipientId, senderDomain, from);

		// request tracking images
		requestTrackingImages(extractor, from, recipient, recipientId, senderDomain, encodings);
	}

	/** Makes requests for tracking images present in the message. */
	private void requestTrackingImages(
		LinkExtractor extractor,
		String from,
		String recipient,
		int recipientId,
		String senderDomain,
		List<HashChecker.NamedValue<String>> encodings
	) {
		try {
			// make requests for:
			// - images explicitly labeled as 1x1
			// - URLs containing the recipient email address (raw or encoded)
			// - 1 random other image
			Set<String> requests = new HashSet<String>();
			List<String> nonRequestedImages = new ArrayList<String>();
			for (LinkExtractor.Image img : extractor.getInlineImages()) {
				if (img.width.equals("1") && img.height.equals("1"))
					requests.add(img.url);
				else {
					boolean added = false;
					for (HashChecker.NamedValue<String> enc : encodings) {
						if (img.url.contains(enc.getValue())) {
							requests.add(img.url);
							added = true;
							break;
						}
					}
					if (!added)
						nonRequestedImages.add(img.url);
				}
			}
			for (String img : extractor.getInlineCssImages()) {
				boolean added = false;
				for (HashChecker.NamedValue<String> enc : encodings) {
					if (img.contains(enc.getValue())) {
						requests.add(img);
						added = true;
						break;
					}
				}
				if (!added)
					nonRequestedImages.add(img);
			}
			if (!nonRequestedImages.isEmpty()) {
				String img = nonRequestedImages.get(random.nextInt(nonRequestedImages.size()));
				requests.add(img);
			}
			if (requests.isEmpty())
				return;

			// submit all requests
			for (String url : requests) {
				try {
					RequestTask task = new RequestTask(url, senderDomain, from, recipientId, encodings);
					pool.schedule(task, TASK_SCHEDULE_DELAY, TimeUnit.MILLISECONDS);
				} catch (MalformedURLException e) {}
			}
		} catch (Exception e) {
			logger.error("Failed to request tracking images.", e);
		}
	}

	/** Finds leaked email addresses in the given URL. */
	private void findLeakedEmailAddress(
		String url,
		List<HashChecker.NamedValue<String>> encodings,
		boolean isRedirect,
		int recipientId,
		String senderDomain,
		String senderAddress
	) {
		try {
			for (HashChecker.NamedValue<String> enc : encodings) {
				if (url.contains(enc.getValue()))
					db.addLeakedEmailAddress(url, enc.getName(), isRedirect, senderDomain, senderAddress, recipientId);
			}
		} catch (SQLException e) {
			logger.error("Failed to record leaked email address.", e);
		}
	}

	/** Shuts down the executor service. */
	public void shutdown() { pool.shutdown(); }
}
