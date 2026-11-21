package com.jiujitsu.api.domain.user.entity;

import com.jiujitsu.api.domain.boot_strap.entity.OsType;
import com.jiujitsu.api.global.fcm.entity.PushSendLog;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "user_app_info",
        indexes = {
                @Index(name = "idx_user_app_info_user_id", columnList = "user_id")  // 푸시 토큰 조회
        }
)
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class UserAppInfo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String token;

    @Column
    private String deviceId;

    @Column
    @Enumerated(EnumType.STRING)
    private OsType osType;

    @Column
    private String osVersion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;

    @OneToMany(mappedBy = "userAppInfo")
    @Builder.Default
    private List<PushSendLog> pushSendLogs = new ArrayList<>();

    public void changeOsVersion(String osVersion) {
        this.osVersion = osVersion;
    }

    public void assignUser(User user) {
        this.user = user;
    }
}
