package com.jobsearch.mcpserver.tools;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.jobsearch.mcpserver.plugins.AmazonPlugin;
import com.jobsearch.mcpserver.plugins.JobListing;
import com.jobsearch.mcpserver.plugins.JobPlugin;

public class SearchJobsTool {
    public static List<JobListing> execute(String query) {
        // This method is intentionally left blank as the actual search logic is handled by the plugins.
        // The MCP server will route the search query to all registered JobPlugin implementations and aggregate results.
        JobPlugin amazonPlugin = new AmazonPlugin();
        CompletableFuture<List<JobListing>> plugin1 = CompletableFuture.supplyAsync(() -> {
            try {
                return amazonPlugin.search(query);
            } catch (Exception e) {
                System.out.println("Error occurred while searching jobs: " + e.getMessage());
                return List.of(); // Return an empty list in case of error to prevent the entire search from failing
            }
        });

        //List<CompletableFuture<List<JobListing>>> futureList = List.of(plugin1);
        List<JobListing> results = CompletableFuture.allOf(plugin1).thenApply(v -> plugin1.join()).join();

        return results;
    }
}
