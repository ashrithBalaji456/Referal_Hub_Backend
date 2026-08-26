package com.referral.outreach.service.impl;

import com.referral.outreach.dto.RecruiterRequest;
import com.referral.outreach.dto.RecruiterResponse;
import com.referral.outreach.entity.Recruiter;
import com.referral.outreach.entity.RecruiterStatus;
import com.referral.outreach.entity.User;
import com.referral.outreach.entity.UserRecruiterState;
import com.referral.outreach.exception.DuplicateRecruiterException;
import com.referral.outreach.exception.ResourceNotFoundException;
import com.referral.outreach.repository.RecruiterRepository;
import com.referral.outreach.repository.UserRecruiterStateRepository;
import com.referral.outreach.security.SecurityUtils;
import com.referral.outreach.service.RecruiterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecruiterServiceImpl implements RecruiterService {

    private final RecruiterRepository recruiterRepository;
    private final UserRecruiterStateRepository userRecruiterStateRepository;
    private final SecurityUtils securityUtils;

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

    private User getOptionalCurrentUser() {
        try {
            return securityUtils.getAuthenticatedUser();
        } catch (Exception e) {
            return null;
        }
    }

    private Map<Long, UserRecruiterState> getUserStateMap(User user) {
        if (user == null) {
            return Collections.emptyMap();
        }
        return userRecruiterStateRepository.findByUser(user).stream()
                .collect(Collectors.toMap(s -> s.getRecruiter().getId(), s -> s, (s1, s2) -> s1));
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

        User currentUser = getOptionalCurrentUser();
        boolean isPublic = request.getIsPublic() != null ? request.getIsPublic() : true;

        Recruiter recruiter = Recruiter.builder()
                .name(request.getName())
                .email(request.getEmail().trim())
                .title(request.getTitle())
                .company(request.getCompany())
                .status(request.getStatus() != null ? request.getStatus() : RecruiterStatus.ACTIVE)
                .contactSet(request.getContactSet() != null ? request.getContactSet() : 1)
                .isPublic(isPublic)
                .addedBy(currentUser)
                .build();

        Recruiter savedRecruiter = recruiterRepository.save(recruiter);
        log.info("Created recruiter successfully with ID: {} (Public: {})", savedRecruiter.getId(), isPublic);

        UserRecruiterState userState = null;
        if (currentUser != null) {
            userState = UserRecruiterState.builder()
                    .user(currentUser)
                    .recruiter(savedRecruiter)
                    .status(request.getStatus() != null ? request.getStatus() : RecruiterStatus.ACTIVE)
                    .customTitle(request.getTitle())
                    .customCompany(request.getCompany())
                    .isAdded(true)
                    .build();
            userState = userRecruiterStateRepository.save(userState);
        }

        return mapToResponse(savedRecruiter, userState);
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

        // Core shared attributes (Name and Email) are updated globally
        recruiter.setName(request.getName());
        recruiter.setEmail(request.getEmail().trim());
        if (request.getContactSet() != null) {
            recruiter.setContactSet(request.getContactSet());
        }
        if (request.getIsPublic() != null) {
            recruiter.setIsPublic(request.getIsPublic());
        }

        Recruiter updatedRecruiter = recruiterRepository.save(recruiter);

        // Per-user overrides (Status, Title, Company) are saved privately to UserRecruiterState
        User currentUser = getOptionalCurrentUser();
        UserRecruiterState userState = null;
        if (currentUser != null) {
            userState = userRecruiterStateRepository.findByUserAndRecruiter(currentUser, updatedRecruiter)
                    .orElseGet(() -> UserRecruiterState.builder()
                            .user(currentUser)
                            .recruiter(updatedRecruiter)
                            .status(RecruiterStatus.ACTIVE)
                            .isAdded(true)
                            .build());

            if (request.getStatus() != null) {
                userState.setStatus(request.getStatus());
            }
            userState.setCustomTitle(request.getTitle());
            userState.setCustomCompany(request.getCompany());
            userState.setIsAdded(true);
            userState = userRecruiterStateRepository.save(userState);
        }

        log.info("Updated recruiter ID: {} successfully", id);
        return mapToResponse(updatedRecruiter, userState);
    }

    @Override
    @Transactional(readOnly = true)
    public RecruiterResponse getRecruiterById(Long id) {
        log.info("Fetching recruiter ID: {}", id);
        Recruiter recruiter = recruiterRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Recruiter not found with ID: " + id));
        
        User currentUser = getOptionalCurrentUser();
        UserRecruiterState userState = currentUser != null ? 
                userRecruiterStateRepository.findByUserAndRecruiter(currentUser, recruiter).orElse(null) : null;

        return mapToResponse(recruiter, userState);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RecruiterResponse> getAllRecruiters() {
        log.info("Fetching all active recruiters for current user");
        User currentUser = getOptionalCurrentUser();
        if (currentUser == null) {
            return recruiterRepository.findAll().stream()
                    .map(r -> mapToResponse(r, null))
                    .collect(Collectors.toList());
        }

        Map<Long, UserRecruiterState> stateMap = getUserStateMap(currentUser);
        List<Recruiter> allRecruiters = recruiterRepository.findAll();

        return allRecruiters.stream()
                .filter(r -> {
                    UserRecruiterState state = stateMap.get(r.getId());
                    // 1. Explicit per-user state override
                    if (state != null) {
                        return Boolean.TRUE.equals(state.getIsAdded());
                    }

                    // 2. Initial System/Seeded recruiters (addedBy == null) are added by default for all users
                    if (r.getAddedBy() == null) {
                        return true;
                    }

                    // 3. User-created recruiters are automatically added for the creator
                    if (r.getAddedBy().getId().equals(currentUser.getId())) {
                        return true;
                    }

                    return false;
                })
                .map(r -> mapToResponse(r, stateMap.get(r.getId())))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<RecruiterResponse> getWaitingRecruiters() {
        log.info("Fetching waiting public recruiters for current user");
        User currentUser = getOptionalCurrentUser();
        if (currentUser == null) {
            return Collections.emptyList();
        }

        // Only user-created public recruiters (addedBy != null) go to waiting for other users
        List<Recruiter> publicRecruiters = recruiterRepository.findAll().stream()
                .filter(r -> Boolean.TRUE.equals(r.getIsPublic()) && r.getAddedBy() != null)
                .collect(Collectors.toList());

        Map<Long, UserRecruiterState> stateMap = getUserStateMap(currentUser);

        return publicRecruiters.stream()
                .filter(r -> {
                    // Exclude if created by current user
                    if (r.getAddedBy().getId().equals(currentUser.getId())) {
                        return false;
                    }
                    UserRecruiterState state = stateMap.get(r.getId());
                    // Exclude if state exists for current user (already added or dismissed)
                    if (state != null) {
                        return false;
                    }
                    return true;
                })
                .map(r -> mapToResponse(r, stateMap.get(r.getId())))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public List<RecruiterResponse> addWaitingRecruiters(List<Long> recruiterIds) {
        User currentUser = getOptionalCurrentUser();
        if (currentUser == null || recruiterIds == null || recruiterIds.isEmpty()) {
            return getAllRecruiters();
        }

        log.info("User {} adding {} waiting recruiter(s) to personal list", currentUser.getUsername(), recruiterIds.size());
        final User targetUser = currentUser;

        for (Long recruiterId : recruiterIds) {
            Recruiter recruiter = recruiterRepository.findById(recruiterId).orElse(null);
            if (recruiter != null) {
                final Recruiter targetRecruiter = recruiter;
                UserRecruiterState state = userRecruiterStateRepository.findByUserAndRecruiter(targetUser, targetRecruiter)
                        .orElseGet(() -> UserRecruiterState.builder()
                                .user(targetUser)
                                .recruiter(targetRecruiter)
                                .status(RecruiterStatus.ACTIVE)
                                .build());

                state.setIsAdded(true);
                userRecruiterStateRepository.save(state);
            }
        }

        return getAllRecruiters();
    }

    @Override
    @Transactional
    public List<RecruiterResponse> addAllWaitingRecruiters() {
        List<RecruiterResponse> waiting = getWaitingRecruiters();
        List<Long> ids = waiting.stream().map(RecruiterResponse::getId).collect(Collectors.toList());
        return addWaitingRecruiters(ids);
    }

    @Override
    @Transactional
    public List<RecruiterResponse> dismissWaitingRecruiters(List<Long> recruiterIds) {
        User currentUser = getOptionalCurrentUser();
        if (currentUser == null || recruiterIds == null || recruiterIds.isEmpty()) {
            return getWaitingRecruiters();
        }

        log.info("User {} dismissing {} waiting recruiter(s)", currentUser.getUsername(), recruiterIds.size());
        final User targetUser = currentUser;

        for (Long recruiterId : recruiterIds) {
            Recruiter recruiter = recruiterRepository.findById(recruiterId).orElse(null);
            if (recruiter != null) {
                final Recruiter targetRecruiter = recruiter;
                UserRecruiterState state = userRecruiterStateRepository.findByUserAndRecruiter(targetUser, targetRecruiter)
                        .orElseGet(() -> UserRecruiterState.builder()
                                .user(targetUser)
                                .recruiter(targetRecruiter)
                                .status(RecruiterStatus.INACTIVE)
                                .build());

                state.setIsAdded(false);
                userRecruiterStateRepository.save(state);
            }
        }

        return getWaitingRecruiters();
    }

    @Override
    @Transactional
    public List<RecruiterResponse> dismissAllWaitingRecruiters() {
        List<RecruiterResponse> waiting = getWaitingRecruiters();
        List<Long> ids = waiting.stream().map(RecruiterResponse::getId).collect(Collectors.toList());
        return dismissWaitingRecruiters(ids);
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
        log.info("Updating status of recruiter ID: {} to {} for current user", id, status);
        Recruiter recruiter = recruiterRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Recruiter not found with ID: " + id));
        
        User currentUser = getOptionalCurrentUser();
        UserRecruiterState userState = null;
        if (currentUser != null) {
            final User user = currentUser;
            final Recruiter targetRecruiter = recruiter;
            userState = userRecruiterStateRepository.findByUserAndRecruiter(user, targetRecruiter)
                    .orElseGet(() -> UserRecruiterState.builder()
                            .user(user)
                            .recruiter(targetRecruiter)
                            .status(RecruiterStatus.ACTIVE)
                            .isAdded(true)
                            .build());

            userState.setStatus(status);
            userState.setIsAdded(true);
            userState = userRecruiterStateRepository.save(userState);
        } else {
            recruiter.setStatus(status);
            recruiter = recruiterRepository.save(recruiter);
        }

        log.info("Updated status of recruiter ID: {} successfully for current user", id);
        return mapToResponse(recruiter, userState);
    }

    @Override
    @Transactional
    public int importRecruitersFromCsv(org.springframework.web.multipart.MultipartFile file, Integer setNumber, Boolean isPublic) {
        boolean isPublicVal = isPublic != null ? isPublic : true;
        log.info("Importing recruiters from CSV (Public: {}). Set number: {}", isPublicVal, setNumber);
        int count = 0;
        User currentUser = getOptionalCurrentUser();

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
                            .isPublic(isPublicVal)
                            .addedBy(currentUser)
                            .build();

                    Recruiter savedRecruiter = recruiterRepository.save(recruiter);

                    if (currentUser != null) {
                        UserRecruiterState userState = UserRecruiterState.builder()
                                .user(currentUser)
                                .recruiter(savedRecruiter)
                                .status(RecruiterStatus.ACTIVE)
                                .customTitle(title)
                                .customCompany(company)
                                .isAdded(true)
                                .build();
                        userRecruiterStateRepository.save(userState);
                    }

                    count++;
                }
            }
        } catch (Exception e) {
            log.error("Failed to import recruiters from CSV", e);
            throw new RuntimeException("CSV import failed: " + e.getMessage(), e);
        }
        log.info("Imported {} recruiters successfully into public pool for set {}", count, setNumber);
        return count;
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportRecruitersToCsv(Integer setNumber) {
        log.info("Exporting recruiters to CSV with user-private states. Set number filter: {}", setNumber);
        List<RecruiterResponse> list = getAllRecruiters().stream()
                .filter(r -> setNumber == null || setNumber <= 0 || (r.getContactSet() != null && r.getContactSet().equals(setNumber)))
                .collect(Collectors.toList());

        StringBuilder sb = new StringBuilder();
        sb.append("Name,Email,Title,Company,Contact Set,Status,Last Contacted\n");
        for (RecruiterResponse r : list) {
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

    private RecruiterResponse mapToResponse(Recruiter recruiter, UserRecruiterState userState) {
        RecruiterStatus status = (userState != null && userState.getStatus() != null) ? 
                userState.getStatus() : (recruiter.getStatus() != null ? recruiter.getStatus() : RecruiterStatus.ACTIVE);

        java.time.LocalDateTime lastContactedDate = (userState != null && userState.getLastContactedDate() != null) ? 
                userState.getLastContactedDate() : recruiter.getLastContactedDate();

        String title = (userState != null && userState.getCustomTitle() != null && !userState.getCustomTitle().isBlank()) ? 
                userState.getCustomTitle() : recruiter.getTitle();

        String company = (userState != null && userState.getCustomCompany() != null && !userState.getCustomCompany().isBlank()) ? 
                userState.getCustomCompany() : recruiter.getCompany();

        User currentUser = getOptionalCurrentUser();
        boolean defaultIsAdded;
        if (recruiter.getAddedBy() == null) {
            defaultIsAdded = true;
        } else if (currentUser != null && recruiter.getAddedBy().getId().equals(currentUser.getId())) {
            defaultIsAdded = true;
        } else {
            defaultIsAdded = false;
        }

        Boolean isAdded = (userState != null && userState.getIsAdded() != null) ? 
                userState.getIsAdded() : defaultIsAdded;

        String addedByUsername = recruiter.getAddedBy() != null ? recruiter.getAddedBy().getUsername() : "System";

        return RecruiterResponse.builder()
                .id(recruiter.getId())
                .name(recruiter.getName())
                .email(recruiter.getEmail())
                .title(title)
                .company(company)
                .status(status)
                .contactSet(recruiter.getContactSet())
                .lastContactedDate(lastContactedDate)
                .isPublic(recruiter.getIsPublic() != null ? recruiter.getIsPublic() : true)
                .isAdded(isAdded)
                .addedByUsername(addedByUsername)
                .build();
    }
}
