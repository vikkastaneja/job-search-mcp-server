package com.jobsearch.mcpserver.util;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
public class AmazonJobSearchUrlBuilderTest {

    private static final String BASE_URL = "https://amazon.jobs/en/search.json";

    @Test
    public void defaultBuild_producesDocumentedDefaultUrl() {
        AmazonJobSearchUrlBuilder builder  = new AmazonJobSearchUrlBuilder();
        String url = builder.build();
        assertEquals(BASE_URL + "?radius=24km&offset=0&result_limit=10&sort=recent&loc_group_id=seattle-metro&loc_query=Greater+Seattle+Area%2C+WA%2C+United+States&base_query=Software+Development+Manager", url);        
    }

    @Test
    public void customBuild_withRadius_producesCorrectUrl() {
        AmazonJobSearchUrlBuilder builder  = new AmazonJobSearchUrlBuilder().withRadius("50km");
        String url = builder.build();
        assertEquals(BASE_URL + "?radius=50km&offset=0&result_limit=10&sort=recent&loc_group_id=seattle-metro&loc_query=Greater+Seattle+Area%2C+WA%2C+United+States&base_query=Software+Development+Manager", url);        
    }

    @Test
    public void customBuild_withOffset_producesCorrectUrl() {
        AmazonJobSearchUrlBuilder builder  = new AmazonJobSearchUrlBuilder().withOffset(5);
        String url = builder.build();
        assertEquals(BASE_URL + "?radius=24km&offset=5&result_limit=10&sort=recent&loc_group_id=seattle-metro&loc_query=Greater+Seattle+Area%2C+WA%2C+United+States&base_query=Software+Development+Manager", url);        
    }

    @Test
    public void customBuild_withResultLimit_producesCorrectUrl() {
        AmazonJobSearchUrlBuilder builder  = new AmazonJobSearchUrlBuilder().withResultLimit(20);
        String url = builder.build();
        assertEquals(BASE_URL + "?radius=24km&offset=0&result_limit=20&sort=recent&loc_group_id=seattle-metro&loc_query=Greater+Seattle+Area%2C+WA%2C+United+States&base_query=Software+Development+Manager", url);        
    }

    @Test
    public void customBuild_withSort_producesCorrectUrl() {
        AmazonJobSearchUrlBuilder builder  = new AmazonJobSearchUrlBuilder().withSort("recent");
        String url = builder.build();
        assertEquals(BASE_URL + "?radius=24km&offset=0&result_limit=10&sort=recent&loc_group_id=seattle-metro&loc_query=Greater+Seattle+Area%2C+WA%2C+United+States&base_query=Software+Development+Manager", url);        
    }

    @Test
    public void customBuild_withLocQuery_producesCorrectUrl() {
        AmazonJobSearchUrlBuilder builder  = new AmazonJobSearchUrlBuilder().withLocQuery("Greater Seattle Area, WA, United States");
        String url = builder.build();
        assertEquals(BASE_URL + "?radius=24km&offset=0&result_limit=10&sort=recent&loc_group_id=seattle-metro&loc_query=Greater+Seattle+Area%2C+WA%2C+United+States&base_query=Software+Development+Manager", url);        
    }

    @Test
    public void customBuild_withLocGroupId_producesCorrectUrl() {
        AmazonJobSearchUrlBuilder builder  = new AmazonJobSearchUrlBuilder().withLocGroupId("seattle-metro");
        String url = builder.build();
        assertEquals(BASE_URL + "?radius=24km&offset=0&result_limit=10&sort=recent&loc_group_id=seattle-metro&loc_query=Greater+Seattle+Area%2C+WA%2C+United+States&base_query=Software+Development+Manager", url);        
    }

    @Test
    public void customBuild_withBaseQuery_producesCorrectUrl() {
        AmazonJobSearchUrlBuilder builder  = new AmazonJobSearchUrlBuilder().withBaseQuery("Software Development Manager");
        String url = builder.build();
        assertEquals(BASE_URL + "?radius=24km&offset=0&result_limit=10&sort=recent&loc_group_id=seattle-metro&loc_query=Greater+Seattle+Area%2C+WA%2C+United+States&base_query=Software+Development+Manager", url);        
    }

    @Test
    public void customBuild_buildUri_producesCorrectUrl() {
        AmazonJobSearchUrlBuilder builder  = new AmazonJobSearchUrlBuilder();
        String url = builder.buildURI().toString();
        assertEquals(BASE_URL + "?radius=24km&offset=0&result_limit=10&sort=recent&loc_group_id=seattle-metro&loc_query=Greater+Seattle+Area%2C+WA%2C+United+States&base_query=Software+Development+Manager", url);        
    }
}