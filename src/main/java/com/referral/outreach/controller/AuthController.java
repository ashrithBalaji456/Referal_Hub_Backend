package com.referral.outreach.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.referral.outreach.dto.JwtAuthenticationResponse;
import com.referral.outreach.dto.LoginRequest;
import com.referral.outreach.dto.SignUpRequest;
import com.referral.outreach.entity.CandidateProfile;
import com.referral.outreach.entity.EmailTemplate;
import com.referral.outreach.entity.User;
import com.referral.outreach.entity.PasswordResetToken;
import com.referral.outreach.repository.CandidateProfileRepository;
import com.referral.outreach.repository.TemplateRepository;
import com.referral.outreach.repository.UserRepository;
import com.referral.outreach.repository.PasswordResetTokenRepository;
import com.referral.outreach.service.MailService;
import com.referral.outreach.security.JwtTokenProvider;
import com.referral.outreach.security.UserPrincipal;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final CandidateProfileRepository candidateProfileRepository;
    private final TemplateRepository templateRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final ObjectMapper objectMapper;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final MailService mailService;

    @Value("${app.candidate-name:Gudla Ashrith Balaji}")
    private String defaultCandidateName;

    @Value("${app.target-role:Java Backend Developer}")
    private String defaultTargetRole;

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        log.info("REST request to authenticate user: {}", loginRequest.getUsername());
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getUsername(),
                        loginRequest.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = tokenProvider.generateToken(authentication);
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();

        return ResponseEntity.ok(new JwtAuthenticationResponse(jwt, userPrincipal.getUsername(), userPrincipal.getEmail()));
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody SignUpRequest signUpRequest) {
        log.info("REST request to register user: {}", signUpRequest.getUsername());
        if (userRepository.existsByUsername(signUpRequest.getUsername())) {
            return new ResponseEntity<>(Map.of("message", "Username is already taken!"), HttpStatus.BAD_REQUEST);
        }

        if (userRepository.existsByEmail(signUpRequest.getEmail())) {
            return new ResponseEntity<>(Map.of("message", "Email Address already in use!"), HttpStatus.BAD_REQUEST);
        }

        // Creating user's account
        User user = User.builder()
                .username(signUpRequest.getUsername())
                .email(signUpRequest.getEmail())
                .password(passwordEncoder.encode(signUpRequest.getPassword()))
                .build();

        User savedUser = userRepository.save(user);

        // Seeding dynamic default CandidateProfile for new user
        Map<String, String> defaults = new HashMap<>();
        String defaultJson = "{}";
        try {
            defaultJson = objectMapper.writeValueAsString(defaults);
        } catch (Exception e) {
            log.error("Failed to serialize dynamic custom variables", e);
        }

        CandidateProfile profile = CandidateProfile.builder()
                .fullName(defaultCandidateName)
                .roleName(defaultTargetRole)
                .email(signUpRequest.getEmail())
                .linkedinUrl("linkedin.com/in/ashrith-balaji-gudla-5768302a8/")
                .githubUrl("")
                .phoneNumber("")
                .location("")
                .customFieldsJson(defaultJson)
                .user(savedUser)
                .build();
        candidateProfileRepository.save(profile);

        // Seeding default outreach templates for new user
        seedDefaultTemplatesForUser(savedUser);

        return new ResponseEntity<>(Map.of("message", "User registered successfully!"), HttpStatus.CREATED);
    }

    private void seedDefaultTemplatesForUser(User user) {
        // Universal Referral Template
        String universalBody = "Dear {{recruiterName}},\n\n" +
                "I hope you're doing well.\n\n" +
                "I’m {{candidateName}}, a Computer Science and Engineering student graduating in 2026, with a strong interest in {{roleName}} and Software Engineering opportunities.\n\n" +
                "I have hands-on experience building backend applications using layered architecture, implementing RESTful APIs, and managing database operations with Spring Data JPA and PostgreSQL. I would be highly grateful if you could consider my profile for any suitable opportunities at {{companyName}}.\n\n" +
                "I have attached my resume for your reference. I would appreciate the opportunity to discuss how my skills and projects could contribute to your team.\n\n" +
                "LinkedIn: {{linkedin}}\n\n" +
                "Thank you for your time and consideration.\n\n" +
                "Best regards,\n" +
                "{{candidateName}}\n" +
                "{{roleName}}";

        EmailTemplate universalTemplate = EmailTemplate.builder()
                .templateName("Universal Referral Template")
                .subject("Job Application / Referral Request - {{candidateName}} - {{roleName}}")
                .body(universalBody)
                .user(user)
                .build();
        templateRepository.save(universalTemplate);

        // Dubai Outreach Template
        String dubaiBody = "Dear {{recruiterName}},\n\n" +
                "I hope you're doing well.\n\n" +
                "I’m {{candidateName}}, a Computer Science and Engineering graduate (2026), writing to express my interest in joining your team in Dubai. I specialize in Java, Spring Boot, REST APIs, and backend software engineering.\n\n" +
                "I have hands-on experience building backend applications using layered architecture, implementing RESTful APIs, managing database operations with Spring Data JPA and PostgreSQL, and implementing rate-limited schedules.\n\n" +
                "I am currently looking for remote entry-level opportunities in Dubai and the UAE, and I am fully open to relocating in the future if required. I am interested in roles such as:\n\n" +
                "• Java Developer / Backend Engineer\n" +
                "• Software Engineer / Associate Software Engineer\n" +
                "• Spring Boot Developer\n\n" +
                "I would be highly grateful if you could consider my profile for any suitable current or upcoming opportunities at your organization, {{companyName}}.\n\n" +
                "I have attached my resume for your reference. I would appreciate the opportunity to discuss how my skills and projects could contribute to your team.\n\n" +
                "LinkedIn: {{linkedin}}\n\n" +
                "Thank you for your time and consideration. I look forward to hearing from you.\n\n" +
                "Best regards,\n" +
                "{{candidateName}}\n" +
                "{{roleName}}\n" +
                "Email: ashrithbalajigudla@gmail.com";

        EmailTemplate dubaiTemplate = EmailTemplate.builder()
                .templateName("Dubai Outreach Template")
                .subject("Job Referral Request - {{candidateName}} - {{roleName}} (Remote / Relocation)")
                .body(dubaiBody)
                .user(user)
                .build();
        templateRepository.save(dubaiTemplate);

        // Follow-Up Outreach Template
        String followUpBody = "Dear {{recruiterName}},\n\n" +
                "I hope you're having a great week.\n\n" +
                "I am following up on my previous message regarding potential {{roleName}} opportunities at {{companyName}}.\n\n" +
                "As a Computer Science & Engineering graduate specializing in Java, Spring Boot, and RESTful microservices, I am very eager to learn how my technical background and hands-on project experience could support your engineering goals.\n\n" +
                "I have re-attached my resume for convenience. If you have a few minutes for a quick chat or can refer me to the hiring manager for backend engineering roles, I would be deeply grateful.\n\n" +
                "LinkedIn: {{linkedin}}\n\n" +
                "Thank you again for your time!\n\n" +
                "Best regards,\n" +
                "{{candidateName}}\n" +
                "{{roleName}}";

        EmailTemplate followUpTemplate = EmailTemplate.builder()
                .templateName("Follow-Up Outreach Template")
                .subject("Following Up - Referral Inquiry - {{candidateName}} - {{roleName}}")
                .body(followUpBody)
                .user(user)
                .build();
        templateRepository.save(followUpTemplate);

        // Direct Engineering Pitch Template
        String pitchBody = "Hi {{recruiterName}},\n\n" +
                "I came across your team's work at {{companyName}} and wanted to reach out directly regarding {{roleName}} roles.\n\n" +
                "I am {{candidateName}}, a CSE graduate (2026) specializing in backend engineering. My technical foundation includes:\n" +
                "• Core Java & Spring Boot (REST APIs, Layered Architecture)\n" +
                "• Database Management (Spring Data JPA, PostgreSQL, MySQL)\n" +
                "• Automated Outreach & Rate-Limited Batch Processing\n\n" +
                "I would love the opportunity to contribute to high-impact projects at {{companyName}}. Please let me know if you are open to a brief conversation or if I can submit my profile for current openings.\n\n" +
                "Resume attached. Portfolio / LinkedIn: {{linkedin}}\n\n" +
                "Best regards,\n" +
                "{{candidateName}}";

        EmailTemplate pitchTemplate = EmailTemplate.builder()
                .templateName("Direct Engineering Pitch Template")
                .subject("Backend Engineer Inquiry - {{candidateName}} - {{companyName}}")
                .body(pitchBody)
                .user(user)
                .build();
        templateRepository.save(pitchTemplate);
    }

    @PostMapping("/forgot-password")
    @org.springframework.transaction.annotation.Transactional
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        log.info("REST request to trigger forgot password for email: {}", email);
        if (email == null || email.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Email is required"));
        }

        User user = userRepository.findByEmail(email.trim()).orElse(null);
        if (user == null) {
            // Prevent email harvesting: return success even if user not found
            return ResponseEntity.ok(Map.of("message", "If that email exists in our system, we've sent a link to reset your password."));
        }

        // Delete existing tokens for this user first and flush immediately to satisfy unique constraint
        passwordResetTokenRepository.deleteByUser(user);
        passwordResetTokenRepository.flush();

        // Generate reset token
        String token = UUID.randomUUID().toString();
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .token(token)
                .user(user)
                .expiryDate(LocalDateTime.now().plusHours(24))
                .build();
        passwordResetTokenRepository.save(resetToken);

        // Dispatch email
        try {
            mailService.sendPasswordResetEmail(user.getEmail(), token);
        } catch (Exception e) {
            log.error("Failed to dispatch password reset email for user {}: {}", email, e.getMessage());
            log.warn("[DEV MODE] Reset link for {}: reset-password?token={}", email, token);
            return ResponseEntity.ok(Map.of(
                "message", "Password reset link generated! To receive emails in production, ensure the RESEND_API_KEY environment variable is set in your Render dashboard."
            ));
        }

        return ResponseEntity.ok(Map.of("message", "If that email exists in our system, we've sent a link to reset your password."));
    }

    @PostMapping("/reset-password")
    @org.springframework.transaction.annotation.Transactional
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> request) {
        String token = request.get("token");
        String newPassword = request.get("newPassword");
        log.info("REST request to reset password using token");

        if (token == null || token.trim().isEmpty() || newPassword == null || newPassword.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Token and new password are required"));
        }

        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(token.trim()).orElse(null);
        if (resetToken == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Invalid or expired token"));
        }

        if (resetToken.isExpired()) {
            passwordResetTokenRepository.delete(resetToken);
            return ResponseEntity.badRequest().body(Map.of("message", "Token has expired"));
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(newPassword.trim()));
        userRepository.save(user);

        // Delete token
        passwordResetTokenRepository.delete(resetToken);

        return ResponseEntity.ok(Map.of("message", "Password has been reset successfully."));
    }
}
