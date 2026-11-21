package com.jiujitsu.api.domain.announcement.service;

import com.jiujitsu.api.domain.announcement.dto.AnnouncementDetailResponse;
import com.jiujitsu.api.domain.announcement.dto.AnnouncementListResponse;
import com.jiujitsu.api.domain.announcement.entity.Announcement;
import com.jiujitsu.api.domain.announcement.mapper.AnnouncementMapper;
import com.jiujitsu.api.domain.announcement.repository.AnnouncementRepository;
import com.jiujitsu.api.global.exception.ErrorCode;
import com.jiujitsu.api.global.exception.ErrorException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AnnouncementQueryServiceTest {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 12, 1, 10, 0);

    @Mock
    private AnnouncementRepository announcementRepository;

    private AnnouncementQueryService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(NOW.atZone(SEOUL).toInstant(), SEOUL);
        service = new AnnouncementQueryService(announcementRepository, new AnnouncementMapper(), clock);
    }

    private Announcement announcement(long id, String title, String body, LocalDateTime publishedAt) {
        Announcement announcement = Announcement.create(title, body, publishedAt);
        ReflectionTestUtils.setField(announcement, "id", id);
        return announcement;
    }

    @Test
    @DisplayName("목록: 지금(Clock) 기준으로 조회하고, 클라이언트 sort 는 버리고 page · size 만 넘긴다")
    void getList() {
        Announcement a = announcement(1L, "약관 변경 안내", "본문", NOW.minusDays(1));
        given(announcementRepository.findPublished(eq(NOW), any(Pageable.class)))
                .willAnswer(inv -> new PageImpl<>(List.of(a), inv.getArgument(1), 1));

        Page<AnnouncementListResponse> result = service.getList(PageRequest.of(2, 5, Sort.by("title")));

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(announcementRepository).findPublished(eq(NOW), captor.capture());
        assertThat(captor.getValue().getPageNumber()).isEqualTo(2);
        assertThat(captor.getValue().getPageSize()).isEqualTo(5);
        assertThat(captor.getValue().getSort().isUnsorted()).isTrue();
        assertThat(result.getContent())
                .containsExactly(new AnnouncementListResponse(1L, "약관 변경 안내", NOW.minusDays(1)));
    }

    @Test
    @DisplayName("상세: 본문 줄바꿈을 그대로 준다")
    void getDetail() {
        String body = "첫 줄\n둘째 줄 https://example.com";
        given(announcementRepository.findPublishedById(1L, NOW))
                .willReturn(Optional.of(announcement(1L, "제목", body, NOW)));

        AnnouncementDetailResponse result = service.getDetail(1L);

        assertThat(result).isEqualTo(new AnnouncementDetailResponse(1L, "제목", body, NOW));
    }

    @Test
    @DisplayName("상세: 없거나 게시 전이면 404 ANNOUNCEMENT_NOT_FOUND")
    void getDetail_notFound() {
        given(announcementRepository.findPublishedById(9L, NOW)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.getDetail(9L))
                .isInstanceOf(ErrorException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ANNOUNCEMENT_NOT_FOUND);
    }
}
