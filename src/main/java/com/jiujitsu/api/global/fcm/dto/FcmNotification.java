package com.jiujitsu.api.global.fcm.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FcmNotification {

    private String title;
    private String body;
}
