package com.referral.outreach.service.impl;

import com.referral.outreach.dto.ResumeResponse;
import com.referral.outreach.entity.Resume;
import com.referral.outreach.exception.InvalidFileException;
import com.referral.outreach.exception.ResourceNotFoundException;
import com.referral.outreach.repository.ResumeRepository;
import com.referral.outreach.service.ResumeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeServiceImpl implements ResumeService {

    private final ResumeRepository resumeRepository;

    @Value("${app.upload.dir:./uploads/resumes}")
    private String uploadDir;

    @Override
    @Transactional
    public ResumeResponse uploadResume(MultipartFile file) {
        if (file.isEmpty()) {
            throw new InvalidFileException("Cannot upload an empty file");
        }

        String contentType = file.getContentType();
        String originalFilename = file.getOriginalFilename();

        log.info("Starting upload for file: {}, Content-Type: {}", originalFilename, contentType);

        // Validate content type and file extension
        if (contentType == null || !contentType.equalsIgnoreCase("application/pdf")) {
            throw new InvalidFileException("Only PDF files are allowed");
        }

        if (originalFilename == null || !originalFilename.toLowerCase().endsWith(".pdf")) {
            throw new InvalidFileException("Only files with a .pdf extension are allowed");
        }

        try {
            // Ensure target directory exists
            Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
            Files.createDirectories(uploadPath);

            // Generate safe unique filename
            String cleanOriginalName = StringUtils.cleanPath(originalFilename);
            String extension = ".pdf";
            String baseName = cleanOriginalName.substring(0, cleanOriginalName.lastIndexOf('.'));
            String sanitizedBase = baseName.replaceAll("[^a-zA-Z0-9._-]", "_");
            String safeFilename = UUID.randomUUID() + "_" + sanitizedBase + extension;

            Path targetLocation = uploadPath.resolve(safeFilename);
            log.info("Saving file to target location: {}", targetLocation);

            // Copy file to the local directory
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            // Save metadata
            Resume resume = Resume.builder()
                    .filename(safeFilename)
                    .originalFilename(cleanOriginalName)
                    .filePath(targetLocation.toString())
                    .fileSize(file.getSize())
                    .contentType(contentType)
                    .isActive(false) // Not active by default
                    .build();

            Resume savedResume = resumeRepository.save(resume);
            log.info("Saved resume metadata in database with ID: {}", savedResume.getId());

            return mapToResponse(savedResume);
        } catch (IOException ex) {
            log.error("Failed to store file", ex);
            throw new InvalidFileException("Could not store file. Please try again!");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ResumeResponse getResumeById(Long id) {
        log.info("Fetching resume metadata for ID: {}", id);
        Resume resume = resumeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Resume not found with ID: " + id));
        return mapToResponse(resume);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResumeResponse> getAllResumes() {
        log.info("Fetching all resume metadata");
        return resumeRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteResume(Long id) {
        log.info("Deleting resume metadata and file for ID: {}", id);
        Resume resume = resumeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Resume not found with ID: " + id));

        // Delete physical file
        try {
            Path filePath = Paths.get(resume.getFilePath());
            Files.deleteIfExists(filePath);
            log.info("Deleted physical file: {}", filePath);
        } catch (IOException e) {
            log.warn("Failed to delete physical file: {}", resume.getFilePath(), e);
        }

        resumeRepository.delete(resume);
        log.info("Deleted resume ID: {} metadata successfully", id);
    }

    @Override
    @Transactional
    public ResumeResponse markAsActive(Long id) {
        log.info("Marking resume ID: {} as active", id);
        Resume resume = resumeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Resume not found with ID: " + id));

        // Deactivate all others
        resumeRepository.deactivateOthers(id);

        resume.setActive(true);
        Resume updated = resumeRepository.save(resume);
        log.info("Resume ID: {} marked active successfully", id);
        return mapToResponse(updated);
    }

    @Override
    @Transactional(readOnly = true)
    public ResumeResponse getActiveResume() {
        log.info("Fetching active resume");
        Resume resume = resumeRepository.findByIsActiveTrue()
                .orElseThrow(() -> new ResourceNotFoundException("No active resume found"));
        return mapToResponse(resume);
    }

    private ResumeResponse mapToResponse(Resume resume) {
        return ResumeResponse.builder()
                .id(resume.getId())
                .filename(resume.getFilename())
                .originalFilename(resume.getOriginalFilename())
                .fileSize(resume.getFileSize())
                .contentType(resume.getContentType())
                .isActive(resume.isActive())
                .uploadedTimestamp(resume.getUploadedTimestamp())
                .build();
    }
}
