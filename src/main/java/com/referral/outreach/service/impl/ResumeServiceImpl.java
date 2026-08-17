package com.referral.outreach.service.impl;

import com.referral.outreach.dto.ResumeResponse;
import com.referral.outreach.entity.Resume;
import com.referral.outreach.entity.User;
import com.referral.outreach.exception.InvalidFileException;
import com.referral.outreach.exception.ResourceNotFoundException;
import com.referral.outreach.repository.CampaignRepository;
import com.referral.outreach.repository.ResumeRepository;
import com.referral.outreach.security.SecurityUtils;
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
    private final CampaignRepository campaignRepository;
    private final SecurityUtils securityUtils;

    @Value("${app.upload.dir:./uploads/resumes}")
    private String uploadDir;

    @Override
    @Transactional
    public ResumeResponse uploadResume(MultipartFile file) {
        User user = securityUtils.getAuthenticatedUser();
        if (file.isEmpty()) {
            throw new InvalidFileException("Cannot upload an empty file");
        }

        String contentType = file.getContentType();
        String originalFilename = file.getOriginalFilename();

        log.info("Starting upload for file: {}, Content-Type: {} by user: {}", originalFilename, contentType, user.getUsername());

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
                    .user(user)
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
        User user = securityUtils.getAuthenticatedUser();
        log.info("Fetching resume metadata for ID: {} by user: {}", id, user.getUsername());
        
        Resume resume = resumeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Resume not found with ID: " + id));

        if (!resume.getUser().getId().equals(user.getId())) {
            throw new ResourceNotFoundException("Resume not found with ID: " + id);
        }

        return mapToResponse(resume);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResumeResponse> getAllResumes() {
        User user = securityUtils.getAuthenticatedUser();
        log.info("Fetching all resume metadata for user: {}", user.getUsername());
        return resumeRepository.findByUser(user).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteResume(Long id) {
        User user = securityUtils.getAuthenticatedUser();
        log.info("Deleting resume metadata and file for ID: {} by user: {}", id, user.getUsername());
        
        Resume resume = resumeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Resume not found with ID: " + id));

        if (!resume.getUser().getId().equals(user.getId())) {
            throw new ResourceNotFoundException("Resume not found with ID: " + id);
        }

        if (campaignRepository.existsByUserAndResumeId(user, id)) {
            throw new IllegalArgumentException("Cannot delete resume because it is currently used in one or more campaigns.");
        }

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
        User user = securityUtils.getAuthenticatedUser();
        log.info("Marking resume ID: {} as active for user: {}", id, user.getUsername());
        
        Resume resume = resumeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Resume not found with ID: " + id));

        if (!resume.getUser().getId().equals(user.getId())) {
            throw new ResourceNotFoundException("Resume not found with ID: " + id);
        }

        // Deactivate all others belonging to this user
        resumeRepository.deactivateOthers(id, user.getId());

        resume.setActive(true);
        Resume updated = resumeRepository.save(resume);
        log.info("Resume ID: {} marked active successfully", id);
        return mapToResponse(updated);
    }

    @Override
    @Transactional(readOnly = true)
    public ResumeResponse getActiveResume() {
        User user = securityUtils.getAuthenticatedUser();
        log.info("Fetching active resume for user: {}", user.getUsername());
        Resume resume = resumeRepository.findByUserAndIsActiveTrue(user)
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
