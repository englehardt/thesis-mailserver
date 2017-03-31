package itdelatrisu.mailserver;

import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.dbcp2.BasicDataSource;

/**
 * Database connection manager.
 */
public class MailDB {
	/** Delimiter for URLs. */
	private static final char URL_DELIMITER = '\r';

	/** Maximum allowed length of a URL. */
	private static final int MAX_URL_LENGTH = 2048;

	/** The data source. */
	private final BasicDataSource dataSource;

	/** Represents a mail user. */
	public class MailUser {
		private final int id;
		private final String email, site, url;
		private final Date ts;

		/** Constructor. */
		public MailUser(int id, String email, String site, String url, Date ts) {
			this.id = id;
			this.email = email;
			this.site = site;
			this.url = url;
			this.ts = ts;
		}

		/** Returns the unique user ID. */
		public int getId() { return id; }

		/** Returns the unique email address. */
		public String getEmail() { return email; }

		/** Returns the registration site title. */
		public String getRegistrationSiteTitle() { return site; }

		/** Returns the registration site URL. */
		public String getRegistrationSiteUrl() { return url; }

		/** Returns the registration date. */
		public Date getRegistrationDate() { return ts; }
	}

	/** Represents a link group. */
	public class LinkGroup {
		private final int id;
		private final String senderDomain, senderAddress;
		private final int recipientId;
		private final String[] urls;

		/** Constructor. */
		public LinkGroup(int id, String senderDomain, String senderAddress, int recipientId, String[] urls) {
			this.id = id;
			this.senderDomain = senderDomain;
			this.senderAddress = senderAddress;
			this.recipientId = recipientId;
			this.urls = urls;
		}

		/** Returns the unique link group ID. */
		public int getId() { return id; }

		/** Returns the sender domain. */
		public String getSenderDomain() { return senderDomain; }

		/** Returns the sender email address. */
		public String getSenderAddress() { return senderAddress; }

		/** Returns the recipient's user ID. */
		public int getRecipientId() { return recipientId; }

		/** Returns the link URLs in this group. */
		public String[] getUrls() { return urls; }
	}

	/** Initializes the connection pool. */
	public MailDB(String driver, String url, String username, String password) {
		dataSource = new BasicDataSource();
		dataSource.setDriverClassName(driver);
		dataSource.setUrl(url);
		dataSource.setUsername(username);
		dataSource.setPassword(password);
	}

	/** Returns a database connection. */
	private Connection getConnection() throws SQLException {
		return dataSource.getConnection();
	}

	/** Truncates the given URL if it is too long. */
	private String truncateUrl(String url) {
		if (url.length() <= MAX_URL_LENGTH)
			return url;

		String marker = "[TRUNCATED]";
		return url.substring(0, MAX_URL_LENGTH - marker.length()) + marker;
	}

	/** Adds a mail entry to the database. */
	public void addMailEntry(
		String recipient,
		String sender,
		Date sentDate,
		String subject,
		String filename
	) throws SQLException {
		try (
			Connection connection = getConnection();
			PreparedStatement stmt = connection.prepareStatement(
				"INSERT INTO `inbox` VALUES(?, ?, ?, ?, ?)"
			);
		) {
			stmt.setString(1, recipient);
			stmt.setString(2, sender);
			stmt.setTimestamp(3, sentDate == null ? null : new Timestamp(sentDate.getTime()));
			stmt.setString(4, subject);
			stmt.setString(5, filename);
			stmt.executeUpdate();
		}
	}

	/** Adds a redirect chain to the database. */
	public synchronized void addRedirects(
		Request req,
		String senderDomain,
		String senderAddress,
		int recipientId
	) throws SQLException {
		if (req.getRedirects().isEmpty())
			return;
		try (
			Connection connection = getConnection();
			PreparedStatement stmt = connection.prepareStatement(
				"INSERT INTO `redirects` VALUES(?, ?, ?, ?, ?, ?, ?)"
			);
		) {
			String requestUrl = req.getURL().toString();
			List<URL> redirects = req.getRedirects();
			for (int i = 0; i < redirects.size(); i++) {
				stmt.setString(1, senderDomain);
				stmt.setString(2, senderAddress);
				stmt.setInt(3, recipientId);
				stmt.setString(4, truncateUrl(requestUrl));
				try {
					stmt.setString(5, Utils.getDomainName(redirects.get(i).toString()));
				} catch (MalformedURLException e) {
					stmt.setString(5, redirects.get(i).getHost());
				}
				stmt.setString(6, truncateUrl(redirects.get(i).toString()));
				stmt.setInt(7, i + 1);
				stmt.executeUpdate();
			}
		}
	}

	/** Adds a URL containing an email address to the database. */
	public void addLeakedEmailAddress(
		String url,
		String type,
		String encoding,
		boolean isRedirect,
		boolean isIntentional,
		String senderDomain,
		String senderAddress,
		int recipientId
	) throws SQLException {
		try (
			Connection connection = getConnection();
			PreparedStatement stmt = connection.prepareStatement(
				"INSERT INTO `leaked_emails` VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?)"
			);
		) {
			stmt.setString(1, senderDomain);
			stmt.setString(2, senderAddress);
			stmt.setInt(3, recipientId);
			stmt.setString(4, encoding);
			stmt.setString(5, truncateUrl(url));
			try {
				stmt.setString(6, Utils.getDomainName(url));
			} catch (MalformedURLException e) {
				stmt.setString(6, "");
			}
			stmt.setString(7, type);
			stmt.setBoolean(8, isRedirect);
			stmt.setBoolean(9, isIntentional);
			stmt.executeUpdate();
		}
	}

	/** Adds a mail user to the database, and returns false if the user already existed. */
	public boolean addMailUser(String email, String site, String url) throws SQLException {
		try (
			Connection connection = getConnection();
			PreparedStatement stmt = connection.prepareStatement(
				"INSERT IGNORE INTO `users` (`email`, `register_site`, `register_url`) VALUES(?, ?, ?)"
			);
		) {
			stmt.setString(1, email);
			stmt.setString(2, site);
			stmt.setString(3, truncateUrl(url));
			int rows = stmt.executeUpdate();
			return rows > 0;
		}
	}

	/** Returns whether the given user exists. */
	public boolean userExists(String email) throws SQLException {
		try (
			Connection connection = getConnection();
			PreparedStatement stmt = connection.prepareStatement(
				"SELECT EXISTS(SELECT 1 FROM `users` WHERE `email` = ?)"
			);
		) {
			stmt.setString(1, email);
			stmt.executeQuery();
			try (ResultSet rs = stmt.executeQuery()) {
				return rs.next() ? rs.getBoolean(1) : false;
			}
		}
	}

	/** Returns user data for the given email address, or null if it does not exist. */
	public MailUser getUserInfo(String email) throws SQLException {
		try (
			Connection connection = getConnection();
			PreparedStatement stmt = connection.prepareStatement(
				"SELECT `id`, `register_site`, `register_url`, `register_time` FROM `users` WHERE `email` = ?"
			);
		) {
			stmt.setString(1, email);
			stmt.executeQuery();
			try (ResultSet rs = stmt.executeQuery()) {
				return (!rs.next()) ? null : new MailUser(rs.getInt(1), email, rs.getString(2), rs.getString(3), rs.getTimestamp(4));
			}
		}
	}

	/** Returns user data for the given user ID, or null if it does not exist. */
	public MailUser getUserInfo(int id) throws SQLException {
		try (
			Connection connection = getConnection();
			PreparedStatement stmt = connection.prepareStatement(
				"SELECT `email`, `register_site`, `register_url`, `register_time` FROM `users` WHERE `id` = ?"
			);
		) {
			stmt.setInt(1, id);
			stmt.executeQuery();
			try (ResultSet rs = stmt.executeQuery()) {
				return (!rs.next()) ? null : new MailUser(id, rs.getString(1), rs.getString(2), rs.getString(3), rs.getTimestamp(4));
			}
		}
	}

	/** Returns a list of all user data. */
	public List<MailUser> getUsers() throws SQLException {
		try (
			Connection connection = getConnection();
			Statement stmt = connection.createStatement();
		) {
			String sql = "SELECT `id`, `email`, `register_site`, `register_url`, `register_time` FROM `users`";
			List<MailUser> users = new ArrayList<MailUser>();
			try (ResultSet rs = stmt.executeQuery(sql)) {
				while (rs.next())
					users.add(new MailUser(rs.getInt(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getTimestamp(5)));
			}
			return users;
		}
	}

	/** Adds a group of links to the database. */
	public void addLinkGroup(
		List<String> urls,
		String senderDomain,
		String senderAddress,
		int recipientId
	) throws SQLException {
		try (
			Connection connection = getConnection();
			PreparedStatement stmt = connection.prepareStatement(
				"INSERT INTO `link_groups` (`sender_domain`, `sender_address`, `recipient_id`, `urls`) VALUES(?, ?, ?, ?)"
			);
		) {
			stmt.setString(1, senderDomain);
			stmt.setString(2, senderAddress);
			stmt.setInt(3, recipientId);
			stmt.setString(4, String.join(Character.toString(URL_DELIMITER), urls));
			stmt.executeUpdate();
		}
	}

	/** Retrieves a random group of links from the database, or null if none exists. */
	public LinkGroup getLinkGroup() throws SQLException {
		try (
			Connection connection = getConnection();
			Statement stmt = connection.createStatement();
		) {
			String sql = "SELECT `id`, `sender_domain`, `sender_address`, `recipient_id`, `urls` FROM `link_groups` ORDER BY RAND() LIMIT 1";
			try (ResultSet rs = stmt.executeQuery(sql)) {
				if (!rs.next())
					return null;
				String[] urls = rs.getString(5).split(Character.toString(URL_DELIMITER));
				return new LinkGroup(rs.getInt(1), rs.getString(2), rs.getString(3), rs.getInt(4), urls);
			}
		}
	}

	/** Retrieves link group data for the given ID from the database, or null if it does not exist. */
	public LinkGroup getLinkGroup(int id) throws SQLException {
		try (
			Connection connection = getConnection();
			PreparedStatement stmt = connection.prepareStatement(
				"SELECT `sender_domain`, `sender_address`, `recipient_id`, `urls` FROM `link_groups` WHERE `id` = ?"
			);
		) {
			stmt.setInt(1, id);
			stmt.executeQuery();
			try (ResultSet rs = stmt.executeQuery()) {
				if (!rs.next())
					return null;
				String[] urls = rs.getString(4).split(Character.toString(URL_DELIMITER));
				return new LinkGroup(id, rs.getString(1), rs.getString(2), rs.getInt(3), urls);
			}
		}
	}

	/** Removes link group data for the given ID from the database. */
	public void removeLinkGroup(int id) throws SQLException {
		try (
			Connection connection = getConnection();
			PreparedStatement stmt = connection.prepareStatement(
				"DELETE FROM `link_groups` WHERE `id` = ?"
			);
		) {
			stmt.setInt(1, id);
			stmt.executeUpdate();
		}
	}
}
