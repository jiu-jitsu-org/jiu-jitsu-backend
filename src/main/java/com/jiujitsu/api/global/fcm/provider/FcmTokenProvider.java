package com.jiujitsu.api.global.fcm.provider;

import com.google.auth.oauth2.GoogleCredentials;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

@Component
public class FcmTokenProvider {
    public String getAccessToken() throws IOException {

        GoogleCredentials googleCredentials =
                GoogleCredentials.getApplicationDefault().createScoped(
                        List.of("https://www.googleapis.com/auth/firebase.messaging")
                );

        googleCredentials.refreshIfExpired();

        return googleCredentials.getAccessToken().getTokenValue();
    }
}
