package com.jiujitsu.api.domain.user.service;

import com.jiujitsu.api.domain.community.profile.entity.CommunityProfile;
import com.jiujitsu.api.domain.community.profile.entity.OwnerProfile;
import com.jiujitsu.api.domain.community.profile.repository.CommunityProfileRepository;
import com.jiujitsu.api.domain.community.profile.repository.OwnerProfileRepository;
import com.jiujitsu.api.domain.file.ImageFile;
import com.jiujitsu.api.domain.file.dto.ImageInfo;
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
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class OwnerVerificationService {

    private final OwnerVerificationRequestRepository ownerVerificationRequestRepository;
    private final UserRepository userRepository;
    private final ImageFileRepository imageFileRepository;
    private final CommunityProfileRepository communityProfileRepository;
    private final OwnerProfileRepository ownerProfileRepository;
    private final AuthenticationFacade authenticationFacade;
    private final UserMapper userMapper;

    /**
     * 관장/사범 인증 신청 (심사 대기 중이면 서류만 교체)
     */
    public UserProfileResponse request(Long imageFileId) {
        // 동시 신청으로 PENDING 이 2건 생기지 않도록 유저 행을 잠근 뒤 확인 (잠금 순서: users → 신청)
        User user = authenticationFacade.getCurrentUserForUpdate();

        if (user.getRole() != UserRole.USER) {
            throw new ErrorException(ErrorCode.PERMISSION_DENIED);
        }

        ImageFile imageFile = imageFileRepository.findById(imageFileId)
                .orElseThrow(() -> new ErrorException(ErrorCode.IMAGE_FILE_NOT_FOUND));

        OwnerVerificationRequest request = ownerVerificationRequestRepository.findTopByUserOrderByIdDesc(user)
                .filter(OwnerVerificationRequest::isPending)
                .map(pending -> {
                    pending.replaceDocument(imageFile);
                    return pending;
                })
                .orElseGet(() -> ownerVerificationRequestRepository.save(OwnerVerificationRequest.builder()
                        .user(user)
                        .documentImageFile(imageFile)
                        .build()));

        return userMapper.toUserProfileResponse(user, request);
    }

    /**
     * 유저의 최신 신청 (프로필 응답의 ownerVerification 기준)
     */
    @Transactional(readOnly = true)
    public OwnerVerificationRequest findLatest(User user) {
        return ownerVerificationRequestRepository.findTopByUserOrderByIdDesc(user).orElse(null);
    }

    /**
     * (관리자) 신청 목록 - 신청 최신순 고정, 클라이언트 sort 는 무시
     */
    @Transactional(readOnly = true)
    public Page<AdminOwnerRequestResponse> getRequests(OwnerVerificationStatus status, Pageable pageable) {
        Page<OwnerVerificationRequest> requests = ownerVerificationRequestRepository.findByStatusWithUser(
                status, PageRequest.of(pageable.getPageNumber(), pageable.getPageSize()));

        List<Long> userIds = requests.getContent().stream()
                .map(request -> request.getUser().getId())
                .distinct()
                .toList();
        Set<Long> userIdsWithProfile = userIds.isEmpty()
                ? Set.of()
                : new HashSet<>(communityProfileRepository.findUserIdsByUserIdIn(userIds));

        return requests.map(request -> toAdminResponse(request, userIdsWithProfile.contains(request.getUser().getId())));
    }

    /**
     * (관리자) 승인 - 관장/사범 권한 부여 + 관장 프로필 생성
     */
    public void approve(Long requestId) {
        User admin = authenticationFacade.getCurrentUser();
        OwnerVerificationRequest request = findRequestForUpdate(requestId);
        User applicant = request.getUser();

        // 처리된 신청이거나, 같은 유저의 다른 신청이 먼저 승인된 경우
        if (!request.isPending() || applicant.getRole() != UserRole.USER) {
            throw new ErrorException(ErrorCode.OWNER_REQUEST_NOT_PENDING);
        }

        CommunityProfile communityProfile = communityProfileRepository.findByUser(applicant)
                .orElseThrow(() -> new ErrorException(ErrorCode.REQUIRED_PROFILE));

        request.approve(admin);
        applicant.updateRole(UserRole.OWNER);

        if (communityProfile.getOwnerProfile() == null) {
            OwnerProfile ownerProfile = ownerProfileRepository.save(OwnerProfile.builder().build());
            communityProfile.insertOwnerProfile(ownerProfile);
        }
    }

    /**
     * (관리자) 거절 - 권한은 그대로, 신청자는 다시 신청 가능
     */
    public void reject(Long requestId, String reason) {
        User admin = authenticationFacade.getCurrentUser();
        OwnerVerificationRequest request = findRequestForUpdate(requestId);
        request.reject(admin, StringUtils.trimToNull(reason));
    }

    /**
     * 잠금 순서를 신청 API 와 같게(신청자 users → 신청) 맞춰 교착을 막는다.
     * 둘 다 이 트랜잭션의 첫 로드라 잠금 후 최신 값을 읽는다.
     */
    private OwnerVerificationRequest findRequestForUpdate(Long requestId) {
        Long applicantId = ownerVerificationRequestRepository.findUserIdById(requestId)
                .orElseThrow(() -> new ErrorException(ErrorCode.OWNER_REQUEST_NOT_FOUND));
        userRepository.findByIdForUpdate(applicantId);
        return ownerVerificationRequestRepository.findByIdForUpdate(requestId)
                .orElseThrow(() -> new ErrorException(ErrorCode.OWNER_REQUEST_NOT_FOUND));
    }

    private AdminOwnerRequestResponse toAdminResponse(OwnerVerificationRequest request, boolean hasCommunityProfile) {
        User applicant = request.getUser();
        return new AdminOwnerRequestResponse(
                request.getId(),
                applicant.getId(),
                applicant.getNickname(),
                applicant.getEmail(),
                ImageInfo.from(applicant.getProfileImageFile()),
                ImageInfo.from(request.getDocumentImageFile()),
                hasCommunityProfile,
                request.getStatus(),
                request.getRejectReason(),
                request.getCreatedAt(),
                request.getReviewedAt()
        );
    }
}
