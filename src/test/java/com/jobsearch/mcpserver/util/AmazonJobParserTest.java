package com.jobsearch.mcpserver.util;

import org.junit.jupiter.api.Test;

import com.jobsearch.mcpserver.plugins.JobListing;

import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.util.List;

public class AmazonJobParserTest {

    private AmazonJobParser amazonJobParser;

    @BeforeEach
    public void setUp() {
        amazonJobParser = new AmazonJobParser();
    }

    @Test
    public void testParseValidJobData() {
        // Test parsing valid job data
        String jobData = "{\"jobs\":[{\"jobId\":\"123\",\"title\":\"Software Engineer\",\"normalized_location\":\"Seattle\",\"description_short\":\"Develop and maintain software\", \"job_path\":\"dummy_path\"}]}";
        List<JobListing> jobListings = amazonJobParser.parse(jobData);
        assertNotNull(jobListings);
        assertEquals(1, jobListings.size());
        JobListing job = jobListings.get(0);
        assertEquals("Software Engineer", job.getTitle());
        assertEquals("Seattle", job.getLocation());
        assertEquals("Develop and maintain software", job.getDescription());
        assertEquals("https://amazon.jobs/dummy_path", job.getUrl());
    }

    @Test
    public void testParseValidJobDataWithEmptyJobPath() {
        // Test parsing valid job data
        String jobData = "{\"jobs\":[{\"jobId\":\"123\",\"title\":\"Software Engineer\",\"normalized_location\":\"Seattle\",\"description_short\":\"Develop and maintain software\"}]}";
        List<JobListing> jobListings = amazonJobParser.parse(jobData);
        assertNotNull(jobListings);
        assertEquals(1, jobListings.size());
        JobListing job = jobListings.get(0);
        assertEquals("Software Engineer", job.getTitle());
        assertEquals("Seattle", job.getLocation());
        assertEquals("Develop and maintain software", job.getDescription());
        assertEquals("", job.getUrl());
    }

    @Test
    public void testParseNullJobData() {
        // Test parsing null input
        // assertThrows(IllegalArgumentException.class, () -> {
        //     amazonJobParser.parse(null);
        // });
    }

    @Test
    public void testParseEmptyInputString() {
        // Test parsing empty string
        assertEquals(0, amazonJobParser.parse("").size());
    }

    @Test
    public void testParseEmptyJobList() {
        // Test parsing empty job list
        assertEquals(0, amazonJobParser.parse("{\"jobs\":[]}").size());
    }

    @Test
    public void testParseNullInputString() {
        // Test parsing null input
        assertEquals(0, amazonJobParser.parse(null).size());
    }

    @Test
    public void testParseMalFormedJson() {
        // Test parsing invalid JSON
        
        List<JobListing> jobListings = amazonJobParser.parse("This is invlid JSON");
        assertEquals(0, jobListings.size());
    }

    @Test
    public void testParseJobsNodeNotArray() {
        // Test parsing invalid JSON
        
        List<JobListing> jobListings = amazonJobParser.parse("{\"jobs\": {\"jobId\":\"123\",\"title\":\"Software Engineer\"}}");
        assertEquals(0, jobListings.size());
    }

    @Test
    public void testParseJobTitleEmpty() {
        // Test parsing invalid JSON
        
        List<JobListing> jobListings = amazonJobParser.parse("{\"jobs\": [{\"jobId\":\"123\",\"title\":\"\"}]}");
        assertEquals(1, jobListings.size());
        JobListing job = jobListings.get(0);
        assertEquals("", job.getTitle());
    }

    @Test
    public void testParseJobEmptyFields() {
        // Test parsing invalid JSON
        
        List<JobListing> jobListings = amazonJobParser.parse("{\"jobs\": [{\"\":\"\",\"title\":\"\"}]}");
        assertEquals(1, jobListings.size());
        JobListing job = jobListings.get(0);
        assertEquals("", job.getTitle());
    }

    @Test
    public void testParseNoJobsNode() {
        // Test parsing invalid JSON
        
        List<JobListing> jobListings = amazonJobParser.parse("{\"someOtherNode\": []}");
        assertEquals(0, jobListings.size());
    }
}
