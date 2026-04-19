package com.coniv.mait.global.email.util;

public final class HtmlEscapeUtils {

	private HtmlEscapeUtils() {
	}

	public static String escape(final String value) {
		if (value == null) {
			return "";
		}
		return value
			.replace("&", "&amp;")
			.replace("<", "&lt;")
			.replace(">", "&gt;")
			.replace("\"", "&quot;")
			.replace("'", "&#39;");
	}
}
