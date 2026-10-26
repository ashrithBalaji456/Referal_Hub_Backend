# 🚀 Referral Outreach Manager — Comprehensive Backend Code & Architecture Documentation
> **File Name:** `Readme_Code_Explanation.md`  
> **Author & Project Owner:** Gudla Ashrith Balaji  
> **Target Audience:** Complete beginners with **zero prior knowledge** in Java, Spring Boot, or Backend Architecture.

---

## 📑 Master Table of Contents
1. [Executive Summary & System Purpose](#1-executive-summary--system-purpose)
2. [Absolute Beginner's Primer: Core Concepts Explained](#2-absolute-beginners-primer-core-concepts-explained)
   - 2.1 What is Java & Spring Boot?
   - 2.2 What is an Annotation (`@`)?
   - 2.3 What is Dependency Injection & Inversion of Control (IoC)?
   - 2.4 What is a REST API & HTTP Protocol?
   - 2.5 What is JWT (JSON Web Token) Stateless Authentication?
   - 2.6 What is Spring Data JPA & ORM?
3. [End-to-End Architecture & Directory Layout](#3-end-to-end-architecture--directory-layout)
4. [Master Configuration Files Breakdown](#4-master-configuration-files-breakdown)
   - 4.1 `pom.xml` (Maven Dependencies & Build Lifecycle)
   - 4.2 `application.yml` (Main Server, Database & Cloud Config)
   - 4.3 `application-dev.yml` (Development Profile)
   - 4.4 `application-test.yml` (Automated Testing Profile with H2)
5. [Application Bootstrap & Entry Point](#5-application-bootstrap--entry-point)
   - 5.1 `OutreachApplication.java`
6. [Security & Authentication Engine (`com.referral.outreach.security`)](#6-security--authentication-engine)
   - 6.1 `WebSecurityConfig.java` (CORS, CSRF, Route Whitelisting, SecurityFilterChain)
   - 6.2 `JwtTokenProvider.java` (Cryptographic Token Issuance & Validation)
   - 6.3 `JwtAuthenticationFilter.java` (Per-Request Filter & SecurityContext Injection)
   - 6.4 `JwtAuthenticationEntryPoint.java` (401 Unauthorized Handler)
   - 6.5 `UserPrincipal.java` (Spring Security UserDetails Model)
   - 6.6 `CustomUserDetailsService.java` (Database User Loader)
   - 6.7 `SecurityUtils.java` (Logged-In User Extraction Utility)
7. [Database Entities & Data Modeling (`com.referral.outreach.entity`)](#7-database-entities--data-modeling)
   - 7.1 `User.java` (System Accounts)
   - 7.2 `CandidateProfile.java` (Personal Branding & Custom JSON Fields)
   - 7.3 `Recruiter.java` (HR & Recruiter Directory)
   - 7.4 `RecruiterStatus.java` (Active / Inactive Enum)
   - 7.5 `UserRecruiterState.java` (Per-User Status Overrides & Adoptions)
   - 7.6 `EmailTemplate.java` (Dynamic Outreach Templates)
   - 7.7 `Resume.java` (Dual Storage: Disk & PostgreSQL Base64)
   - 7.8 `Campaign.java` (Mapping Template + Resume + Set + Title Group)
   - 7.9 `EmailHistory.java` & `EmailHistoryStatus.java` (Audit Trail)
   - 7.10 `PasswordResetToken.java` (Account Recovery)
8. [Data Transfer Objects (`com.referral.outreach.dto`)](#8-data-transfer-objects-dtos)
9. [Database Access Layer (`com.referral.outreach.repository`)](#9-database-access-layer-repositories)
10. [Business Logic & External Integrations (`com.referral.outreach.service`)](#10-business-logic--services)
    - 10.1 `BrevoClient.java` (Pure HTTP REST Dispatcher to Brevo API)
    - 10.2 `MailService.java` & `MailServiceImpl.java` (Email Workflow & Variable Replacement)
    - 10.3 `RecruiterService.java` & `RecruiterServiceImpl.java` (CRUD, Import/Export, Waiting Queue)
    - 10.4 `CampaignService.java` & `CampaignServiceImpl.java` (Sequences, Batch Dispatches)
    - 10.5 `TemplateService.java` & `TemplateServiceImpl.java` (Template CRUD & Timestamps)
    - 10.6 `ResumeService.java` & `ResumeServiceImpl.java` (Validation, Dual Storage, Activation)
    - 10.7 `CandidateProfileService.java` & `CandidateProfileServiceImpl.java`
    - 10.8 `EmailHistoryService.java` & `EmailHistoryServiceImpl.java` (Dynamic JPA Specifications)
11. [REST Controllers & API Endpoints (`com.referral.outreach.controller`)](#11-rest-controllers--api-endpoints)
    - 11.1 `AuthController.java` (Login, Register, Forgot Password, Reset Password)
    - 11.2 `RecruiterController.java` (Directory Management, Waiting List, CSV Import/Export)
    - 11.3 `CampaignController.java` (Sequences, Previews, Batch Triggers, Scheduler Triggers)
    - 11.4 `TemplateController.java` (Template CRUD)
    - 11.5 `ResumeController.java` (Upload, Activate, Download, Delete)
    - 11.6 `CandidateProfileController.java` (Profiler Settings)
    - 11.7 `EmailHistoryController.java` (Delivery Logs)
    - 11.8 `HealthController.java` (Liveness Check)
12. [Automated Scheduler (`com.referral.outreach.scheduler`)](#12-automated-scheduler)
    - 12.1 `WeeklyOutreachScheduler.java` (Rate-Limited Background Cron Execution)
13. [Dynamic Template Parser Engine (`com.referral.outreach.util`)](#13-dynamic-template-parser-engine)
    - 13.1 `TemplateParser.java` (Regex & Placeholder Substitution)
14. [Startup Data Seeder (`com.referral.outreach.config`)](#14-startup-data-seeder)
    - 14.1 `DataLoader.java` (Auto-Seeding 2101+ Recruiters, Resumes, Templates)
15. [Global Exception Handling (`com.referral.outreach.exception`)](#15-global-exception-handling)
    - 15.1 `GlobalExceptionHandler.java` (Centralized HTTP Error Mapping)
    - 15.2 Custom Exception Classes
16. [Complete Step-by-Step Workflow Walkthroughs (Life of a Request)](#16-complete-step-by-step-workflow-walkthroughs)
17. [Comprehensive Annotation Reference & Cheat Sheet](#17-comprehensive-annotation-reference--cheat-sheet)

---

## 1. Executive Summary & System Purpose

The **Referral Outreach Manager** is an automated enterprise backend application designed for software engineers, graduates, and job seekers. It automates personalized, rate-limited email outreach to hiring managers, talent acquisition professionals, and HR recruiters across various companies worldwide.

### What problem does it solve?
1. **Manual Email Fatigue**: Sending hundreds of cold emails manually takes dozens of hours each week.
2. **Generic Email Rejection**: Recruiters ignore generic copy-pasted emails. This system customizes each message with `{{recruiterName}}`, `{{companyName}}`, `{{roleName}}`, `{{candidateName}}`, `{{linkedin}}`, and custom variables.
3. **Spam & Blacklisting Prevention**: It enforces rate-limiting delays (2 seconds between dispatches) and a **30-day cooldown** so the same recruiter is never spammed repeatedly.
4. **Multi-User Privacy & Shared Master Directory**: Initial 2101+ verified recruiters are automatically accessible by all users, while newly added recruiters can be made **Public (Master)** or kept strictly **Private (Only Me)**. Each user has their own private email templates, resumes, campaigns, and delivery logs.
5. **Reliable Email Delivery with Brevo HTTP API**: Instead of fragile local SMTP connections (which are often blocked by cloud providers like Render, AWS, or GCP), it communicates with Brevo (formerly Sendinblue) via HTTPS REST requests attaching the candidate's PDF resume in Base64 encoding.

---

## 2. Absolute Beginner's Primer: Core Concepts Explained

If you have never written Java or backend code, understand these fundamental building blocks:

### 2.1 What is Java & Spring Boot?
- **Java**: A strongly typed, compiled object-oriented programming language. Code is organized into *classes* that contain *fields* (variables) and *methods* (functions).
- **Spring Boot**: A framework built on top of Java that eliminates boilerplate configuration. It provides an embedded web server (Apache Tomcat), connects to databases, and manages the lifecycle of your code objects.

### 2.2 What is an Annotation (`@`)?
In Java, an annotation starts with `@` (e.g., `@RestController`, `@Entity`, `@Service`).  
Think of an annotation as a **super-powered sticker** placed on top of a class, method, or field. Spring Boot scans your code during startup, reads these stickers, and automatically wires up behavior behind the scenes without you writing hundreds of lines of plumbing.

### 2.3 What is Dependency Injection & Inversion of Control (IoC)?
- **Without Spring (Old Way)**: If Class A needs Class B, it manually creates it: `ClassB b = new ClassB();`. This creates tight coupling.
- **With Spring Boot (IoC)**: Spring maintains an application container (the "ApplicationContext") that creates and holds single instances (called **Beans**) of all your classes. When Class A needs Class B, Spring automatically "injects" Class B into Class A's constructor. This is called **Dependency Injection**.
- We use Lombok's `@RequiredArgsConstructor` on classes, which generates a constructor for all `final` fields, allowing Spring to inject required services automatically.

### 2.4 What is a REST API & HTTP Protocol?
The Frontend (React/Vite) and Backend (Spring Boot) communicate over the network using HTTP. The backend exposes **Endpoints** (URLs):
- **GET**: Retrieve data (e.g., `GET /api/recruiters` fetches the contact list).
- **POST**: Create new data or trigger an action (e.g., `POST /api/auth/login`).
- **PUT**: Update existing data completely (e.g., `PUT /api/campaigns/1`).
- **PATCH**: Partially update existing data (e.g., `PATCH /api/recruiters/1/status`).
- **DELETE**: Remove data (e.g., `DELETE /api/recruiters/5`).

### 2.5 What is JWT (JSON Web Token) Stateless Authentication?
HTTP is stateless—the server does not remember who you are from one request to another.  
1. When you log in with your username and password, the backend verifies your credentials and creates a signed string called a **JWT Token**.
2. This token contains encrypted claims (your user ID, username, expiration date).
3. The frontend stores this token and sends it in the HTTP header of every subsequent request:  
   `Authorization: Bearer <token_string>`
4. The backend's `JwtAuthenticationFilter` intercepts the request, checks the cryptographic signature using a secret key, and instantly knows who you are without requiring database queries on every single request.

### 2.6 What is Spring Data JPA & ORM?
- **Database Table**: A relational SQL table (e.g., PostgreSQL table `users` with columns `id`, `username`, `email`).
- **Java Object (Entity)**: A Java class representing a row in that table.
- **ORM (Object-Relational Mapping)**: Spring Data JPA (powered by Hibernate) bridges the gap. You interact with Java objects, and Hibernate automatically generates SQL queries (`SELECT`, `INSERT`, `UPDATE`, `DELETE`) for you.

---

## 3. End-to-End Architecture & Directory Layout

The application follows the industry-standard **Layered Architecture (Separation of Concerns)**:

```
[ Frontend (React + Vite) ]
          │  HTTP / HTTPS REST Calls with JWT Bearer Token
          ▼
[ WebSecurityConfig & JwtAuthenticationFilter ] (Intercepts & authenticates token)
          │
          ▼
[ Controller Layer ] (Validates request input, routes HTTP endpoints)
          │
          ▼
[ Service Layer ] (Executes business rules, template parsing, rate limiting)
     ┌────┴──────────────────────────┬────────────────────────┐
     ▼                               ▼                        ▼
[ Repository Layer ]         [ BrevoClient ]      [ WeeklyOutreachScheduler ]
 (Spring Data JPA)        (Sends HTTPS emails)      (Cron timer every Monday)
     │                               │
     ▼                               ▼
[ PostgreSQL Database ]     [ Brevo Cloud API ]
```

---

## 4. Master Configuration Files Breakdown

### 4.1 `pom.xml` (Maven Dependencies & Build Lifecycle)
Every dependency has a precise purpose:
- **`spring-boot-starter-web`**: Provides the embedded Apache Tomcat container, Spring MVC for REST controllers (`@RestController`, `@GetMapping`, `@PostMapping`), and Jackson JSON serialization.
- **`spring-boot-starter-data-jpa`**: Brings in Hibernate ORM, connection pooling (HikariCP), and JPA repositories to interact with databases without raw SQL.
- **`spring-boot-starter-validation`**: Enables Jakarta Bean Validation (`@Valid`, `@NotBlank`, `@Email`, `@Size`) to automatically reject malformed HTTP payloads before execution.
- **`postgresql`**: The official JDBC driver enabling communication between Java and PostgreSQL databases.
- **`lombok`**: Compiler plugin generating getters, setters, constructors, builders, and SLF4J loggers at compile time.
- **`spring-boot-starter-security`**: Enforces route protection, password hashing (BCrypt), and request filtering.
- **`jjwt-api`, `jjwt-impl`, `jjwt-jackson`**: JSON Web Token library for HMAC-SHA256 token generation and parsing.
- **`h2`**: Fast in-memory database used exclusively during unit tests to prevent test runs from polluting live databases.
- **`spring-boot-maven-plugin`**: Packages the entire project into a self-contained executable JAR file.

---

### 4.2 `application.yml` (Main Configuration)
```yaml
spring:
  application:
    name: referral-outreach-manager
  datasource:
    url: ${DB_URL:jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:mail}}
    username: ${DB_USER:postgres}
    password: ${DB_PASSWORD:1234}
    driver-class-name: org.postgresql.Driver
  jpa:
    hibernate:
      ddl-auto: ${DDL_AUTO:update}
    show-sql: ${SHOW_SQL:true}
    properties:
      hibernate:
        format_sql: true
  jackson:
    time-zone: Asia/Kolkata
  servlet:
    multipart:
      max-file-size: 5MB
      max-request-size: 5MB

brevo:
  api:
    key: ${BREVO_API_KEY:}
  from:
    email: ${BREVO_FROM_EMAIL:ashrithbalajigudla@gmail.com}
    name: ${BREVO_FROM_NAME:Gudla Ashrith Balaji}

app:
  upload:
    dir: ${UPLOAD_DIR:./uploads/resumes}
  scheduler:
    cron: "0 0 9 * * MON" # Every Monday at 9:00 AM
    cooldown-days: 30     # Prevent duplicate outreach for 30 days
  candidate-name: ${CANDIDATE_NAME:Gudla Ashrith Balaji}
  target-role: ${TARGET_ROLE:Java Backend Developer}
```

#### Line-by-Line Breakdown:
1. `spring.application.name`: Names the Spring microservice context.
2. `spring.datasource.url`: Uses environment variable `${DB_URL}` if present on cloud platforms (e.g., Render, Docker). Otherwise, assembles `jdbc:postgresql://localhost:5432/mail` from `${DB_HOST:localhost}`, `${DB_PORT:5432}`, and `${DB_NAME:mail}`.
3. `spring.datasource.username` & `password`: Credentials with defaults `postgres` / `1234`.
4. `spring.jpa.hibernate.ddl-auto: update`: Automatically adds missing columns or tables without dropping existing records.
5. `show-sql: true` & `format_sql: true`: Logs nicely indented SQL queries in the terminal for real-time visibility.
6. `jackson.time-zone: Asia/Kolkata`: Enforces Indian Standard Time (IST - UTC+05:30) for all JSON date/time fields.
7. `servlet.multipart.max-file-size: 5MB`: Allows candidate resume uploads up to 5MB.
8. `brevo.api.key: ${BREVO_API_KEY:}`: Injects the Brevo API key from environment variables. The fallback colon ensures the application does not crash on startup if the variable is temporarily unset.
9. `brevo.from.email` & `name`: Sender credentials verified in Brevo.
10. `app.scheduler.cron: "0 0 9 * * MON"`: Cron string scheduling weekly outreach every Monday at 9:00 AM.
11. `app.scheduler.cooldown-days: 30`: Enforces a 30-day minimum gap between contacting the same recruiter twice.
12. `app.candidate-name` & `target-role`: Fallback variables for template generation.

---

### 4.3 `application-dev.yml` & `application-test.yml`
- **`application-dev.yml`**: Overrides development properties with PostgreSQL dialect configuration.
- **`application-test.yml`**: Uses an in-memory H2 database (`jdbc:h2:mem:testdb;MODE=PostgreSQL`) and `ddl-auto: create-drop` so that all 37 automated tests run instantly in isolation without requiring a live PostgreSQL instance.

---

## 5. Application Bootstrap & Entry Point

### 5.1 `OutreachApplication.java`
```java
package com.referral.outreach;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

@SpringBootApplication
@EnableScheduling
public class OutreachApplication {

    @PostConstruct
    public void init() {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kolkata"));
    }

    public static void main(String[] args) {
        SpringApplication.run(OutreachApplication.class, args);
    }
}
```

#### Line-by-Line Breakdown:
- **`package com.referral.outreach;`**: Defines the root package namespace.
- **`@SpringBootApplication`**: Combines three crucial annotations:
  1. `@Configuration`: Declares this class as a source of bean definitions.
  2. `@EnableAutoConfiguration`: Tells Spring Boot to configure Tomcat, Hibernate, and Jackson based on dependencies.
  3. `@ComponentScan`: Tells Spring to scan all classes in `com.referral.outreach` and instantiate components.
- **`@EnableScheduling`**: Boots the background timer engine to run `@Scheduled` methods (e.g. `WeeklyOutreachScheduler`).
- **`@PostConstruct public void init()`**: Executes once when Spring starts. It sets Java's global JVM timezone to `Asia/Kolkata` so all `LocalDateTime.now()` calls accurately record Indian Standard Time.
- **`SpringApplication.run(OutreachApplication.class, args);`**: Starts the embedded Tomcat web server on port 8080 and boots the application.

---

## 6. Security & Authentication Engine

### 6.1 `WebSecurityConfig.java`
Defines the network security perimeter:
- **`@EnableWebSecurity`**: Activates Spring Security's web protection.
- **`@Bean PasswordEncoder passwordEncoder()`**: Configures `BCryptPasswordEncoder` using a 10-round salt hashing algorithm so raw passwords are never stored in the database.
- **`@Bean SecurityFilterChain securityFilterChain(HttpSecurity http)`**:
  - `cors(...)`: Applies cross-origin headers allowing Vite/React frontend to connect.
  - `csrf(...).disable()`: Disables CSRF protection since REST APIs authenticate via Bearer JWT tokens rather than vulnerable browser cookies.
  - `exceptionHandling(...).authenticationEntryPoint(unauthorizedHandler)`: Dispatches a clean JSON 401 error if an unauthorized user attempts to access protected routes.
  - `sessionManagement(...).sessionCreationPolicy(SessionCreationPolicy.STATELESS)`: Configures the server to store zero session state in memory.
  - `authorizeHttpRequests(...)`:
    - Whitelists pre-flight `OPTIONS /**`, health check `/health`, and auth routes `/api/auth/**`.
    - Enforces authentication for all other endpoints via `.anyRequest().authenticated()`.
  - `http.addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class)`: Ensures our JWT filter inspects tokens **before** Spring attempts username/password authentication.
- **`corsConfigurationSource()`**: Allows connections from `localhost:5173`, `localhost:3000`, `https://referal-hub-frontend.vercel.app`, and cloud domains, supporting HTTP methods `GET`, `POST`, `PUT`, `PATCH`, `DELETE`, `OPTIONS`.

---

### 6.2 `JwtTokenProvider.java`
Handles the cryptographic lifecycle of JSON Web Tokens:
- **Secret Key Generation**: Derives an HMAC-SHA256 key from a secure 256-bit string.
- **`generateToken(Authentication authentication)`**:
  - Extracts the logged-in `UserPrincipal`.
  - Sets token issue date to `now` and expiration to `now + 86400000 ms` (24 hours).
  - Subject claim is set to the user's numeric database ID (`Long.toString(userPrincipal.getId())`).
  - Cryptographically signs the token with `SignatureAlgorithm.HS256`.
- **`getUserIdFromJWT(String token)`**: Parses the token claims and extracts the user ID.
- **`validateToken(String authToken)`**: Parses the token and returns `true` if valid. Catches and logs `SignatureException`, `MalformedJwtException`, `ExpiredJwtException`, and `IllegalArgumentException`.

---

### 6.3 `JwtAuthenticationFilter.java`
Runs on **every single incoming HTTP request** via `OncePerRequestFilter`:
1. Reads the `Authorization` request header.
2. Checks if the header begins with `"Bearer "`.
3. Strips the prefix to extract the raw token string.
4. Invokes `tokenProvider.validateToken(jwt)`.
5. If valid, extracts the `userId` and loads the user from `CustomUserDetailsService.loadUserById(userId)`.
6. Creates an authenticated `UsernamePasswordAuthenticationToken` and sets it inside Spring's `SecurityContextHolder.getContext().setAuthentication(...)`.
7. Allows the request to proceed down the filter chain to the targeted controller.

---

### 6.4 `SecurityUtils.java`
A helper service injected wherever user context is needed:
- **`getAuthenticatedUser()`**: Fetches the authentication token from `SecurityContextHolder`, casts the principal to `UserPrincipal`, and queries `UserRepository` for the live `User` entity. If unauthenticated, throws a `ResourceNotFoundException`.
- **`getCurrentUserId()`**: Fast method returning the user's database ID directly from the token principal.

---

## 7. Database Entities & Data Modeling

### 7.1 `User.java` (Table: `users`)
- `id`: Primary key (`@Id @GeneratedValue(strategy = GenerationType.IDENTITY)`).
- `username`: Unique username string (`@Column(unique = true, nullable = false)`).
- `email`: User's primary email (`@Column(unique = true, nullable = false)`).
- `password`: BCrypt-hashed password.
- Relationships:
  - `@OneToMany(mappedBy = "user")` with `EmailTemplate`.
  - `@OneToMany(mappedBy = "user")` with `Resume`.
  - `@OneToMany(mappedBy = "user")` with `Campaign`.

### 7.2 `CandidateProfile.java` (Table: `candidate_profile`)
- Stores personal branding details substituted into email templates.
- Fields: `fullName`, `roleName`, `email`, `linkedinUrl`, `githubUrl`, `phoneNumber`, `location`.
- `customFieldsJson`: Stored as `@Column(columnDefinition = "TEXT")`, holding arbitrary user-defined JSON variables (e.g. `{"portfolio": "...", "noticePeriod": "Immediate"}`) for flexible template placeholders.
- Relationship: `@OneToOne @JoinColumn(name = "user_id")` mapped to `User`.

### 7.3 `Recruiter.java` (Table: `recruiter`)
- The core contact record:
  - `name`: Recruiter or team name (e.g., "Recruitment Team", "John Doe").
  - `email`: Corporate email (`@Column(nullable = false, unique = true)`).
  - `title`: Professional role (e.g., "Technical Recruiter", "HR Specialist").
  - `company`: Employer company name.
  - `contactSet`: Integer partition (1, 2, 3...) for campaign targeting.
  - `status`: Enum `ACTIVE` or `INACTIVE` (`@Enumerated(EnumType.STRING)`).
  - `lastContactedDate`: Global timestamp of the most recent outreach.
  - `isPublic`: Boolean flag:
    - `true`: Public (Master) contact available for other users to adopt.
    - `false`: Private contact visible exclusively to the creator.
  - `addedBy`: Reference (`@ManyToOne`) to the `User` who created the contact (`null` for system-seeded contacts).

### 7.4 `UserRecruiterState.java` (Table: `user_recruiter_state`)
**Multi-User Isolation**:  
Prevents actions by one user from corrupting another user's contact directory!
- `user`: Reference to the specific user.
- `recruiter`: Reference to the recruiter.
- `status`: The user's private status override (`ACTIVE` or `INACTIVE`).
- `lastContactedDate`: The timestamp when **this specific user** contacted this recruiter.
- `customTitle` & `customCompany`: Allows personal corrections without modifying the global record.
- `isAdded`:
  - `true`: Recruiter is part of the user's active directory.
  - `false`: Recruiter was dismissed from the waiting queue.

### 7.5 `EmailTemplate.java` (Table: `email_template`)
- `templateName`: Title of the email template.
- `subject`: Subject line containing placeholder tags (`{{candidateName}}`, etc.).
- `body`: Full email body text (`@Column(columnDefinition = "TEXT")`).
- `createdTimestamp` & `updatedTimestamp`: Timestamps updated on creation and edits.
- Relationship: `@ManyToOne @JoinColumn(name = "user_id")` ensures private template ownership.

### 7.6 `Resume.java` (Table: `resume`)
Dual-storage PDF resume management:
- `filename`: Stored UUID filename on disk.
- `originalFilename`: Clean display name (e.g., `Ashrith_Balaji_Resume.pdf`).
- `filePath`: Local file path.
- `fileSize`: File length in bytes.
- `contentType`: MIME type (`application/pdf`).
- `fileData`: **Full Base64 string stored directly in PostgreSQL (`@Column(columnDefinition = "TEXT")`)**.  
  *Why dual storage?* Ephemeral cloud environments (like Render or Heroku) wipe the local disk on restarts. Storing the Base64 payload directly in PostgreSQL guarantees resumes are never lost!
- `isActive`: Boolean indicating the current default resume for campaigns.
- Relationship: `@ManyToOne @JoinColumn(name = "user_id")`.

### 7.7 `Campaign.java` (Table: `campaign`)
Outreach campaign configuration:
- `name`: Campaign sequence name.
- `emailTemplate`: Associated `EmailTemplate`.
- `resume`: Associated `Resume` attachment.
- `isEnabled`: Whether this campaign is active for automatic weekly execution.
- `targetSet`: Target contact set number (e.g. 1, 2, 3, or `null` for all).
- `targetTitleGroup`: Target role category filter (e.g. `TECHNICAL`, `HR`, `LEAD`, `ALL`).
- Relationship: `@ManyToOne @JoinColumn(name = "user_id")`.

### 7.8 `EmailHistory.java` (Table: `email_history`)
The immutable audit log:
- `recruiter`: Associated recruiter.
- `campaign`: Associated campaign (if triggered via sequence).
- `recipientEmail`: Exact recipient email address.
- `subjectUsed`: Compiled subject line with placeholders replaced.
- `sentTimestamp`: Dispatch timestamp in IST.
- `status`: Enum `SUCCESS` or `FAILED`.
- `errorMessage`: Detailed Brevo error message if delivery failed.
- Relationship: `@ManyToOne @JoinColumn(name = "user_id")`.

### 7.9 `PasswordResetToken.java` (Table: `password_reset_token`)
- `token`: Unique UUID reset token string.
- `user`: Associated `User` entity.
- `expiryDate`: Expiration timestamp (24 hours from creation).
- `isExpired()`: Helper method returning `LocalDateTime.now().isAfter(expiryDate)`.

---

## 8. Data Transfer Objects (DTOs)

Located in `com.referral.outreach.dto`:
- **`SignUpRequest`**: Validates `@NotBlank username`, `@NotBlank @Email email`, and `@NotBlank @Size(min = 6) password`.
- **`LoginRequest`**: Captures `@NotBlank username` and `@NotBlank password`.
- **`JwtAuthenticationResponse`**: Returns `accessToken`, `tokenType: "Bearer"`, `username`, and `email`.
- **`RecruiterRequest`**: Validates recruiter `name`, `email`, `title`, `company`, `contactSet`, and `isPublic`.
- **`RecruiterResponse`**: Returns formatted recruiter metadata, per-user status, and `isAdded` state.
- **`TemplateRequest`**: Validates `templateName`, `subject`, and `body`.
- **`TemplateResponse`**: Returns template details with formatted timestamps.
- **`CampaignRequest`**: Validates sequence name, template ID, resume ID, target set, and title group.
- **`CampaignResponse`**: Returns full campaign status, active flag, and associated template/resume names.
- **`PreviewResponse`**: Returns pre-compiled `recipientEmail`, `subject`, and `body` for UI preview modals.
- **`ResumeResponse`**: Returns resume filename, size, active status, and upload timestamp.
- **`EmailHistoryResponse`**: Formats audit logs with recruiter name, company, campaign, and timestamp.
- **`ErrorResponse`**: Standardized JSON error response returned by `GlobalExceptionHandler`.

---

## 9. Database Access Layer (Repositories)

Located in `com.referral.outreach.repository`:
- **`UserRepository`**: Methods `findByUsername`, `findByEmail`, `existsByUsername`, `existsByEmail`.
- **`RecruiterRepository`**:
  - `existsByEmail(String email)`: Checks duplicate emails.
  - `existsByEmailAndIdNot(String email, Long id)`: Checks duplicates during updates.
  - `findEligibleRecruitersFiltered(...)`: Custom `@Query` matching active recruiters outside the 30-day cooldown window, filtered by set and title category.
- **`UserRecruiterStateRepository`**: Methods `findByUser`, `findByUserAndRecruiter`.
- **`CampaignRepository`**:
  - `findByUser(User user)`: Retrieves campaigns owned by the user.
  - `findAllByIsEnabledTrue()`: Retrieves all active campaigns across the system for the weekly scheduler.
  - `@Modifying @Query disableOthers(...)`: Auto-disables other campaigns when a user enables one.
- **`TemplateRepository`**: Methods `findByUser`, `findByUserAndTemplateName`, `existsByUserAndTemplateName`.
- **`ResumeRepository`**: Methods `findByUser`, `findAllByUserAndIsActiveTrue`, `deactivateOthers(...)`.
- **`EmailHistoryRepository`**: Extends `JpaSpecificationExecutor<EmailHistory>` for dynamic filtering.
- **`CandidateProfileRepository`**: Method `findByUser(User user)`.
- **`PasswordResetTokenRepository`**: Methods `findByToken`, `deleteByUser`.

---

## 10. Business Logic & Services

### 10.1 `BrevoClient.java` (Pure HTTP REST Dispatcher to Brevo API)
Communicates directly with Brevo's cloud servers:
1. Builds a JSON payload containing:
   - `sender`: From name and verified email.
   - `to`: Recipient email and name.
   - `subject`: Compiled subject line.
   - `htmlContent` & `textContent`: The personalized message formatted with HTML linebreaks.
   - `attachment`: Array containing the resume filename and Base64-encoded PDF content.
2. Sends an HTTP POST request to `https://api.brevo.com/v3/smtp/email` with header `api-key: ${BREVO_API_KEY}`.
3. Parses Brevo's HTTP response:
   - Status 200–299: Extracts and returns the unique Brevo `messageId`.
   - Status 400+: Throws a runtime exception detailing why Brevo rejected the email (e.g. invalid recipient domain, quota exceeded).

### 10.2 `MailServiceImpl.java`
The core email dispatcher:
- **`getCandidateVariables(User user)`**: Loads candidate profile data (name, role, email, LinkedIn, GitHub, phone, location) plus custom dynamic fields from `customFieldsJson`. Populates alias mappings so `{{candidateName}}`, `{{name}}`, `{{roleName}}`, `{{mail}}`, `{{email}}`, `{{linkedin}}`, etc. all resolve seamlessly.
- **`sendOutreachEmail(recruiterId, templateId, resumeId, campaignId)`**:
  1. Resolves recruiter, template, and resume.
  2. Resolves the resume file: checks Base64 data from PostgreSQL first, falls back to disk file, or falls back to classpath default resume.
  3. Uses `TemplateParser` to compile subject and body.
  4. Calls `brevoClient.sendEmail(...)`.
  5. Updates `recruiter.lastContactedDate = LocalDateTime.now()`.
  6. Updates or creates `UserRecruiterState.lastContactedDate = LocalDateTime.now()`.
  7. Inserts an immutable `EmailHistory` record (`SUCCESS` or `FAILED`).

### 10.3 `RecruiterServiceImpl.java`
- **`getAllRecruiters()`**:
  - Automatically includes all ~2100 system-seeded recruiters (`addedBy == null`) for every user by default.
  - Includes user-created recruiters owned by the current user.
  - Applies per-user overrides from `UserRecruiterState`.
- **`getWaitingRecruiters()`**:
  - Excludes system recruiters (since they are already active).
  - Returns only new public recruiters created by **other** users that the current user has not yet adopted or dismissed.
- **`addWaitingRecruiters(List<Long> recruiterIds)`**: Adopts waiting recruiters into the user's personal directory by creating `UserRecruiterState(isAdded = true)`.
- **`dismissWaitingRecruiters(List<Long> recruiterIds)`**: Permanently dismisses waiting recruiters from the user's waiting tab by setting `UserRecruiterState(isAdded = false)`.
- **`importRecruitersFromCsv(file, setNumber, isPublic)`**:
  - Streams and parses uploaded CSV files line-by-line (`Name,Email,Title,Company`).
  - Skips invalid email formats and existing duplicates.
  - Saves contacts with the user's chosen privacy preference (Public Master vs. Private Only Me).
- **`exportRecruitersToCsv(setNumber)`**: Generates a downloadable UTF-8 CSV file containing the active recruiter directory.

### 10.4 `CampaignServiceImpl.java`
- Controls campaign lifecycle, enabling/disabling sequences, and preview generation.
- **`triggerCampaignBatch(campaignId, limit)`**:
  - Finds up to `limit` eligible recruiters who are active and not in cooldown.
  - Spawns an asynchronous background thread (`new Thread(...)`).
  - Iterates through the batch, sending each email via `mailService.sendOutreachEmail(...)`.
  - Enforces a **2-second rate-limiting delay** (`Thread.sleep(2000)`) between each dispatch to maintain high sender reputation and avoid spam triggers.
  - Checks mid-flight if the campaign was paused or disabled by the user to safely abort.

### 10.5 `ResumeServiceImpl.java`
- Validates uploaded files: verifies `contentType == "application/pdf"` and `.pdf` extension.
- Generates a sanitized UUID filename.
- Saves the physical PDF file to disk in `./uploads/resumes`.
- Encodes the file bytes into a **Base64 string** and persists it inside `resume.file_data` in PostgreSQL.
- **`markAsActive(id)`**: Uses `resumeRepository.deactivateOthers(id, userId)` to ensure only one resume is active per user at any given time.

### 10.6 `EmailHistoryServiceImpl.java`
Uses Spring Data JPA `Specification<EmailHistory>` to build dynamic SQL queries filtering audit logs by `recruiterId`, `company`, `status`, `startDate`, and `endDate`, strictly filtered by the authenticated user's ID.

---

## 11. REST Controllers & API Endpoints

### 11.1 `AuthController.java` (`/api/auth`)
- **`POST /api/auth/login`**: Authenticates credentials via `AuthenticationManager`, generates a JWT token, and returns `JwtAuthenticationResponse`.
- **`POST /api/auth/register`**: Validates unique username and email, hashes password with BCrypt, creates `User`, provisions a default `CandidateProfile`, and seeds 4 starter outreach templates.
- **`POST /api/auth/forgot-password`**: Generates a secure UUID reset token, saves it with a 24-hour expiration, and dispatches an account recovery email.
- **`POST /api/auth/reset-password`**: Validates the reset token, updates the user's password with BCrypt hashing, and invalidates the token.

### 11.2 `RecruiterController.java` (`/api/recruiters`)
- **`GET /api/recruiters`**: Returns all recruiters active for the authenticated user.
- **`POST /api/recruiters`**: Creates a new recruiter (respecting `isPublic: true/false`).
- **`PUT /api/recruiters/{id}`**: Updates recruiter details, saving status/title/company overrides privately to the user's state.
- **`DELETE /api/recruiters/{id}`**: Removes recruiter from user directory.
- **`PATCH /api/recruiters/{id}/status?status=ACTIVE`**: Toggles recruiter status.
- **`POST /api/recruiters/import`**: Accepts CSV file upload, target set number, and `isPublic` parameter.
- **`GET /api/recruiters/export`**: Returns downloadable CSV file attachment.
- **`GET /api/recruiters/waiting`**: Lists public community recruiters waiting for adoption.
- **`POST /api/recruiters/add-waiting`** & **`POST /api/recruiters/add-all-waiting`**: Adopts waiting recruiters.
- **`POST /api/recruiters/dismiss-waiting`** & **`POST /api/recruiters/dismiss-all-waiting`**: Dismisses waiting recruiters.

### 11.3 `CampaignController.java` (`/api/campaigns`)
- **`GET /api/campaigns`**: Lists all campaigns for current user.
- **`POST /api/campaigns`**: Creates a campaign sequence.
- **`PUT /api/campaigns/{id}`**: Updates campaign sequence.
- **`DELETE /api/campaigns/{id}`**: Deletes a campaign sequence and cascades cleanup.
- **`PUT /api/campaigns/{id}/enable`** & **`PUT /api/campaigns/{id}/disable`**: Toggles campaign activation.
- **`GET /api/campaigns/{id}/preview/{recruiterId}`**: Generates compiled preview of subject and body.
- **`POST /api/campaigns/{id}/trigger/{recruiterId}`**: Manually dispatches email to a single recruiter immediately.
- **`POST /api/campaigns/{id}/trigger-batch?limit=10`**: Triggers rate-limited batch outreach in a background thread.
- **`POST /api/campaigns/trigger-scheduler`**: Manually triggers the weekly scheduler immediately.

### 11.4 `ResumeController.java` (`/api/resumes`)
- **`POST /api/resumes`**: Accepts multipart file upload of PDF resume.
- **`GET /api/resumes/active`**: Fetches current active resume metadata.
- **`GET /api/resumes/{id}`**: Fetches resume by ID.
- **`GET /api/resumes`**: Lists all uploaded resumes for user.
- **`DELETE /api/resumes/{id}`**: Deletes physical file and database record.
- **`PATCH /api/resumes/{id}/active`**: Activates selected resume and deactivates others.

### 11.5 `TemplateController.java` (`/api/templates`)
- Standard CRUD endpoints: `GET /api/templates`, `POST /api/templates`, `GET /api/templates/{id}`, `PUT /api/templates/{id}`, `DELETE /api/templates/{id}`.

### 11.6 `CandidateProfileController.java` (`/api/candidate-profile`)
- **`GET /api/candidate-profile`**: Retrieves candidate profile information.
- **`PUT /api/candidate-profile`**: Saves candidate profile and custom JSON variables.

### 11.7 `EmailHistoryController.java` (`/api/email-history`)
- **`GET /api/email-history`**: Retrieves paginated/filtered email audit records by recruiter, company, status, and dates.

### 11.8 `HealthController.java` (`/health`)
- **`GET /health`**: Returns HTTP 200 OK `"OK"` for Docker/cloud container health checks.

---

## 12. Automated Scheduler

### 12.1 `WeeklyOutreachScheduler.java`
Runs on a background thread:
- **`@Scheduled(cron = "${app.scheduler.cron:0 0 9 * * MON}")`**:  
  Standard 6-field UNIX cron expression: `Seconds Minutes Hours Day-of-Month Month Day-of-Week`.  
  Triggers automatically **every Monday morning at 9:00 AM**.
- **Execution Flow**:
  1. Queries all active, enabled campaigns across the system (`findAllByIsEnabledTrue()`).
  2. For each active campaign, calculates the 30-day cooldown threshold (`LocalDateTime.now().minusDays(cooldownDays)`).
  3. Filters recruiters matching the campaign's contact set and target title category.
  4. Dispatches personalized emails via `mailService.sendOutreachEmail(...)`.
  5. Implements a **2-second rate-limiting delay** (`Thread.sleep(2000)`) between each email to guarantee high deliverability and prevent blacklisting.
  6. Checks if the campaign was turned off mid-sequence to gracefully halt if instructed.

---

## 13. Dynamic Template Parser Engine

### 13.1 `TemplateParser.java`
Replaces variable tags within template subjects and email bodies:
- Supports double-curly-bracket syntax: `{{variableName}}`.
- Case-insensitive replacement: handles `{{recruiterName}}`, `{{recruiter_name}}`, `{{RECRUITERNAME}}`.
- Pre-mapped universal aliases:
  - `{{recruiterName}}` / `{{recruiter_name}}` -> Recruiter's full name.
  - `{{companyName}}` / `{{company}}` -> Company name.
  - `{{candidateName}}` / `{{name}}` -> Candidate full name.
  - `{{roleName}}` / `{{targetRole}}` -> Candidate target job title.
  - `{{mail}}` / `{{email}}` -> Candidate contact email.
  - `{{linkedin}}` / `{{linkedinUrl}}` -> LinkedIn profile link.
  - `{{github}}` / `{{githubUrl}}` -> GitHub repository link.
  - Dynamic user-defined variables from `candidate_profile.customFieldsJson`.

---

## 14. Startup Data Seeder

### 14.1 `DataLoader.java`
Implements Spring's `CommandLineRunner`, executing immediately after the server boots up:
1. **Database Schema Cleanup**: Executes safety DDL to normalize legacy columns and ensures all recruiters have valid `contact_set` integers.
2. **Default User Account**: If no users exist, seeds primary user `ashrith` (`password: password`) and an initial `CandidateProfile`.
3. **Template Seeding**: Automatically seeds 4 comprehensive templates for every user:
   - Universal Referral Template
   - Dubai Outreach Template (Remote / Relocation)
   - Follow-Up Outreach Template
   - Direct Engineering Pitch Template
4. **Resume Seeding & Base64 Normalization**: Reads `default_resume.pdf` from classpath resources and populates the `file_data` Base64 column across all resumes in the database.
5. **CSV Recruiter Seeding**: If the recruiter table is empty, reads `recruiters.csv` from the classpath and bulk-loads ~2100 verified recruiters into Set 1.
6. **Set 3 Dubai Recruiters**: Injects verified corporate hiring emails for companies across Dubai and the UAE into Set 3.

---

## 15. Global Exception Handling

### 15.1 `GlobalExceptionHandler.java`
Annotated with `@RestControllerAdvice`:
- Intercepts exceptions thrown anywhere in controllers and transforms them into clean JSON error responses (`ErrorResponse` DTO) with proper HTTP status codes:
  - `ResourceNotFoundException` -> **404 Not Found**
  - `DuplicateRecruiterException` & `DuplicateTemplateException` -> **409 Conflict**
  - `InvalidFileException` -> **400 Bad Request**
  - `MailSendingException` -> **400 Bad Request**
  - `MethodArgumentNotValidException` -> **400 Bad Request** (formats clean validation error messages)
  - `MaxUploadSizeExceededException` -> **400 Bad Request** (formats clean 5MB limit error)
  - `BadCredentialsException` -> **401 Unauthorized** ("Invalid username or password")
  - `Exception` (catch-all) -> **500 Internal Server Error**

---

## 16. Complete Step-by-Step Workflow Walkthroughs

### Workflow 1: User Login & JWT Issuance
```
User Enters Credentials -> POST /api/auth/login -> AuthController
  └─> AuthenticationManager checks username & BCrypt password against DB
        └─> On Success: JwtTokenProvider generates HMAC-SHA256 Token
              └─> Returns { accessToken: "eyJhbG...", tokenType: "Bearer" }
                    └─> Frontend stores token in localStorage & attaches to all future headers
```

### Workflow 2: Recruiter Visibility & Waiting Queue
```
User Adds Recruiter (or Imports CSV)
  ├─> If marked "Private (Only Me)":
  │     └─> Saved with isPublic = false, addedBy = currentUser
  │     └─> Appears in currentUser's directory ONLY
  └─> If marked "Master (Public)":
        └─> Saved with isPublic = true, addedBy = currentUser
        └─> Appears in currentUser's directory
        └─> Appears in other users' "Waiting Recruiters" tab
              └─> Other users can click "Adopt" (adds to their list) or "Dismiss" (removes from waiting)
```

### Workflow 3: Single & Batch Email Outreach Execution
```
User clicks "Send Outreach" -> CampaignService
  ├─> Loads Recruiter, Template, Resume, and CandidateProfile
  ├─> TemplateParser replaces all {{placeholders}} with dynamic values
  ├─> BrevoClient formats JSON payload with Base64 PDF attachment
  ├─> HTTPS POST to https://api.brevo.com/v3/smtp/email
  ├─> Brevo responds with 200 OK & Message ID
  ├─> Recruiter's lastContactedDate updated to current timestamp (cooldown starts)
  └─> EmailHistory record created with status = SUCCESS
```

---

## 17. Comprehensive Annotation Reference & Cheat Sheet

| Annotation | Location | Exact Purpose in this Project |
|---|---|---|
| `@SpringBootApplication` | `OutreachApplication` | Enables component scanning, auto-configuration, and configuration bean registration. |
| `@EnableScheduling` | `OutreachApplication` | Powers background cron jobs for the weekly scheduler. |
| `@PostConstruct` | Methods | Runs automatically once after bean initialization (used to set IST timezone). |
| `@Configuration` | Classes | Declares the class as a creator of Spring beans (e.g. `WebSecurityConfig`). |
| `@EnableWebSecurity` | `WebSecurityConfig` | Activates custom Spring Security web filters and authorization rules. |
| `@Bean` | Methods inside config | Produces a Spring-managed object (e.g. `PasswordEncoder`, `SecurityFilterChain`). |
| `@Component` | Classes | Marks a general utility class as a Spring Bean (e.g. `BrevoClient`, `DataLoader`). |
| `@Service` | Classes | Marks classes containing core business logic (e.g. `MailServiceImpl`). |
| `@Repository` | Interfaces | Marks database access interfaces extending `JpaRepository`. |
| `@RestController` | Classes | Combines `@Controller` and `@ResponseBody` to return raw JSON data. |
| `@RequestMapping` | Classes / Methods | Sets the base URL path for controller endpoints (e.g. `/api/recruiters`). |
| `@GetMapping` | Methods | Maps HTTP GET requests to fetch data. |
| `@PostMapping` | Methods | Maps HTTP POST requests to create resources or trigger actions. |
| `@PutMapping` | Methods | Maps HTTP PUT requests to update existing resources. |
| `@PatchMapping` | Methods | Maps HTTP PATCH requests to partially update resources (e.g. `/status`). |
| `@DeleteMapping` | Methods | Maps HTTP DELETE requests to delete records. |
| `@PathVariable` | Method parameters | Extracts parameters from URL path (e.g. `/api/recruiters/{id}`). |
| `@RequestParam` | Method parameters | Extracts query parameters or form fields (e.g. `?setNumber=2`). |
| `@RequestBody` | Method parameters | Deserializes the incoming JSON body into a Java DTO object. |
| `@Valid` | Method parameters | Triggers validation annotations (e.g. `@NotBlank`, `@Email`) on the DTO. |
| `@Transactional` | Methods / Classes | Wraps method execution in a database transaction; rolls back on unhandled error. |
| `@Scheduled` | Methods | Defines recurring background schedules using cron expressions. |
| `@Value` | Fields | Injects properties and environment variables from `application.yml`. |
| `@Slf4j` | Classes | Generates a SLF4J `log` object for logging info, warnings, and errors. |
| `@Entity` | Classes | Marks a class as a persistent relational database table in JPA. |
| `@Table` | Classes | Specifies the exact database table name (e.g. `table = "recruiter"`). |
| `@Id` | Fields | Declares the primary key of the database table. |
| `@GeneratedValue` | Fields | Configures database auto-increment identity strategy. |
| `@Column` | Fields | Customizes column attributes (e.g. `nullable = false`, `unique = true`, `TEXT`). |
| `@ManyToOne` | Fields | Defines foreign key relationship where many rows reference one parent row. |
| `@OneToMany` | Fields | Defines relationship where one parent row owns multiple child rows. |
| `@OneToOne` | Fields | Defines direct 1-to-1 relationship (e.g. `User` to `CandidateProfile`). |
| `@JoinColumn` | Fields | Specifies the physical foreign key column name in the database table. |
| `@Enumerated` | Fields | Persists Java enum values as human-readable Strings in the database. |
| `@Data` | Classes (Lombok) | Generates getters, setters, `toString()`, `equals()`, and `hashCode()`. |
| `@Builder` | Classes (Lombok) | Implements the Builder design pattern for clean object creation. |
| `@NoArgsConstructor` | Classes (Lombok) | Generates a zero-argument constructor required by Hibernate. |
| `@AllArgsConstructor`| Classes (Lombok) | Generates a constructor accepting all class fields. |
| `@RequiredArgsConstructor` | Classes (Lombok) | Generates constructor for `final` fields, enabling Spring constructor dependency injection. |
| `@RestControllerAdvice`| `GlobalExceptionHandler`| Intercepts exceptions across all controllers globally to format error responses. |
| `@ExceptionHandler`| Methods | Catches specific exception classes and defines the HTTP response. |
| `@Modifying` | Repository methods | Instructs Spring Data JPA that the query executes an `UPDATE` or `DELETE` statement. |
| `@Query` | Repository methods | Defines custom JPQL or native SQL queries directly on repository methods. |
| `@Profile` | Classes | Restricts component loading to specific active profiles (e.g. `@Profile("!test")`). |

---

### 🎉 Conclusion
You now have the complete, line-by-line architectural reference for the entire Referral Outreach Manager backend. Every single component, method, annotation, security filter, and workflow is thoroughly documented.
