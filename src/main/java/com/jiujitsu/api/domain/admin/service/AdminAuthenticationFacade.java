package com.jiujitsu.api.domain.admin.service;

import com.jiujitsu.api.domain.admin.entity.Account;
import com.jiujitsu.api.domain.admin.repository.AccountRepository;
import com.jiujitsu.api.global.exception.ErrorCode;
import com.jiujitsu.api.global.exception.ErrorException;
import com.jiujitsu.api.global.util.AuthenticationUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AdminAuthenticationFacade {

    private final AccountRepository accountRepository;

    public Account getCurrentAccount() {
        Long id = AuthenticationUtil.getCurrentUserId()
                .orElseThrow(() -> new ErrorException(ErrorCode.LOGIN_NOT_ACCESS));
        return accountRepository.findById(id)
                .orElseThrow(() -> new ErrorException(ErrorCode.LOGIN_NOT_ACCESS));
    }
}
