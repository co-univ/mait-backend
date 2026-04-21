package com.coniv.mait.global.email.template;

public final class EmailLayoutTemplate {

	private static final String HEADER = """
		<!DOCTYPE html>
		<html lang="ko">
		<head>
		  <meta charset="UTF-8">
		  <meta name="viewport" content="width=device-width, initial-scale=1.0">
		  <title>MAIT</title>
		</head>
		<body style="margin:0;padding:0;background-color:#f4f5f6;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI','Pretendard','Apple SD Gothic Neo','Malgun Gothic',sans-serif;color:#1e2124;">
		  <table role="presentation" cellpadding="0" cellspacing="0" border="0" width="100%%" style="background-color:#f4f5f6;padding:32px 16px;">
		    <tr>
		      <td align="center">
		        <table role="presentation" cellpadding="0" cellspacing="0" border="0" width="100%%" style="max-width:600px;background-color:#ffffff;border-radius:12px;overflow:hidden;box-shadow:0 1px 2px rgba(19,20,22,0.06);">
		          <tr>
		            <td style="padding:28px 32px;border-bottom:1px solid #e6e8ea;">
		              <span style="display:inline-block;font-size:22px;font-weight:800;letter-spacing:-0.02em;color:#256ef4;">MAIT</span>
		            </td>
		          </tr>
		          <tr>
		            <td style="padding:32px;">
		""";

	private static final String FOOTER = """
		            </td>
		          </tr>
		          <tr>
		            <td style="padding:24px 32px;background-color:#fafbfc;border-top:1px solid #e6e8ea;">
		              <p style="margin:0 0 8px 0;font-size:12px;line-height:1.6;color:#8a949e;">
		                본 메일은 발신 전용으로, 회신은 처리되지 않습니다.
		              </p>
		              <p style="margin:0;font-size:12px;line-height:1.6;color:#8a949e;">
		                &copy; MAIT. All rights reserved.
		              </p>
		            </td>
		          </tr>
		        </table>
		      </td>
		    </tr>
		  </table>
		</body>
		</html>
		""";

	private EmailLayoutTemplate() {
	}

	public static String subject(final String content) {
		return "[MAIT] " + content;
	}

	public static String wrap(final String contentHtml) {
		return HEADER + contentHtml + FOOTER;
	}
}
