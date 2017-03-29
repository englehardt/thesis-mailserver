package itdelatrisu.mailserver;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import spark.QueryParamsMap;
import spark.Spark;

/**
 * Web server.
 */
public class WebServer {
	private static final Logger logger = LoggerFactory.getLogger(WebServer.class);

	/** Default port. */
	private static final int DEFAULT_PORT = 8080;

	/** The database instance. */
	private final MailDB db;

	/** The email address generator. */
	private final EmailAddressGenerator generator;

	/** The mail server's domain name. */
	private final String domain;

	/** The port. */
	private final int port;

	/** Initializes the web server. */
	public WebServer(MailDB db, String domain) {
		this(db, domain, DEFAULT_PORT);
	}

	/** Initializes the web server. */
	public WebServer(MailDB db, String domain, int port) {
		this.db = db;
		this.generator = new EmailAddressGenerator();
		this.domain = domain;
		this.port = port;
		Spark.port(port);
	}

	/** Starts the server. */
	public void start() {
		Spark.post("/register", this::register);
		Spark.get("/visit", this::visit);
		Spark.post("/results", this::results);
	}

	/** Stops the server. */
	public void stop() { Spark.stop(); }

	/** Returns the server's port. */
	public int getPort() { return port; }

	/**
	 * Registers for a site, creating and returning a new email address.
	 * POST /register : site, url -> email
	 */
	private String register(spark.Request request, spark.Response response) {
		// parse request data
		QueryParamsMap map = request.queryMap();
		String site = map.value("site"), url = map.value("url");
		if (site == null || url == null)
			return badRequest(response);

		logger.info("/register: {} - {}", site, url);

		// generate an email address
		int retries = 3;  // in case generator picks a duplicate
		while (retries-- > 0) {
			String email = generator.generate(domain);
			try {
				if (db.addMailUser(email, site, url)) {
					logger.info("Created new user {}.", email);
					return email;
				}
			} catch (SQLException e) {
				logger.error("Failed to create new user.", e);
				break;
			}
		}
		return internalServerError(response);
	}

	/**
	 * Retrieves a group of URLs to visit.
	 * GET /visit -> {id: int, links: [string...]}
	 */
	private String visit(spark.Request request, spark.Response response) {
		// get a random link group
		MailDB.LinkGroup linkGroup;
		try {
			linkGroup = db.getLinkGroup();
			if (linkGroup == null) {
				response.type("application/json");
				return "{}";
			}
		} catch (SQLException e) {
			logger.error("Failed to retrieve link group.", e);
			return internalServerError(response);
		}

		logger.info("/visit -> ID {} ({} links)", linkGroup.getId(), linkGroup.getUrls().length);

		// encode the data
		JSONObject json = new JSONObject();
		json.put("id", linkGroup.getId());
		json.put("links", new JSONArray(linkGroup.getUrls()));
		response.type("application/json");
		return json.toString();
	}

	/**
	 * Submits all requests generated from a group of URLs (from {@link #visit(spark.Request, spark.Response)}).
	 * POST /results : {id: int, requests: [[url, referrer, post], []...]}
	 */
	private String results(spark.Request request, spark.Response response) {
		// decode request data
		if (request.body().isEmpty())
			return badRequest(response);
		MailDB.LinkGroup linkGroup;
		String[][] urls;
		try {
			JSONObject json = new JSONObject(request.body());
			if (!json.has("id") || !json.has("requests"))
				return badRequest(response);

			// parse the URL list
			JSONArray urlsJson = json.getJSONArray("requests");
			urls = new String[urlsJson.length()][3];
			for (int i = 0; i < urlsJson.length(); i++) {
				JSONArray ar = urlsJson.getJSONArray(i);
				urls[i][0] = ar.getString(0);
				urls[i][1] = ar.isNull(1) ? null : ar.getString(1);
				urls[i][2] = ar.isNull(2) ? null : ar.getString(2);
			}

			// get the link group data
			int id = json.getInt("id");
			linkGroup = db.getLinkGroup(id);
			if (linkGroup == null)
				return badRequest(response);
		} catch (JSONException e) {
			return badRequest(response);
		} catch (SQLException e) {
			return internalServerError(response);
		}

		logger.info("/results: ID {} (received {} results)", linkGroup.getId(), urls.length);

		// get recipient email
		MailDB.MailUser user;
		try {
			user = db.getUserInfo(linkGroup.getRecipientId());
			if (user == null)
				return badRequest(response);
		} catch (SQLException e) {
			return internalServerError(response);
		}

		// check for leaked email address in URLs
		Set<String> baseUrls = new HashSet<String>(Arrays.asList(linkGroup.getUrls()));
		List<HashChecker.NamedValue<String>> encodings = HashChecker.getEncodings(user.getEmail());
		for (String[] urlContainer : urls) {
			String url = urlContainer[0], referrer = urlContainer[1], postBody = urlContainer[2];
			if (baseUrls.contains(url))
				continue;
			try {
				for (HashChecker.NamedValue<String> enc : encodings) {
					String type;
					if (postBody != null && postBody.contains(enc.getValue()))
						type = "link-post";
					else if (url.contains(enc.getValue()))
						type = "link-request";
					else if (referrer != null && referrer.contains(enc.getValue()))
						type = "link-referrer";
					else
						continue;
					db.addLeakedEmailAddress(
						url, type, enc.getName(), true,
						linkGroup.getSenderDomain(), linkGroup.getSenderAddress(), linkGroup.getRecipientId()
					);
				}
			} catch (Exception e) {
				return internalServerError(response);
			}
		}

		// remove the link group
		try {
			db.removeLinkGroup(linkGroup.getId());
		} catch (SQLException e) {
			logger.error("Failed to remove link group.", e);
		}

		return "";
	}

	/** Returns a 400 Bad Request response. */
	private String badRequest(spark.Response response) {
		response.status(400);
		return "<html><body><h2>400 Bad Request</h2></body></html>";
	}

	/** Returns a 500 Internal Server Error response. */
	private String internalServerError(spark.Response response) {
		response.status(500);
		return "<html><body><h2>500 Internal Server Error</h2></body></html>";
	}
}
