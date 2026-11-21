package com.oncetold.oncetold.security;

import com.oncetold.oncetold.entity.Role;
import com.oncetold.oncetold.entity.User;
import com.oncetold.oncetold.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AgentBootstrapTest {
    private final UserRepository repository = mock(UserRepository.class);
    private final DefaultApplicationArguments arguments = new DefaultApplicationArguments(new String[0]);

    private AgentBootstrap bootstrap(String allowlist, boolean enabled, String email) {
        AgentBootstrap bootstrap = new AgentBootstrap(repository, new AgentAccessPolicy(allowlist));
        ReflectionTestUtils.setField(bootstrap, "enabled", enabled);
        ReflectionTestUtils.setField(bootstrap, "email", email);
        return bootstrap;
    }

    @Test
    void disabledBootstrapNeverTouchesDatabase() {
        assertDoesNotThrow(() -> bootstrap("owner@example.invalid", false, "owner@example.invalid").run(arguments));
        verifyNoInteractions(repository);
    }

    @Test
    void bootstrapMustBeExplicitlyAllowlisted() {
        assertThrows(IllegalStateException.class,
                () -> bootstrap("someone-else@example.invalid", true, "owner@example.invalid").run(arguments));
        verifyNoInteractions(repository);
    }

    @Test
    void approvedExistingCustomerCanBePromotedWithoutChangingPassword() {
        User account = User.builder().id(14L).name("Owner")
                .email("owner@example.invalid").passwordHash("preserve-me")
                .role(Role.CUSTOMER).build();
        when(repository.findByEmail(account.getEmail())).thenReturn(Optional.of(account));
        assertDoesNotThrow(() -> bootstrap(account.getEmail(), true, account.getEmail()).run(arguments));
        assertEquals(Role.AGENT, account.getRole());
        assertEquals("preserve-me", account.getPasswordHash());
        verify(repository).save(account);
    }
}
