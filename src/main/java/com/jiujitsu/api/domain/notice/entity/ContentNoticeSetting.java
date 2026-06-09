package com.jiujitsu.api.domain.notice.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "content_notice_setting",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "content_id"}))
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContentNoticeSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "content_id", nullable = false)
    private Long contentId;

    @Column(nullable = false)
    private Boolean enabled;  // 게시물 알림 전체 켜기/끄기

    public void toggle() {
        this.enabled = !this.enabled;
    }
}
