package com.jiujitsu.api.global.properties;

import com.jiujitsu.api.domain.notice.entity.Notice;
import com.jiujitsu.api.global.fcm.entity.FcmPushType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// 좋아요 푸시 기준 (#196): 1·5·10, 이후 10마다
class LikePushPropertiesTest {

    private final LikePushProperties policy = new LikePushProperties(List.of(1, 5, 10), 10);

    @ParameterizedTest(name = "좋아요 {0}개 → 기준 {1}")
    @CsvSource({"0,0", "1,1", "4,1", "5,5", "9,5", "10,10", "19,10", "20,20", "35,30", "100,100"})
    void reachedThreshold(long count, int expected) {
        assertThat(policy.reachedThreshold(count)).isEqualTo(expected);
    }

    @Test
    @DisplayName("설정을 바꾸면 기준이 따라간다 (사용자가 늘어 간격을 넓힐 때)")
    void configurable() {
        LikePushProperties wide = new LikePushProperties(List.of(1, 10), 50);
        assertThat(wide.reachedThreshold(9)).isEqualTo(1);
        assertThat(wide.reachedThreshold(59)).isEqualTo(10);
        assertThat(wide.reachedThreshold(60)).isEqualTo(60);
        assertThat(wide.reachedThreshold(110)).isEqualTo(110);
    }

    @Test
    @DisplayName("기준이 오름차순 양수가 아니거나 step 이 0 이하면 기동 실패")
    void invalidConfig() {
        assertThatThrownBy(() -> new LikePushProperties(List.of(5, 1), 10)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new LikePushProperties(List.of(0, 5), 10)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new LikePushProperties(List.of(1), 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new LikePushProperties(List.of(), 10)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("문구에 개수가 들어간다")
    void title() {
        assertThat(Notice.likeTitle(FcmPushType.CONTENTS_LIKE, 10)).isEqualTo("내 게시글에 좋아요가 10개 달렸어요");
        assertThat(Notice.likeTitle(FcmPushType.COMMENTS_LIKE, 7)).isEqualTo("내 댓글에 좋아요가 7개 달렸어요");
    }
}
