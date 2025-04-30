package com.textsummarizer.util;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import java.io.IOException;

public class WebScraper {
    
    public static String extractTextFromUrl(String url) throws IOException {
        Document doc = Jsoup.connect(url)
            .userAgent("Mozilla/5.0")
            .timeout(10000)
            .get();

        // Remove unwanted elements
        doc.select("script, style, nav, footer, header, aside").remove();

        // Get the main content
        Elements paragraphs = doc.select("p, h1, h2, h3, h4, h5, h6");
        
        StringBuilder content = new StringBuilder();
        paragraphs.forEach(element -> {
            content.append(element.text()).append("\n\n");
        });

        return content.toString().trim();
    }
} 