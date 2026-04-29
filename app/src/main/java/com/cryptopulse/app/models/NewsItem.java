package com.cryptopulse.app.models;

public class NewsItem {
    private String id, title, snippet, body;
    private String category, author, timeAgo, readTime;
    private String imageUrl, url;
    private String titleOriginal;
    public String getTitleOriginal()      { return titleOriginal; }
    public void setTitleOriginal(String v){ titleOriginal = v; }
    public NewsItem() {}

    public String getId()       { return id; }
    public void setId(String v) { id = v; }

    public String getTitle()       { return title; }
    public void setTitle(String v) { title = v; }

    public String getSnippet()       { return snippet; }
    public void setSnippet(String v) { snippet = v; }

    public String getBody()       { return body; }
    public void setBody(String v) { body = v; }

    public String getCategory()       { return category; }
    public void setCategory(String v) { category = v; }

    public String getAuthor()       { return author; }
    public void setAuthor(String v) { author = v; }

    public String getTimeAgo()       { return timeAgo; }
    public void setTimeAgo(String v) { timeAgo = v; }

    public String getReadTime()       { return readTime; }
    public void setReadTime(String v) { readTime = v; }

    public String getImageUrl()       { return imageUrl; }
    public void setImageUrl(String v) { imageUrl = v; }

    public String getUrl()       { return url; }
    public void setUrl(String v) { url = v; }
}