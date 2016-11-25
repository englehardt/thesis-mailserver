package itdelatrisu.mailserver;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;

import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.internet.MimeMessage;

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
			logger.error("Failed to create root mail directory '{}'.", mailDir.getAbsolutePath());
	}

	/** Stores the message and returns the newly-created file. */
	public File store(String from, String recipient, String data) {
		// {root_mail_dir}/{recipient}/{timestamp}.eml
		File dir = new File(mailDir, Utils.cleanFileName(recipient, '_'));
		if (!dir.isDirectory() && !dir.mkdirs()) {
			logger.error("Failed to create mail directory '{}'.", dir.getAbsolutePath());
			dir = mailDir;
		}
		String filename = String.format("%d.eml", System.currentTimeMillis());
		File file = new File(dir, filename);

		// write contents to file
		try (Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {
			writer.write(stripAttachments(data));
		} catch (IOException e) {
			logger.error("Failed to write email to disk.", e);
		}

		return file;
	}

	/** Strips attachments in the given message. */
	private String stripAttachments(String data) {
		try {
			// parse MIME message
			MimeMessage message = Utils.toMimeMessage(data);
			Object content = message.getContent();
			if (!(content instanceof Multipart))
				return data;  // not a multipart message

			// strip attachments
			Multipart multipart = (Multipart) content;
			if (stripAttachments(multipart)) {
				message.setContent(multipart);
				message.saveChanges();
				return Utils.messageToString(message);
			} else
				return data;  // content unmodified
		} catch (MessagingException | IOException e) {
			logger.error("Error while stripping attachments.", e);
			return data;
		}
	}

	/**
	 * Recursively strips attachments from a multipart message,
	 * and returns whether the message was modified.
	 */
	private boolean stripAttachments(Multipart multipart) throws MessagingException, IOException {
		boolean modified = false;
		for (int i = multipart.getCount() - 1; i >= 0; i--) {
			Part part = multipart.getBodyPart(i);
			String contentType = part.getContentType();
			if (contentType.startsWith("multipart/")) {
				if (stripAttachments((Multipart) part.getContent()))
					modified = true;
			} else if (discardMimeType(contentType)) {
				modified = true;
				multipart.removeBodyPart(i);
			}
		}
		return modified;
	}

	/** Returns whether to discard content with this MIME type. */
	private boolean discardMimeType(String contentType) {
		return !contentType.startsWith("text/");
	}
}
