package com.jobsearch.mcpserver.plugins;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Collections;
import java.util.List;

import com.jobsearch.mcpserver.util.AmazonJobParser;
import com.jobsearch.mcpserver.util.AmazonJobSearchUrlBuilder;

public class AmazonJobSearchService {

    // Shared thread-safe HttpClient optimized with explicit connection configuration settings
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    /**
     * Executes the lifecycle: Assembles target parameters, connects over the network,
     * pulls down the JSON string, and processes it into mapped entity domain models.
     */
    public List<JobListing> fetchSeattleSoftwareManagerJobs() {
        
        // 1. Instantiate and build the target resource locator string using your Builder class
        // Defaults automatically target Greater Seattle Area + Software Development Manager
        URI requestUri = new AmazonJobSearchUrlBuilder().buildURI();

        // 2. Formulate the explicit immutable request object structure
        HttpRequest request = HttpRequest.newBuilder()
                .uri(requestUri)
                .timeout(Duration.ofSeconds(60))
                .header("Accept", "application/json")
                // Adding a realistic User-Agent prevents edge proxies from instantly treating automation as a bad bot
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Java-HttpClient/11")
                .GET()
                .build();

        try {
            // 3. Dispatch the synchronized blocking request network call payload thread 
            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

            // Check if the server responded with an OK status (HTTP 200)
            if (response.statusCode() == 200) {
                String rawJsonBody = response.body();
                
                // 4. Pass the validated payload directly into the parsing engine class layer
                return AmazonJobParser.parse(rawJsonBody);
            } else {
                System.err.println("API request failed. HTTP Status Code: " + response.statusCode());
                System.err.println("Response Payload: " + response.body());
            }

        } catch (IOException e) {
            System.err.println("Network I/O layer failure or transmission disruption occurred: " + e.getMessage());
        } catch (InterruptedException e) {
            System.err.println("Network thread execution context was prematurely halted: " + e.getMessage());
            Thread.currentThread().interrupt(); // Restore interrupted status state handler flag
        }

        return Collections.emptyList();
    }

    // Direct entry execution verification route 
    // public static void main(String[] args) {
        // AmazonJobSearchService service = new AmazonJobSearchService();
        
        // System.out.println("Initiating HTTP connection context to Amazon Jobs API platform...");
        // List<JobListing> activeJobs = service.fetchSeattleSoftwareManagerJobs();
        
        // System.out.println("\n--- Discovery Processing Loop Finished ---");
        // System.out.println("Total mapped domain instances loaded: " + activeJobs.size());
        // System.out.println("------------------------------------------");
        
        // // Output clean structured results extracted straight from the target JSON payload arrays
        // activeJobs.stream().limit(3).forEach(job -> {
        //     System.out.println("Role Title : " + job.getTitle());
        //     System.out.println("Company    : " + job.getCompany());
        //     System.out.println("Location   : " + job.getLocation());
        //     System.out.println("Direct link: " + job.getUrl());
        //     System.out.println("Source Node: " + job.getSource());
        //     System.out.println();
        // });
    // }
}