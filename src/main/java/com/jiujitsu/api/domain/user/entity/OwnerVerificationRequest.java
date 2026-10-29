package com.jiujitsu.api.domain.user.entity;

import com.jiujitsu.api.domain.file.ImageFile;
import com.jiujitsu.api.global.exception.ErrorCode;
import com.jiujitsu.api.global.exception.ErrorException;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 관장/사범 인증 신청. 재신청은 새 row 로 쌓아 이력을 남긴다.
 */
@Entity
@Table(name = "owner_verification_request")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class OwnerVerificationRequest {

    public static final int REJECT_REASON_MAX_LENGTH = 500;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;    // 신청자 (영구 삭제 시 신청도 삭제)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_image_file_id")
    private ImageFile documentImageFile;    // 제출 서류 (이미지 삭제 시 null)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private OwnerVerificationStatus status = OwnerVerificationStatus.PENDING;

    @Column(length = REJECT_REASON_MAX_LENGTH)
    private String rejectReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private User reviewedBy;    // 처리한 관리자

    @Column
    private LocalDateTime reviewedAt;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;    // 신청 시각

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public boolean isPending() {
        return status == OwnerVerificationStatus.PENDING;
    }

    /**
     * 심사 대기 중 서류 재업로드
     */
    public void replaceDocument(ImageFile documentImageFile) {
        validatePending();
        this.documentImageFile = documentImageFile;
    }

    public void approve(User admin) {
        validatePending();
        this.status = OwnerVerificationStatus.APPROVED;
        this.reviewedBy = admin;
        this.reviewedAt = LocalDateTime.now();
    }

    public void reject(User admin, String rejectReason) {
        validatePending();
        this.status = OwnerVerificationStatus.REJECTED;
        this.rejectReason = rejectReason;
        this.reviewedBy = admin;
        this.reviewedAt = LocalDateTime.now();
    }

    public void clearDocumentImageFile() {
        this.documentImageFile = null;
    }

    private void validatePending() {
        if (!isPending()) {
            throw new ErrorException(ErrorCode.OWNER_REQUEST_NOT_PENDING);
        }
    }
}
