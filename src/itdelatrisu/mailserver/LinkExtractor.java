package itdelatrisu.mailserver;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Link extractor.
 */
public class LinkExtractor {
	private static final Logger logger = LoggerFactory.getLogger(LinkExtractor.class);

	/** All links. */
	private final List<String> links;

	/**
	 * Extracts links from an HTML body.
	 * @param html the HTML body
	 */
	public LinkExtractor(String html) {
		this.links = extractLinksFromHtml(html);
	}

	/**
	 * Extracts links from a MIME message.
	 * @param message the MIME message
	 */
	public LinkExtractor(Part message) throws MessagingException, IOException {
		this.links = extractLinks(message);
	}

	/** Returns all extracted links. */
	public List<String> getLinks() { return links; }

	/** Returns all links contained in the HTML sectionsn of a MIME message. */
	private List<String> extractLinks(Part message) throws MessagingException, IOException {
		// html: parse and return
		if (message.getContentType().startsWith("text/html"))
			return extractLinksFromHtml((String) message.getContent());

		// multipart: recursively check parts for html
		if (message.getContentType().startsWith("multipart/")) {
			List<String> list = new ArrayList<String>();
			Multipart multipart = (Multipart) message.getContent();
			for (int i = 0; i < multipart.getCount(); i++)
				list.addAll(extractLinks(multipart.getBodyPart(i)));
			return list;
		}

		// nothing
		return new ArrayList<String>(0);
	}

	/** Returns all links contained in an HTML body. */
	private List<String> extractLinksFromHtml(String html) {
		List<String> list = new ArrayList<String>();

		// parse document
		Document doc = Jsoup.parse(html);

		// media
		for (Element src : doc.select("[src]")) {
			list.add(src.attr("abs:src"));

			if (src.tagName().equals("img"))
				logger.info("image with size {}x{}: {}", src.attr("width"), src.attr("height"), src.attr("abs:src"));
		}

		// imports
		for (Element link : doc.select("link[href]")) {
			list.add(link.attr("abs:href"));

			if (link.attr("abs:href").endsWith(".css"))
				logger.info("external stylesheet: {}", link.attr("abs:href"));
		}

		// links
		for (Element link : doc.select("a[href]"))
			list.add(link.attr("abs:href"));

		// css
		for (Element css : doc.select("style"))
			list.addAll(extractLinksFromCSS(css.data()));

		return list;
	}

	/** Returns all links contained in a CSS body. */
	private List<String> extractLinksFromCSS(String css) {
		List<String> list = new ArrayList<String>();
		Pattern pattern = Pattern.compile("url\\((?!['\"]?(?:data):)['\"]?([^'\"\\)]*)['\"]?\\)");
		Matcher matcher = pattern.matcher(css);
		while (matcher.find())
			list.add(matcher.group(1));
		return list;
	}
}
