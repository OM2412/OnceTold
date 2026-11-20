package com.oncetold.oncetold.service;

import com.oncetold.oncetold.dto.RegisterRequest;
import com.oncetold.oncetold.dto.LoginRequest;
import com.oncetold.oncetold.entity.Role;
import com.oncetold.oncetold.entity.User;
import com.oncetold.oncetold.repository.UserRepository;
import com.oncetold.oncetold.security.AgentAccessPolicy;
import com.oncetold.oncetold.security.JwtUtil;
import com.oncetold.oncetold.security.UserDetailsServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AgentAccessSecurityTest {
    private final UserRepository users = mock(UserRepository.class);
    private final PasswordEncoder passwords = mock(PasswordEncoder.class);
    private final JwtUtil jwt = mock(JwtUtil.class);

    @Test
    void publicRegistrationCannotCreateSupportAgents() {
        AuthService auth = new AuthService(users, passwords, jwt, new AgentAccessPolicy(""));
        RegisterRequest request = new RegisterRequest();
        request.setRole(Role.AGENT);
        request.setEmail("untrusted@example.invalid");
        request.setName("Untrusted");
        request.setPassword("long-password");
        assertThrows(IllegalArgumentException.class, () -> auth.register(request));
        verifyNoInteractions(users);
    }

    @Test
    void existingUnapprovedAgentsCannotLogInOrReuseOldJwt() {
        User agent = User.builder().id(1L).email("old-agent@example.invalid")
                .passwordHash("hash").role(Role.AGENT).build();
        when(users.findByEmail(agent.getEmail())).thenReturn(Optional.of(agent));
        AgentAccessPolicy emptyPolicy = new AgentAccessPolicy("");
        AuthService auth = new AuthService(users, passwords, jwt, emptyPolicy);
        LoginRequest login = new LoginRequest();
        login.setEmail(agent.getEmail());
        login.setPassword("irrelevant");
        assertThrows(BadCredentialsException.class, () -> auth.login(login));
        assertThrows(UsernameNotFoundException.class,
                () -> new UserDetailsServiceImpl(users, emptyPolicy).loadUserByUsername(agent.getEmail()));
    }

    @Test
    void onlyExplicitAllowlistCanAccessStaffEndpoints() {
        AgentAccessPolicy policy = new AgentAccessPolicy("trusted@example.invalid, other@example.invalid");
        assertTrue(policy.isAllowed("TRUSTED@example.invalid"));
        assertFalse(policy.isAllowed("imposter@example.invalid"));
        assertFalse(policy.isAllowed(null));
    }
}
