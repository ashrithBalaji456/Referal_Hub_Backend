package com.referral.outreach.config;

import com.referral.outreach.entity.*;
import com.referral.outreach.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.core.io.ClassPathResource;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@Component
@Profile("!test")
@RequiredArgsConstructor
public class DataLoader implements CommandLineRunner {

    private final TemplateRepository templateRepository;
    private final ResumeRepository resumeRepository;
    private final CampaignRepository campaignRepository;
    private final RecruiterRepository recruiterRepository;
    private final UserRepository userRepository;
    private final CandidateProfileRepository candidateProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @Value("${app.upload.dir:./uploads/resumes}")
    private String uploadDir;

    @Override
    public void run(String... args) throws Exception {
        log.info("Checking database for seed data...");

        try {
            jdbcTemplate.execute("ALTER TABLE recruiter DROP COLUMN IF EXISTS role_category CASCADE;");
            jdbcTemplate.execute("UPDATE recruiter SET contact_set = 1 WHERE contact_set IS NULL;");
        } catch (Exception e) {
            log.warn("Could not execute DDL/DML startup cleanup statements: {}", e.getMessage());
        }

        // 0. Seed Default User
        User defaultUser = null;
        if (userRepository.count() == 0) {
            log.info("Seeding default user ashrith...");
            defaultUser = User.builder()
                    .username("ashrith")
                    .email("ashrithbalajigudla@gmail.com")
                    .password(passwordEncoder.encode("password"))
                    .build();
            defaultUser = userRepository.save(defaultUser);

            CandidateProfile defaultProfile = CandidateProfile.builder()
                    .fullName("Gudla Ashrith Balaji")
                    .roleName("Java Backend Developer")
                    .email("ashrithbalajigudla@gmail.com")
                    .linkedinUrl("linkedin.com/in/ashrith-balaji-gudla-5768302a8/")
                    .githubUrl("")
                    .phoneNumber("")
                    .location("")
                    .customFieldsJson("{}")
                    .user(defaultUser)
                    .build();
            candidateProfileRepository.save(defaultProfile);
            log.info("Successfully seeded default user ashrith with profile.");
        } else {
            defaultUser = userRepository.findByUsername("ashrith").orElse(null);
            if (defaultUser == null) {
                defaultUser = userRepository.findAll().stream().findFirst().orElse(null);
            }
        }

        // If no user could be seeded or found, abort template/resume seeding as it requires user
        if (defaultUser == null) {
            log.warn("No user found or seeded. Skipping template, resume, and campaign seeding.");
            return;
        }

        // 1. Seed Templates for all users missing default templates
        seedDefaultTemplatesForAllUsers();

        // Retrieve default user's universal template for campaign linkage
        EmailTemplate template = templateRepository.findByUserAndTemplateName(defaultUser, "Universal Referral Template").orElse(null);

        // 2. Seed / Normalize Resume per user
        Resume resume = null;
        java.util.Map<User, java.util.List<Resume>> activeResumesByUser = resumeRepository.findAllByIsActiveTrue()
                .stream()
                .filter(r -> r.getUser() != null)
                .collect(java.util.stream.Collectors.groupingBy(Resume::getUser));

        for (java.util.Map.Entry<User, java.util.List<Resume>> entry : activeResumesByUser.entrySet()) {
            java.util.List<Resume> userActiveResumes = entry.getValue();
            if (userActiveResumes.size() > 1) {
                log.warn("User {} has {} active resumes. Normalizing so only the most recent remains active...", entry.getKey().getUsername(), userActiveResumes.size());
                userActiveResumes.sort((r1, r2) -> Long.compare(r2.getId(), r1.getId()));
                for (int i = 1; i < userActiveResumes.size(); i++) {
                    Resume r = userActiveResumes.get(i);
                    r.setActive(false);
                    resumeRepository.save(r);
                }
            }
        }

        // Ensure all existing resumes in database have fileData populated (Backfill for Render/Cloud)
        byte[] defaultBytes = null;
        try {
            ClassPathResource defaultResResource = new ClassPathResource("default_resume.pdf");
            if (defaultResResource.exists()) {
                defaultBytes = defaultResResource.getInputStream().readAllBytes();
            }
        } catch (Exception ex) {
            log.warn("Could not read default_resume.pdf from classpath: {}", ex.getMessage());
        }

        List<Resume> allResumes = resumeRepository.findAll();
        for (Resume r : allResumes) {
            if (r.getFileData() == null || r.getFileData().isBlank()) {
                try {
                    File physicalFile = r.getFilePath() != null ? new File(r.getFilePath()) : null;
                    if (physicalFile != null && physicalFile.exists()) {
                        byte[] bytes = Files.readAllBytes(physicalFile.toPath());
                        r.setFileData(java.util.Base64.getEncoder().encodeToString(bytes));
                        resumeRepository.save(r);
                        log.info("Backfilled fileData for resume ID: {} from disk file: {}", r.getId(), r.getOriginalFilename());
                    } else if (defaultBytes != null) {
                        r.setFileData(java.util.Base64.getEncoder().encodeToString(defaultBytes));
                        resumeRepository.save(r);
                        log.info("Backfilled fileData for resume ID: {} from default_resume.pdf classpath resource", r.getId());
                    }
                } catch (Exception ex) {
                    log.error("Failed to backfill fileData for resume ID: {}", r.getId(), ex);
                }
            }
        }

        java.util.List<Resume> defaultUserActiveResumes = resumeRepository.findAllByUserAndIsActiveTrue(defaultUser);
        if (!defaultUserActiveResumes.isEmpty()) {
            resume = defaultUserActiveResumes.get(0);
        } else if (resumeRepository.count() == 0) {
            byte[] resumeBytes = null;
            File sourceFile = new File("C:\\Users\\ashri\\SpringNew\\Profile\\assets\\AshrithBalaji_BackendDeveloper_Resume.pdf");
            if (sourceFile.exists()) {
                try {
                    resumeBytes = Files.readAllBytes(sourceFile.toPath());
                } catch (Exception ignored) {}
            }
            if (resumeBytes == null) {
                resumeBytes = defaultBytes;
            }

            if (resumeBytes != null && resumeBytes.length > 0) {
                log.info("Creating default active resume in database...");
                String base64Data = java.util.Base64.getEncoder().encodeToString(resumeBytes);

                resume = Resume.builder()
                        .filename("AshrithBalaji_BackendDeveloper_Resume.pdf")
                        .originalFilename("AshrithBalaji_BackendDeveloper_Resume.pdf")
                        .filePath("./uploads/resumes/AshrithBalaji_BackendDeveloper_Resume.pdf")
                        .fileSize((long) resumeBytes.length)
                        .contentType("application/pdf")
                        .fileData(base64Data)
                        .isActive(true)
                        .user(defaultUser)
                        .build();

                resume = resumeRepository.save(resume);
                log.info("Successfully created default active resume in DB with fileData length: {}", base64Data.length());
            }
        } else {
            log.info("No active resume found for default user.");
        }

        // 3. Seed Campaign
        if (campaignRepository.count() == 0 && template != null) {
            log.info("Seeding default universal outreach campaign...");
            Campaign campaign = Campaign.builder()
                    .name("Universal Outreach Campaign")
                    .emailTemplate(template)
                    .resume(resume)
                    .isEnabled(true)
                    .user(defaultUser)
                    .build();

            campaignRepository.save(campaign);
            log.info("Successfully seeded and enabled campaign: {}", campaign.getName());
        }

        // 4. Seed Recruiters from CSV
        boolean needsReSeeding = false;
        if (recruiterRepository.count() == 0) {
            needsReSeeding = true;
        } else {
            // Check if title is missing or empty in seeded recruiters
            Recruiter sample = recruiterRepository.findAll().stream().findFirst().orElse(null);
            if (sample == null || sample.getTitle() == null || sample.getTitle().trim().isEmpty() || sample.getTitle().equals("HR Professional")) {
                log.info("Detected old seeded recruiters without proper titles. Clearing database to re-seed...");
                jdbcTemplate.execute("TRUNCATE TABLE email_history CASCADE;");
                jdbcTemplate.execute("TRUNCATE TABLE campaign CASCADE;");
                jdbcTemplate.execute("TRUNCATE TABLE recruiter CASCADE;");
                
                // Re-create the default campaign since it was truncated
                if (template != null) {
                    Campaign campaign = Campaign.builder()
                            .name("Universal Outreach Campaign")
                            .emailTemplate(template)
                            .resume(resume)
                            .isEnabled(true)
                            .user(defaultUser)
                            .build();
                    campaignRepository.save(campaign);
                    log.info("Re-seeded default universal campaign after truncate.");
                }
                needsReSeeding = true;
            }
        }

        if (needsReSeeding) {
            log.info("Seeding recruiters from CSV file...");
            ClassPathResource resource = new ClassPathResource("recruiters.csv");
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
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
                        
                        Recruiter recruiter = Recruiter.builder()
                                .name(name)
                                .email(email)
                                .title(title)
                                .company(company)
                                .status(RecruiterStatus.ACTIVE)
                                .contactSet(1)
                                .build();
                        recruiterRepository.save(recruiter);
                    }
                }
                log.info("Successfully seeded recruiters from CSV.");
            } catch (Exception e) {
                log.error("Failed to seed recruiters from CSV", e);
            }
        }

        // 5. Seed or Update Dubai Outreach Template
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
                "LinkedIn: linkedin.com/in/ashrith-balaji-gudla-5768302a8/\n\n" +
                "Thank you for your time and consideration. I look forward to hearing from you.\n\n" +
                "Best regards,\n" +
                "{{candidateName}}\n" +
                "{{roleName}}\n" +
                "Email: ashrithbalajigudla@gmail.com";

        String dubaiSubject = "Job Referral Request - {{candidateName}} - {{roleName}} (Remote / Relocation)";

        EmailTemplate dubaiTemplate = templateRepository.findByUserAndTemplateName(defaultUser, "Dubai Outreach Template").orElse(null);
        if (dubaiTemplate == null) {
            log.info("Seeding Dubai Outreach Template...");
            dubaiTemplate = EmailTemplate.builder()
                    .templateName("Dubai Outreach Template")
                    .subject(dubaiSubject)
                    .body(dubaiBody)
                    .user(defaultUser)
                    .build();
            templateRepository.save(dubaiTemplate);
            log.info("Successfully seeded template: Dubai Outreach Template");
        } else {
            log.info("Updating existing Dubai Outreach Template to Remote/Relocation version...");
            dubaiTemplate.setSubject(dubaiSubject);
            dubaiTemplate.setBody(dubaiBody);
            templateRepository.save(dubaiTemplate);
            log.info("Successfully updated template: Dubai Outreach Template");
        }

        // 6. Seed Dubai Recruiters (Set 3)
        String[] dubaiEmails = {
            "CV@Arabianfal.com", "Cv@aldawaa.com.sa", "Hr.qrm@hotmail.com", "Rec@alojaimi.com",
            "Recruitment@rawabiholding.com", "Job.s6@hotmail.com", "Careers@dnata.com", "Hr@alesayi_motors.com",
            "Hr@alarayan.com", "Recruiting.ksa@mcmermott.com", "Recruitment@aytb.com", "Wadaef@gtecorp.com",
            "Careers@nesr.com", "Recruit@farm.com.sa", "Info@alhumamlaw.com", "Hr1@gulfteksaudi.com",
            "Catcosa@catcosa.com", "Cv@tafear.com", "Recruitment@sraco.com.sa", "Career@sidco.com.sa",
            "Info@atco.com.sa", "HRdepartmental@sa.yokogawa.com", "Recruitment@shadeco.com", "Klc.hr@alkafaa.com",
            "Kbr-amcdehr@kbr.com", "Recruitment@batook.com", "Career@shawarmer.com", "Hrsupport@archirodon.net",
            "Jobs@binajinah.com", "Marketing.np@nesma.com", "Wadaef2019@abdulla-fouad.com", "Careers@innosoft.sa",
            "M@startime.com.sa", "job@musk.sa.com", "Shababwatansa@gmail.com", "j@sabksa.com",
            "al.alshaikh@bonyan.sa", "ymohammed@innovest.com.sa", "s.alwadi@nhc.sa", "i.atassi@artar.com.sa",
            "oalkhunaizi@darwaemaar.com", "jobs@almasah.net", "recruitment.amjad@gmail.com", "jobrydlaw@gmail.com",
            "Job@almoosahospital.com.sa", "recruitment@almoosahospital.com.sa", "jobs@sghgroup.net", "career.dmm@sghgroup.net",
            "talent.acquisition@drsulaimanalhabib.com", "Careers@jhah.com", "hrd@alahsahospital.com.sa", "career@almanahospital.com.sa",
            "info@familycare.com.sa", "info@ramclinics.com", "HR.DSFHR@fakeeh.care", "career@mouwasat.com",
            "careers@dallah-hospital.com", "hr.phc@drsulaimanalhabib.com", "careers@almurjanhospital.com", "hiringnow.ksa@gmail.com",
            "recruitment@wecareksa.com"
        };

        boolean hasDubaiRecruiters = recruiterRepository.findAll().stream()
                .anyMatch(r -> r.getContactSet() == 3);

        if (!hasDubaiRecruiters) {
            log.info("Seeding Dubai Recruiters (Set 3)...");
            for (String email : dubaiEmails) {
                String cleanEmail = email.trim();
                if (!recruiterRepository.existsByEmail(cleanEmail)) {
                    String company = extractCompanyFromEmail(cleanEmail);
                    Recruiter recruiter = Recruiter.builder()
                            .name("Recruitment Team")
                            .email(cleanEmail)
                            .title("Dubai Recruitment Specialist")
                            .company(company)
                            .status(RecruiterStatus.ACTIVE)
                            .contactSet(3)
                            .build();
                    recruiterRepository.save(recruiter);
                }
            }
            log.info("Successfully seeded Dubai Recruiters into Set 3.");
        }
    }

    private void seedDefaultTemplatesForAllUsers() {
        for (User user : userRepository.findAll()) {
            // Universal Referral Template
            if (!templateRepository.existsByUserAndTemplateName(user, "Universal Referral Template")) {
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
                log.info("Seeded Universal Referral Template for user: {}", user.getUsername());
            }

            // Dubai Outreach Template
            if (!templateRepository.existsByUserAndTemplateName(user, "Dubai Outreach Template")) {
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
                log.info("Seeded Dubai Outreach Template for user: {}", user.getUsername());
            }

            // Follow-Up Outreach Template
            if (!templateRepository.existsByUserAndTemplateName(user, "Follow-Up Outreach Template")) {
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
                log.info("Seeded Follow-Up Outreach Template for user: {}", user.getUsername());
            }

            // Direct Engineering Pitch Template
            if (!templateRepository.existsByUserAndTemplateName(user, "Direct Engineering Pitch Template")) {
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
                log.info("Seeded Direct Engineering Pitch Template for user: {}", user.getUsername());
            }
        }
    }

    private String extractCompanyFromEmail(String email) {
        try {
            int atIndex = email.indexOf('@');
            if (atIndex == -1) return "Dubai Recruiter";
            String domain = email.substring(atIndex + 1).toLowerCase();
            if (domain.contains("gmail") || domain.contains("hotmail") || domain.contains("yahoo") || domain.contains("outlook")) {
                return "Dubai Recruiter";
            }
            int dotIndex = domain.indexOf('.');
            if (dotIndex == -1) return "Dubai Recruiter";
            String rawCompany = domain.substring(0, dotIndex);
            if (rawCompany.length() <= 1) return "Dubai Recruiter";
            
            // Capitalize first letter
            return rawCompany.substring(0, 1).toUpperCase() + rawCompany.substring(1);
        } catch (Exception e) {
            return "Dubai Recruiter";
        }
    }
}
