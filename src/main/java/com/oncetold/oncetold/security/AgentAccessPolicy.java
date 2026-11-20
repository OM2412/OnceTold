package com.oncetold.oncetold.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/** Public registration must never grant staff access. Only explicitly approved staff can log in. */
@Component
public class AgentAccessPolicy {
    private final Set<String> allowedEmails;

    public AgentAccessPolicy(@Value("${app.security.agent-allowlist:}") String rawEmails) {
        allowedEmails = Arrays.stream(rawEmails.split(","))
                .map(email -> email.trim().toLowerCase(Locale.ROOT))
                .filter(email -> !email.isBlank())
                .collect(Collectors.toUnmodifiableSet());
    }

    public boolean isAllowed(String email) {
        return email != null && allowedEmails.contains(email.trim().toLowerCase(Locale.ROOT));
    }
}
