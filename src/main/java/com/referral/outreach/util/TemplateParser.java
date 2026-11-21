package com.referral.outreach.util;

import java.util.Map;

public class TemplateParser {

    public static String compile(String templateText, String recruiterName, String companyName, Map<String, String> variables) {
        if (templateText == null) {
            return "";
        }
        
        Map<String, String> map = new java.util.HashMap<>();
        if (variables != null) {
            map.putAll(variables);
        }
        
        String rName = recruiterName != null ? recruiterName : "";
        if ("-".equals(rName.trim())) {
            rName = "Recruitment Team";
        }
        String cName = companyName != null ? companyName : "";
        if ("-".equals(cName.trim())) {
            cName = "your organization";
        }
        
        map.putIfAbsent("recruiterName", rName);
        map.putIfAbsent("recruiter_name", rName);
        map.putIfAbsent("companyName", cName);
        map.putIfAbsent("company_name", cName);
        map.putIfAbsent("company", cName);

        String compiled = templateText;
        for (Map.Entry<String, String> entry : map.entrySet()) {
            if (entry.getKey() == null) continue;
            String val = entry.getValue() != null ? entry.getValue() : "";
            String key = entry.getKey();
            
            // Replace {{key}} exact
            compiled = compiled.replace("{{" + key + "}}", val);
            
            // Also replace {{Key}} or {{KEY}} or {{key_lowered}}
            compiled = compiled.replace("{{" + key.toLowerCase() + "}}", val);
        }

        return compiled;
    }

    private static final java.util.Set<String> GENERIC_KEYWORDS = new java.util.HashSet<>(java.util.Arrays.asList(
            "hr", "hrd", "hro", "humanresources", "human", "resource", "resources",
            "career", "careers", "job", "jobs", "work", "working",
            "recruit", "recruiter", "recruiting", "recruitment", "resourcing",
            "talent", "talentacquisition", "ta", "acquisitions",
            "hiring", "hire", "hires",
            "resume", "resumes", "cv", "cvs", "profile", "profiles",
            "apply", "application", "applications", "applicant",
            "info", "information", "enquiry", "inquiry", "inquiries",
            "contact", "contacts", "contactus", "connect",
            "admin", "administrator", "administration",
            "support", "help", "helpdesk", "desk",
            "sales", "marketing", "bd", "bdm", "business",
            "team", "staff", "staffing", "office", "corporate", "corp",
            "general", "frontdesk", "reception", "lead", "leads",
            "hello", "hi", "welcome", "join", "joinus", "people", "peopleservices",
            "campus", "university", "fresher", "freshers", "intern", "internship", "interns",
            "fellowship", "fellowships", "scholarship", "trainee", "graduate", "graduates",
            "operations", "ops", "feedback", "press", "media", "billing", "accounts",
            "finance", "legal", "compliance", "security", "it", "tech", "technical",
            "developer", "engineering", "services", "solutions", "consulting",
            "global", "apac", "emea", "india", "usa", "uk", "dubai", "uae",
            "delhi", "noida", "gurgaon", "gurugram", "mumbai", "pune", "bangalore", "bengaluru", "hyderabad", "chennai", "kolkata", "ahmedabad", "blr", "hyd",
            "gcp", "aws", "java", "dotnet", "dev", "qa", "test", "app", "sys", "web", "inc", "ltd", "pvt", "llc", "group", "systems", "soft"
    ));

    public static String extractNameFromEmail(String email) {
        if (email == null || !email.contains("@")) {
            return null;
        }
        String local = email.substring(0, email.indexOf('@')).trim().toLowerCase();
        if (local.isEmpty() || GENERIC_KEYWORDS.contains(local)) {
            return null;
        }

        for (String kw : new String[]{"hr", "jobs", "job", "careers", "career", "resume", "resumes", "cv", "recruit", "recruitment", "hiring", "talent", "apply", "info", "contact", "fresher", "freshers", "intern"}) {
            if (local.equals(kw) || local.startsWith(kw + "_") || local.startsWith(kw + ".") || local.startsWith(kw + "-")
                    || local.endsWith("_" + kw) || local.endsWith("." + kw) || local.endsWith("-" + kw)) {
                return null;
            }
            if (local.startsWith(kw) && (local.length() <= kw.length() + 4 || GENERIC_KEYWORDS.contains(local.substring(kw.length())))) {
                return null;
            }
        }

        String[] rawParts = local.split("[._-]+");
        java.util.List<String> parts = new java.util.ArrayList<>();
        for (String raw : rawParts) {
            String cleaned = raw.replaceAll("\\d+", "").trim();
            if (cleaned.length() >= 2 && cleaned.matches("[a-zA-Z]+") && !GENERIC_KEYWORDS.contains(cleaned)) {
                parts.add(cleaned);
            }
        }

        if (parts.isEmpty()) {
            return null;
        }

        // 2 parts: atul.mohan -> Atul Mohan
        if (parts.size() == 2) {
            String first = parts.get(0);
            String last = parts.get(1);
            boolean v1 = hasVowels(first);
            boolean v2 = hasVowels(last);
            if (!v1 && !v2) return null;
            if (v1 && !v2 && first.length() >= 3) return capitalize(first);
            if (!v1 && v2 && last.length() >= 3) return capitalize(last);
            return capitalize(first) + " " + capitalize(last);
        }

        // 3 parts: satya.narayana.rao
        if (parts.size() == 3) {
            java.util.List<String> valid = new java.util.ArrayList<>();
            for (String p : parts) {
                if (hasVowels(p) && p.length() >= 2) {
                    valid.add(capitalize(p));
                }
            }
            if (valid.size() >= 2) {
                return String.join(" ", valid);
            }
        }

        // 1 part: shivani -> Shivani
        if (parts.size() == 1) {
            String p = parts.get(0);
            if (p.length() < 3 || p.length() > 14 || !hasVowels(p)) {
                return null;
            }
            for (String kw : new String[]{"job", "career", "recruit", "talent", "hire", "resume", "team", "work", "desk", "tech", "corp", "inc", "ltd", "india", "blr", "hyd", "pune", "delhi", "fresh", "grad", "fellow", "solut", "soft", "syst", "hr", "hro", "hrd"}) {
                if (p.contains(kw)) {
                    return null;
                }
            }
            int vCount = 0;
            for (char c : p.toCharArray()) {
                if ("aeiouy".indexOf(c) >= 0) vCount++;
            }
            if (vCount < 1 || (p.length() >= 6 && vCount < 2)) {
                return null;
            }
            return capitalize(p);
        }

        return null;
    }

    private static boolean hasVowels(String s) {
        if (s == null) return false;
        for (char c : s.toLowerCase().toCharArray()) {
            if ("aeiouy".indexOf(c) >= 0) return true;
        }
        return false;
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return "";
        return Character.toUpperCase(s.charAt(0)) + s.substring(1).toLowerCase();
    }

    public static String compile(String templateText, String recruiterName, String companyName, String candidateName, String roleName) {
        Map<String, String> vars = new java.util.HashMap<>();
        vars.put("candidateName", candidateName);
        vars.put("roleName", roleName);
        return compile(templateText, recruiterName, companyName, vars);
    }
}
