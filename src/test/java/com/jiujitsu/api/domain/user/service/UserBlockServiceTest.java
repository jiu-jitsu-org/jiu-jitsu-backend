package com.jiujitsu.api.domain.user.service;

import com.jiujitsu.api.domain.user.entity.User;
import com.jiujitsu.api.domain.user.entity.UserBlock;
import com.jiujitsu.api.domain.user.entity.UserRole;
import com.jiujitsu.api.domain.user.entity.UserStatus;
import com.jiujitsu.api.domain.user.repository.UserBlockRepository;
import com.jiujitsu.api.domain.user.repository.UserRepository;
import com.jiujitsu.api.global.exception.ErrorCode;
import com.jiujitsu.api.global.exception.ErrorException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.SQLException;
import java.util.Optional;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * 차단은 멱등 — 이미 차단했으면 다시 요청해도 풀리지 않는다 (#232)
 */
@ExtendWith(MockitoExtension.class)
class UserBlockServiceTest {

    @InjectMocks
    private UserBlockService userBlockService;

    @Mock private AuthenticationFacade authenticationFacade;
    @Mock private UserRepository userRepository;
    @Mock private UserBlockRepository userBlockRepository;
    @Mock private TransactionTemplate transactionTemplate;

    private static final Long ME = 1L;
    private static final Long TARGET = 2L;

    private final User me = user(ME, UserStatus.ACTIVE);

    @BeforeEach
    void setUp() {
        lenient().when(authenticationFacade.getCurrentUser()).thenReturn(me);
        willAnswer(invocation -> {
            invocation.<Consumer<Object>>getArgument(0).accept(null);
            return null;
        }).given(transactionTemplate).executeWithoutResult(any());
    }

    @Test
    @DisplayName("차단하지 않은 회원이면 차단을 저장하고 true")
    void block_new_saves() {
        User target = user(TARGET, UserStatus.ACTIVE);
        given(userRepository.findById(TARGET)).willReturn(Optional.of(target));
        given(userBlockRepository.existsByBlockerIdAndBlockedId(ME, TARGET)).willReturn(false);

        assertThat(userBlockService.block(TARGET)).isTrue();

        ArgumentCaptor<UserBlock> saved = ArgumentCaptor.forClass(UserBlock.class);
        verify(userBlockRepository).save(saved.capture());
        assertThat(saved.getValue().getBlocker()).isSameAs(me);
        assertThat(saved.getValue().getBlocked()).isSameAs(target);
    }

    @Test
    @DisplayName("이미 차단한 회원을 다시 차단해도 지우지 않고 그대로 true (토글 아님)")
    void block_alreadyBlocked_staysBlocked() {
        given(userRepository.findById(TARGET)).willReturn(Optional.of(user(TARGET, UserStatus.ACTIVE)));
        given(userBlockRepository.existsByBlockerIdAndBlockedId(ME, TARGET)).willReturn(true);

        assertThat(userBlockService.block(TARGET)).isTrue();

        verify(userBlockRepository, never()).save(any());
        verify(userBlockRepository, never()).delete(any());
    }

    @Test
    @DisplayName("동시 요청으로 유니크 제약에 걸리면 이미 차단된 것이므로 true (500 아님)")
    void block_uniqueViolation_respondsBlocked() {
        given(userRepository.findById(TARGET)).willReturn(Optional.of(user(TARGET, UserStatus.ACTIVE)));
        given(userBlockRepository.existsByBlockerIdAndBlockedId(ME, TARGET)).willReturn(false);
        given(userBlockRepository.save(any())).willThrow(new DataIntegrityViolationException("could not execute statement",
                new SQLException("duplicate key value violates unique constraint", "23505")));

        assertThat(userBlockService.block(TARGET)).isTrue();
    }

    @Test
    @DisplayName("유니크 위반이 아닌 무결성 오류(FK 등)는 그대로 던진다")
    void block_otherIntegrityError_rethrows() {
        given(userRepository.findById(TARGET)).willReturn(Optional.of(user(TARGET, UserStatus.ACTIVE)));
        given(userBlockRepository.existsByBlockerIdAndBlockedId(ME, TARGET)).willReturn(false);
        DataIntegrityViolationException failure = new DataIntegrityViolationException("could not execute statement",
                new SQLException("violates foreign key constraint", "23503"));
        given(userBlockRepository.save(any())).willThrow(failure);

        assertThatThrownBy(() -> userBlockService.block(TARGET)).isSameAs(failure);
    }

    @Test
    @DisplayName("자기 자신은 차단할 수 없다 (U0006)")
    void block_self_rejected() {
        assertThatThrownBy(() -> userBlockService.block(ME))
                .isInstanceOf(ErrorException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.SELF_BLOCK_NOT_ALLOWED);
    }

    @Test
    @DisplayName("없는 회원은 404 MEMBER_NOT_FOUND (401 USER_NOT_FOUND 아님 — 앱이 로그인 만료로 처리하지 않게)")
    void block_notFound_404() {
        given(userRepository.findById(TARGET)).willReturn(Optional.empty());

        assertThatThrownBy(() -> userBlockService.block(TARGET))
                .isInstanceOf(ErrorException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.MEMBER_NOT_FOUND);
    }

    @Test
    @DisplayName("탈퇴(유예 포함) 회원은 공개 프로필처럼 MEMBER_NOT_FOUND")
    void block_deleted_404() {
        given(userRepository.findById(TARGET)).willReturn(Optional.of(user(TARGET, UserStatus.DELETED)));

        assertThatThrownBy(() -> userBlockService.block(TARGET))
                .isInstanceOf(ErrorException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.MEMBER_NOT_FOUND);
        verify(userBlockRepository, never()).save(any());
    }

    private static User user(Long id, UserStatus status) {
        return User.builder().id(id).nickname("회원" + id).role(UserRole.USER).status(status).build();
    }
}
