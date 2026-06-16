package com.jiujitsu.api.domain.notice.entity;

import com.jiujitsu.api.domain.notice.dto.NoticeSettingRequest;
import com.jiujitsu.api.domain.user.entity.User;
import com.jiujitsu.api.global.fcm.entity.NoticeGroupType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

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

    @OneToOne
    private User user;

    public UserNoticeSetting(User user) {
        this.securityEnabled = true;
        this.serviceEnabled = true;
        this.communityEnabled = true;
        this.marketingEnabled = false;
        this.user = user;
    }

    public void update(NoticeSettingRequest request) {
        this.securityEnabled = request.securityEnabled();
        this.serviceEnabled = request.serviceEnabled();
        this.communityEnabled = request.communityEnabled();
        this.marketingEnabled = request.marketingEnabled();
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
