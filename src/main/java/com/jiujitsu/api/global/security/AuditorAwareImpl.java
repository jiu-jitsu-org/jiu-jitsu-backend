package com.jiujitsu.api.global.security;

import com.jiujitsu.api.domain.user.entity.User;
import lombok.NonNull;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class AuditorAwareImpl implements AuditorAware<User> {

    @Override
    @NonNull
    public Optional<User> getCurrentAuditor() {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return Optional.empty();
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof CustomUserPrincipal customUser) {
            User user = new User();
            user.setId(customUser.getUserId());
            return Optional.of(user);
        }

        if (principal instanceof Long userId) {
            User user = new User();
            user.setId(userId);
            return Optional.of(user);
        }

        return Optional.empty();
    }
}
