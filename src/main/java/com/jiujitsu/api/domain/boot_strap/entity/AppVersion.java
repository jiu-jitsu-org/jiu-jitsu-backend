package com.jiujitsu.api.domain.boot_strap.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

@Entity
@Table(name = "app_version")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class AppVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // H2에서도 잘 동작
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String osName;              // 예: ANDROID, IOS

    @Column(length = 50)
    private String minVersion;          // 최소 허용 버전 (예: 1.2.0)

    @Column(length = 50)
    private String nowVersion;          // 최신(권장) 버전

    @Column(nullable = false)
    private boolean needForceUpdate;    // 강제 업데이트 여부

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private Instant updatedAt;

}
