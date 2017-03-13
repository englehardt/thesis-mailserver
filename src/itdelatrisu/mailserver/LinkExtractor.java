package itdelatrisu.mailserver;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/**
 * Link extractor.
 */
public class LinkExtractor {
	/** Image data. */
	public class Image {
		/** Source URL. */
		public final String url;

		/** Dimension attributes. */
		public final String width, height;

		/** Creates a new image. */
		public Image(String url, String width, String height) {
			this.url = url;
			this.width = width;
			this.height = height;
		}
	}

	/** All links. */
	private final List<String> links = new ArrayList<String>();

	/** All inline images (e.g. in 'img' tags). */
	private final List<Image> inlineImages = new ArrayList<Image>();

	/** All images in inline CSS (e.g. in 'url()' values). */
	private final List<String> inlineCssImages = new ArrayList<String>();

	/** All imports (e.g. external stylesheets). */
	private final List<String> imports = new ArrayList<String>();

	/** All inline links (e.g. in 'a' tags). */
	private final List<String> inlineLinks = new ArrayList<String>();

	/** All other media (e.g. with 'src' keys, but not 'img' tags). */
	private final List<String> media = new ArrayList<String>();

	/**
	 * Extracts links from an HTML body.
	 * @param html the HTML body
	 */
	public LinkExtractor(String html) {
		extractLinksFromHtml(html);
	}

	/** Returns all extracted links. */
	public List<String> getAllLinks() { return links; }

	/** Returns all inline images (e.g. in 'img' tags). */
	public List<Image> getInlineImages() { return inlineImages; }

	/** Returns all images in inline CSS (e.g. in 'url()' values). */
	public List<String> getInlineCssImages() { return inlineCssImages; }

	/** Returns all imports (e.g. external stylesheets). */
	public List<String> getImports() { return imports; }

	/** Returns all inline links (e.g. in 'a' tags). */
	public List<String> getInlineLinks() { return inlineLinks; }

	/** Returns all other media (e.g. with 'src' keys, but not 'img' tags). */
	public List<String> getMedia() { return media; }

	/** Finds all links contained in an HTML body. */
	private void extractLinksFromHtml(String html) {
		// parse document
		Document doc = Jsoup.parse(html);

		// media
		for (Element src : doc.select("[src]")) {
			String url = src.attr("abs:src");
			if (!url.startsWith("http"))
				continue;
			if (src.tagName().equals("img")) {
				String width = src.attr("width").trim(), height = src.attr("height").trim();
				inlineImages.add(new Image(url, width, height));
			} else
				media.add(url);
			links.add(url);
		}

		// imports
		for (Element link : doc.select("link[href]")) {
			String url = link.attr("abs:href");
			if (!url.startsWith("http"))
				continue;
			imports.add(url);
			links.add(url);
		}

		// links
		for (Element link : doc.select("a[href]")) {
			String url = link.attr("abs:href");
			if (!url.startsWith("http"))
				continue;
			inlineLinks.add(url);
			links.add(url);
		}

		// css
		for (Element css : doc.select("style")) {
			List<String> cssLinks = extractLinksFromCSS(css.data());
			inlineCssImages.addAll(cssLinks);
			links.addAll(cssLinks);
		}
	}

	/** Returns all links contained in a CSS body. */
	private List<String> extractLinksFromCSS(String css) {
		List<String> list = new ArrayList<String>();
		Pattern pattern = Pattern.compile("url\\((?!['\"]?(?:data):)['\"]?([^'\"\\)]*)['\"]?\\)");
		Matcher matcher = pattern.matcher(css);
		while (matcher.find()) {
			String url = matcher.group(1).trim();
			if (!url.startsWith("http"))
				continue;
			list.add(url);
		}
		return list;
	}
}
