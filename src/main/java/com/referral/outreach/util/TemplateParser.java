package com.referral.outreach.util;

public class TemplateParser {

    public static String compile(String templateText, String recruiterName, String companyName, java.util.Map<String, String> variables) {
        if (templateText == null) {
            return "";
        }
        String compiled = templateText
                .replace("{{recruiterName}}", recruiterName != null ? recruiterName : "")
                .replace("{{companyName}}", companyName != null ? companyName : "");
        if (variables != null) {
            for (java.util.Map.Entry<String, String> entry : variables.entrySet()) {
                String placeholder = "{{" + entry.getKey() + "}}";
                compiled = compiled.replace(placeholder, entry.getValue() != null ? entry.getValue() : "");
            }
        }
        return compiled;
    }

    public static String compile(String templateText, String recruiterName, String companyName, String candidateName, String roleName) {
        java.util.Map<String, String> vars = new java.util.HashMap<>();
        vars.put("candidateName", candidateName);
        vars.put("roleName", roleName);
        return compile(templateText, recruiterName, companyName, vars);
    }
}
