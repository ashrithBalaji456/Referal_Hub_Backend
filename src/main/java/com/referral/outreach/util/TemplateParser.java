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
        String cName = companyName != null ? companyName : "";
        
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

    public static String compile(String templateText, String recruiterName, String companyName, String candidateName, String roleName) {
        Map<String, String> vars = new java.util.HashMap<>();
        vars.put("candidateName", candidateName);
        vars.put("roleName", roleName);
        return compile(templateText, recruiterName, companyName, vars);
    }
}
