package com.referral.outreach.repository;

import com.referral.outreach.entity.EmailTemplate;
import com.referral.outreach.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface TemplateRepository extends JpaRepository<EmailTemplate, Long> {

    List<EmailTemplate> findByUser(User user);

    Optional<EmailTemplate> findByTemplateName(String templateName);
    Optional<EmailTemplate> findByUserAndTemplateName(User user, String templateName);

    boolean existsByTemplateName(String templateName);
    boolean existsByUserAndTemplateName(User user, String templateName);

    boolean existsByUserAndTemplateNameAndIdNot(User user, String templateName, Long id);
}
