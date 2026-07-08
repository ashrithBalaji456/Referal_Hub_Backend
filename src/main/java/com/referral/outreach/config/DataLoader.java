package com.referral.outreach.config;

import com.referral.outreach.entity.*;
import com.referral.outreach.repository.CampaignRepository;
import com.referral.outreach.repository.RecruiterRepository;
import com.referral.outreach.repository.ResumeRepository;
import com.referral.outreach.repository.TemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
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

@Slf4j
@Component
@Profile("!test")
@RequiredArgsConstructor
public class DataLoader implements CommandLineRunner {

    private final TemplateRepository templateRepository;
    private final ResumeRepository resumeRepository;
    private final CampaignRepository campaignRepository;
    private final RecruiterRepository recruiterRepository;

    @Value("${app.upload.dir:./uploads/resumes}")
    private String uploadDir;

    @Override
    public void run(String... args) throws Exception {
        log.info("Checking database for seed data...");

        // 1. Seed Template
        EmailTemplate template = null;
        if (templateRepository.count() == 0) {
            log.info("Seeding default universal email template...");
            String body = "Dear {{recruiterName}},\n\n" +
                    "I hope you're doing well.\n\n" +
                    "I’m {{candidateName}}, a Computer Science and Engineering student graduating in 2026, with a strong interest in {{roleName}} and Software Engineering opportunities.\n\n" +
                    "I have hands-on experience working with Java, Spring Boot, REST APIs, Spring Data JPA, PostgreSQL, and MySQL. Through my projects, I have built backend applications using layered architecture, implemented RESTful APIs, managed database operations, and developed business logic for real-world use cases.\n\n" +
                    "I am currently looking for entry-level opportunities in roles such as:\n\n" +
                    "• Java Developer\n" +
                    "• Backend Developer\n" +
                    "• Software Engineer\n" +
                    "• Associate Software Engineer\n" +
                    "• Spring Boot Developer\n\n" +
                    "I would be grateful if you could consider my profile for any suitable current or upcoming opportunities at your organization, {{companyName}}.\n\n" +
                    "I have attached my resume for your reference. I would appreciate the opportunity to discuss how my skills and projects could contribute to your team.\n\n" +
                    "LinkedIn: linkedin.com/in/ashrith-balaji-gudla-5768302a8/\n\n" +
                    "Thank you for your time and consideration. I look forward to hearing from you.\n\n" +
                    "Best regards,\n" +
                    "{{candidateName}}\n" +
                    "{{roleName}}\n" +
                    "Email: ashrithbalajigudla@gmail.com\n" +
                    "LinkedIn: linkedin.com/in/ashrith-balaji-gudla-5768302a8/";

            template = EmailTemplate.builder()
                    .templateName("Universal Referral Template")
                    .subject("Job Application / Referral Request - {{candidateName}} - {{roleName}}")
                    .body(body)
                    .build();

            template = templateRepository.save(template);
            log.info("Successfully seeded template: {}", template.getTemplateName());
        } else {
            template = templateRepository.findByTemplateName("Universal Referral Template").orElse(null);
        }

        // 2. Seed Resume
        Resume resume = null;
        if (resumeRepository.count() == 0) {
            File sourceFile = new File("C:\\Users\\ashri\\SpringNew\\Profile\\assets\\AshrithBalaji_BackendDeveloper_Resume.pdf");
            if (sourceFile.exists()) {
                log.info("Found resume file at {}. Importing into database...", sourceFile.getAbsolutePath());
                
                Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
                Files.createDirectories(uploadPath);

                String safeFilename = UUID.randomUUID() + "_AshrithBalaji_BackendDeveloper_Resume.pdf";
                Path targetLocation = uploadPath.resolve(safeFilename);

                Files.copy(sourceFile.toPath(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

                resume = Resume.builder()
                        .filename(safeFilename)
                        .originalFilename("AshrithBalaji_BackendDeveloper_Resume.pdf")
                        .filePath(targetLocation.toString())
                        .fileSize(sourceFile.length())
                        .contentType("application/pdf")
                        .isActive(true)
                        .build();

                resume = resumeRepository.save(resume);
                log.info("Successfully imported active resume to DB: {}", resume.getOriginalFilename());
            } else {
                log.warn("Resume file not found at C:\\Users\\ashri\\SpringNew\\Profile\\assets\\AshrithBalaji_BackendDeveloper_Resume.pdf. Skipping resume import.");
            }
        } else {
            resume = resumeRepository.findByIsActiveTrue().orElse(null);
        }

        // 3. Seed Campaign
        if (campaignRepository.count() == 0 && template != null && resume != null) {
            log.info("Seeding default universal outreach campaign...");
            Campaign campaign = Campaign.builder()
                    .name("Universal Outreach Campaign")
                    .emailTemplate(template)
                    .resume(resume)
                    .isEnabled(true)
                    .build();

            campaignRepository.save(campaign);
            log.info("Successfully seeded and enabled campaign: {}", campaign.getName());
        }

        // 4. Seed Recruiters from CSV
        if (recruiterRepository.count() == 0) {
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
                        String name = parts[0].trim();
                        String email = parts[1].trim();
                        String company = parts[2].trim();
                        RoleCategory category = RoleCategory.valueOf(parts[3].trim());
                        
                        Recruiter recruiter = Recruiter.builder()
                                .name(name)
                                .email(email)
                                .company(company)
                                .roleCategory(category)
                                .status(RecruiterStatus.ACTIVE)
                                .build();
                        recruiterRepository.save(recruiter);
                    }
                }
                log.info("Successfully seeded recruiters from CSV.");
            } catch (Exception e) {
                log.error("Failed to seed recruiters from CSV", e);
            }
        }
    }
}
