package com.referral.outreach.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.referral.outreach.dto.TemplateRequest;
import com.referral.outreach.entity.EmailTemplate;
import com.referral.outreach.repository.TemplateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class TemplateControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TemplateRepository templateRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    public void setup() {
        templateRepository.deleteAll();
    }

    @Test
    public void testCreateTemplate_Success() throws Exception {
        TemplateRequest request = TemplateRequest.builder()
                .templateName("Standard Referral")
                .subject("Referral request for {{roleName}}")
                .body("Hello {{recruiterName}}, I'm interested in joining {{companyName}}.")
                .build();

        mockMvc.perform(post("/api/templates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.templateName", is("Standard Referral")))
                .andExpect(jsonPath("$.subject", is("Referral request for {{roleName}}")));

        assertTrue(templateRepository.existsByTemplateName("Standard Referral"));
    }

    @Test
    public void testCreateTemplate_DuplicateName() throws Exception {
        templateRepository.save(EmailTemplate.builder()
                .templateName("Standard Referral")
                .subject("Referral request")
                .body("Body")
                .build());

        TemplateRequest request = TemplateRequest.builder()
                .templateName("Standard Referral")
                .subject("Another referral request")
                .body("Another Body")
                .build();

        mockMvc.perform(post("/api/templates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", is("Template with name Standard Referral already exists")));
    }

    @Test
    public void testGetTemplateById_NotFound() throws Exception {
        mockMvc.perform(get("/api/templates/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", is("Template not found with ID: 999")));
    }

    @Test
    public void testUpdateTemplate_Success() throws Exception {
        EmailTemplate saved = templateRepository.save(EmailTemplate.builder()
                .templateName("Old Template")
                .subject("Old Subject")
                .body("Old Body")
                .build());

        TemplateRequest request = TemplateRequest.builder()
                .templateName("Updated Template")
                .subject("Updated Subject")
                .body("Updated Body")
                .build();

        mockMvc.perform(put("/api/templates/" + saved.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.templateName", is("Updated Template")))
                .andExpect(jsonPath("$.subject", is("Updated Subject")));
    }

    @Test
    public void testDeleteTemplate_Success() throws Exception {
        EmailTemplate saved = templateRepository.save(EmailTemplate.builder()
                .templateName("Standard Referral")
                .subject("Referral request")
                .body("Body")
                .build());

        mockMvc.perform(delete("/api/templates/" + saved.getId()))
                .andExpect(status().isNoContent());

        assertFalse(templateRepository.existsById(saved.getId()));
    }
}
