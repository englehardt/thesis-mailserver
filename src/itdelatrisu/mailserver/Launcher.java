package itdelatrisu.mailserver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Launcher class.
 */
public class Launcher {
	private static final Logger logger = LoggerFactory.getLogger(Launcher.class);

	public static void main(String[] args) {
		logger.info("Starting mail server...");
		MailServer server = new MailServer();
		server.start();
		logger.info("Server running on {} (port {}).", server.getHostName(), server.getPort());
	}
}
