package itdelatrisu.mailserver;

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
				stmt.setString(4, requestUrl);
				stmt.setString(5, redirects.get(i).getHost());
				stmt.setString(6, redirects.get(i).toString());
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
		String senderDomain,
		String senderAddress,
		int recipientId
	) throws SQLException {
		try (
			Connection connection = getConnection();
			PreparedStatement stmt = connection.prepareStatement(
				"INSERT INTO `leaked_emails` VALUES(?, ?, ?, ?, ?, ?, ?)"
			);
		) {
			stmt.setString(1, senderDomain);
			stmt.setString(2, senderAddress);
			stmt.setInt(3, recipientId);
			stmt.setString(4, encoding);
			stmt.setString(5, url);
			stmt.setString(6, type);
			stmt.setBoolean(7, isRedirect);
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
			stmt.setString(3, url);
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
}
