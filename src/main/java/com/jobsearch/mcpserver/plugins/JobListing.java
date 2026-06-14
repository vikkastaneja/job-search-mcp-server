package com.jobsearch.mcpserver.plugins;

public class JobListing {
    private String title;
    private String company;
    private String location;
    private String description;
    private String url;
    private String source;

    public JobListing(String title, String company, String location, String description, String url, String source) {
        this.title = title;
        this.company = company;
        this.location = location;
        this.description = description;
        this.url = url;
        this.source = source;
    }
    
    public String getTitle() {
        return title;
    }
    
    public String getCompany() {
        return company;
    }
    
    public String getLocation() {
        return location;
    }
    
    public String getDescription() {
        return description;
    }
    
    public String getUrl() {
        return url;
    }
    
    public String getSource() {
        return source;
    }
}
