package com.jiujitsu.api.domain.community.competition.entity;

import com.jiujitsu.api.global.entity.BaseEntity;
import com.jiujitsu.api.global.exception.ErrorCode;
import com.jiujitsu.api.global.exception.ErrorException;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

@Entity
// 등록 대회 = 회차 단위 (같은 이름이라도 연도가 다르면 다른 대회)
// PostgreSQL 유니크는 NULL 끼리 겹침으로 보지 않으므로, 연도 없는 같은 이름은 CompetitionService 에서 걸러낸다
@Table(name = "competitions", uniqueConstraints = {
        @UniqueConstraint(name = "uk_competition_search_name_year", columnNames = {"search_name", "competition_year"})
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
