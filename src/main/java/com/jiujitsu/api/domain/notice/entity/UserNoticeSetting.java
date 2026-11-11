package com.jiujitsu.api.domain.notice.entity;

import com.jiujitsu.api.domain.notice.dto.NoticeSettingRequest;
import com.jiujitsu.api.domain.user.entity.User;
import com.jiujitsu.api.global.fcm.entity.NoticeGroupType;
import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "user_notice_setting")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserNoticeSetting {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    private Boolean securityEnabled;    // 계정보안알림

    @Column
    private Boolean serviceEnabled;     // 서비스공지알림

    @Column
    private Boolean communityEnabled;   // 커뮤니티활동알림

    @Column
    private Boolean marketingEnabled;   // 마케팅정보알림

    @Column
    private LocalDateTime marketingAgreedAt; // 마케팅 수신 동의 일시 (미동의 시 null)

    @Column
    private Boolean competitionEnabled;  // 관심 대회 마감 알림. 처음엔 꺼짐 (이 칸이 생기기 전 가입자는 null = 꺼짐)

    @Column
    private LocalDateTime competitionNoticeAskedAt; // 처음 관심 등록 때 관심 대회 알림을 물어본 시각 (안 물었으면 null)

    @OneToOne
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;

    /**
     * 회원가입 시 호출. isMarketingAgreed 에 따라 marketingEnabled 와 동의 일시를 초기화한다.
     */
    public UserNoticeSetting(User user, boolean isMarketingAgreed) {
        this.securityEnabled = true;
        this.serviceEnabled = true;
        this.communityEnabled = true;
        this.marketingEnabled = isMarketingAgreed;
        this.marketingAgreedAt = isMarketingAgreed ? LocalDateTime.now() : null;
        this.competitionEnabled = false;
        this.user = user;
    }

    public void update(NoticeSettingRequest request) {
        this.securityEnabled = request.securityEnabled();
        this.serviceEnabled = request.serviceEnabled();
        this.communityEnabled = request.communityEnabled();
        // 이 칸을 모르는 옛 앱은 보내지 않는다 — null 이면 그대로 둔다
        if (request.competitionEnabled() != null) {
            this.competitionEnabled = request.competitionEnabled();
        }

        // marketingEnabled 값이 실제로 변경될 때만 동의 일시를 갱신한다.
        if (!Objects.equals(this.marketingEnabled, request.marketingEnabled())) {
            this.marketingEnabled = request.marketingEnabled();
            this.marketingAgreedAt = Boolean.TRUE.equals(request.marketingEnabled())
                    ? LocalDateTime.now()
                    : null;
        }
    }

    public Boolean getEnabledByPushType(NoticeGroupType noticeGroupType) {
        boolean enabled = false;

        switch (noticeGroupType) {
            case SECURITY -> enabled = this.securityEnabled;
            case SERVICE -> enabled = this.serviceEnabled;
            case COMMUNITY -> enabled = this.communityEnabled;
            case MARKETING -> enabled = this.marketingEnabled;
            case COMPETITION -> enabled = isCompetitionEnabled();
        }

        return enabled;
    }

    public boolean isCompetitionEnabled() {
        return Boolean.TRUE.equals(this.competitionEnabled);
    }

    public boolean isCompetitionNoticeAsked() {
        return this.competitionNoticeAskedAt != null;
    }

    /**
     * 처음 관심 등록 때 묻는 "이 대회 마감 알림을 받을까요?"의 답 (알림 받기 = true, 나중에 = false).
     * 알림 받기면 관심 대회 알림을 켜고, 나중에면 지금 값을 그대로 둔다 (설정에서 미리 켠 사람을 끄지 않게).
     * 어느 쪽이든 물어봤음을 남겨 다시 묻지 않게 한다 (기기를 바꿔도)
     */
    public void answerCompetitionNoticePrompt(boolean enabled, LocalDateTime now) {
        if (enabled) {
            this.competitionEnabled = true;
        }
        if (this.competitionNoticeAskedAt == null) {
            this.competitionNoticeAskedAt = now;
        }
    }
}
