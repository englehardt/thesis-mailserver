package itdelatrisu.mailserver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Launcher class.
 */
public class Launcher {
	private static final Logger logger = LoggerFactory.getLogger(Launcher.class);
	private static final String DOMAIN_NAME = "lorveskel.me";

	public static void main(String[] args) {
		// allow multipart messages with no body parts
		System.setProperty("mail.mime.multipart.allowempty", "true");

		String domain = DOMAIN_NAME;
		logger.info("Initializing for domain [{}]...", domain);

		// get mail database instance
		MailDB db = new MailDB();

		// start mail server
		MailServer mailServer = new MailServer(db);
		mailServer.start();
		logger.info("Mail server running on port {}.", mailServer.getPort());

		// start web server
		WebServer webServer = new WebServer(db, domain);
		webServer.start();
		logger.info("Web server running on port {}.", webServer.getPort());
	}
}
