package com.jiujitsu.api.domain.file.event;

import com.jiujitsu.api.global.properties.ImageKitProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Slf4j
@Component
@RequiredArgsConstructor
public class CdnDeleteEventListener {

    private final ImageKitProperties imagekitProperties;
    private final WebClient webClient;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleImageFileDeleted(ImageFileDeletedEvent event) {
        String authHeader = "Basic " + Base64.getEncoder()
                .encodeToString((imagekitProperties.privateKey() + ":").getBytes(StandardCharsets.UTF_8));
        try {
            webClient.delete()
                    .uri(imagekitProperties.apiUrl() + event.cdnId())
                    .header(HttpHeaders.AUTHORIZATION, authHeader)
                    .retrieve()
                    .onStatus(
                            status -> !status.is2xxSuccessful() && status != HttpStatus.NOT_FOUND,
                            response -> Mono.error(new RuntimeException("CDN 삭제 실패"))
                    )
                    .toBodilessEntity()
                    .block();
        } catch (Exception e) {
            log.error("CDN 파일 삭제 실패 - cdnId: {}", event.cdnId(), e);
        }
    }
}
