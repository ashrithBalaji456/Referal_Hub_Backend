# 🚀 Referral Hub

### Automated Job Outreach & Referral Management Platform

A full-stack platform for managing professional contacts, creating
reusable email templates, attaching resumes, and running controlled
scheduled outreach campaigns.
**Built in July 2026**

[Frontend
Repository](https://github.com/ashrithBalaji456/Referal_Hub_Frontend) •
[Backend
Repository](https://github.com/ashrithBalaji456/Referal_Hub_Backend)
:::

------------------------------------------------------------------------

## 📌 About the Project

**Referral Hub** is a full-stack outreach automation platform created to
simplify and organize job application and referral workflows.

Instead of manually composing the same email repeatedly, the application
centralizes:

-   professional contact management,
-   reusable email templates,
-   personalized subject and body generation,
-   PDF resume attachments,
-   scheduled campaign execution,
-   duplicate-send prevention,
-   cooldown-based recipient eligibility,
-   and email activity tracking.

The project was built as a practical learning project around **Spring
Scheduler** and **Spring Mail**, then expanded into a complete
full-stack campaign-management system.

> \[!IMPORTANT\] Referral Hub is designed for controlled and responsible
> professional outreach. Verify contact information, respect opt-out
> requests, avoid repeated unsolicited emails, and follow applicable
> laws and provider policies.

------------------------------------------------------------------------

## ✨ Features

### 👥 Contact Management

-   Create, update, view, activate, and deactivate professional
    contacts.
-   Store recipient name, email, company, title, and contact category.
-   Prevent duplicate email records.
-   Track contact status and previous outreach activity.
-   Exclude invalid, bounced, inactive, or do-not-contact recipients.

### 📝 Reusable Email Templates

-   Store reusable subject and body templates.
-   Personalize emails using placeholders such as:
    -   `{{recipientName}}`
    -   `{{companyName}}`
    -   `{{candidateName}}`
    -   `{{roleName}}`
-   Preview personalized content before campaign execution.

### 📎 Resume Attachment Management

-   Upload PDF resumes.
-   Validate file type and size.
-   Store resume metadata.
-   Select the active resume for a campaign.
-   Attach the selected PDF using MIME email support.

### 📅 Scheduled Campaigns

-   Configure campaign execution through Spring Scheduler.
-   Use cron expressions for weekly execution.
-   Configure scheduler timezone.
-   Select eligible recipients before sending.
-   Process controlled batches instead of uncontrolled mass sends.

### 📬 Email Delivery Workflow

-   Build personalized MIME messages.
-   Send email through SMTP using Spring Mail.
-   Attach the active resume.
-   Continue processing other recipients if one send fails.
-   Track submission and failure states.

### 🛡️ Outreach Safety Controls

-   Duplicate-send prevention.
-   Configurable cooldown period.
-   Recipient eligibility checks.
-   Contact activation/deactivation.
-   Batch-size control.
-   Bounce-aware contact status design.
-   `DO_NOT_CONTACT` handling.

### 📊 Activity & History

-   Store email attempt history.
-   Track recipient, campaign, subject, timestamp, and status.
-   Preserve error information for failed attempts.
-   Support campaign auditing and debugging.

------------------------------------------------------------------------

## 🧰 Tech Stack

  Layer             Technologies
  ----------------- ---------------------------------------------------
  Frontend          React 18, Vite, Tailwind CSS, Axios, Lucide React
  Backend           Java 17, Spring Boot 3.3.1
  API               Spring Web, REST APIs
  Persistence       Spring Data JPA, Hibernate
  Database          PostgreSQL
  Validation        Jakarta Bean Validation
  Email             Spring Mail, SMTP, MIME attachments
  Automation        Spring Scheduler, Cron Expressions
  Build Tool        Maven
  Logging           SLF4J
  Testing           Spring Boot Test, H2
  API Testing       Postman
  Version Control   Git, GitHub

------------------------------------------------------------------------

## 🏗️ System Architecture

``` mermaid
flowchart LR
    U[User] --> FE[React Frontend]
    FE -->|Axios / REST| API[Spring Boot REST API]

    API --> CS[Contact Service]
    API --> TS[Template Service]
    API --> RS[Resume Service]
    API --> CPS[Campaign Service]

    SCH[Spring Scheduler] --> CPS

    CS --> DB[(PostgreSQL)]
    TS --> DB
    RS --> DB
    CPS --> DB

    CPS --> EL[Eligibility & Cooldown Check]
    EL --> PS[Template Personalization]
    PS --> MS[Spring Mail Service]
    MS --> SMTP[SMTP Server]
    SMTP --> REC[Recipient Mail Server]

    MS --> HIST[Email History Service]
    HIST --> DB
```

------------------------------------------------------------------------

## 🔄 Complete Workflow

``` mermaid
flowchart TD
    A[Add / Import Contacts] --> B[Validate and Deduplicate]
    B --> C[(Store in PostgreSQL)]
    C --> D[Create Email Template]
    D --> E[Upload PDF Resume]
    E --> F[Create Campaign]
    F --> G[Choose Recipients]
    G --> H[Scheduler Trigger or Manual Run]
    H --> I{Recipient Eligible?}

    I -- No --> J[Skip Recipient]
    I -- Yes --> K[Replace Template Placeholders]
    K --> L[Attach Active Resume]
    L --> M[Build MIME Email]
    M --> N[Submit through SMTP]

    N --> O{Submission Result}
    O -- Accepted --> P[Save SUBMITTED]
    O -- Immediate Failure --> Q[Save FAILED]

    P --> R[Update Contact History]
    Q --> S[Store Error Details]
```

### Workflow Explanation

1.  **Contact onboarding** --- Contacts are added through the frontend
    or imported into the system.
2.  **Validation** --- The backend validates required fields and
    prevents duplicate recipient emails.
3.  **Template creation** --- A reusable subject and email body are
    stored.
4.  **Resume selection** --- A PDF resume is uploaded and selected for
    the campaign.
5.  **Campaign setup** --- The user chooses the template, resume,
    schedule, and target recipients.
6.  **Scheduler trigger** --- Spring Scheduler starts campaign
    processing according to the configured cron expression.
7.  **Eligibility check** --- The backend filters inactive, recently
    contacted, bounced, and do-not-contact recipients.
8.  **Personalization** --- Template placeholders are replaced with
    recipient and campaign values.
9.  **MIME generation** --- The system creates the email and attaches
    the PDF resume.
10. **SMTP submission** --- Spring Mail submits the message to the
    configured SMTP server.
11. **History update** --- The system stores the submission result and
    error details where applicable.

> \[!NOTE\] Successful SMTP submission does not guarantee inbox
> delivery. A recipient server can reject a message later and generate a
> bounce notification. The project therefore distinguishes submission
> success from final delivery semantics.

------------------------------------------------------------------------

## 🧩 Backend Layered Architecture

``` text
src/main/java/com/referral/outreach/
│
├── controller/        # REST endpoints
├── service/           # Business logic
├── repository/        # Spring Data JPA repositories
├── entity/            # JPA entities
├── dto/               # API request/response models
├── scheduler/         # Scheduled campaign triggers
├── config/            # CORS, mail and application configuration
├── exception/         # Custom exceptions and global handling
└── util/              # Template/file helper logic
```

The scheduler remains thin: it triggers the campaign service, while
eligibility checks, personalization, sending, and persistence remain in
dedicated services.

------------------------------------------------------------------------

## 🗃️ Data Model

``` mermaid
erDiagram
    CONTACT ||--o{ CAMPAIGN_RECIPIENT : receives
    CAMPAIGN ||--o{ CAMPAIGN_RECIPIENT : contains
    CAMPAIGN }o--|| EMAIL_TEMPLATE : uses
    CAMPAIGN }o--|| RESUME : attaches
    CONTACT ||--o{ EMAIL_HISTORY : has
    CAMPAIGN ||--o{ EMAIL_HISTORY : generates

    CONTACT {
        bigint id PK
        string name
        string email UK
        string title
        string company
        string contactType
        string status
        datetime lastContactedAt
    }

    EMAIL_TEMPLATE {
        bigint id PK
        string templateName
        string subject
        text body
        datetime createdAt
        datetime updatedAt
    }

    RESUME {
        bigint id PK
        string originalFileName
        string storedFileName
        string storagePath
        string contentType
        bigint fileSize
        boolean active
    }

    CAMPAIGN {
        bigint id PK
        string campaignName
        bigint templateId FK
        bigint resumeId FK
        boolean enabled
        datetime createdAt
    }

    CAMPAIGN_RECIPIENT {
        bigint id PK
        bigint campaignId FK
        bigint contactId FK
        string status
    }

    EMAIL_HISTORY {
        bigint id PK
        bigint contactId FK
        bigint campaignId FK
        string recipientEmail
        string subject
        string status
        datetime sentAt
        text errorMessage
    }
```

------------------------------------------------------------------------

## 📧 Email Status Lifecycle

``` mermaid
stateDiagram-v2
    [*] --> PENDING
    PENDING --> SUBMITTED: SMTP accepts submission
    PENDING --> FAILED: Immediate send failure
    SUBMITTED --> BOUNCED: Delivery failure detected
    SUBMITTED --> [*]
    FAILED --> [*]
    BOUNCED --> [*]
```

Suggested states:

  Status        Meaning
  ------------- --------------------------------------------
  `PENDING`     Waiting for processing
  `SUBMITTED`   SMTP server accepted the message
  `FAILED`      Immediate submission failure
  `BOUNCED`     Recipient server reported delivery failure

------------------------------------------------------------------------

## 📡 API Overview

A typical API organization:

  Module      Base Endpoint          Purpose
  ----------- ---------------------- --------------------------------------
  Contacts    `/api/contacts`        Contact CRUD and status management
  Templates   `/api/templates`       Email template CRUD
  Resumes     `/api/resumes`         Upload and manage PDF resumes
  Campaigns   `/api/campaigns`       Campaign configuration and execution
  History     `/api/email-history`   Outreach attempt history

> Endpoint names can be adjusted to match the implementation in the
> backend repository.

------------------------------------------------------------------------

## ⚙️ Local Setup

### Prerequisites

Make sure the following are installed:

-   Java 17+
-   Maven 3.9+
-   Node.js 18+
-   PostgreSQL
-   Git

### 1. Clone the repositories

``` bash
git clone https://github.com/ashrithBalaji456/Referal_Hub_Backend.git
git clone https://github.com/ashrithBalaji456/Referal_Hub_Frontend.git
```

### 2. Create the PostgreSQL database

``` sql
CREATE DATABASE referral_outreach_db;
```

### 3. Configure backend environment variables

``` env
DB_HOST=localhost
DB_PORT=5432
DB_NAME=referral_outreach_db
DB_USER=postgres
DB_PASSWORD=your_postgres_password

SMTP_HOST=smtp.gmail.com
SMTP_PORT=587
SMTP_USERNAME=your_email@gmail.com
SMTP_PASSWORD=your_app_password
```

Never commit `.env` files, database passwords, SMTP credentials, or app
passwords.

### 4. Example backend configuration

``` yaml
spring:
  datasource:
    url: jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:referral_outreach_db}
    username: ${DB_USER:postgres}
    password: ${DB_PASSWORD}
    driver-class-name: org.postgresql.Driver

  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
    properties:
      hibernate:
        format_sql: true

  mail:
    host: ${SMTP_HOST:smtp.gmail.com}
    port: ${SMTP_PORT:587}
    username: ${SMTP_USERNAME}
    password: ${SMTP_PASSWORD}
    properties:
      mail:
        smtp:
          auth: true
          starttls:
            enable: true
            required: true

referral:
  scheduler:
    cron: "0 0 10 * * MON"
    zone: "Asia/Kolkata"
  campaign:
    batch-size: 10
    cooldown-days: 30
```

### 5. Run the backend

``` bash
cd Referal_Hub_Backend
mvn spring-boot:run
```

### 6. Run the frontend

``` bash
cd Referal_Hub_Frontend
npm install
npm run dev
```

The Vite development server will print the local frontend URL in the
terminal.

------------------------------------------------------------------------

## ⏰ Scheduler Example

``` java
@Scheduled(
    cron = "${referral.scheduler.cron}",
    zone = "${referral.scheduler.zone}"
)
public void processWeeklyCampaign() {
    campaignService.processWeeklyCampaign();
}
```

Example cron:

``` text
0 0 10 * * MON
```

This represents a weekly Monday execution at 10:00 AM in the configured
timezone.

------------------------------------------------------------------------

## 🔐 Security & Configuration Practices

-   SMTP credentials are read from environment variables.
-   Database credentials are externalized.
-   Secrets are not committed to source control.
-   Contact email addresses are unique.
-   Invalid and bounced contacts can be excluded.
-   Cooldown rules reduce repeated outreach.
-   Batch controls prevent uncontrolled campaign execution.
-   File uploads should be validated for MIME type, extension, and size.
-   Production environments should use schema migrations rather than
    relying on `ddl-auto: update`.

------------------------------------------------------------------------

## 🧪 Testing Strategy

Recommended test coverage:

-   Contact CRUD service tests.
-   Duplicate-email validation tests.
-   Template placeholder replacement tests.
-   Recipient eligibility and cooldown tests.
-   Resume file validation tests.
-   Campaign processing tests.
-   Mail-service integration tests using a local SMTP test server.
-   Repository tests with H2 or PostgreSQL Testcontainers.
-   API integration tests for major workflows.

The backend already includes Spring Boot Test and H2 test dependencies.

------------------------------------------------------------------------

## 🚧 Challenges & Learnings

### 1. SMTP accepted does not mean delivered

One of the key engineering learnings from the project was that:

``` text
Application → SMTP Accepted → Recipient Server → Inbox or Bounce
```

A successful `JavaMailSender.send(...)` call confirms submission to the
SMTP transport path; it does not prove final inbox delivery.

### 2. Scheduler logic should not contain business logic

The scheduler only triggers execution. The campaign service coordinates:

``` text
Scheduler
   ↓
Campaign Service
   ↓
Eligibility Check
   ↓
Template Personalization
   ↓
Mail Service
   ↓
History Service
```

This keeps the code testable and makes future migration to Quartz or
another job scheduler easier.

### 3. Contact data needs lifecycle management

Imported professional contact data can become stale. The system
therefore benefits from explicit states such as:

``` text
UNVERIFIED → ACTIVE → BOUNCED
                  ↘ DO_NOT_CONTACT
```

------------------------------------------------------------------------

## 🗺️ Future Enhancements

-   [ ] Authentication and role-based authorization.
-   [ ] Quartz Scheduler for database-persisted dynamic jobs.
-   [ ] Email provider webhooks for automated bounce processing.
-   [ ] CSV/XLSX contact import with validation reports.
-   [ ] Campaign analytics dashboard.
-   [ ] Retry strategy with exponential backoff for transient failures.
-   [ ] Docker Compose for frontend, backend, and PostgreSQL.
-   [ ] Flyway database migrations.
-   [ ] Testcontainers-based PostgreSQL integration testing.
-   [ ] Deployment pipeline with GitHub Actions.
-   [ ] Template preview and test-send mode.
-   [ ] Per-domain and per-campaign sending limits.

------------------------------------------------------------------------

## 📂 Repositories

  ----------------------------------------------------------------------------------------------------------------------
  Component                           Repository
  ----------------------------------- ----------------------------------------------------------------------------------
  🎨 Frontend                         [Referal_Hub_Frontend](https://github.com/ashrithBalaji456/Referal_Hub_Frontend)

  ⚙️ Backend                          [Referal_Hub_Backend](https://github.com/ashrithBalaji456/Referal_Hub_Backend)
  ----------------------------------------------------------------------------------------------------------------------

------------------------------------------------------------------------

## 👨‍💻 Author

**Gudla Ashrith Balaji**

Java Backend Developer focused on building REST APIs, Spring Boot
applications, database-backed systems, and backend automation.

-   LinkedIn:
    https://www.linkedin.com/in/ashrith-balaji-gudla-5768302a8/
-   GitHub: https://github.com/ashrithBalaji456

------------------------------------------------------------------------

## ⭐ Support

If you find this project useful, consider giving the repositories a ⭐.

### Built with ☕ Java, 🌱 Spring Boot, ⚛️ React and 🐘 PostgreSQL

**Schedule responsibly. Personalize thoughtfully. Track clearly.**
:::
