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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SnsProvider snsProvider;

    @Column(nullable = false, unique = true)
    private String snsId;

    @Column(nullable = false)
    private Boolean ownerRequested;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_request_image_file_id")
    private ImageFile ownerRequestImageFile;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus status;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column
    private LocalDateTime deletedAt;

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

    public boolean isWithinGracePeriod() {
        return deletedAt != null && deletedAt.plusDays(30).isAfter(LocalDateTime.now());
    }

    public boolean isGracePeriodExpired() {
        return deletedAt != null && LocalDateTime.now().isAfter(deletedAt.plusDays(30));
    }

    public void requestOwner(ImageFile ownerRequestImageFile) {
        this.ownerRequested = true;
        this.ownerRequestImageFile = ownerRequestImageFile;
    }
}
