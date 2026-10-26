# 🚀 Referral Outreach Manager — Comprehensive Backend Code & Architecture Documentation
> **File Name:** `Readme_Code_Explanation.md`  
> **Author & Project Owner:** Gudla Ashrith Balaji  
> **Target Audience:** Complete beginners with **zero prior knowledge** in Java, Spring Boot, or Backend Architecture.

---

## 📑 Table of Contents
1. [Executive Summary & System Purpose](#1-executive-summary--system-purpose)
2. [Absolute Beginner's Primer: Core Concepts Explained](#2-absolute-beginners-primer-core-concepts-explained)
   - What is Java & Spring Boot?
   - What is an Annotation (`@`)?
   - What is Dependency Injection & Inversion of Control (IoC)?
   - What is a REST API & HTTP Methods?
   - What is JWT (JSON Web Token) Authentication?
   - What is Spring Data JPA & ORM?
3. [End-to-End Architecture & Directory Layout](#3-end-to-end-architecture--directory-layout)
4. [Master Configuration Files Explained Line-by-Line](#4-master-configuration-files-explained-line-by-line)
   - `pom.xml` (Maven Dependencies & Build Lifecycle)
   - `application.yml` (Main Server & Database Configuration)
   - `application-dev.yml` (Development Profile)
   - `application-test.yml` (Unit & Integration Testing Profile)
5. [Application Bootstrap & Entry Point](#5-application-bootstrap--entry-point)
   - `OutreachApplication.java`
6. [Security & Authentication Engine (`com.referral.outreach.security`)](#6-security--authentication-engine)
   - `WebSecurityConfig.java` (CORS, CSRF, Route Protection, Filter Chain)
   - `JwtTokenProvider.java` (Token Generation, Signing, Validation)
   - `JwtAuthenticationFilter.java` (Intercepting Every HTTP Request)
   - `JwtAuthenticationEntryPoint.java` (401 Unauthorized Handler)
   - `CustomUserDetailsService.java` & `UserPrincipal.java` (User Identity)
   - `SecurityUtils.java` (Logged-In User Extraction)
7. [Database Entities & Data Modeling (`com.referral.outreach.entity`)](#7-database-entities--data-modeling)
   - `User.java` (System Accounts)
   - `CandidateProfile.java` (User Details, Links, Custom Variables)
   - `Recruiter.java` (HR & Recruiter Directory)
   - `RecruiterStatus.java` (ACTIVE / INACTIVE Status)
   - `UserRecruiterState.java` (Per-User Status Overrides & Adoptions)
   - `EmailTemplate.java` (Dynamic Outreach Templates)
   - `Resume.java` (PDF Resume Storage on Disk & Base64 in Database)
   - `Campaign.java` (Mapping Template + Resume + Set + Title Group)
   - `EmailHistory.java` & `EmailHistoryStatus.java` (Audit Logs)
   - `PasswordResetToken.java` (Account Recovery)
8. [Data Transfer Objects (`com.referral.outreach.dto`)](#8-data-transfer-objects-dtos)
9. [Database Access Layer (`com.referral.outreach.repository`)](#9-database-access-layer-repositories)
10. [Business Logic & External Integrations (`com.referral.outreach.service`)](#10-business-logic--services)
    - `BrevoClient.java` (Pure HTTP REST Dispatcher to Brevo API)
    - `MailService.java` & `MailServiceImpl.java` (Email Workflow & Placeholder Compilation)
    - `RecruiterService.java` & `RecruiterServiceImpl.java` (CRUD, Import/Export, Waiting Queue)
    - `CampaignService.java` & `CampaignServiceImpl.java` (Sequences, Batch Dispatches)
    - `TemplateService.java` & `TemplateServiceImpl.java`
    - `ResumeService.java` & `ResumeServiceImpl.java`
    - `CandidateProfileService.java` & `CandidateProfileServiceImpl.java`
    - `EmailHistoryService.java` & `EmailHistoryServiceImpl.java`
11. [REST Controllers & API Endpoints (`com.referral.outreach.controller`)](#11-rest-controllers--api-endpoints)
12. [Automated Scheduler (`com.referral.outreach.scheduler`)](#12-automated-scheduler)
    - `WeeklyOutreachScheduler.java` (Rate-Limited Background Execution)
13. [Dynamic Template Parser Engine (`com.referral.outreach.util`)](#13-dynamic-template-parser-engine)
    - `TemplateParser.java` (Regex & Placeholder Substitution)
14. [Startup Data Seeder (`com.referral.outreach.config`)](#14-startup-data-seeder)
    - `DataLoader.java` (Auto-Seeding 2101+ Recruiters, Resumes, Templates)
15. [Global Exception Handling (`com.referral.outreach.exception`)](#15-global-exception-handling)
16. [Complete Step-by-Step Workflow Walkthroughs (Life of a Request)](#16-complete-step-by-step-workflow-walkthroughs)
17. [Comprehensive Annotation Cheat Sheet](#17-comprehensive-annotation-cheat-sheet)

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

### 2.4 What is a REST API & HTTP Methods?
The Frontend (React/Vite) and Backend (Spring Boot) communicate over the network using HTTP. The backend exposes **Endpoints** (URLs):
- **GET**: Retrieve data (e.g., `GET /api/recruiters` fetches the contact list).
- **POST**: Create new data or trigger an action (e.g., `POST /api/auth/login`).
- **PUT**: Update existing data completely (e.g., `PUT /api/campaigns/1`).
- **DELETE**: Remove data (e.g., `DELETE /api/recruiters/5`).

### 2.5 What is JWT (JSON Web Token)?
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

### Directory Tree:
```
Backend/
├── pom.xml                                   <-- Maven project definition & libraries
├── src/
│   ├── main/
│   │   ├── java/com/referral/outreach/
│   │   │   ├── OutreachApplication.java      <-- Spring Boot Application Entry Point
│   │   │   ├── config/                       <-- Data seeding & database setup
│   │   │   ├── controller/                   <-- REST API Endpoints
│   │   │   ├── dto/                          <-- Request & Response Data Transfer Objects
│   │   │   ├── entity/                       <-- JPA Database Entities
│   │   │   ├── exception/                    <-- Custom Exceptions & Global Error Handler
│   │   │   ├── repository/                   <-- Spring Data JPA Repositories
│   │   │   ├── scheduler/                    <-- Background Cron Task
│   │   │   ├── security/                     <-- Security, JWT Provider, Filters, CORS
│   │   │   ├── service/                      <-- Business Logic Interfaces & Clients
│   │   │   │   └── impl/                     <-- Business Logic Implementations
│   │   │   └── util/                         <-- Template Parser & String Utilities
│   │   └── resources/
│   │       ├── application.yml               <-- Primary App Configuration
│   │       ├── application-dev.yml           <-- Development Overrides
│   │       ├── default_resume.pdf            <-- Fallback resume file
│   │       └── recruiters.csv                <-- Seed file with ~2100 HR contacts
│   └── test/
│       ├── java/                             <-- 37 Unit & Integration Tests
│       └── resources/
│           └── application-test.yml          <-- In-Memory H2 DB for Automated Tests
└── uploads/
    └── resumes/                              <-- Physical uploaded PDF storage
```

---

## 4. Master Configuration Files Explained Line-by-Line

### 4.1 `pom.xml` (Project Object Model)
Maven uses this XML file to manage dependencies, Java versions, and build plugins:

```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.3.1</version>
</parent>
```
- **Explanation**: Inherits default configuration, dependency versions, and compiler settings from Spring Boot 3.3.1.

```xml
<properties>
    <java.version>17</java.version>
</properties>
```
- **Explanation**: Enforces modern Java 17 LTS (Long Term Support) compiler target.

#### Core Dependencies:
1. `spring-boot-starter-web`: Embeds Apache Tomcat server, provides Spring MVC for creating REST controllers (`@RestController`, `@GetMapping`).
2. `spring-boot-starter-data-jpa`: Adds Hibernate ORM and Spring Data JPA for effortless database interaction without writing raw SQL.
3. `spring-boot-starter-validation`: Adds `@Valid`, `@NotBlank`, `@Email`, `@Size` validation annotations to automatically reject bad data before it enters business logic.
4. `postgresql`: The official JDBC database driver that enables Java to communicate with PostgreSQL.
5. `lombok`: A compiler library that automatically generates getters, setters, constructors, builders, and loggers during compilation, keeping code clean.
6. `spring-boot-starter-security`: Provides authentication, authorization, password encryption (BCrypt), and request filtering.
7. `jjwt-api`, `jjwt-impl`, `jjwt-jackson`: The standard Java JWT library used to create, digitally sign, and parse JSON Web Tokens using HMAC-SHA algorithms.
8. `h2`: Fast in-memory database used exclusively during unit tests so tests do not touch your live PostgreSQL database.
9. `spring-boot-maven-plugin`: Packages the application into an executable `.jar` file containing all libraries.

---

### 4.2 `application.yml` (Main Configuration)

```yaml
spring:
  application:
    name: referral-outreach-manager
```
- **Explanation**: Assigns an identifier to the Spring Boot microservice.

```yaml
  datasource:
    url: ${DB_URL:jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:mail}}
    username: ${DB_USER:postgres}
    password: ${DB_PASSWORD:1234}
    driver-class-name: org.postgresql.Driver
```
- **Explanation**: Connects to the PostgreSQL database. Uses intelligent fallback syntax `${VAR:default}`:
  - If an environment variable named `DB_URL` exists (e.g. on Render or Docker), it uses that.
  - Otherwise, it falls back to `jdbc:postgresql://localhost:5432/mail`.
  - Default user: `postgres`, default password: `1234`.

```yaml
  jpa:
    hibernate:
      ddl-auto: ${DDL_AUTO:update}
    show-sql: ${SHOW_SQL:true}
    properties:
      hibernate:
        format_sql: true
```
- **`ddl-auto: update`**: Hibernate compares Java `@Entity` classes against PostgreSQL tables. If a column or table is missing, Hibernate creates or alters it automatically without destroying existing data.
- **`show-sql: true` & `format_sql: true`**: Prints formatted SQL queries executed by the application directly into the console for easy debugging.

```yaml
  jackson:
    time-zone: Asia/Kolkata
```
- **Explanation**: Guarantees that all JSON dates, timestamps, and audit records format into Indian Standard Time (IST - UTC+05:30).

```yaml
  servlet:
    multipart:
      max-file-size: 5MB
      max-request-size: 5MB
```
- **Explanation**: Allows users to upload PDF resumes up to 5 Megabytes in size.

```yaml
brevo:
  api:
    key: ${BREVO_API_KEY:}
  from:
    email: ${BREVO_FROM_EMAIL:ashrithbalajigudla@gmail.com}
    name: ${BREVO_FROM_NAME:Gudla Ashrith Balaji}
```
- **Explanation**: Configures the Brevo API integration.
  - `key`: The secret Brevo API key passed as an environment variable `${BREVO_API_KEY:}` with an optional fallback colon to avoid startup crashes if not yet supplied.
  - `from.email`: The verified sender email address registered with Brevo.
  - `from.name`: The sender name displayed in recruiters' email inboxes.

```yaml
app:
  upload:
    dir: ${UPLOAD_DIR:./uploads/resumes}
  scheduler:
    cron: "0 0 9 * * MON" # Every Monday at 9:00 AM
    cooldown-days: 30     # Prevent duplicate outreach for 30 days
  candidate-name: ${CANDIDATE_NAME:Gudla Ashrith Balaji}
  target-role: ${TARGET_ROLE:Java Backend Developer}
```
- **Explanation**: Custom application properties:
  - `upload.dir`: Directory where uploaded PDF resumes are saved.
  - `scheduler.cron`: Cron expression defining when the weekly emailer runs (Monday 9:00 AM).
  - `cooldown-days`: 30-day safeguard preventing contacting the same recruiter twice within a month.
  - `candidate-name` & `target-role`: Default fallback values for template variable replacement.

---

## 5. Application Bootstrap & Entry Point

### `OutreachApplication.java`
```java
package com.referral.outreach;

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
- **`@SpringBootApplication`**: The master annotation combining:
  1. `@Configuration`: Marks the class as a source of bean definitions.
  2. `@EnableAutoConfiguration`: Tells Spring Boot to automatically configure web servers, database pools, and serializers based on classpath libraries.
  3. `@ComponentScan`: Automatically scans `com.referral.outreach` and all subpackages to discover and instantiate components, services, and controllers.
- **`@EnableScheduling`**: Enables background scheduling engine to execute cron jobs like `WeeklyOutreachScheduler`.
- **`@PostConstruct init()`**: Executes immediately once the Spring container starts, setting the JVM's default timezone to `Asia/Kolkata` so all `LocalDateTime.now()` calls match Indian Standard Time.
- **`main(...)`**: The standard Java entry point that boots the Tomcat server on port 8080.

---

## 6. Security & Authentication Engine

### 6.1 `WebSecurityConfig.java`
The command center for application network security:
- **`@Configuration` & `@EnableWebSecurity`**: Declares custom Spring Security filter chains.
- **`@Bean passwordEncoder()`**: Configures `BCryptPasswordEncoder`, a one-way mathematical hashing algorithm with random salting. Even if someone accesses the database, passwords cannot be decrypted.
- **`@Bean securityFilterChain(HttpSecurity http)`**:
  - `cors(...)`: Applies cross-origin rules to allow the React frontend to communicate with the backend.
  - `csrf(...).disable()`: Disables CSRF (Cross-Site Request Forgery) because the API is stateless and authenticates via Bearer JWT tokens, not browser session cookies.
  - `sessionManagement(STATELESS)`: Prevents Spring from storing HTTP sessions in memory.
  - `authorizeHttpRequests(...)`:
    - Public endpoints: `OPTIONS /**` (pre-flight checks), `/health` (monitoring), `/api/auth/**` (registration and login).
    - Protected endpoints: `.anyRequest().authenticated()` — all other URLs require a valid JWT token.
  - `addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class)`: Injects our custom token validator ahead of Spring's standard filter.
- **`corsConfigurationSource()`**: Configures allowed origins:
  - `http://localhost:5173`, `http://localhost:3000` (Local Vite and React dev servers).
  - `https://referal-hub-frontend.vercel.app`, `https://*.vercel.app`, `https://*.onrender.com` (Production cloud domains).
  - Supports custom origin via `FRONTEND_URL` environment variable.

### 6.2 `JwtTokenProvider.java`
Responsible for token cryptography:
- **`generateToken(Authentication authentication)`**: Reads the user identity, sets the issue date to now, expiration to 24 hours (86,400,000 ms), and signs the payload with HMAC-SHA256 (`Keys.hmacShaKeyFor(...)`).
- **`getUserIdFromJWT(String token)`**: Decrypts the token claims and extracts the user's numeric database ID.
- **`validateToken(String authToken)`**: Verifies token signature, expiration date, and formatting. Catches `SignatureException`, `MalformedJwtException`, `ExpiredJwtException`, and `IllegalArgumentException`, logging specific warnings.

### 6.3 `JwtAuthenticationFilter.java`
Extends `OncePerRequestFilter`:
- Executes on **every single incoming HTTP request**.
- Reads the `Authorization` HTTP header.
- Checks if it starts with `Bearer `.
- Extracts and validates the token string.
- Retrieves the user details via `CustomUserDetailsService`.
- Builds an authenticated `UsernamePasswordAuthenticationToken` and stores it in Spring's `SecurityContextHolder.getContext().setAuthentication(auth)`.
- Downstream controllers can now immediately identify the user via `SecurityUtils`.

### 6.4 `SecurityUtils.java`
Helper component injected into services:
- **`getAuthenticatedUser()`**: Fetches the currently authenticated `UserPrincipal` from the `SecurityContext` and retrieves the full `User` entity from `UserRepository`. Throws a `ResourceNotFoundException` if not authenticated.
- **`getCurrentUserId()`**: Fast shortcut returning the current user's database ID.

---

## 7. Database Entities & Data Modeling

All entities reside in `com.referral.outreach.entity`.

### 7.1 `User.java` (Table: `users`)
- `id`: Primary key (`BIGSERIAL`).
- `username`: Unique login handle (`@Column(unique = true, nullable = false)`).
- `email`: User's primary email address.
- `password`: BCrypt hashed password string.
- Relationships:
  - `@OneToMany` with `EmailTemplate` (one user owns multiple templates).
  - `@OneToMany` with `Resume` (one user owns uploaded resumes).
  - `@OneToMany` with `Campaign` (one user owns outreach campaigns).

### 7.2 `CandidateProfile.java` (Table: `candidate_profile`)
Stores personal branding variables substituted into email templates:
- `fullName`: Display name (e.g. "Gudla Ashrith Balaji").
- `roleName`: Target job title (e.g. "Java Backend Developer").
- `email`: Contact email address.
- `linkedinUrl`: Full LinkedIn profile URL.
- `githubUrl`: GitHub portfolio link.
- `phoneNumber`: Phone number.
- `location`: Current city / country.
- `customFieldsJson`: Open-ended JSON string (`@Column(columnDefinition = "TEXT")`) allowing users to add custom template variables dynamically (e.g. `{"portfolio": "mywebsite.com", "noticePeriod": "Immediate"}`).
- `@OneToOne` with `User`.

### 7.3 `Recruiter.java` (Table: `recruiter`)
The master contact record:
- `id`: Unique recruiter ID.
- `name`: Recruiter or Hiring Manager name (e.g. "Recruitment Team").
- `email`: Recruiter's corporate email address (`@Column(nullable = false, unique = true)`).
- `title`: Professional title (e.g. "Technical Recruiter", "HR Manager").
- `company`: Company name (e.g. "Google", "Amazon", "Infosys").
- `contactSet`: Integer partition (Set 1, Set 2, Set 3, etc.) for batching campaigns.
- `status`: Enum `ACTIVE` or `INACTIVE`.
- `lastContactedDate`: Timestamp of the last email dispatched to this recruiter.
- `isPublic`: Boolean flag:
  - `true`: Public (Master) contact shared across users.
  - `false`: Private contact visible only to the creator.
- `addedBy`: Reference (`@ManyToOne`) to the `User` who created the contact (`null` for initial system-seeded recruiters).

### 7.4 `UserRecruiterState.java` (Table: `user_recruiter_state`)
**The Multi-User Separation Key**:  
Ensures one user contacting a recruiter does not disrupt other users' pipelines!
- `user`: Reference to the user.
- `recruiter`: Reference to the recruiter.
- `status`: The user's private status for this recruiter (`ACTIVE` or `INACTIVE`).
- `lastContactedDate`: The timestamp when *this specific user* last contacted this recruiter.
- `customTitle` & `customCompany`: Allows a user to customize a recruiter's title or company without modifying the global master record.
- `isAdded`:
  - `true`: The recruiter is part of the user's active directory.
  - `false`: The user dismissed this recruiter from their waiting list.

### 7.5 `EmailTemplate.java` (Table: `email_template`)
- `templateName`: Human-readable title (e.g. "Universal Referral Template").
- `subject`: Email subject containing placeholders (e.g. `Job Referral Request - {{candidateName}} - {{roleName}}`).
- `body`: Full email body text with placeholders (`@Column(columnDefinition = "TEXT")`).
- `createdTimestamp` & `updatedTimestamp`: Automatically managed date records.
- `@ManyToOne` with `User`: Ensures each user manages their own private templates.

### 7.6 `Resume.java` (Table: `resume`)
Dual-storage PDF resume management:
- `filename`: Stored UUID filename on disk.
- `originalFilename`: The original uploaded name (e.g. `Ashrith_Balaji_Resume.pdf`).
- `filePath`: Local file system path.
- `fileSize`: File length in bytes.
- `contentType`: MIME type (`application/pdf`).
- `fileData`: **Full Base64-encoded string stored directly in PostgreSQL (`@Column(columnDefinition = "TEXT")`)**.  
  *Why dual storage?* Ephemeral cloud environments (like Render or Heroku) wipe the local disk on restarts. Storing the Base64 payload directly in PostgreSQL guarantees resumes are never lost!
- `isActive`: Boolean flag indicating which resume is currently active for outreach campaigns.
- `@ManyToOne` with `User`.

### 7.7 `Campaign.java` (Table: `campaign`)
The outreach configuration mapping:
- `name`: Campaign title (e.g. "Batch 1 - Tech Recruiters").
- `emailTemplate`: Associated `EmailTemplate`.
- `resume`: Associated `Resume` attachment.
- `isEnabled`: Whether this campaign is active for automatic weekly execution. (Only one campaign can be enabled per user at a time).
- `targetSet`: Which contact set to target (e.g. 1, 2, 3, or `null` for all).
- `targetTitleGroup`: Filter group (e.g. `TECHNICAL`, `HR`, `LEAD`, or `ALL`).
- `@ManyToOne` with `User`.

### 7.8 `EmailHistory.java` (Table: `email_history`)
The immutable audit trail:
- `recruiter`: Recruiter who received the email.
- `campaign`: Campaign that triggered the email.
- `recipientEmail`: Email address dispatched to.
- `subjectUsed`: The final compiled subject line with all variables replaced.
- `sentTimestamp`: Exact timestamp of dispatch (IST).
- `status`: `SUCCESS` or `FAILED`.
- `errorMessage`: Full Brevo API error message if dispatch failed.
- `@ManyToOne` with `User`.

---

## 8. Data Transfer Objects (DTOs)

DTOs reside in `com.referral.outreach.dto`.  
**Why use DTOs instead of Database Entities?**
1. **Security**: Never expose sensitive fields (like hashed passwords) to the frontend.
2. **Decoupling**: The database schema can evolve without breaking the frontend API contracts.
3. **Input Validation**: Annotations like `@NotBlank`, `@Email`, and `@Size` validate user input before touching the database.

### Key DTOs:
- `SignUpRequest`: Validates `username`, `email`, and `password` on registration.
- `LoginRequest`: Captures `usernameOrEmail` and `password`.
- `JwtAuthenticationResponse`: Returns `accessToken`, `tokenType: "Bearer"`, and user profile data upon successful login.
- `RecruiterRequest` & `RecruiterResponse`: Carries recruiter contact details, contact set, public/private visibility, and per-user custom fields.
- `TemplateRequest` & `TemplateResponse`: Handles template name, subject, and body text.
- `CampaignRequest` & `CampaignResponse`: Handles sequence settings, target sets, and active toggles.
- `PreviewResponse`: Returns preview of the compiled subject and body before sending.
- `ResumeResponse`: Returns metadata of the candidate's active and archived resumes.
- `EmailHistoryResponse`: Formats delivery logs with formatted timestamps for the audit table.

---

## 9. Database Access Layer (Repositories)

All repositories reside in `com.referral.outreach.repository` and extend Spring Data's `JpaRepository<Entity, ID>`.

### The Magic of Spring Data JPA:
You write only an interface with method signatures. Spring automatically parses the method name and creates the SQL query:
- `findByUsername(String username)`: Generates `SELECT * FROM users WHERE username = ?`.
- `existsByEmail(String email)`: Generates `SELECT count(*) > 0 FROM recruiter WHERE email = ?`.

### Custom Repository Methods Explained:
1. **`RecruiterRepository.java`**:
   ```java
   @Query("SELECT r FROM Recruiter r WHERE r.status = :status " +
          "AND (r.lastContactedDate IS NULL OR r.lastContactedDate < :cooldownLimit) " +
          "AND (:targetSet IS NULL OR r.contactSet = :targetSet) " +
          "AND (:isAllGroup = true OR UPPER(r.title) LIKE ...)")
   List<Recruiter> findEligibleRecruitersFiltered(...);
   ```
   - Dynamically selects active recruiters who have **not** been emailed within the 30-day cooldown period, match the target contact set, and match the specified recruiter title category.
2. **`CampaignRepository.java`**:
   ```java
   @Modifying
   @Query("UPDATE Campaign c SET c.isEnabled = false WHERE c.id != :campaignId AND c.user.id = :userId")
   void disableOthers(@Param("campaignId") Long campaignId, @Param("userId") Long userId);
   ```
   - Ensures single active campaign discipline: when a user enables one campaign, all their other campaigns are automatically disabled.

---

## 10. Business Logic & Services

Located in `com.referral.outreach.service` and `service.impl`.

### 10.1 `BrevoClient.java` (Pure HTTP API Client)
Instead of relying on heavy third-party SDKs or SMTP ports that cloud providers block, `BrevoClient` uses Java 11's built-in `HttpClient`:
1. Constructs a JSON request payload containing:
   - `sender`: Name and email verified in Brevo.
   - `to`: Recipient email and name.
   - `subject`: Compiled email subject.
   - `htmlContent` & `textContent`: The personalized message formatted with HTML linebreaks.
   - `attachment`: Array containing the resume file name and its Base64-encoded file data.
2. Sends an HTTP POST request to `https://api.brevo.com/v3/smtp/email` with header `api-key: ${BREVO_API_KEY}`.
3. Parses Brevo's HTTP response:
   - Status 200–299: Returns the unique Brevo `messageId`.
   - Status 400+: Throws a runtime exception detailing why Brevo rejected the email (e.g. invalid recipient domain, quota exceeded).

### 10.2 `MailServiceImpl.java`
The core outreach coordinator:
- **`getCandidateVariables(User user)`**: Loads the candidate's profile, links, and custom JSON fields. Populates alias maps so `{{candidateName}}`, `{{name}}`, `{{roleName}}`, `{{mail}}`, `{{email}}`, `{{linkedin}}`, etc. all resolve seamlessly.
- **`sendOutreachEmail(recruiterId, templateId, resumeId, campaignId)`**:
  1. Loads the recruiter, template, and resume.
  2. Resolves the resume file: checks Base64 data from PostgreSQL first, falls back to disk file, or falls back to classpath default resume.
  3. Uses `TemplateParser` to compile subject and body.
  4. Calls `brevoClient.sendEmail(...)`.
  5. Updates `recruiter.lastContactedDate = LocalDateTime.now()`.
  6. Updates or creates `UserRecruiterState.lastContactedDate = LocalDateTime.now()`.
  7. Inserts an immutable `EmailHistory` record (`SUCCESS` or `FAILED`).

### 10.3 `RecruiterServiceImpl.java`
Manages the contact directory:
- **`getAllRecruiters()`**:
  - Automatically includes all ~2100 system-seeded recruiters (`addedBy == null`) for every user by default.
  - Includes user-created recruiters owned by the current user.
  - Applies per-user overrides from `UserRecruiterState`.
- **`getWaitingRecruiters()`**:
  - Excludes system recruiters (since they are already active).
  - Returns only new public recruiters created by **other** users that the current user has not yet adopted or dismissed.
- **`importRecruitersFromCsv(file, setNumber, isPublic)`**:
  - Streams and parses uploaded CSV files line-by-line (`Name,Email,Title,Company`).
  - Skips invalid email formats and existing duplicates.
  - Saves contacts with the user's chosen privacy preference (Public Master vs. Private Only Me).
- **`exportRecruitersToCsv(setNumber)`**:
  - Generates a downloadable UTF-8 CSV file containing the active recruiter directory.

### 10.4 `CampaignServiceImpl.java`
- Controls campaign lifecycle, enabling/disabling sequences, and preview generation.
- **`triggerCampaignBatch(campaignId, limit)`**:
  - Finds up to `limit` eligible recruiters who are active and not in cooldown.
  - Spawns an asynchronous background thread (`new Thread(...)`).
  - Iterates through the batch, sending each email via `mailService.sendOutreachEmail(...)`.
  - Enforces a **2-second rate-limiting delay** (`Thread.sleep(2000)`) between each dispatch to maintain high sender reputation and avoid spam triggers.
  - Checks mid-flight if the campaign was paused or disabled by the user to safely abort.

---

## 11. REST Controllers & API Endpoints

All controllers reside in `com.referral.outreach.controller` and are annotated with `@RestController` and `@RequestMapping`.

| Method | Endpoint | Description | Auth Required |
|---|---|---|---|
| `GET` | `/health` | Server liveness & health check | No |
| `POST` | `/api/auth/signup` | Register new user account | No |
| `POST` | `/api/auth/login` | Authenticate user & return JWT token | No |
| `POST` | `/api/auth/forgot-password` | Request password reset token | No |
| `POST` | `/api/auth/reset-password` | Set new password using reset token | No |
| `GET` | `/api/recruiters` | Get all active recruiters for logged-in user | Yes |
| `POST` | `/api/recruiters` | Create a new recruiter (Public or Private) | Yes |
| `PUT` | `/api/recruiters/{id}` | Update recruiter details (Customized per-user) | Yes |
| `DELETE` | `/api/recruiters/{id}` | Delete recruiter from user's directory | Yes |
| `PATCH` | `/api/recruiters/{id}/status`| Toggle recruiter status (ACTIVE / INACTIVE) | Yes |
| `POST` | `/api/recruiters/import` | Bulk CSV import with visibility selector | Yes |
| `GET` | `/api/recruiters/export` | Download recruiters as CSV | Yes |
| `GET` | `/api/recruiters/waiting`| List newly shared community recruiters | Yes |
| `POST` | `/api/recruiters/waiting/add` | Adopt waiting recruiters into user directory | Yes |
| `POST` | `/api/recruiters/waiting/dismiss` | Dismiss waiting recruiters permanently | Yes |
| `GET` | `/api/templates` | List all email templates for logged-in user | Yes |
| `POST` | `/api/templates` | Create a new outreach email template | Yes |
| `PUT` | `/api/templates/{id}` | Update template content and placeholders | Yes |
| `DELETE` | `/api/templates/{id}` | Delete an email template | Yes |
| `GET` | `/api/resumes` | List uploaded resumes for user | Yes |
| `POST` | `/api/resumes/upload` | Upload new PDF resume (Stored on disk & DB) | Yes |
| `PUT` | `/api/resumes/{id}/activate` | Set resume as the primary active document | Yes |
| `GET` | `/api/resumes/{id}/download` | Download physical PDF resume | Yes |
| `GET` | `/api/campaigns` | List all campaign sequences for user | Yes |
| `POST` | `/api/campaigns` | Create new campaign mapping template & resume | Yes |
| `PUT` | `/api/campaigns/{id}/enable`| Enable campaign (auto-disables all others) | Yes |
| `GET` | `/api/campaigns/{id}/preview/{recId}` | Preview compiled email for a recruiter | Yes |
| `POST` | `/api/campaigns/{id}/trigger/{recId}` | Manually send email to single recruiter | Yes |
| `POST` | `/api/campaigns/{id}/trigger-batch` | Send to batch of contacts with rate limit | Yes |
| `POST` | `/api/campaigns/trigger-scheduler` | Trigger weekly automated sequence immediately | Yes |
| `GET` | `/api/history` | Retrieve filtered email audit history | Yes |
| `GET` | `/api/profile` | Retrieve candidate profile variables | Yes |
| `POST` | `/api/profile/save` | Update candidate profiler & custom JSON fields | Yes |

---

## 12. Automated Scheduler

### `WeeklyOutreachScheduler.java`
Runs on a scheduled background thread:
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

### `TemplateParser.java`
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

### `DataLoader.java`
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

### `GlobalExceptionHandler.java`
Annotated with `@RestControllerAdvice`:
- Intercepts exceptions thrown anywhere in controllers and transforms them into clean JSON error responses (`ErrorResponse` DTO) with proper HTTP status codes:
  - `ResourceNotFoundException` -> **404 Not Found**
  - `DuplicateRecruiterException` & `DuplicateTemplateException` -> **409 Conflict**
  - `InvalidFileException` -> **400 Bad Request**
  - `MailSendingException` -> **502 Bad Gateway**
  - `MethodArgumentNotValidException` -> **400 Bad Request** (formats clean validation error messages)
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

## 17. Comprehensive Annotation Cheat Sheet

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
| `@Enumerated` | Fields | Persists Java enum values as human-readable Strings in the database. |
| `@Data` | Classes (Lombok) | Generates getters, setters, `toString()`, `equals()`, and `hashCode()`. |
| `@Builder` | Classes (Lombok) | Implements the Builder design pattern for clean object creation. |
| `@NoArgsConstructor` | Classes (Lombok) | Generates a zero-argument constructor required by Hibernate. |
| `@AllArgsConstructor`| Classes (Lombok) | Generates a constructor accepting all class fields. |
| `@RestControllerAdvice`| `GlobalExceptionHandler`| Intercepts exceptions across all controllers globally to format error responses. |
| `@ExceptionHandler`| Methods | Catches specific exception classes and defines the HTTP response. |

---

### 🎉 Conclusion
You now have the complete, line-by-line architectural reference for the entire Referral Outreach Manager backend. Every single component, method, annotation, security filter, and workflow is thoroughly documented.
