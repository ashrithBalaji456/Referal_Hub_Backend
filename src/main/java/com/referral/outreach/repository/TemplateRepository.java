package com.referral.outreach.repository;

import com.referral.outreach.entity.EmailTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface TemplateRepository extends JpaRepository<EmailTemplate, Long> {

    Optional<EmailTemplate> findByTemplateName(String templateName);

    boolean existsByTemplateName(String templateName);

    boolean existsByTemplateNameAndIdNot(String templateName, Long id);
}
