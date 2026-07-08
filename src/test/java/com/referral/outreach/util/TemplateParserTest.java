package com.referral.outreach.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TemplateParserTest {

    @Test
    public void testCompile_Success() {
        String template = "Hello {{recruiterName}}, I would like to apply for the {{roleName}} role at {{companyName}}. Regards, {{candidateName}}.";
        String result = TemplateParser.compile(template, "John Doe", "Acme Corp", "Ashish", "Java Backend Developer");
        
        String expected = "Hello John Doe, I would like to apply for the Java Backend Developer role at Acme Corp. Regards, Ashish.";
        assertEquals(expected, result);
    }

    @Test
    public void testCompile_NullValues() {
        String template = "Hello {{recruiterName}}, at {{companyName}}.";
        String result = TemplateParser.compile(template, null, null, null, null);
        
        String expected = "Hello , at .";
        assertEquals(expected, result);
    }

    @Test
    public void testCompile_NoPlaceholders() {
        String template = "Hello world";
        String result = TemplateParser.compile(template, "John", "Acme", "Ashish", "Java");
        
        assertEquals("Hello world", result);
    }

    @Test
    public void testCompile_NullTemplate() {
        String result = TemplateParser.compile(null, "John", "Acme", "Ashish", "Java");
        assertEquals("", result);
    }
}
