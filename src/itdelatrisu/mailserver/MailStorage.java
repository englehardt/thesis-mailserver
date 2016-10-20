package itdelatrisu.mailserver;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Storage handler for incoming mail.
 */
public class MailStorage {
	private static final Logger logger = LoggerFactory.getLogger(MailStorage.class);

	/** List of illegal filename characters. */
	private final static int[] illegalChars = {
		34, 60, 62, 124, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10,
		11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23,
		24, 25, 26, 27, 28, 29, 30, 31, 58, 42, 63, 92, 47
	};
	static { Arrays.sort(illegalChars); }

	/**
	 * Cleans a file name.
	 * @param badFileName the original name string
	 * @param replace the character to replace illegal characters with (or 0 if none)
	 * @return the cleaned file name
	 * @author Sarel Botha (http://stackoverflow.com/a/5626340)
	 */
	private static String cleanFileName(String badFileName, char replace) {
		boolean doReplace = (replace > 0 && Arrays.binarySearch(illegalChars, replace) < 0);
		StringBuilder cleanName = new StringBuilder();
		for (int i = 0, n = badFileName.length(); i < n; i++) {
			int c = badFileName.charAt(i);
			if (Arrays.binarySearch(illegalChars, c) < 0)
				cleanName.append((char) c);
			else if (doReplace)
				cleanName.append(replace);
		}
		return cleanName.toString();
	}

	private final File mailDir;

	/** Initializes the storage module. */
	public MailStorage() {
		mailDir = new File("mail");
		if (!mailDir.isDirectory() && !mailDir.mkdirs())
			logger.error("Failed to create root mail directory '{}'.", mailDir.getAbsolutePath());
	}

	/** Stores the message and returns the newly-created file. */
	public File store(String from, String recipient, String data) {
		// {root_mail_dir}/{recipient}/{timestamp}.eml
		File dir = new File(mailDir, cleanFileName(recipient, '_'));
		if (!dir.isDirectory() && !dir.mkdirs()) {
			logger.error("Failed to create mail directory '{}'.", dir.getAbsolutePath());
			dir = mailDir;
		}
		String filename = String.format("%d.eml", System.currentTimeMillis());
		File file = new File(dir, filename);

		// write contents to file
		try (Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {
			writer.write(data);
		} catch (IOException e) {
			logger.error("Failed to write email to disk.", e);
		}

		return file;
	}
}
