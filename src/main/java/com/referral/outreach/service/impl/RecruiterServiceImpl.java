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

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecruiterServiceImpl implements RecruiterService {

    private final RecruiterRepository recruiterRepository;

    @Override
    @Transactional
    public RecruiterResponse createRecruiter(RecruiterRequest request) {
        log.info("Creating recruiter with email: {}", request.getEmail());
        if (recruiterRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateRecruiterException("Recruiter with email " + request.getEmail() + " already exists");
        }

        Recruiter recruiter = Recruiter.builder()
                .name(request.getName())
                .email(request.getEmail())
                .company(request.getCompany())
                .roleCategory(request.getRoleCategory())
                .status(request.getStatus())
                .build();

        Recruiter savedRecruiter = recruiterRepository.save(recruiter);
        log.info("Created recruiter successfully with ID: {}", savedRecruiter.getId());
        return mapToResponse(savedRecruiter);
    }

    @Override
    @Transactional
    public RecruiterResponse updateRecruiter(Long id, RecruiterRequest request) {
        log.info("Updating recruiter ID: {}", id);
        Recruiter recruiter = recruiterRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Recruiter not found with ID: " + id));

        if (recruiterRepository.existsByEmailAndIdNot(request.getEmail(), id)) {
            throw new DuplicateRecruiterException("Another recruiter with email " + request.getEmail() + " already exists");
        }

        recruiter.setName(request.getName());
        recruiter.setEmail(request.getEmail());
        recruiter.setCompany(request.getCompany());
        recruiter.setRoleCategory(request.getRoleCategory());
        recruiter.setStatus(request.getStatus());

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

    private RecruiterResponse mapToResponse(Recruiter recruiter) {
        return RecruiterResponse.builder()
                .id(recruiter.getId())
                .name(recruiter.getName())
                .email(recruiter.getEmail())
                .company(recruiter.getCompany())
                .roleCategory(recruiter.getRoleCategory())
                .status(recruiter.getStatus())
                .lastContactedDate(recruiter.getLastContactedDate())
                .build();
    }
}
