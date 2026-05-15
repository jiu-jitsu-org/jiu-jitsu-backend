package com.jiujitsu.api.domain.notice.entity;

import com.jiujitsu.api.global.entity.BaseEntity;
import com.jiujitsu.api.global.fcm.entity.FcmPushType;
import com.jiujitsu.api.global.fcm.entity.PushActionType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "notice")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notice extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    private Long userId;

    @Column(nullable = false)
    private String title;   // 제목

    @Column
    private String body;    // 내용

    @Column
    @Enumerated(EnumType.STRING)
    private FcmPushType pushType;

    @Column
    @Enumerated(EnumType.STRING)
    private PushActionType pushActionType;

    @Column
    private String data;

    @Column
    private Boolean isRead;

    public void setRead() {
        this.isRead = true;
    }
}
