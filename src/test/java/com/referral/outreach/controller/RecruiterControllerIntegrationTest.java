package com.referral.outreach.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.referral.outreach.dto.RecruiterRequest;
import com.referral.outreach.entity.Recruiter;
import com.referral.outreach.entity.RecruiterStatus;
import com.referral.outreach.entity.RoleCategory;
import com.referral.outreach.repository.RecruiterRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.hasSize;
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
public class RecruiterControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RecruiterRepository recruiterRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    public void setup() {
        recruiterRepository.deleteAll();
    }

    @Test
    public void testCreateRecruiter_Success() throws Exception {
        RecruiterRequest request = RecruiterRequest.builder()
                .name("Jane Doe")
                .email("jane.doe@example.com")
                .company("Google")
                .roleCategory(RoleCategory.JAVA_BACKEND_DEVELOPER)
                .status(RecruiterStatus.ACTIVE)
                .build();

        mockMvc.perform(post("/api/recruiters")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name", is("Jane Doe")))
                .andExpect(jsonPath("$.email", is("jane.doe@example.com")))
                .andExpect(jsonPath("$.company", is("Google")))
                .andExpect(jsonPath("$.roleCategory", is("JAVA_BACKEND_DEVELOPER")))
                .andExpect(jsonPath("$.status", is("ACTIVE")));

        assertTrue(recruiterRepository.existsByEmail("jane.doe@example.com"));
    }

    @Test
    public void testCreateRecruiter_DuplicateEmail() throws Exception {
        Recruiter existing = Recruiter.builder()
                .name("Jane Doe")
                .email("jane.doe@example.com")
                .company("Google")
                .roleCategory(RoleCategory.JAVA_BACKEND_DEVELOPER)
                .status(RecruiterStatus.ACTIVE)
                .build();
        recruiterRepository.save(existing);

        RecruiterRequest request = RecruiterRequest.builder()
                .name("Jane Smith")
                .email("jane.doe@example.com") // duplicate
                .company("Meta")
                .roleCategory(RoleCategory.SPRING_BOOT_DEVELOPER)
                .status(RecruiterStatus.ACTIVE)
                .build();

        mockMvc.perform(post("/api/recruiters")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error", is("Conflict")))
                .andExpect(jsonPath("$.message", is("Recruiter with email jane.doe@example.com already exists")));
    }

    @Test
    public void testCreateRecruiter_InvalidEmail() throws Exception {
        RecruiterRequest request = RecruiterRequest.builder()
                .name("Jane Doe")
                .email("not-an-email")
                .company("Google")
                .roleCategory(RoleCategory.JAVA_BACKEND_DEVELOPER)
                .status(RecruiterStatus.ACTIVE)
                .build();

        mockMvc.perform(post("/api/recruiters")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("Bad Request")));
    }

    @Test
    public void testGetRecruiterById_NotFound() throws Exception {
        mockMvc.perform(get("/api/recruiters/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error", is("Not Found")))
                .andExpect(jsonPath("$.message", is("Recruiter not found with ID: 999")));
    }

    @Test
    public void testUpdateRecruiter_Success() throws Exception {
        Recruiter saved = recruiterRepository.save(Recruiter.builder()
                .name("Jane Doe")
                .email("jane.doe@example.com")
                .company("Google")
                .roleCategory(RoleCategory.JAVA_BACKEND_DEVELOPER)
                .status(RecruiterStatus.ACTIVE)
                .build());

        RecruiterRequest request = RecruiterRequest.builder()
                .name("Jane Doe Updated")
                .email("jane.doe@example.com")
                .company("Alphabet")
                .roleCategory(RoleCategory.SPRING_BOOT_DEVELOPER)
                .status(RecruiterStatus.INACTIVE)
                .build();

        mockMvc.perform(put("/api/recruiters/" + saved.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Jane Doe Updated")))
                .andExpect(jsonPath("$.company", is("Alphabet")))
                .andExpect(jsonPath("$.status", is("INACTIVE")));
    }

    @Test
    public void testDeleteRecruiter_Success() throws Exception {
        Recruiter saved = recruiterRepository.save(Recruiter.builder()
                .name("Jane Doe")
                .email("jane.doe@example.com")
                .company("Google")
                .roleCategory(RoleCategory.JAVA_BACKEND_DEVELOPER)
                .status(RecruiterStatus.ACTIVE)
                .build());

        mockMvc.perform(delete("/api/recruiters/" + saved.getId()))
                .andExpect(status().isNoContent());

        assertFalse(recruiterRepository.existsById(saved.getId()));
    }

    @Test
    public void testUpdateStatus_Success() throws Exception {
        Recruiter saved = recruiterRepository.save(Recruiter.builder()
                .name("Jane Doe")
                .email("jane.doe@example.com")
                .company("Google")
                .roleCategory(RoleCategory.JAVA_BACKEND_DEVELOPER)
                .status(RecruiterStatus.ACTIVE)
                .build());

        mockMvc.perform(patch("/api/recruiters/" + saved.getId() + "/status")
                        .param("status", "INACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("INACTIVE")));
    }
}
