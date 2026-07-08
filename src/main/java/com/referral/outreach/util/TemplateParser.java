package com.referral.outreach.util;

public class TemplateParser {

    public static String compile(String templateText, String recruiterName, String companyName, String candidateName, String roleName) {
        if (templateText == null) {
            return "";
        }
        return templateText
                .replace("{{recruiterName}}", recruiterName != null ? recruiterName : "")
                .replace("{{companyName}}", companyName != null ? companyName : "")
                .replace("{{candidateName}}", candidateName != null ? candidateName : "")
                .replace("{{roleName}}", roleName != null ? roleName : "");
    }
}
