package com.referral.outreach.service.impl;

import com.referral.outreach.dto.RecruiterRequest;
import com.referral.outreach.dto.RecruiterResponse;
import com.referral.outreach.entity.Recruiter;
import com.referral.outreach.entity.RecruiterStatus;
import com.referral.outreach.exception.DuplicateRecruiterException;
import com.referral.outreach.exception.ResourceNotFoundException;
import com.referral.outreach.repository.RecruiterRepository;
import com.referral.outreach.service.RecruiterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecruiterServiceImpl implements RecruiterService {

    private final RecruiterRepository recruiterRepository;

    // OWASP Standard Email Validation Regex
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$"
    );

    private boolean isValidEmail(String email) {
        if (email == null) {
            return false;
        }
        return EMAIL_PATTERN.matcher(email.trim()).matches();
    }

    @Override
    @Transactional
    public RecruiterResponse createRecruiter(RecruiterRequest request) {
        log.info("Creating recruiter with email: {}", request.getEmail());
        
        if (!isValidEmail(request.getEmail())) {
            throw new IllegalArgumentException("Invalid email format: " + request.getEmail());
        }

        if (recruiterRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateRecruiterException("Recruiter with email " + request.getEmail() + " already exists");
        }

        Recruiter recruiter = Recruiter.builder()
                .name(request.getName())
                .email(request.getEmail().trim())
                .title(request.getTitle())
                .company(request.getCompany())
                .status(request.getStatus())
                .contactSet(request.getContactSet() != null ? request.getContactSet() : 1)
                .build();

        Recruiter savedRecruiter = recruiterRepository.save(recruiter);
        log.info("Created recruiter successfully with ID: {}", savedRecruiter.getId());
        return mapToResponse(savedRecruiter);
    }

    @Override
    @Transactional
    public RecruiterResponse updateRecruiter(Long id, RecruiterRequest request) {
        log.info("Updating recruiter ID: {}", id);

        if (!isValidEmail(request.getEmail())) {
            throw new IllegalArgumentException("Invalid email format: " + request.getEmail());
        }

        Recruiter recruiter = recruiterRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Recruiter not found with ID: " + id));

        if (recruiterRepository.existsByEmailAndIdNot(request.getEmail(), id)) {
            throw new DuplicateRecruiterException("Another recruiter with email " + request.getEmail() + " already exists");
        }

        recruiter.setName(request.getName());
        recruiter.setEmail(request.getEmail().trim());
        recruiter.setTitle(request.getTitle());
        recruiter.setCompany(request.getCompany());
        recruiter.setStatus(request.getStatus());
        if (request.getContactSet() != null) {
            recruiter.setContactSet(request.getContactSet());
        }

        Recruiter updatedRecruiter = recruiterRepository.save(recruiter);
        log.info("Updated recruiter ID: {} successfully", id);
        return mapToResponse(updatedRecruiter);
    }

    @Override
    @Transactional(readOnly = true)
    public RecruiterResponse getRecruiterById(Long id) {
        log.info("Fetching recruiter ID: {}", id);
        Recruiter recruiter = recruiterRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Recruiter not found with ID: " + id));
        return mapToResponse(recruiter);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RecruiterResponse> getAllRecruiters() {
        log.info("Fetching all recruiters");
        return recruiterRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteRecruiter(Long id) {
        log.info("Deleting recruiter ID: {}", id);
        if (!recruiterRepository.existsById(id)) {
            throw new ResourceNotFoundException("Recruiter not found with ID: " + id);
        }
        recruiterRepository.deleteById(id);
        log.info("Deleted recruiter ID: {} successfully", id);
    }

    @Override
    @Transactional
    public RecruiterResponse updateStatus(Long id, RecruiterStatus status) {
        log.info("Updating status of recruiter ID: {} to {}", id, status);
        Recruiter recruiter = recruiterRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Recruiter not found with ID: " + id));
        
        recruiter.setStatus(status);
        Recruiter updatedRecruiter = recruiterRepository.save(recruiter);
        log.info("Updated status of recruiter ID: {} successfully", id);
        return mapToResponse(updatedRecruiter);
    }

    @Override
    @Transactional
    public int importRecruitersFromCsv(org.springframework.web.multipart.MultipartFile file, Integer setNumber) {
        log.info("Importing recruiters from CSV. Set number: {}", setNumber);
        int count = 0;
        try (BufferedReader reader = new BufferedReader(new java.io.InputStreamReader(file.getInputStream(), java.nio.charset.StandardCharsets.UTF_8))) {
            String line;
            boolean isHeader = true;
            while ((line = reader.readLine()) != null) {
                if (isHeader) {
                    isHeader = false;
                    continue;
                }
                String[] parts = line.split(",");
                if (parts.length >= 4) {
                    String name = parts[0].replaceAll("^\"|\"$", "").trim();
                    String email = parts[1].replaceAll("^\"|\"$", "").trim();
                    String title = parts[2].replaceAll("^\"|\"$", "").trim();
                    String company = parts[3].replaceAll("^\"|\"$", "").trim();

                    if (name.isEmpty() || email.isEmpty()) {
                        continue;
                    }

                    if (!isValidEmail(email)) {
                        log.warn("Skipping import for invalid email format: {}", email);
                        continue;
                    }

                    if (recruiterRepository.existsByEmail(email)) {
                        log.warn("Skipping import for duplicate email: {}", email);
                        continue;
                    }

                    Recruiter recruiter = Recruiter.builder()
                            .name(name)
                            .email(email)
                            .title(title)
                            .company(company)
                            .status(RecruiterStatus.ACTIVE)
                            .contactSet(setNumber != null ? setNumber : 1)
                            .build();

                    recruiterRepository.save(recruiter);
                    count++;
                }
            }
        } catch (Exception e) {
            log.error("Failed to import recruiters from CSV", e);
            throw new RuntimeException("CSV import failed: " + e.getMessage(), e);
        }
        log.info("Imported {} recruiters successfully for set {}", count, setNumber);
        return count;
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportRecruitersToCsv(Integer setNumber) {
        log.info("Exporting recruiters to CSV. Set number filter: {}", setNumber);
        List<Recruiter> list;
        if (setNumber != null && setNumber > 0) {
            list = recruiterRepository.findAll().stream()
                    .filter(r -> (r.getContactSet() != null ? r.getContactSet() : 1) == setNumber)
                    .collect(Collectors.toList());
        } else {
            list = recruiterRepository.findAll();
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Name,Email,Title,Company,Contact Set,Status,Last Contacted\n");
        for (Recruiter r : list) {
            sb.append("\"").append(r.getName() != null ? r.getName().replace("\"", "\"\"") : "").append("\",");
            sb.append("\"").append(r.getEmail() != null ? r.getEmail().replace("\"", "\"\"") : "").append("\",");
            sb.append("\"").append(r.getTitle() != null ? r.getTitle().replace("\"", "\"\"") : "").append("\",");
            sb.append("\"").append(r.getCompany() != null ? r.getCompany().replace("\"", "\"\"") : "").append("\",");
            sb.append(r.getContactSet() != null ? r.getContactSet() : 1).append(",");
            sb.append(r.getStatus() != null ? r.getStatus().name() : "ACTIVE").append(",");
            sb.append("\"").append(r.getLastContactedDate() != null ? r.getLastContactedDate().toString() : "Never").append("\"\n");
        }
        return sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    private RecruiterResponse mapToResponse(Recruiter recruiter) {
        return RecruiterResponse.builder()
                .id(recruiter.getId())
                .name(recruiter.getName())
                .email(recruiter.getEmail())
                .title(recruiter.getTitle())
                .company(recruiter.getCompany())
                .status(recruiter.getStatus())
                .contactSet(recruiter.getContactSet())
                .lastContactedDate(recruiter.getLastContactedDate())
                .build();
    }
}
