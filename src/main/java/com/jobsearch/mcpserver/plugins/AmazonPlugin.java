package com.jobsearch.mcpserver.plugins;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import com.jobsearch.mcpserver.util.AmazonJobParser;
import com.jobsearch.mcpserver.util.AmazonJobSearchUrlBuilder;

public class AmazonPlugin implements JobPlugin {

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    @Override
    public List<JobListing> search(String query) throws Exception {
        // Implement the logic to search for jobs on Amazon's job portal
        // This is a placeholder implementation and should be replaced with actual scraping or API calls
        List<JobListing> results = new ArrayList<>();
        // results.add(new JobListing("Software Engineer", "Amazon", "Seattle, WA", "Develop and maintain software applications.", "https://www.amazon.jobs/en/jobs/123456/software-engineer", "Amazon"));

        URI requestUri = new AmazonJobSearchUrlBuilder().withBaseQuery(query).buildURI();

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
        return results;
    }
    
    
}
