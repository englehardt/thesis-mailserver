package itdelatrisu.mailserver;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Properties;
import java.util.Scanner;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;

/**
 * Utility methods.
 */
public class Utils {
	/** List of illegal filename characters. */
	private final static int[] illegalChars = {
		34, 60, 62, 124, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10,
		11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23,
		24, 25, 26, 27, 28, 29, 30, 31, 58, 42, 63, 92, 47
	};
	static { Arrays.sort(illegalChars); }

	private Utils() {}

	/**
	 * Cleans a file name.
	 * @param badFileName the original name string
	 * @param replace the character to replace illegal characters with (or 0 if none)
	 * @return the cleaned file name
	 * @author Sarel Botha (http://stackoverflow.com/a/5626340)
	 */
	public static String cleanFileName(String badFileName, char replace) {
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

	/** Reads the input stream and returns the data as a string. */
	public static String streamToString(InputStream is) {
		try (Scanner s = new Scanner(is)) {
			return s.useDelimiter("\\A").hasNext() ? s.next() : "";
		}
	}

	/** Parses mail data into a MimeMessage. */
	public static MimeMessage toMimeMessage(String content) throws MessagingException {
		Session s = Session.getDefaultInstance(new Properties());
		InputStream is = new ByteArrayInputStream(content.getBytes());
		return new MimeMessage(s, is);
	}

	/** Returns a string representation of the MIME message. */
	public static String messageToString(MimeMessage message)
		throws IOException, MessagingException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		message.writeTo(baos);
		return baos.toString(StandardCharsets.UTF_8.name());
	}
}
