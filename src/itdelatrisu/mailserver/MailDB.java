package itdelatrisu.mailserver;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;

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
}
