package com.jiujitsu.api.domain.user.service;

import com.jiujitsu.api.domain.user.entity.AccountType;
import com.jiujitsu.api.domain.user.entity.User;
import com.jiujitsu.api.domain.user.repository.UserRepository;
import com.jiujitsu.api.global.exception.ErrorCode;
import com.jiujitsu.api.global.exception.ErrorException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceAccountTypeTest {

    @InjectMocks private UserService userService;

    @Mock private UserRepository userRepository;

    @Test
    @DisplayName("관리자가 계정 구분을 바꾸면 회원에 반영되고 분석용 ID는 그대로다")
    void updateAccountType() {
        User user = User.builder().id(5L).build();
        String analyticsId = user.getAnalyticsId();
        when(userRepository.findById(5L)).thenReturn(Optional.of(user));

        userService.updateAccountType(5L, AccountType.INTERNAL);

        assertThat(user.getAccountType()).isEqualTo(AccountType.INTERNAL);
        assertThat(user.getAnalyticsId()).isEqualTo(analyticsId);
    }

    @Test
    @DisplayName("없는 회원이면 USER_NOT_FOUND")
    void updateAccountType_userNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updateAccountType(99L, AccountType.OPERATOR))
                .isInstanceOf(ErrorException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.USER_NOT_FOUND);
    }
}
