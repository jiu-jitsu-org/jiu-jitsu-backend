package com.jiujitsu.api.global.fcm.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@Getter
@Builder
public class FcmMessage {

    private String token;
    private FcmNotification fcmNotification;
    private Map<String, String> data;
}
