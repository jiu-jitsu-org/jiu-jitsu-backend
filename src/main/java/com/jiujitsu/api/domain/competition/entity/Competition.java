package com.jiujitsu.api.domain.competition.entity;

import com.jiujitsu.api.global.entity.BaseEntity;
import com.jiujitsu.api.global.exception.ErrorCode;
import com.jiujitsu.api.global.exception.ErrorException;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.ColumnDefault;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@Entity
// 등록 대회 = 회차 단위 (같은 이름이라도 연도가 다르면 다른 대회)
// PostgreSQL 유니크는 NULL 끼리 겹침으로 보지 않으므로, 연도 없는 같은 이름은 CompetitionService 에서 걸러낸다
// 대회 탭 목록은 공개 대회를 개최일 순으로 조회하므로 (published, open_date) 인덱스를 둔다
@Table(name = "competitions",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_competition_search_name_year", columnNames = {"search_name", "competition_year"})
        },
        indexes = {
                @Index(name = "idx_competition_published_open_date", columnList = "published, open_date")
        })
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Competition extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    public static final int NAME_MAX_LENGTH = 100;

    @Column(nullable = false, length = NAME_MAX_LENGTH)
    private String name;    // 대회명 (표시용)

    // 검색·중복 판단용 이름. 공백 제거 + 소문자 (normalize 참고)
    // DB 함수(LOWER/REPLACE) 대신 저장 시점에 정규화해 H2·PostgreSQL 동작을 같게 둔다
    @Column(name = "search_name", nullable = false, length = NAME_MAX_LENGTH)
    private String searchName;

    @Column(name = "competition_year")
    private Integer competitionYear;    // 개최 연도 (모르면 null)

    // --- 대회 이벤트 정보 (대회 탭 노출용) ---
    // 하나의 Competition 은 데이터 완성도만 다를 뿐 같은 개념이다.
    // published=false 는 대회 탭 목록/상세에 노출하지 않는다. 두 가지다:
    // - 대회일 없음: 프로필 대회 기록 자동완성용 "이름(+연도)" 카탈로그 엔트리 (아래 이벤트 필드가 비어 있음)
    // - 대회일 있음: 관리자가 정보를 채우는 중인, 공개 전 대회 (관리자 대회 목록에 나옴)
    // published=true 는 관리자가 일정·장소 등을 채워 공개한 대회(쓰기/수집은 별도 이슈).
    // NOT NULL 컬럼은 DB 기본값을 함께 둔다: ddl-auto update 로 기존 행이 있는 테이블에 추가될 때
    // 기본값이 없으면 PostgreSQL 이 ADD COLUMN 을 거부한다
    @ColumnDefault("false")
    @Column(name = "published", nullable = false)
    private boolean published;

    @Column(name = "open_date")
    private LocalDate openDate;              // 대회일

    @Column(name = "start_time")
    private LocalTime startTime;             // 시작 시각 (모르면 null)

    @Enumerated(EnumType.STRING)
    @Column(name = "region", length = 20)
    private CompetitionRegion region;        // 지역(시·도)

    // 장소는 표시용 글자 두 칸. 필터는 region(시·도)으로만 한다
    // place 는 목록에도 쓰는 짧은 표기라 대회마다 다르게 적는다("서울 송파", "수원", "부산") — region 에서 조합하지 않는다
    @Column(name = "place", length = 200)
    private String place;                    // 짧은 장소 표기 (예: "서울 송파")

    @Column(name = "venue", length = 100)
    private String venue;                    // 경기장 이름 (상세에서 place 뒤에 붙임)

    @Column(name = "host", length = 100)
    private String host;                     // 주최 단체

    @Column(name = "notice_url", length = 500)
    private String noticeUrl;                // 대회 공지 링크

    // 종목(도복/노기) — 둘 다면 둘 다. 프로필 대회 기록의 출전 부문과 같은 어휘(CompetitionUniform)
    // 값 객체 집합이라 별도 엔티티 대신 @ElementCollection 으로 둔다
    // - uniform NOT NULL → (competition_id, uniform) 복합 PK 가 생겨 같은 종목 중복 행을 DB 가 막는다
    // - 대회 행이 지워지면 종목 행도 함께 지운다 (CompetitionRecord 의 @OnDelete 와 같은 DB 레벨 처리)
    //   Hibernate 는 @ElementCollection 에 @OnDelete 를 허용하지 않아 FK 정의로 ON DELETE CASCADE 를 명시한다
    // - 목록 조회 시 대회마다 따로 불러오지 않도록 배치 로딩
    @ElementCollection
    @CollectionTable(name = "competition_uniforms",
            joinColumns = @JoinColumn(name = "competition_id"),
            foreignKey = @ForeignKey(name = "fk_competition_uniforms_competition",
                    foreignKeyDefinition = "foreign key (competition_id) references competitions (id) on delete cascade"))
    @Column(name = "uniform", nullable = false, length = 10)
    @Enumerated(EnumType.STRING)
    @BatchSize(size = 100)
    @Builder.Default
    private Set<CompetitionUniform> uniforms = new LinkedHashSet<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "audience", length = 20)
    private CompetitionAudience audience;    // 대상 제한 (null = 제한 없음)

    // 접수 일정 (대회일 = openDate)
    @Column(name = "registration_start_date")
    private LocalDate registrationStartDate; // 접수 시작

    @Column(name = "early_bird_deadline")
    private LocalDate earlyBirdDeadline;     // 얼리버드 마감

    @Column(name = "registration_deadline")
    private LocalDate registrationDeadline;  // 접수 마감

    // 참가비 단계 — 대회마다 단계 수가 달라(얼리버드·기본, 1차·2차 얼리버드 등) 목록으로 둔다. 모르면 비어 있음
    // uniforms 와 같은 값 객체 컬렉션. 입력 순서 = 표시 순서(fee_order), 대회 행이 지워지면 함께 지운다
    @ElementCollection
    @CollectionTable(name = "competition_fees",
            joinColumns = @JoinColumn(name = "competition_id"),
            foreignKey = @ForeignKey(name = "fk_competition_fees_competition",
                    foreignKeyDefinition = "foreign key (competition_id) references competitions (id) on delete cascade"))
    @OrderColumn(name = "fee_order")
    @BatchSize(size = 100)
    @Builder.Default
    private List<CompetitionFee> fees = new ArrayList<>();

    @Column(name = "fee_note", length = 100)
    private String feeNote;                  // 예: "성인 1종목 기준"

    // 메타
    @Column(name = "last_verified_date")
    private LocalDate lastVerifiedDate;      // 정보 기준일(마지막 확인일)

    @ColumnDefault("0")
    @Column(name = "view_count", nullable = false)
    private long viewCount;                  // 조회수 (CompetitionRepository#incrementViewCount 로만 올린다)

    // 웹 페이지(공개 일정)에서 복사한 대회명에 섞여 들어오는 문자를 정리한다
    // - \s 는 ASCII 공백만 잡으므로 NBSP(U+00A0)·전각 공백(U+3000) 등 유니코드 공백(\p{Z})도 함께 본다
    // - 폭 없는 공백(U+200B)·BOM(U+FEFF) 같은 서식 문자(\p{Cf}), NUL 등 공백이 아닌 제어 문자(\p{Cc})는 지운다
    // - 표시용 이름은 NFC(분리된 한글 자모 결합)만, 검색용 이름은 NFKC(전각 영숫자 Ｓｅｏｕｌ 등까지 통일)
    //   표시용에 NFKC 를 쓰면 "ㅇㅇ컵"의 호환 자모, "㈜" 같은 원문 표기가 바뀐다
    private static final Pattern SPACES = Pattern.compile("[\\s\\p{Z}]+");
    private static final Pattern INVISIBLES = Pattern.compile("[\\p{Cf}\\p{Cc}&&[^\\s]]+");

    // 중복 판단 키: 정규화 이름 + 연도 (연도 없음도 하나의 값으로 본다)
    public String duplicateKey() {
        return searchName + "|" + competitionYear;
    }

    /**
     * 종목(읽기 전용). 컬렉션을 밖에서 바꾸지 못하게 한다
     */
    public Set<CompetitionUniform> getUniforms() {
        return Collections.unmodifiableSet(uniforms);
    }

    /**
     * 참가비 단계(읽기 전용, 입력 순서)
     */
    public List<CompetitionFee> getFees() {
        return Collections.unmodifiableList(fees);
    }

    /**
     * 오늘 기준 접수/진행 상태 파생 (저장하지 않음). 공개 대회만 상태가 있다.
     * 대회일이 지나면 ENDED, 접수 마감이 지났으면 CLOSED, 접수 시작 전이면 UPCOMING, 그 외 OPEN.
     * 비어 있는 일정은 제약으로 보지 않는다(채워진 정보로만 판단). 대회 당일은 접수 마감 뒤라면 CLOSED.
     * 오늘 날짜는 호출부가 Clock 으로 구해 넘긴다(테스트에서 날짜 고정).
     *
     * @throws IllegalStateException 미공개 대회 — 대회 탭에 없는 대회라 상태를 판단하지 않는다.
     *                               조회 쪽에서 published 필터를 빠뜨린 프로그래밍 오류를 바로 드러낸다
     */
    public CompetitionStatus status(LocalDate today) {
        if (!published) {
            throw new IllegalStateException("미공개 대회는 접수 상태가 없습니다. competitionId=" + id);
        }
        if (openDate != null && today.isAfter(openDate)) {
            return CompetitionStatus.ENDED;
        }
        if (registrationDeadline != null && today.isAfter(registrationDeadline)) {
            return CompetitionStatus.CLOSED;
        }
        if (registrationStartDate != null && today.isBefore(registrationStartDate)) {
            return CompetitionStatus.UPCOMING;
        }
        return CompetitionStatus.OPEN;
    }

    /**
     * 오늘 기준 얼리버드 접수 중인지 — 얼리버드 마감일이 있고 아직 지나지 않았으며 접수가 열려 있는 상태.
     * 응답 보조 값일 뿐, 어느 마감을 보여 주고 강조할지는 앱 화면 규칙이 날짜 원본으로 정한다.
     */
    public boolean isEarlyBirdOpen(LocalDate today) {
        // status 를 먼저 불러 미공개 엔트리는 얼리버드 정보가 없어도 똑같이 막는다
        return status(today) == CompetitionStatus.OPEN
                && earlyBirdDeadline != null
                && !today.isAfter(earlyBirdDeadline);
    }

    /**
     * 공개에 필요한데 비어 있는 칸 (CompetitionRequiredField 선언 순서). 다 채워졌으면 빈 목록
     * 관리자 목록·상세에서 채울 칸을 보여 준다. 저장 시 강제하지 않으므로(validateEventInfo 의 필수 칸 검사는 공개 대회의 대회일뿐 — 그 밖엔 연도·일정 순서·참가비 규칙)
     * 공개 대회도 빈 칸이 있을 수 있다 (공개 전환 때만 검사 — publish 참고)
     */
    public List<CompetitionRequiredField> missingRequiredFields() {
        List<CompetitionRequiredField> missing = new ArrayList<>();
        if (openDate == null) {
            missing.add(CompetitionRequiredField.OPEN_DATE);
        }
        if (startTime == null) {
            missing.add(CompetitionRequiredField.START_TIME);
        }
        if (region == null) {
            missing.add(CompetitionRequiredField.REGION);
        }
        if (uniforms.isEmpty()) {
            missing.add(CompetitionRequiredField.UNIFORMS);
        }
        if (registrationStartDate == null) {
            missing.add(CompetitionRequiredField.REGISTRATION_START_DATE);
        }
        if (registrationDeadline == null) {
            missing.add(CompetitionRequiredField.REGISTRATION_DEADLINE);
        }
        if (noticeUrl == null || noticeUrl.isBlank()) {
            missing.add(CompetitionRequiredField.NOTICE_URL);
        }
        if (lastVerifiedDate == null) {
            missing.add(CompetitionRequiredField.LAST_VERIFIED_DATE);
        }
        return missing;
    }

    /**
     * 확인 완료 — 운영자가 대회 공지와 맞춰 본 날로 마지막 확인일을 바꾼다 (공개 여부와 무관)
     */
    public void verify(LocalDate today) {
        this.lastVerifiedDate = today;
    }

    /**
     * 대회 탭에 공개. 공개 = 운영자가 공지와 맞춰 본 대회라 마지막 확인일도 오늘로 바꾼다
     * 그래서 마지막 확인일을 뺀 필수 칸이 하나라도 비면 COMPETITION_REQUIRED_FIELDS_MISSING (어떤 칸인지는 missingRequiredFields)
     * 이미 공개면 그대로 둔다 (확인일 갱신은 verify). 검사는 공개로 바꿀 때만 — 이미 공개된 대회는 빈 칸이 있어도 공개를 유지한다
     */
    public void publish(LocalDate today) {
        if (published) {
            return;
        }
        boolean missing = missingRequiredFields().stream()
                .anyMatch(field -> field != CompetitionRequiredField.LAST_VERIFIED_DATE);
        if (missing) {
            throw new ErrorException(ErrorCode.COMPETITION_REQUIRED_FIELDS_MISSING);
        }
        this.published = true;
        this.lastVerifiedDate = today;
    }

    /**
     * 대회 탭에서 내림. 관심·알림 설정·게시글 연결은 지우지 않는다 — 공개 대회만 보는 곳에서 숨겨졌다가 다시 공개하면 돌아온다
     */
    public void unpublish() {
        this.published = false;
    }

    /**
     * 자동완성용 카탈로그 엔트리인가 (대회일 없음 — 관리자 일괄 등록으로만 생긴다)
     */
    public boolean isCatalogEntry() {
        return openDate == null;
    }

    /**
     * 대회명·연도 바꾸기 (표시용·검색용 이름을 함께 정리). 등록할 수 없는 이름이면 WRONG_PARAMETER (isRegistrableName 참고)
     * 다른 대회와 겹치는지는 호출부가 본다 (DB 유니크 uk_competition_search_name_year 가 최종 방어)
     */
    public void rename(String name, Integer competitionYear) {
        if (!isRegistrableName(name)) {
            throw new ErrorException(ErrorCode.WRONG_PARAMETER);
        }
        this.name = cleanDisplayName(name);
        this.searchName = normalize(this.name);
        this.competitionYear = competitionYear;
    }

    /**
     * 대회 정보 칸 전부 교체 (관리자 입력 — 보낸 값 그대로, 비어 있는 칸은 비움). 공개 여부·마지막 확인일·조회수는 그대로
     * 종목·참가비는 Hibernate 가 들고 있는 컬렉션을 비우고 채운다 (불변 리스트를 할당하면 merge 시 clear 에서 터진다)
     * 참가비만 바뀌면 @PreUpdate 가 돌지 않을 수 있어 끝에 validateEventInfo 를 직접 부른다 — 위반이면 COMPETITION_INVALID_EVENT_INFO
     */
    public void changeEventInfo(CompetitionEventInfo info) {
        this.openDate = info.openDate();
        this.startTime = info.startTime();
        this.region = info.region();
        this.place = info.place();
        this.venue = info.venue();
        this.host = info.host();
        this.noticeUrl = info.noticeUrl();
        this.uniforms.clear();
        this.uniforms.addAll(info.uniforms());
        this.audience = info.audience();
        this.registrationStartDate = info.registrationStartDate();
        this.earlyBirdDeadline = info.earlyBirdDeadline();
        this.registrationDeadline = info.registrationDeadline();
        this.fees.clear();
        this.fees.addAll(info.fees());
        this.feeNote = info.feeNote();
        validateEventInfo();
    }

    /**
     * 이벤트 정보 불변식 검증. 위반이면 COMPETITION_INVALID_EVENT_INFO
     * - 공개 대회는 대회일이 있어야 한다
     * - 대회일과 연도가 둘 다 있으면 연도 = 대회일 연도 (자동완성용 대회와 이름 + 연도로 이어 붙이므로 어긋나면 엉뚱한 행에 붙거나 중복이 생긴다)
     * - 일정 순서: 접수 시작 ≤ 얼리버드 마감 ≤ 접수 마감 ≤ 대회일 (비어 있는 값은 건너뛰고 채워진 것끼리 비교)
     * - 참가비 단계: validateFees 참고
     * 저장 직전(@PrePersist/@PreUpdate)에 항상 확인해 어떤 쓰기 경로로도 잘못된 데이터가 저장되지 않게 한다.
     * 쓰기 쪽(관리자 입력·정보 수집)은 저장 전에 직접 호출해 400 으로 응답할 것 —
     * 커밋 시점 flush 에서 터지면 트랜잭션 예외로 감싸져 에러 코드가 사라진다
     */
    @PrePersist
    @PreUpdate
    public void validateEventInfo() {
        if (published && openDate == null) {
            throw new ErrorException(ErrorCode.COMPETITION_INVALID_EVENT_INFO);
        }
        validateYear(competitionYear, openDate);
        List<LocalDate> schedule = Stream.of(registrationStartDate, earlyBirdDeadline, registrationDeadline, openDate)
                .filter(Objects::nonNull)
                .toList();
        for (int i = 1; i < schedule.size(); i++) {
            if (schedule.get(i).isBefore(schedule.get(i - 1))) {
                throw new ErrorException(ErrorCode.COMPETITION_INVALID_EVENT_INFO);
            }
        }
        validateFees();
    }

    /**
     * 참가비 단계 불변식 (단계가 없으면 = 참가비 모름, 검사할 것 없음)
     * - 금액은 0 이상(무료 대회·무료 단계 허용)이고 이름은 비어 있지 않고 20자 이하(넘으면 flush 시점 500 이 되므로 여기서 막음)
     * - 기한은 목록 순서대로 엄격히 오름차순이고, 기한 없는 단계(기본 참가비)는 맨 마지막 하나만
     * - 기한은 접수 기간 안: 접수 시작 이상. 상한(접수 마감, 없으면 대회일 이하)은 따로 보지 않는다 —
     *   일정 순서 검사가 얼리버드 마감 ≤ 접수 마감 ≤ 대회일을 먼저 보장하고, 기한은 오름차순에 마지막이 얼리버드 마감과 같아야 해서
     *   상한을 넘는 기한은 아래 얼리버드 마감 일치 검사에서 걸린다
     * - 얼리버드 마감일은 일정의 earlyBirdDeadline 하나가 기준이다(상태·강조·알림이 쓰는 날짜).
     *   기한 있는 단계(= 할인 단계)가 있으면 earlyBirdDeadline 이 있어야 하고, 그 날짜는 **마지막 할인 단계의 기한**과 같아야 한다 —
     *   할인이 끝나는 날이 얼리버드 마감. 1·2차 얼리버드에서 1차 날짜를 넣으면 2차 할인 중에 얼리버드가 끝난 것으로 보이므로 막는다.
     *   기한 있는 단계가 없으면(기본 참가비만 앎) 얼리버드 마감일만 따로 있어도 된다
     * fees 만 바꾸는 쓰기는 소유 엔티티 칸이 그대로라 @PreUpdate 가 돌지 않을 수 있다 —
     * 참가비를 바꾸는 메서드를 만들면 그 안에서 validateEventInfo 를 직접 부를 것
     */
    private void validateFees() {
        LocalDate previousUntil = null;
        for (int i = 0; i < fees.size(); i++) {
            CompetitionFee fee = fees.get(i);
            boolean last = i == fees.size() - 1;
            if (fee == null || fee.getAmount() < 0 || fee.getLabel() == null || fee.getLabel().isBlank()
                    || fee.getLabel().length() > CompetitionFee.LABEL_MAX_LENGTH) {
                throw new ErrorException(ErrorCode.COMPETITION_INVALID_EVENT_INFO);
            }
            LocalDate until = fee.getUntil();
            if (until == null) {
                if (!last) {
                    throw new ErrorException(ErrorCode.COMPETITION_INVALID_EVENT_INFO);
                }
                continue;
            }
            if ((previousUntil != null && !until.isAfter(previousUntil))
                    || (registrationStartDate != null && until.isBefore(registrationStartDate))) {
                throw new ErrorException(ErrorCode.COMPETITION_INVALID_EVENT_INFO);
            }
            previousUntil = until;
        }

        boolean hasDeadlineTier = previousUntil != null;
        if (hasDeadlineTier && earlyBirdDeadline == null) {
            throw new ErrorException(ErrorCode.COMPETITION_INVALID_EVENT_INFO);
        }
        if (hasDeadlineTier && !earlyBirdDeadline.equals(previousUntil)) {
            throw new ErrorException(ErrorCode.COMPETITION_INVALID_EVENT_INFO);
        }
    }

    /**
     * 대회일과 연도가 둘 다 있으면 연도 = 대회일 연도. 아니면 COMPETITION_INVALID_EVENT_INFO
     * validateEventInfo 의 일부. 관리자 쓰기는 이름 + 연도로 같은 대회를 찾기 전에 먼저 부른다 — 연도 오타가 중복(R0008)으로 보이지 않게
     */
    public static void validateYear(Integer competitionYear, LocalDate openDate) {
        if (openDate != null && competitionYear != null && openDate.getYear() != competitionYear) {
            throw new ErrorException(ErrorCode.COMPETITION_INVALID_EVENT_INFO);
        }
    }

    /**
     * 정리한 뒤에도 등록할 수 있는 이름인가 (비지 않고 최대 길이 이내)
     * @NotBlank·@Size 는 정리 전 원문 기준이라 NBSP 만 있는 이름, 소문자 변환으로 길어지는 이름을 놓친다
     */
    public static boolean isRegistrableName(String name) {
        String displayName = cleanDisplayName(name);
        String searchName = normalize(displayName);
        return !searchName.isEmpty()
                && displayName.length() <= NAME_MAX_LENGTH
                && searchName.length() <= NAME_MAX_LENGTH;
    }

    /**
     * 등록 대회 생성. 등록할 수 없는 이름이면 WRONG_PARAMETER (isRegistrableName 참고)
     */
    public static Competition of(String name, Integer competitionYear) {
        if (!isRegistrableName(name)) {
            throw new ErrorException(ErrorCode.WRONG_PARAMETER);
        }
        String displayName = cleanDisplayName(name);
        String searchName = normalize(displayName);
        return Competition.builder()
                .name(displayName)
                .searchName(searchName)
                .competitionYear(competitionYear)
                .build();
    }

    /**
     * 검색·중복 판단용 정규화: 모든 공백 제거 + 소문자
     * 예) "서울 오픈 2026" → "서울오픈2026", "Seoul Open" → "seoulopen"
     */
    public static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return SPACES.matcher(clean(value, Normalizer.Form.NFKC)).replaceAll("").toLowerCase(Locale.ROOT);
    }

    /**
     * 표시용 이름: 연속 공백을 하나로 줄이고 앞뒤 공백 제거
     */
    public static String cleanDisplayName(String value) {
        if (value == null) {
            return "";
        }
        return SPACES.matcher(clean(value, Normalizer.Form.NFC)).replaceAll(" ").trim();
    }

    private static String clean(String value, Normalizer.Form form) {
        return INVISIBLES.matcher(Normalizer.normalize(value, form)).replaceAll("");
    }
}
