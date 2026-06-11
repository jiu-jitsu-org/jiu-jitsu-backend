package com.jiujitsu.api.domain.file.service;

import com.jiujitsu.api.domain.community.content.repository.ContentRepository;
import com.jiujitsu.api.domain.community.profile.entity.OwnerProfile;
import com.jiujitsu.api.domain.community.profile.repository.OwnerProfileRepository;
import com.jiujitsu.api.domain.file.ImageFile;
import com.jiujitsu.api.domain.file.ImageFileStatus;
import com.jiujitsu.api.domain.file.dto.CdnSignatureResponse;
import com.jiujitsu.api.domain.file.dto.ImageFileRegisterRequest;
import com.jiujitsu.api.domain.file.dto.ImageFileResponse;
import com.jiujitsu.api.domain.file.event.ImageFileDeletedEvent;
import com.jiujitsu.api.domain.file.properties.ImageKitProperties;
import com.jiujitsu.api.domain.file.repository.ImageFileRepository;
import com.jiujitsu.api.domain.user.entity.User;
import com.jiujitsu.api.domain.user.repository.UserRepository;
import com.jiujitsu.api.global.exception.ErrorCode;
import com.jiujitsu.api.global.exception.ErrorException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ImageService {

    private final ImageKitProperties imagekitProperties;
    private final ImageFileRepository imageFileRepository;
    private final ContentRepository contentRepository;
    private final UserRepository userRepository;
    private final OwnerProfileRepository ownerProfileRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void deleteImageFile(Long id) {
        ImageFile imageFile = imageFileRepository.findById(id)
                .orElseThrow(() -> new ErrorException(ErrorCode.IMAGE_FILE_NOT_FOUND));

        // DB 커밋 후 CDN 삭제 (커밋 전 실패 시 이벤트 발행 안 됨)
        if (imageFile.getCdnId() != null) {
            eventPublisher.publishEvent(new ImageFileDeletedEvent(imageFile.getCdnId()));
        }

        //todo: 아래 각 도메인 참조가 현재 역방향성이라 이벤트 생성해서 처리하든 추후 아키텍쳐 개선이 필요함.

        // Content join table 정리
        contentRepository.findByImageFile(imageFile)
                .forEach(content -> content.getImageFiles().remove(imageFile));

        // User 프로필 이미지 참조 제거
        userRepository.findByProfileImageFile(imageFile)
                .ifPresent(user -> user.updateProfileImage(null));

        // User 관장 신청 이미지 참조 제거
        userRepository.findByOwnerRequestImageFile(imageFile)
                .ifPresent(User::clearOwnerRequestImageFile);

        // OwnerProfile 서류 이미지 참조 제거
        ownerProfileRepository.findByDocumentImageFile(imageFile)
                .ifPresent(OwnerProfile::clearDocumentImageFile);

        imageFileRepository.delete(imageFile);
    }

    /**
     * 이미지 파일 저장
     */
    @Transactional
    public ImageFileResponse registerImageFile(ImageFileRegisterRequest request) {
        ImageFile imageFile = ImageFile.builder()
                .cdnId(request.cdnId())
                .imageUrl(request.imageUrl())
                .status(ImageFileStatus.TEMP)
                .build();
        return ImageFileResponse.from(imageFileRepository.save(imageFile));
    }

    /**
     * CDN 요청용 서명 생성
     */
    public CdnSignatureResponse getCdnSignature() {
        String token = UUID.randomUUID().toString().replace("-", "");
        long expire = (System.currentTimeMillis() / 1000L) + 240;
        String signature = generateSignature(token, expire);
        return new CdnSignatureResponse(token, expire, signature);
    }

    private String generateSignature(String token, long expire) {
        try {
            String data = token + expire;
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(imagekitProperties.privateKey().getBytes(StandardCharsets.UTF_8), "HmacSHA1"));
            return HexFormat.of().formatHex(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new ErrorException(ErrorCode.FAILED_SIGNATURE);
        }
    }
}
