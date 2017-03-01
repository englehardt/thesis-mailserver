package itdelatrisu.mailserver;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;

import org.apache.commons.dbcp2.BasicDataSource;

/**
 * Database connection manager.
 */
public class MailDB {
	private final BasicDataSource dataSource;

	/** Initializes the connection pool. */
	public MailDB() {
		dataSource = new BasicDataSource();
		dataSource.setDriverClassName("com.mysql.jdbc.Driver");
		dataSource.setUrl("jdbc:mysql://localhost:3306/mail");
		dataSource.setUsername("mailserver");
		dataSource.setPassword("S6TTAykTfAEMJjqN");
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
}
