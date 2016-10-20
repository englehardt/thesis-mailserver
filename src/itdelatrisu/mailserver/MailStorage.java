package itdelatrisu.mailserver;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Storage handler for incoming mail.
 */
public class MailStorage {
	private static final Logger logger = LoggerFactory.getLogger(MailStorage.class);

	private final File mailDir;

	/** Initializes the storage module. */
	public MailStorage() {
		mailDir = new File("mail");
		if (!mailDir.isDirectory() && !mailDir.mkdirs())
			logger.error("Failed to create mail directory '{}'.", mailDir.getAbsolutePath());
	}

	/** Stores the message and returns the newly-created file. */
	public File store(String from, String recipient, String data) {
		// TODO
		String filename = Long.toString(System.currentTimeMillis());
		File file = new File("mail", filename);
		try (Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {
			writer.write(data);
		} catch (IOException e) {
			logger.error("Failed to write email to disk.", e);
		}
		return file;
	}
}
