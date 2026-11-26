package com.jiujitsu.api.global.config;

import com.jiujitsu.api.domain.admin.entity.Account;
import com.jiujitsu.api.domain.admin.entity.AccountRole;
import com.jiujitsu.api.domain.admin.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile("local")
@RequiredArgsConstructor
public class LocalDataInitializer implements ApplicationRunner {

    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        createAdminIfNotExists("admin@test.com", "admin1234", "테스트관리자");
        log.info("[LocalDataInitializer] 테스트 어드민 계정 준비 완료");
        log.info("[LocalDataInitializer] email: admin@test.com / password: admin1234");
    }

    private void createAdminIfNotExists(String email, String password, String name) {
        if (accountRepository.existsByEmail(email)) return;

        accountRepository.save(Account.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(password))
                .name(name)
                .role(AccountRole.OPERATOR)
                .build());
    }
}
