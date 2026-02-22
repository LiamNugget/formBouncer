package io.formshield.security;

import io.formshield.model.ApiKey;
import io.formshield.model.User;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

public class ApiKeyAuthentication extends AbstractAuthenticationToken {

    private final User user;
    private final ApiKey apiKey;

    public ApiKeyAuthentication(User user, ApiKey apiKey, Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.user = user;
        this.apiKey = apiKey;
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return null;
    }

    @Override
    public Object getPrincipal() {
        return user;
    }

    public ApiKey getApiKey() {
        return apiKey;
    }

    public User getUser() {
        return user;
    }
}
