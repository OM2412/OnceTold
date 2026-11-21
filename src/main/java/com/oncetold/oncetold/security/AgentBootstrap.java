package com.oncetold.oncetold.security;

import com.oncetold.oncetold.entity.Role;
import com.oncetold.oncetold.entity.User;
import com.oncetold.oncetold.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Explicitly provision an existing customer as staff during a controlled deploy.
 * This is disabled unless BOTH variables are set by an operator:
 * APP_AGENT_BOOTSTRAP_ENABLED=true and APP_AGENT_BOOTSTRAP_EMAIL=<email>.
 * The email must also appear in APP_AGENT_ALLOWLIST.
 * Remove both bootstrap variables once the promotion has succeeded.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AgentBootstrap implements ApplicationRunner {

    private final UserRepository users;
    private final AgentAccessPolicy policy;

    @Value("${app.security.agent-bootstrap-enabled:false}")
    private boolean enabled;

    @Value("${app.security.agent-bootstrap-email:}")
    private String email;

    @Override
    public void run(ApplicationArguments args) {
        if (!enabled) return;
        String target = email == null ? "" : email.trim();
        if (target.isEmpty() || !policy.isAllowed(target)) {
            throw new IllegalStateException("Agent bootstrap requires an explicitly allowlisted email");
        }
        User user = users.findByEmail(target)
                .orElseThrow(() -> new IllegalStateException("Agent bootstrap account does not exist"));
        if (user.getRole() != Role.AGENT) {
            user.setRole(Role.AGENT);
            users.save(user);
        }
        log.info("Trusted support-agent bootstrap completed. Disable bootstrap settings after this deploy.");
    }
}
