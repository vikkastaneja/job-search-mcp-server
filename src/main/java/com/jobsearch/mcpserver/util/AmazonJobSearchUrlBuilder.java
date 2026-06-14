package com.jobsearch.mcpserver.util;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class AmazonJobSearchUrlBuilder {

    private static final String BASE_URL = "https://amazon.jobs/en/search.json";

    // Instance variables matching the query parameters
    private String radius = "24km";
    private int offset = 0;
    private int resultLimit = 10;
    private String sort = "recent";
    private String locGroupId = "seattle-metro";
    private String locQuery = "Greater Seattle Area, WA, United States";
    private String baseQuery = "Software Development Manager";

    // Private constructor to force the use of the builder/fluent methods
    public AmazonJobSearchUrlBuilder() {
    }

    // Fluent setter methods for configurability
    public AmazonJobSearchUrlBuilder withRadius(String radius) {
        this.radius = radius;
        return this;
    }

    public AmazonJobSearchUrlBuilder withOffset(int offset) {
        this.offset = offset;
        return this;
    }

    public AmazonJobSearchUrlBuilder withResultLimit(int resultLimit) {
        this.resultLimit = resultLimit;
        return this;
    }

    public AmazonJobSearchUrlBuilder withSort(String sort) {
        this.sort = sort;
        return this;
    }

    public AmazonJobSearchUrlBuilder withLocGroupId(String locGroupId) {
        this.locGroupId = locGroupId;
        return this;
    }

    public AmazonJobSearchUrlBuilder withLocQuery(String locQuery) {
        this.locQuery = locQuery;
        return this;
    }

    public AmazonJobSearchUrlBuilder withBaseQuery(String baseQuery) {
        this.baseQuery = baseQuery;
        return this;
    }

    /**
     * Builds the final URL string with safely encoded query parameters.
     */
    public String build() {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("radius", this.radius);
        params.put("offset", String.valueOf(this.offset));
        params.put("result_limit", String.valueOf(this.resultLimit));
        params.put("sort", this.sort);
        params.put("loc_group_id", this.locGroupId);
        params.put("loc_query", this.locQuery);
        params.put("base_query", this.baseQuery);

        // Map the parameters to a "key=value" format and encode the values safely
        String queryString = params.entrySet().stream()
                .map(entry -> entry.getKey() + "=" + URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));

        return BASE_URL + "?" + queryString;
    }

    /**
     * Builds and returns a URI object instead of a raw String.
     */
    public URI buildURI() {
        return URI.create(this.build());
    }

    // Example Usage
    // public static void main(String[] args) {
    //     // 1. Generate the exact default URL you provided
    //     AmazonJobSearchUrlBuilder defaultSearch = new AmazonJobSearchUrlBuilder();
    //     System.out.println("Default URL:");
    //     System.out.println(defaultSearch.build());
    //     System.out.println();

    //     // 2. Generate a custom URL modifying only a few parameters (e.g., pagination or role)
    //     String customUrl = new AmazonJobSearchUrlBuilder()
    //             .withBaseQuery("Senior Software Engineer")
    //             .withOffset(25) // Next page
    //             .withResultLimit(50)
    //             .build();

    //     System.out.println("Customized URL:");
    //     System.out.println(customUrl);
    // }
}
