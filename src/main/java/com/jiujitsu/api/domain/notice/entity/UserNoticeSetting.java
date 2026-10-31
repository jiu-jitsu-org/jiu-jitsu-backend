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
        this.user = user;
    }

    public void update(NoticeSettingRequest request) {
        this.securityEnabled = request.securityEnabled();
        this.serviceEnabled = request.serviceEnabled();
        this.communityEnabled = request.communityEnabled();

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
        }

        return enabled;
    }
}
