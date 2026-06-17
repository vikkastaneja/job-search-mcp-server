package com.jobsearch.mcpserver.tools;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.jobsearch.mcpserver.plugins.JobListing;
import com.jobsearch.mcpserver.plugins.JobPlugin;

public class SearchJobsTool {
    private final List<JobPlugin> plugins;
    public SearchJobsTool(List<JobPlugin> plugins) {
        this.plugins = plugins;
    }

    public List<JobListing> execute(String query) {
        // The MCP server will route the search query to all registered JobPlugin implementations and aggregate results.
        CompletableFuture<List<JobListing>> pluginResults = CompletableFuture.supplyAsync(() -> {
            try {
                return plugins.stream()
                        .flatMap(plugin -> {
                            try {
                                return plugin.search(query).stream();
                            } catch (Exception e) {
                                System.out.println("Error occurred while searching jobs with plugin " + plugin.getClass().getSimpleName() + ": " + e.getMessage());
                                return List.<JobListing>of().stream(); // Return an empty stream in case of error to prevent the entire search from failing
                            }
                        })
                        .toList();
            } catch (Exception e) {
                System.out.println("Error occurred while executing job search: " + e.getMessage());
                return List.of(); // Return an empty list in case of error to prevent the entire search from failing
            }
        });
        
        List<JobListing> results = pluginResults.join();

        return results;
    }
}
