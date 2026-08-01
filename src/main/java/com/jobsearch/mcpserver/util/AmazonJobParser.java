package com.jobsearch.mcpserver.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobsearch.mcpserver.plugins.JobListing;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AmazonJobParser {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String SITE_BASE_URL = "https://amazon.jobs";
    private static final String SOURCE_NAME = "Amazon Jobs";

    /**
     * Parses the raw JSON response string from the Amazon Jobs API 
     * and maps it to a list of JobListing domain objects.
     *
     * @param jsonResponse The raw JSON string returned by the API
     * @return A list of JobListing objects, or an empty list if no jobs are found/error occurs.
     */
    public static List<JobListing> parse(String jsonResponse) {
        if (jsonResponse == null || jsonResponse.trim().isEmpty()) {
            return Collections.emptyList();
        }

        List<JobListing> jobListings = new ArrayList<>();

        try {
            // Read the JSON tree structure
            JsonNode rootNode = OBJECT_MAPPER.readTree(jsonResponse);
            JsonNode jobsNode = rootNode.get("jobs");

            // Verify if the "jobs" array node exists and is populated
            if (jobsNode != null && jobsNode.isArray()) {
                for (JsonNode jobNode : jobsNode) {
                    
                    // Extract fields safely with fallbacks for null values
                    String title = getTextNodeValue(jobNode, "title");
                    String company = getTextNodeValue(jobNode, "company_name");
                    String location = getTextNodeValue(jobNode, "normalized_location");
                    String description = getTextNodeValue(jobNode, "description_short");
                    
                    // Construct the absolute canonical job listing URL
                    String jobPath = getTextNodeValue(jobNode, "job_path");
                    String absoluteUrl = jobPath.isEmpty() ? "" : SITE_BASE_URL + "/" + jobPath;

                    // Instantiating the clean target object
                    JobListing listing = new JobListing(
                            title,
                            company,
                            location,
                            description,
                            absoluteUrl,
                            SOURCE_NAME
                    );
                    
                    jobListings.add(listing);
                }
            }
        } catch (IOException e) {
            // Log exception or rethrow custom runtime exception based on your design
            System.err.println("Failed to parse Amazon Job API response: " + e.getMessage());
        }

        return jobListings;
    }

    /**
     * Internal helper to extract text safely, returning an empty string if null.
     */
    private static String getTextNodeValue(JsonNode node, String fieldName) {
        JsonNode field = node.get(fieldName);
        return (field != null && !field.isNull()) ? field.asText() : "";
    }
}