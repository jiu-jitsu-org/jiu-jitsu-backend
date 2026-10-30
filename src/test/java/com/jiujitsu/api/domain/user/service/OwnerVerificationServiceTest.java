package com.jiujitsu.api.domain.user.service;

import com.jiujitsu.api.domain.community.profile.entity.CommunityProfile;
import com.jiujitsu.api.domain.community.profile.entity.OwnerProfile;
import com.jiujitsu.api.domain.community.profile.repository.CommunityProfileRepository;
import com.jiujitsu.api.domain.community.profile.repository.OwnerProfileRepository;
import com.jiujitsu.api.domain.file.ImageFile;
import com.jiujitsu.api.domain.file.repository.ImageFileRepository;
import com.jiujitsu.api.domain.user.dto.AdminOwnerRequestResponse;
import com.jiujitsu.api.domain.user.dto.UserProfileResponse;
import com.jiujitsu.api.domain.user.entity.OwnerVerificationRequest;
import com.jiujitsu.api.domain.user.entity.OwnerVerificationStatus;
import com.jiujitsu.api.domain.user.entity.User;
import com.jiujitsu.api.domain.user.entity.UserRole;
import com.jiujitsu.api.domain.user.mapper.UserMapper;
import com.jiujitsu.api.domain.user.repository.OwnerVerificationRequestRepository;
import com.jiujitsu.api.domain.user.repository.UserRepository;
import com.jiujitsu.api.global.exception.ErrorCode;
import com.jiujitsu.api.global.exception.ErrorException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OwnerVerificationServiceTest {

    private static final Long IMAGE_ID = 10L;
    private static final Long REQUEST_ID = 100L;

    @InjectMocks
    private OwnerVerificationService ownerVerificationService;

    @Mock private OwnerVerificationRequestRepository ownerVerificationRequestRepository;
    @Mock private UserRepository userRepository;
    @Mock private ImageFileRepository imageFileRepository;
    @Mock private CommunityProfileRepository communityProfileRepository;
    @Mock private OwnerProfileRepository ownerProfileRepository;
    @Mock private AuthenticationFacade authenticationFacade;
    @Spy private UserMapper userMapper = new UserMapper();

    private User user(Long id, UserRole role) {
        return User.builder().id(id).role(role).build();
    }

    private ImageFile image(Long id) {
        return ImageFile.builder().id(id).imageUrl("https://cdn/" + id).build();
    }

    private OwnerVerificationRequest pendingOf(User applicant) {
        return OwnerVerificationRequest.builder()
                .id(REQUEST_ID)
                .user(applicant)
                .documentImageFile(image(1L))
                .build();
    }

    // 신청은 현재 유저를 잠금 조회로 읽는다
    private void givenCurrentUser(User user) {
        given(authenticationFacade.getCurrentUserForUpdate()).willReturn(user);
    }

    // 승인·거절은 신청자 users → 신청 순서로 잠근다
    private void givenLockedRequest(OwnerVerificationRequest request) {
        given(ownerVerificationRequestRepository.findUserIdById(REQUEST_ID)).willReturn(Optional.of(request.getUser().getId()));
        given(userRepository.findByIdForUpdate(request.getUser().getId())).willReturn(Optional.of(request.getUser()));
        given(ownerVerificationRequestRepository.findByIdForUpdate(REQUEST_ID)).willReturn(Optional.of(request));
    }

    private void assertErrorCode(Runnable call, ErrorCode errorCode) {
        assertThatThrownBy(call::run)
                .isInstanceOf(ErrorException.class)
                .extracting(e -> ((ErrorException) e).getErrorCode())
                .isEqualTo(errorCode);
    }

    @Nested
    @DisplayName("신청")
    class Request {

        @Test
        @DisplayName("신청 이력이 없으면 PENDING 신청을 새로 만든다")
        void createNew() {
            User me = user(7L, UserRole.USER);
            givenCurrentUser(me);
            given(imageFileRepository.findById(IMAGE_ID)).willReturn(Optional.of(image(IMAGE_ID)));
            given(ownerVerificationRequestRepository.findTopByUserOrderByIdDesc(me)).willReturn(Optional.empty());
            given(ownerVerificationRequestRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            UserProfileResponse response = ownerVerificationService.request(IMAGE_ID);

            ArgumentCaptor<OwnerVerificationRequest> captor = ArgumentCaptor.forClass(OwnerVerificationRequest.class);
            verify(ownerVerificationRequestRepository).save(captor.capture());
            assertThat(captor.getValue().getUser()).isSameAs(me);
            assertThat(captor.getValue().getDocumentImageFile().getId()).isEqualTo(IMAGE_ID);
            assertThat(response.ownerVerification().status()).isEqualTo("PENDING");
            assertThat(response.ownerVerification().documentImage().id()).isEqualTo(IMAGE_ID);
        }

        @Test
        @DisplayName("심사 대기 중이면 새로 만들지 않고 서류만 교체한다")
        void replaceWhenPending() {
            User me = user(7L, UserRole.USER);
            OwnerVerificationRequest pending = pendingOf(me);
            givenCurrentUser(me);
            given(imageFileRepository.findById(IMAGE_ID)).willReturn(Optional.of(image(IMAGE_ID)));
            given(ownerVerificationRequestRepository.findTopByUserOrderByIdDesc(me)).willReturn(Optional.of(pending));

            ownerVerificationService.request(IMAGE_ID);

            verify(ownerVerificationRequestRepository, never()).save(any());
            assertThat(pending.getDocumentImageFile().getId()).isEqualTo(IMAGE_ID);
        }

        @Test
        @DisplayName("거절된 뒤 다시 신청하면 이전 신청은 두고 새 신청을 만든다")
        void reapplyAfterReject() {
            User me = user(7L, UserRole.USER);
            OwnerVerificationRequest rejected = pendingOf(me);
            rejected.reject(user(1L, UserRole.ADMIN), "불명확");
            givenCurrentUser(me);
            given(imageFileRepository.findById(IMAGE_ID)).willReturn(Optional.of(image(IMAGE_ID)));
            given(ownerVerificationRequestRepository.findTopByUserOrderByIdDesc(me)).willReturn(Optional.of(rejected));
            given(ownerVerificationRequestRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            UserProfileResponse response = ownerVerificationService.request(IMAGE_ID);

            verify(ownerVerificationRequestRepository).save(any());
            assertThat(rejected.getStatus()).isEqualTo(OwnerVerificationStatus.REJECTED);
            assertThat(rejected.getDocumentImageFile().getId()).isEqualTo(1L);
            assertThat(response.ownerVerification().status()).isEqualTo("PENDING");
        }

        @Test
        @DisplayName("이미 관장/사범이면 신청할 수 없다")
        void ownerCannotRequest() {
            givenCurrentUser(user(7L, UserRole.OWNER));

            assertErrorCode(() -> ownerVerificationService.request(IMAGE_ID), ErrorCode.PERMISSION_DENIED);
            verify(ownerVerificationRequestRepository, never()).save(any());
        }

        @Test
        @DisplayName("없는 이미지로는 신청할 수 없다")
        void imageNotFound() {
            givenCurrentUser(user(7L, UserRole.USER));
            given(imageFileRepository.findById(IMAGE_ID)).willReturn(Optional.empty());

            assertErrorCode(() -> ownerVerificationService.request(IMAGE_ID), ErrorCode.IMAGE_FILE_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("목록")
    class GetRequests {

        @Test
        @DisplayName("클라이언트 sort 는 버리고 페이지 번호·크기만 넘기며, 커뮤니티 프로필 유무를 채운다")
        void ignoreSortAndMapProfile() {
            User withProfile = user(7L, UserRole.USER);
            User withoutProfile = user(8L, UserRole.USER);
            given(ownerVerificationRequestRepository.findByStatusWithUser(eq(OwnerVerificationStatus.PENDING), any()))
                    .willAnswer(inv -> new PageImpl<>(List.of(pendingOf(withProfile), pendingOf(withoutProfile)),
                            inv.getArgument(1), 2));
            given(communityProfileRepository.findUserIdsByUserIdIn(List.of(7L, 8L))).willReturn(List.of(7L));

            Page<AdminOwnerRequestResponse> page = ownerVerificationService.getRequests(
                    OwnerVerificationStatus.PENDING, PageRequest.of(1, 5, Sort.by("nickname")));

            ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
            verify(ownerVerificationRequestRepository).findByStatusWithUser(eq(OwnerVerificationStatus.PENDING), captor.capture());
            assertThat(captor.getValue().getSort().isUnsorted()).isTrue();
            assertThat(captor.getValue().getPageNumber()).isEqualTo(1);
            assertThat(captor.getValue().getPageSize()).isEqualTo(5);
            assertThat(page.getContent()).extracting(AdminOwnerRequestResponse::hasCommunityProfile)
                    .containsExactly(true, false);
        }

        @Test
        @DisplayName("빈 페이지면 커뮤니티 프로필을 조회하지 않는다")
        void emptyPage() {
            given(ownerVerificationRequestRepository.findByStatusWithUser(eq(OwnerVerificationStatus.PENDING), any()))
                    .willReturn(Page.empty());

            Page<AdminOwnerRequestResponse> page = ownerVerificationService.getRequests(
                    OwnerVerificationStatus.PENDING, PageRequest.of(0, 20));

            assertThat(page.getContent()).isEmpty();
            verify(communityProfileRepository, never()).findUserIdsByUserIdIn(any());
        }
    }

    @Nested
    @DisplayName("승인")
    class Approve {

        @Test
        @DisplayName("승인하면 OWNER 권한과 관장 프로필이 생기고 신청은 APPROVED 가 된다")
        void approve() {
            User admin = user(1L, UserRole.ADMIN);
            User applicant = user(7L, UserRole.USER);
            OwnerVerificationRequest request = pendingOf(applicant);
            CommunityProfile profile = CommunityProfile.builder().user(applicant).build();
            given(authenticationFacade.getCurrentUser()).willReturn(admin);
            givenLockedRequest(request);
            given(communityProfileRepository.findByUser(applicant)).willReturn(Optional.of(profile));
            given(ownerProfileRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            ownerVerificationService.approve(REQUEST_ID);

            assertThat(applicant.getRole()).isEqualTo(UserRole.OWNER);
            assertThat(profile.getOwnerProfile()).isNotNull();
            assertThat(request.getStatus()).isEqualTo(OwnerVerificationStatus.APPROVED);
            assertThat(request.getReviewedBy()).isSameAs(admin);
        }

        @Test
        @DisplayName("관장 프로필이 이미 있으면 새로 만들지 않는다")
        void keepExistingOwnerProfile() {
            User applicant = user(7L, UserRole.USER);
            OwnerProfile existing = OwnerProfile.builder().id(5L).build();
            CommunityProfile profile = CommunityProfile.builder().user(applicant).ownerProfile(existing).build();
            given(authenticationFacade.getCurrentUser()).willReturn(user(1L, UserRole.ADMIN));
            givenLockedRequest(pendingOf(applicant));
            given(communityProfileRepository.findByUser(applicant)).willReturn(Optional.of(profile));

            ownerVerificationService.approve(REQUEST_ID);

            verify(ownerProfileRepository, never()).save(any());
            assertThat(profile.getOwnerProfile()).isSameAs(existing);
        }

        @Test
        @DisplayName("커뮤니티 프로필이 없으면 승인할 수 없고 아무것도 바뀌지 않는다")
        void requiresCommunityProfile() {
            User applicant = user(7L, UserRole.USER);
            OwnerVerificationRequest request = pendingOf(applicant);
            given(authenticationFacade.getCurrentUser()).willReturn(user(1L, UserRole.ADMIN));
            givenLockedRequest(request);
            given(communityProfileRepository.findByUser(applicant)).willReturn(Optional.empty());

            assertErrorCode(() -> ownerVerificationService.approve(REQUEST_ID), ErrorCode.REQUIRED_PROFILE);
            assertThat(applicant.getRole()).isEqualTo(UserRole.USER);
            assertThat(request.getStatus()).isEqualTo(OwnerVerificationStatus.PENDING);
        }

        @Test
        @DisplayName("이미 처리된 신청은 승인할 수 없다")
        void notPending() {
            User applicant = user(7L, UserRole.USER);
            OwnerVerificationRequest request = pendingOf(applicant);
            request.reject(user(1L, UserRole.ADMIN), null);
            given(authenticationFacade.getCurrentUser()).willReturn(user(1L, UserRole.ADMIN));
            givenLockedRequest(request);

            assertErrorCode(() -> ownerVerificationService.approve(REQUEST_ID), ErrorCode.OWNER_REQUEST_NOT_PENDING);
            assertThat(applicant.getRole()).isEqualTo(UserRole.USER);
        }

        @Test
        @DisplayName("신청자 role 이 USER 가 아니면(비정상 상태) 승인할 수 없다")
        void applicantAlreadyOwner() {
            User applicant = user(7L, UserRole.OWNER);
            given(authenticationFacade.getCurrentUser()).willReturn(user(1L, UserRole.ADMIN));
            givenLockedRequest(pendingOf(applicant));

            assertErrorCode(() -> ownerVerificationService.approve(REQUEST_ID), ErrorCode.OWNER_REQUEST_NOT_PENDING);
        }

        @Test
        @DisplayName("없는 신청은 승인할 수 없다")
        void notFound() {
            given(authenticationFacade.getCurrentUser()).willReturn(user(1L, UserRole.ADMIN));
            given(ownerVerificationRequestRepository.findUserIdById(REQUEST_ID)).willReturn(Optional.empty());

            assertErrorCode(() -> ownerVerificationService.approve(REQUEST_ID), ErrorCode.OWNER_REQUEST_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("거절")
    class Reject {

        @Test
        @DisplayName("거절하면 REJECTED 와 사유가 남고 권한은 그대로다")
        void reject() {
            User admin = user(1L, UserRole.ADMIN);
            User applicant = user(7L, UserRole.USER);
            OwnerVerificationRequest request = pendingOf(applicant);
            given(authenticationFacade.getCurrentUser()).willReturn(admin);
            givenLockedRequest(request);

            ownerVerificationService.reject(REQUEST_ID, "  서류 불명확  ");

            assertThat(request.getStatus()).isEqualTo(OwnerVerificationStatus.REJECTED);
            assertThat(request.getRejectReason()).isEqualTo("서류 불명확");
            assertThat(request.getReviewedBy()).isSameAs(admin);
            assertThat(applicant.getRole()).isEqualTo(UserRole.USER);
        }

        @Test
        @DisplayName("빈 사유는 null 로 저장한다")
        void blankReason() {
            OwnerVerificationRequest request = pendingOf(user(7L, UserRole.USER));
            given(authenticationFacade.getCurrentUser()).willReturn(user(1L, UserRole.ADMIN));
            givenLockedRequest(request);

            ownerVerificationService.reject(REQUEST_ID, "   ");

            assertThat(request.getRejectReason()).isNull();
        }

        @Test
        @DisplayName("이미 처리된 신청은 거절할 수 없다")
        void notPending() {
            OwnerVerificationRequest request = pendingOf(user(7L, UserRole.USER));
            request.approve(user(1L, UserRole.ADMIN));
            given(authenticationFacade.getCurrentUser()).willReturn(user(1L, UserRole.ADMIN));
            givenLockedRequest(request);

            assertErrorCode(() -> ownerVerificationService.reject(REQUEST_ID, null), ErrorCode.OWNER_REQUEST_NOT_PENDING);
        }
    }
}
