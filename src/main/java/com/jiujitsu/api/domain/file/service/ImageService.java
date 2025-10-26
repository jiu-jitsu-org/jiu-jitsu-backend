package com.jiujitsu.api.domain.file.service;

import com.jiujitsu.api.domain.file.dto.CdnSignatureResponse;
import com.jiujitsu.api.global.exception.ErrorCode;
import com.jiujitsu.api.global.exception.ErrorException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ImageService {

    private final String IMAGEKIT_PRIVATE_KEY =  System.getenv("IMAGEKIT_PRIVATE_KEY");

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
