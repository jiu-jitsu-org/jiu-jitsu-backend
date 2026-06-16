package com.jiujitsu.api.global.fcm.entity;

import com.jiujitsu.api.domain.user.entity.UserAppInfo;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "push_send_log")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class PushSendLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    private Boolean isSuccess;

    @Column
    private String errorCode;

    @Column
    private String errorMessage;

    @Column
    @Enumerated(EnumType.STRING)
    private FcmPushType pushType;

    @Column
    private String title;

    @Column
    private String body;

    @Column
    @Enumerated(EnumType.STRING)
    private PushActionType pushActionType;

    @Column
    private String data;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_app_info_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private UserAppInfo userAppInfo;
}
