package com.jiujitsu.api.domain.user.service.sns;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.jiujitsu.api.domain.user.dto.SnsUserInfo;
import com.jiujitsu.api.domain.user.factory.UserFactory;
import com.jiujitsu.api.global.exception.ErrorCode;
import com.jiujitsu.api.global.exception.ErrorException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Slf4j
@Component
@RequiredArgsConstructor
public class GoogleSnsClient implements SnsClient {

    @Override
    public SnsUserInfo getUserInfo(String accessToken) {
        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier
                    .Builder(new NetHttpTransport(), GsonFactory.getDefaultInstance())
                    .setAudience(
                            Arrays.asList("874475612364-d2hf7uua9mnvd9ohqajnoorodqh18ss0.apps.googleusercontent.com",
                                    "874475612364-kusfm0abpdrv99dndudr2s27rto5j8gc.apps.googleusercontent.com"))
                    .build();

            GoogleIdToken response = verifier.verify(accessToken);

            if (response == null) {
                throw new ErrorException(ErrorCode.INVALID_TOKEN);
            }

            String snsId = response.getPayload().getSubject();
            String email = response.getPayload().getEmail();

            return new SnsUserInfo(snsId, email);

        } catch (Exception e) {
            log.error("구글 사용자 정보 조회 실패", e);
            throw new ErrorException(ErrorCode.KEY_PARSING_ERROR);
        }
    }
}
