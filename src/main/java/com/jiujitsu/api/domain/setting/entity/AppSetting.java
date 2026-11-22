package com.jiujitsu.api.domain.setting.entity;

import com.jiujitsu.api.domain.admin.entity.Account;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 서버 설정값 — key/value 형태의 운영 설정
 *
 * 주요 key:
 * - notification_send_time : 마감 알림 발송 시각 (예: "18:00")
 * - pre_check_alert_time   : 알림 전 확인 운영자 알림 시각 (예: "14:00")
 * - discord_webhook_url    : 운영자 디스코드 웹훅 주소 (비면 발송 안 함)
 */
@Entity
@Table(name = "app_settings")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AppSetting {

    @Id
    @Column(name = "setting_key", length = 100)
    private String key;

    @Column(name = "setting_value", nullable = false, length = 255)
    private String value;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by")
    private Account updatedBy;

    public AppSetting(String key, String value) {
        this.key = key;
        this.value = value;
    }

    public void update(String value, Account operator) {
        this.value = value;
        this.updatedAt = LocalDateTime.now();
        this.updatedBy = operator;
    }
}
