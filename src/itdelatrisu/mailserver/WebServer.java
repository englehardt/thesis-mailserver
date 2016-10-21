package itdelatrisu.mailserver;

import java.sql.SQLException;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import spark.QueryParamsMap;
import spark.Spark;

public class WebServer {
	private static final Logger logger = LoggerFactory.getLogger(WebServer.class);

	private final MailDB db;
	private final String domain;
	private final int port;

	/** Initializes the web server. */
	public WebServer(MailDB db, String domain) {
		this.db = db;
		this.domain = domain;
		this.port = 8080;
		Spark.port(port);
	}

	/** Starts the server. */
	public void start() {
		// POST /register : site, url -> email
		Spark.post("/register", (request, response) -> {
			// parse request data
			QueryParamsMap map = request.queryMap();
			String site = map.value("site"), url = map.value("url");
			if (site == null || request == null) {
				response.status(400);
				return "<html><body><h2>400 Bad Request</h2></body></html>";
			}
			logger.info("/request: {} - {}", site, url);

			// generate an email address
			int retries = 3;  // in case UUID generation fails
			while (retries-- > 0) {
				String id = UUID.randomUUID().toString().replaceAll("-", "");
				String email = String.format("%s@%s", id, domain);
				try {
					if (db.addMailUser(email, site, url)) {
						logger.info("Created new user {}.");
						return email;
					}
				} catch (SQLException e) {
					logger.error("Failed to create new user.", e);
					break;
				}
			}

			// return failure
			response.status(500);
			return "<html><body><h2>500 Internal Server Error</h2></body></html>";
		});
	}

	/** Stops the server. */
	public void stop() { Spark.stop(); }

	/** Returns the server's port. */
	public int getPort() { return port; }
}
