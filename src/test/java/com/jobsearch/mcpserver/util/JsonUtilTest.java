package com.jobsearch.mcpserver.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JsonUtilTest {

    public static class TestClass {
        public String name;
        public int age;

        public TestClass() {}

        public TestClass(String name, int age) {
            this.name = name;
            this.age = age;
        }
    }

    @Test
    void testParseValidJson() throws Exception {
        String json = "{\"name\":\"John\",\"age\":30}";
        TestClass result = JsonUtil.fromJson(json, TestClass.class);
        
        assertNotNull(result);
        assertEquals("John", result.name);
        assertEquals(30, result.age);
    }

    @Test
    void testParseInvalidJson() {
        String invalidJson = "{invalid json}";
        
        assertThrows(RuntimeException.class, () -> JsonUtil.fromJson(invalidJson, TestClass.class));
    }

    @Test
    void testParseEmptyJson() throws Exception {
        String json = "{}";
        TestClass result = JsonUtil.fromJson(json, TestClass.class);
        
        assertNotNull(result);
        assertTrue(result instanceof TestClass);
    }
}
