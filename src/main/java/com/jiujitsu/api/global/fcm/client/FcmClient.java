package com.jiujitsu.api.global.fcm.client;

import com.jiujitsu.api.global.fcm.config.FcmFeignConfig;
import com.jiujitsu.api.global.fcm.dto.FcmRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
        name = "fcmClient",
        url = "https://fcm.googleapis.com",
        configuration = FcmFeignConfig.class
)
public interface FcmClient {

    @PostMapping("/v1/projects/{projectId}/messages:send")
    void sendPush(
            @PathVariable("projectId") String projectId,
            @RequestBody FcmRequest request
    );
}
