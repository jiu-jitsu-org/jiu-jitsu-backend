package com.jiujitsu.api.domain.file.service;

import com.jiujitsu.api.domain.community.content.repository.ContentRepository;
import com.jiujitsu.api.domain.community.profile.repository.OwnerProfileRepository;
import com.jiujitsu.api.domain.file.ImageFile;
import com.jiujitsu.api.domain.file.ImageFileStatus;
import com.jiujitsu.api.domain.file.dto.CdnSignatureResponse;
import com.jiujitsu.api.domain.file.dto.ImageFileRegisterRequest;
import com.jiujitsu.api.domain.file.dto.ImageFileResponse;
import com.jiujitsu.api.domain.file.repository.ImageFileRepository;
import com.jiujitsu.api.domain.user.repository.UserRepository;
import com.jiujitsu.api.global.exception.ErrorCode;
import com.jiujitsu.api.global.exception.ErrorException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ImageService {

    private final String IMAGEKIT_PRIVATE_KEY = System.getenv("IMAGEKIT_PRIVATE_KEY");
    private final ImageFileRepository imageFileRepository;
    private final ContentRepository contentRepository;
    private final UserRepository userRepository;
    private final OwnerProfileRepository ownerProfileRepository;
    private final WebClient webClient;

    @Transactional
    public void deleteImageFile(Long id) {
        ImageFile imageFile = imageFileRepository.findById(id)
                .orElseThrow(() -> new ErrorException(ErrorCode.IMAGE_FILE_NOT_FOUND));

        if (imageFile.getCdnId() != null) {
            deleteCdnFile(imageFile.getCdnId());
        }

        // Content join table 정리
        contentRepository.findByImageFile(imageFile)
                .forEach(content -> content.getImageFiles().remove(imageFile));

        // User 프로필 이미지 참조 제거
        userRepository.findByProfileImageFile(imageFile)
                .ifPresent(user -> user.updateProfileImage(null));

        // User 관장 신청 이미지 참조 제거
        userRepository.findByOwnerRequestImageFile(imageFile)
                .ifPresent(user -> user.clearOwnerRequestImageFile());

        // OwnerProfile 서류 이미지 참조 제거
        ownerProfileRepository.findByDocumentImageFile(imageFile)
                .ifPresent(ownerProfile -> ownerProfile.clearDocumentImageFile());

        imageFileRepository.delete(imageFile);
    }

    private void deleteCdnFile(String cdnId) {
        String authHeader = "Basic " + Base64.getEncoder()
                .encodeToString((IMAGEKIT_PRIVATE_KEY + ":").getBytes(StandardCharsets.UTF_8));
        try {
            webClient.delete()
                    .uri("https://api.imagekit.io/v1/files/" + cdnId)
                    .header(HttpHeaders.AUTHORIZATION, authHeader)
                    .retrieve()
                    .onStatus(
                            status -> !status.is2xxSuccessful() && status != HttpStatus.NOT_FOUND,
                            response -> Mono.error(new ErrorException(ErrorCode.FAILED_CDN_DELETE))
                    )
                    .toBodilessEntity()
                    .block();
        } catch (ErrorException e) {
            throw e;
        } catch (Exception e) {
            throw new ErrorException(ErrorCode.FAILED_CDN_DELETE);
        }
    }

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

        // 2️⃣ 만료 시간 (현재시간 + 4분)
        long expire = (System.currentTimeMillis() / 1000L) + 240;

        // 3️⃣ 서명 생성 (HMAC-SHA1)
        String signature = generateSignature(token, expire);

        // 4️⃣ 결과 반환
        return new CdnSignatureResponse(token, expire, signature);
    }

    private String generateSignature(String token, long expire) {

        try {
            String data = token + expire;

            Mac mac = Mac.getInstance("HmacSHA1");
            SecretKeySpec secretKey = new SecretKeySpec(IMAGEKIT_PRIVATE_KEY.getBytes(StandardCharsets.UTF_8), "HmacSHA1");
            mac.init(secretKey);

            byte[] hmacData = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));

            // Hex 문자열로 변환
            StringBuilder hex = new StringBuilder();
            for (byte b : hmacData) {
                hex.append(String.format("%02x", b));
            }

            return hex.toString();
        } catch (Exception e) {
            throw new ErrorException(ErrorCode.FAILED_SIGNATURE);
        }
    }
}
