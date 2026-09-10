package com.jiujitsu.api.domain.notice.factory;

import com.jiujitsu.api.domain.notice.entity.Notice;
import com.jiujitsu.api.global.fcm.entity.FcmPushType;
import com.jiujitsu.api.global.fcm.entity.PushActionType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 알림함에 저장되는 actionType 이 발송 지점이 넘긴 값을 따르는지 검증한다. (#120)
 * 푸시만 고치고 여기를 놓치면 알림함 딥링크는 계속 BOARD_DETAIL 로 남는다.
 */
class NoticeFactoryTest {

    private final NoticeFactory noticeFactory = new NoticeFactory();

    @Test
    @DisplayName("pushData 의 type 을 알림함 actionType 으로 저장한다")
    void createNotice_usesActionTypeFromPushData() {
        Notice notice = noticeFactory.createNotice(
                1L,
                FcmPushType.NEW_CHILD_COMMENTS,
                Map.of("type", PushActionType.BALANCE_DETAIL.name(), "data", "10")
        );

        assertThat(notice.getPushActionType()).isEqualTo(PushActionType.BALANCE_DETAIL);
        assertThat(notice.getData()).isEqualTo("10");
        assertThat(notice.getPushType()).isEqualTo(FcmPushType.NEW_CHILD_COMMENTS);
        assertThat(notice.getIsRead()).isFalse();
    }
}
