package com.jobsearch.mcpserver.plugins;

import java.util.List;

public interface JobPlugin {
    List<JobListing> search(String query) throws Exception;
}
