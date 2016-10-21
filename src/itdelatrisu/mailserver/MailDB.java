package itdelatrisu.mailserver;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.apache.commons.dbcp2.BasicDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Database connection manager.
 */
public class MailDB {
	private static final Logger logger = LoggerFactory.getLogger(MailDB.class);

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
		java.util.Date sentDate,
		String subject,
		String affiliation,
		boolean isSpam,
		String filename
	) throws SQLException {
		try (
			Connection connection = getConnection();
			PreparedStatement stmt = connection.prepareStatement(
				"INSERT INTO `inbox` VALUES(?, ?, ?, ?, ?, ?, ?)"
			);
		) {
			stmt.setString(1, recipient);
			stmt.setString(2, sender);
			stmt.setDate(3, sentDate == null ? null : new Date(sentDate.getTime()));
			stmt.setString(4, subject);
			stmt.setString(5, affiliation);
			stmt.setBoolean(6, isSpam);
			stmt.setString(7, filename);
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
			if (rows > 0) {
				logger.error("Created user {} for site '{}' at URL '{}'.", email, site, url);
				return true;
			} else
				return false;
		}
	}
}
