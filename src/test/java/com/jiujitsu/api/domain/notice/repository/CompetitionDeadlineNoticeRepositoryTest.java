package com.jiujitsu.api.domain.notice.repository;

import com.jiujitsu.api.domain.competition.entity.Competition;
import com.jiujitsu.api.domain.competition.entity.CompetitionFee;
import com.jiujitsu.api.domain.competition.entity.CompetitionInterest;
import com.jiujitsu.api.domain.competition.repository.CompetitionRepository;
import com.jiujitsu.api.domain.notice.entity.CompetitionDeadlineNoticeLog;
import com.jiujitsu.api.domain.notice.entity.CompetitionDeadlineType;
import com.jiujitsu.api.domain.notice.entity.UserNoticeSetting;
import com.jiujitsu.api.domain.user.entity.SnsProvider;
import com.jiujitsu.api.domain.user.entity.User;
import com.jiujitsu.api.domain.user.entity.UserRole;
import com.jiujitsu.api.domain.user.entity.UserStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// 마감 전날 알림 대상 대회·받는 사람·발송 기록 쿼리
@DataJpaTest
class CompetitionDeadlineNoticeRepositoryTest {

    private static final LocalDate D = LocalDate.of(2026, 11, 20);

    @Autowired private CompetitionRepository competitionRepository;
    @Autowired private UserNoticeSettingRepository userNoticeSettingRepository;
    @Autowired private CompetitionDeadlineNoticeLogRepository logRepository;
    @Autowired private TestEntityManager em;

    private Competition competition(String name, boolean published, LocalDate earlyBird, LocalDate registration, List<CompetitionFee> fees) {
        Competition competition = Competition.builder()
                .name(name)
                .searchName(Competition.normalize(name))
                .competitionYear(2026)
                .published(published)
                .openDate(published ? D.plusDays(30) : null)
                .earlyBirdDeadline(earlyBird)
                .registrationDeadline(registration)
                .fees(new ArrayList<>(fees))
                .build();
        return em.persist(competition);
    }

    // competitionEnabled: null 이면 이 칸이 생기기 전 가입자
    private User user(Boolean competitionEnabled) {
        User user = em.persist(User.builder()
                .snsProvider(SnsProvider.KAKAO)
                .snsId(UUID.randomUUID().toString())
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build());
        UserNoticeSetting setting = new UserNoticeSetting(user, false);
        if (competitionEnabled == null) {
            ReflectionTestUtils.setField(setting, "competitionEnabled", null);
        } else {
            setting.answerCompetitionNoticePrompt(competitionEnabled, LocalDateTime.now());
        }
        em.persist(setting);
        return user;
    }

    private CompetitionInterest interest(Competition competition, User user, boolean noticeEnabled) {
        CompetitionInterest interest = new CompetitionInterest(competition, user);
        if (!noticeEnabled) {
            interest.disableNotice();
        }
        return em.persist(interest);
    }

    @Test
    @DisplayName("대상 대회: 그날 끝나는 접수 마감·얼리버드 마감·할인 단계가 있는 공개 대회만, 중복 없이")
    void dueCompetitions() {
        competition("접수", true, null, D, List.of());
        competition("얼리버드", true, D, D.plusDays(5), List.of());
        competition("단계", true, D.plusDays(3), D.plusDays(5),
                List.of(new CompetitionFee("1차", 40000, D), new CompetitionFee("2차", 50000, D.plusDays(3)), new CompetitionFee("기본", 60000, null)));
        competition("둘다", true, D, D, List.of(new CompetitionFee("얼리버드", 50000, D), new CompetitionFee("기본", 60000, null)));
        competition("다른날", true, D.plusDays(1), D.plusDays(2), List.of());
        competition("미공개", false, null, null, List.of());
        em.flush();
        em.clear();

        assertThat(competitionRepository.findPublishedWithDeadlineOn(D))
                .extracting(Competition::getName)
                .containsExactlyInAnyOrder("접수", "얼리버드", "단계", "둘다");
    }

    @Test
    @DisplayName("받는 사람: 관심 + 이 대회 알림 켜짐 + 설정의 관심 대회 알림 켜짐만 (설정 null·꺼짐, 알림 끔, 관심 없음, 다른 대회 제외)")
    void recipients() {
        Competition target = competition("서울", true, D, D.plusDays(5), List.of());
        Competition other = competition("부산", true, D, D.plusDays(5), List.of());
        User ok = user(true);
        User settingOff = user(false);
        User settingNull = user(null);
        User muted = user(true);
        User otherOnly = user(true);
        user(true); // 관심 없음
        interest(target, ok, true);
        interest(target, settingOff, true);
        interest(target, settingNull, true);
        interest(target, muted, false);
        interest(other, otherOnly, true);
        em.flush();
        em.clear();

        assertThat(userNoticeSettingRepository.findCompetitionNoticeRecipientIds(target.getId()))
                .containsExactly(ok.getId());
    }

    @Test
    @DisplayName("발송 기록: 같은 대회·종류·마감일에 받은 유저만 찾고, 같은 기록은 두 번 저장되지 않는다")
    void logs() {
        logRepository.saveAndFlush(new CompetitionDeadlineNoticeLog(1L, CompetitionDeadlineType.EARLY_BIRD, D, 10L));
        logRepository.saveAndFlush(new CompetitionDeadlineNoticeLog(1L, CompetitionDeadlineType.EARLY_BIRD, D.minusDays(3), 11L));
        logRepository.saveAndFlush(new CompetitionDeadlineNoticeLog(1L, CompetitionDeadlineType.REGISTRATION, D, 12L));

        assertThat(logRepository.findNotifiedUserIds(1L, CompetitionDeadlineType.EARLY_BIRD, D, List.of(10L, 11L, 12L, 13L)))
                .containsExactly(10L);
        // 실행 서비스는 DB 메시지의 제약 이름으로 "다른 실행과 겹침"을 가린다 — 실제 예외에 이름이 담기는지 확인
        assertThatThrownBy(() -> logRepository.saveAndFlush(new CompetitionDeadlineNoticeLog(1L, CompetitionDeadlineType.EARLY_BIRD, D, 10L)))
                .isInstanceOf(DataIntegrityViolationException.class)
                .satisfies(e -> assertThat(((DataIntegrityViolationException) e).getMostSpecificCause().getMessage().toLowerCase())
                        .contains(CompetitionDeadlineNoticeLog.UNIQUE_CONSTRAINT));
    }
}
