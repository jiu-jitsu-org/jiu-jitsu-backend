package com.jiujitsu.api.global.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "imagekit")
public record ImageKitProperties(
        String privateKey,  // imageKit 키값
        String apiUrl       // imageKit 파일 삭제 호출 url
) {}
