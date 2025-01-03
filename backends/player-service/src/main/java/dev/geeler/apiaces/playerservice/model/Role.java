package dev.geeler.apiaces.playerservice.model;

import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;

public enum Role implements GrantedAuthority {
    USER("USER"),
    ADMIN("ADMIN");

    private final String name;

    Role(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }


    @Override
    public String getAuthority() {
        return getName();
    }
}
