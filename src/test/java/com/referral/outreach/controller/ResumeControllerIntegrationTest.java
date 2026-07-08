package com.referral.outreach.controller;

import com.referral.outreach.entity.Resume;
import com.referral.outreach.repository.ResumeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

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
public class ResumeControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ResumeRepository resumeRepository;

    @BeforeEach
    public void setup() {
        resumeRepository.deleteAll();
    }

    @Test
    public void testUploadResume_Success() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "my_resume.pdf",
                "application/pdf",
                "Dummy PDF content".getBytes()
        );

        mockMvc.perform(multipart("/api/resumes")
                        .file(file))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.originalFilename", is("my_resume.pdf")))
                .andExpect(jsonPath("$.contentType", is("application/pdf")))
                .andExpect(jsonPath("$.active", is(false)));

        // Verify file stored in DB
        assertFalse(resumeRepository.findAll().isEmpty());
        Resume uploaded = resumeRepository.findAll().get(0);
        
        // Cleanup physical file generated during test
        Files.deleteIfExists(Paths.get(uploaded.getFilePath()));
    }

    @Test
    public void testUploadResume_InvalidExtension() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "my_resume.txt",
                "text/plain",
                "Dummy content".getBytes()
        );

        mockMvc.perform(multipart("/api/resumes")
                        .file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Only PDF files are allowed")));
    }

    @Test
    public void testUploadResume_InvalidContentType() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "my_resume.pdf",
                "text/plain", // Incorrect mime type
                "Dummy content".getBytes()
        );

        mockMvc.perform(multipart("/api/resumes")
                        .file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Only PDF files are allowed")));
    }

    @Test
    public void testMarkAsActive_Success() throws Exception {
        Resume resume1 = resumeRepository.save(Resume.builder()
                .filename("file1.pdf")
                .originalFilename("file1.pdf")
                .filePath("./uploads/file1.pdf")
                .fileSize(100L)
                .contentType("application/pdf")
                .isActive(true)
                .build());

        Resume resume2 = resumeRepository.save(Resume.builder()
                .filename("file2.pdf")
                .originalFilename("file2.pdf")
                .filePath("./uploads/file2.pdf")
                .fileSize(200L)
                .contentType("application/pdf")
                .isActive(false)
                .build());

        mockMvc.perform(patch("/api/resumes/" + resume2.getId() + "/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active", is(true)));

        // Verify in DB that state toggled
        Resume updated1 = resumeRepository.findById(resume1.getId()).get();
        Resume updated2 = resumeRepository.findById(resume2.getId()).get();

        assertFalse(updated1.isActive());
        assertTrue(updated2.isActive());
    }

    @Test
    public void testGetActiveResume_NotFound() throws Exception {
        mockMvc.perform(get("/api/resumes/active"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", is("No active resume found")));
    }

    @Test
    public void testGetActiveResume_Success() throws Exception {
        resumeRepository.save(Resume.builder()
                .filename("active.pdf")
                .originalFilename("active.pdf")
                .filePath("./uploads/active.pdf")
                .fileSize(100L)
                .contentType("application/pdf")
                .isActive(true)
                .build());

        mockMvc.perform(get("/api/resumes/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.originalFilename", is("active.pdf")))
                .andExpect(jsonPath("$.active", is(true)));
    }
}
