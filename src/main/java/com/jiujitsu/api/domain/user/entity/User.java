package com.jiujitsu.api.domain.user.entity;

import com.jiujitsu.api.domain.file.ImageFile;
import com.jiujitsu.api.domain.notice.entity.UserNoticeSetting;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    private String email;

    @Column
    private String nickname;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_image_file_id")
    private ImageFile profileImageFile;

    @Column
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SnsProvider snsProvider;

    @Column(nullable = false, unique = true)
    private String snsId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus status;

    // GA 회원 ID. 회원 번호는 프로필을 여는 값이라 쓰지 않고, 바뀌지 않는 무작위 값을 둔다
    @Column(nullable = false, unique = true, updatable = false)
    @Setter(AccessLevel.NONE)
    @Builder.Default
    private String analyticsId = UUID.randomUUID().toString();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private AccountType accountType = AccountType.MEMBER;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column
    private LocalDateTime deletedAt;

    // 그 유저의 콘텐츠가 받은 신고 수 합계 (신고 접수마다 증가, 감소 없음)
    @Column(name = "report_count", nullable = false)
    @Builder.Default
    private int reportCount = 0;

    // 운영자가 그 유저의 콘텐츠를 숨김·삭제한 횟수 (제재 검토 기준 판단용)
    @Column(name = "violation_count", nullable = false)
    @Builder.Default
    private int violationCount = 0;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<UserAppInfo> appInfos = new ArrayList<>();

    @OneToOne
    private UserNoticeSetting userNoticeSetting;

    public void updateProfile(String nickname, ImageFile profileImageFile) {
        if (nickname != null && !nickname.trim().isEmpty()) {
            this.nickname = nickname;
        }
        if (profileImageFile != null) {
            this.profileImageFile = profileImageFile;
        }
    }

    public void updateNickname(String nickname) {
        this.nickname = nickname;
    }

    public void updateProfileImage(ImageFile profileImageFile) {
        this.profileImageFile = profileImageFile;
    }

    public void updateStatus(UserStatus status) {
        this.status = status;
        if (status == UserStatus.DELETED) {
            this.deletedAt = LocalDateTime.now();
        } else if (status == UserStatus.ACTIVE) {
            this.deletedAt = null;
        }
    }

    public void updateRole(UserRole userRole) {
        this.role = userRole;
    }

    public void updateAccountType(AccountType accountType) {
        this.accountType = accountType;
    }

    public void suspend() {
        this.status = UserStatus.SUSPENDED;
    }

    public void restore() {
        if (this.status == UserStatus.SUSPENDED) {
            this.status = UserStatus.ACTIVE;
        }
    }

    public void incrementReportCount() {
        this.reportCount++;
    }

    public void incrementViolationCount() {
        this.violationCount++;
    }

    public boolean isWithinGracePeriod() {
        return deletedAt != null && deletedAt.plusDays(30).isAfter(LocalDateTime.now());
    }

    public boolean isGracePeriodExpired() {
        return deletedAt != null && LocalDateTime.now().isAfter(deletedAt.plusDays(30));
    }

}
