package com.jiujitsu.api.domain.announcement.entity;

import com.jiujitsu.api.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
// 공지사항 (#274). 알림(notice)과 다른 도메인 — 모든 사용자에게 같은 글을 보여 준다
// 목록 · 상세 모두 게시 시각으로 거르고 정렬하므로 published_at 인덱스
@Table(name = "announcements",
        indexes = {
                @Index(name = "idx_announcement_published_at", columnList = "published_at")
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Announcement extends BaseEntity {

    public static final int TITLE_MAX_LENGTH = 100;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = TITLE_MAX_LENGTH)
    private String title;

    // 평문. 줄바꿈은 그대로 두고, 링크는 화면(FE)이 주소를 찾아 링크로 만든다
    @Column(nullable = false, columnDefinition = "TEXT")
    private String body;

    // 게시 시각 = 보이기 시작하는 시각이자 화면에 보이는 날짜. null 이면 안 보임(임시), 미래면 그때부터 보임(예약)
    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    public static Announcement create(String title, String body, LocalDateTime publishedAt) {
        Announcement announcement = new Announcement();
        announcement.title = title;
        announcement.body = body;
        announcement.publishedAt = publishedAt;
        return announcement;
    }
}
