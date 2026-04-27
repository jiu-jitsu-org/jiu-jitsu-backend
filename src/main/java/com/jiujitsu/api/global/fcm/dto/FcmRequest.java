package com.jiujitsu.api.global.fcm.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FcmRequest {

    private FcmMessage message;
}
