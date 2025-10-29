package com.cattlescan.systemassistant.util;

public class MarkdownConverter {

    public static String toHtml(String text) {
        if (text == null || text.isEmpty()) return "";

        String html = text;

        // Escape raw < > just in case (safety)
        html = html.replaceAll("<", "&lt;").replaceAll(">", "&gt;");

        // --- Bold: **text**
        html = html.replaceAll("\\*\\*(.+?)\\*\\*", "<b>$1</b>");

        // --- Italic: *text*
        html = html.replaceAll("\\*(.+?)\\*", "<i>$1</i>");
        
        html = html.replaceAll("(?m)^###\\s*(.+)$", "<h3>$1</h3>");

        // --- Inline code: `code`
        html = html.replaceAll("`([^`]+)`", "<code>$1</code>");

        // --- Code block: ```code```
        html = html.replaceAll("```([\\s\\S]+?)```", "<pre><code>$1</code></pre>");

        // --- Links: [title](url)
        html = html.replaceAll("\\[(.+?)\\]\\((https?://.+?)\\)", "<a href=\"$2\" target=\"_blank\">$1</a>");

        // --- Bullet points: "- item"
        html = html.replaceAll("(?m)^\\s*[-•] (.+)", "<li>$1</li>");

        // --- Numbered list: "1. item"
        html = html.replaceAll("(?m)^\\s*\\d+\\. (.+)", "<li>$1</li>");

        // Wrap consecutive <li> into <ul>
        html = html.replaceAll("(<li>.*?</li>)", "<ul>$1</ul>");

        // --- Newlines → <br>
        html = html.replace("\n", "<br>");

        return html;
    }
}
