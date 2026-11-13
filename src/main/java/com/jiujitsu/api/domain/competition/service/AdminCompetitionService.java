package com.jiujitsu.api.domain.competition.service;

import com.jiujitsu.api.domain.competition.dto.AdminCompetitionDetailResponse;
import com.jiujitsu.api.domain.competition.dto.AdminCompetitionLinkResponse;
import com.jiujitsu.api.domain.competition.dto.AdminCompetitionListItem;
import com.jiujitsu.api.domain.competition.dto.AdminCompetitionListRequest;
import com.jiujitsu.api.domain.competition.dto.AdminCompetitionRequest;
import com.jiujitsu.api.domain.competition.dto.AdminCompetitionSaveResponse;
import com.jiujitsu.api.domain.competition.entity.Competition;
import com.jiujitsu.api.domain.competition.entity.CompetitionLink;
import com.jiujitsu.api.domain.competition.entity.CompetitionLinkType;
import com.jiujitsu.api.domain.competition.event.CompetitionRenamedEvent;
import com.jiujitsu.api.domain.competition.mapper.CompetitionMapper;
import com.jiujitsu.api.domain.competition.repository.CompetitionLinkRepository;
import com.jiujitsu.api.domain.competition.repository.CompetitionRepository;
import com.jiujitsu.api.global.exception.ErrorCode;
import com.jiujitsu.api.global.exception.ErrorException;
import com.jiujitsu.api.global.util.ConstraintViolationUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static com.jiujitsu.api.domain.competition.repository.CompetitionSpecifications.*;

/**
 * 관리자 대회 관리 — 대회 탭에 나갈 대회(대회일 있는 대회)를 공개 여부와 상관없이 보고, 만들고 고치고, 확인·공개·비공개한다
 * 사용자 읽기 모델(CompetitionQueryService)은 공개 대회만, 자동완성 카탈로그·일괄 등록은 CompetitionService
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminCompetitionService {

    private static final Sort ORDER = Sort.by(Sort.Order.asc("openDate"), Sort.Order.asc("id"));

    private final CompetitionRepository competitionRepository;
    private final CompetitionInterestService competitionInterestService;
    private final CompetitionMapper competitionMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final CompetitionLinkRepository competitionLinkRepository;
    private final Clock clock;

    /**
     * 관리자 대회 목록 — 대회일 있는 대회만(자동완성용 카탈로그 엔트리 제외), 대회일·id 오름차순 고정 (클라이언트 sort 는 무시)
     * from 이 to 보다 뒤면 WRONG_PARAMETER
     */
    public Page<AdminCompetitionListItem> getList(AdminCompetitionListRequest request, Pageable pageable) {
        if (request.from() != null && request.to() != null && request.from().isAfter(request.to())) {
            throw new ErrorException(ErrorCode.WRONG_PARAMETER);
        }
        Specification<Competition> filter = hasOpenDate();
        if (request.published() != null) {
            filter = filter.and(publishedIs(request.published()));
        }
        String keyword = Competition.normalize(request.keyword());
        if (!keyword.isEmpty()) {
            filter = filter.and(searchNameLike("%" + CompetitionService.escapeLike(keyword) + "%"));
        }
        if (request.from() != null) {
            filter = filter.and(openDateFrom(request.from()));
        }
        if (request.to() != null) {
            filter = filter.and(openDateTo(request.to()));
        }
        Pageable sorted = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), ORDER);
        return competitionRepository.findAll(filter, sorted).map(competitionMapper::toAdminListItem);
    }

    /**
     * 관리자 대회 상세 — 공개 여부·대회일 유무와 상관없이. 없으면 COMPETITION_NOT_FOUND
     */
    public AdminCompetitionDetailResponse getDetail(Long competitionId) {
        Competition competition = competitionRepository.findById(competitionId)
                .orElseThrow(() -> new ErrorException(ErrorCode.COMPETITION_NOT_FOUND));
        return toDetail(competition);
    }

    /**
     * 대회 만들기 (처음엔 미공개). 이름(공백·대소문자 무시) + 연도가 같은 대회가
     * - 없으면 새로 만든다
     * - 자동완성용 대회(대회일 없음)면 새로 만들지 않고 그 행에 정보를 채운다 — ID 가 그대로라 프로필 대회 기록 연결이 유지된다
     * - 대회일 있는 대회면 COMPETITION_DUPLICATED
     */
    @Transactional
    public AdminCompetitionSaveResponse create(AdminCompetitionRequest request) {
        Competition.validateYear(request.competitionYear(), request.openDate());
        Optional<Competition> sameKey = findSameKey(request.name(), request.competitionYear());
        if (sameKey.isPresent() && !sameKey.get().isCatalogEntry()) {
            throw new ErrorException(ErrorCode.COMPETITION_DUPLICATED);
        }

        boolean catalogFilled = sameKey.isPresent();
        Competition competition = sameKey.orElseGet(() -> Competition.of(request.name(), request.competitionYear()));
        boolean renamed = catalogFilled && changeName(competition, request);
        competition.changeEventInfo(request.toEventInfo());
        competition.changeRegistrationSite(request.registrationSite(), false);   // 만들기 — 링크가 아직 없다 (자동완성용 행에도 링크를 둘 수 없음)
        saveCheckingDuplicate(competition);
        if (renamed) {
            publishRenamed(competition);
        }
        return new AdminCompetitionSaveResponse(catalogFilled, toDetail(competition));
    }

    /**
     * 대회 고치기 — 전체 교체 (안 보낸 칸·null 은 비운다). 공개 여부·마지막 확인일·조회수는 그대로
     * 대회일 있는 대회만. 자동완성용 대회는 COMPETITION_CATALOG_NOT_EDITABLE (대회 만들기가 그 행을 채운다)
     * 대회명·연도가 바뀌면 다른 대회와 겹치는지 보고(COMPETITION_DUPLICATED), 연결된 프로필 대회 기록도 같은 트랜잭션에서 맞춘다
     * (기록끼리 겹치는지는 보지 않는다 — 같은 프로필에 바뀐 이름·연도로 직접 입력한 기록이 있으면 그 회원의 다음 프로필 저장이 중복으로 막힌다.
     *  프로필 저장도 연결 기록을 대회 값으로 덮어써 결과는 같다)
     * 공개 중인 대회의 마감을 바꿔도 알림은 따로 처리하지 않는다 — 발송 기록이 마감 날짜 기준이라 바뀐 날짜로 다시 보낸다
     */
    @Transactional
    public AdminCompetitionDetailResponse update(Long competitionId, AdminCompetitionRequest request) {
        Competition competition = getEditable(competitionId);
        Competition.validateYear(request.competitionYear(), request.openDate());
        Optional<Competition> sameKey = findSameKey(request.name(), request.competitionYear());
        if (sameKey.isPresent() && !sameKey.get().getId().equals(competitionId)) {
            throw new ErrorException(ErrorCode.COMPETITION_DUPLICATED);
        }

        boolean renamed = changeName(competition, request);
        competition.changeEventInfo(request.toEventInfo());
        competition.changeRegistrationSite(request.registrationSite(),
                competitionLinkRepository.existsByCompetitionIdAndType(competitionId, CompetitionLinkType.REGISTRATION));
        saveCheckingDuplicate(competition);
        if (renamed) {
            publishRenamed(competition);
        }
        return toDetail(competition);
    }

    /**
     * 확인 완료 — 마지막 확인일을 오늘로 (공개·미공개 모두). 자동완성용 대회는 COMPETITION_CATALOG_NOT_EDITABLE
     */
    @Transactional
    public AdminCompetitionDetailResponse verify(Long competitionId) {
        Competition competition = getEditable(competitionId);
        competition.verify(LocalDate.now(clock));
        return toDetail(competition);
    }

    /**
     * 공개 — 필수 칸이 비면 COMPETITION_REQUIRED_FIELDS_MISSING, 공개하면 마지막 확인일도 오늘로. 이미 공개면 그대로
     */
    @Transactional
    public AdminCompetitionDetailResponse publish(Long competitionId) {
        Competition competition = getEditable(competitionId);
        competition.publish(LocalDate.now(clock));
        return toDetail(competition);
    }

    /**
     * 비공개 — 대회 탭·상세·관심 등록·마감 알림·게시글 대회 카드에서 숨겨진다. 관심·알림 설정·게시글 연결은 그대로라 다시 공개하면 돌아온다
     */
    @Transactional
    public AdminCompetitionDetailResponse unpublish(Long competitionId) {
        Competition competition = getEditable(competitionId);
        competition.unpublish();
        return toDetail(competition);
    }

    // 관리자가 고치는 대회 (대회일 있는 대회). 없으면 COMPETITION_NOT_FOUND, 자동완성용이면 COMPETITION_CATALOG_NOT_EDITABLE
    Competition getEditable(Long competitionId) {
        Competition competition = competitionRepository.findById(competitionId)
                .orElseThrow(() -> new ErrorException(ErrorCode.COMPETITION_NOT_FOUND));
        if (competition.isCatalogEntry()) {
            throw new ErrorException(ErrorCode.COMPETITION_CATALOG_NOT_EDITABLE);
        }
        return competition;
    }

    // 이름(정규화) + 연도가 같은 대회 (유니크라 많아야 하나)
    private Optional<Competition> findSameKey(String name, Integer competitionYear) {
        String searchName = Competition.normalize(Competition.cleanDisplayName(name));
        return competitionRepository.findAllBySearchNameIn(List.of(searchName)).stream()
                .filter(competition -> Objects.equals(competition.getCompetitionYear(), competitionYear))
                .findFirst();
    }

    // 표시 이름·연도가 바뀌면 바꾸고 true (공백만 다른 이름도 표시가 바뀌면 바꾼다)
    private static boolean changeName(Competition competition, AdminCompetitionRequest request) {
        boolean changed = !Competition.cleanDisplayName(request.name()).equals(competition.getName())
                || !Objects.equals(request.competitionYear(), competition.getCompetitionYear());
        if (changed) {
            competition.rename(request.name(), request.competitionYear());
        }
        return changed;
    }

    // 동시에 같은 이름·연도로 저장하면 유니크(uk_competition_search_name_year)에 걸린다 — 바로 반영해 중복 에러로 돌려준다
    // 새 대회는 save(IDENTITY 라 이때 INSERT), 이미 있는 대회는 변경 감지라 flush 만. 둘 다 이 try 안에서 터진다
    // 이 저장이 걸릴 수 있는 유니크는 그것뿐이다. 그 밖의 위반(CHECK·NOT NULL 등)은 원인이 드러나게 그대로 던진다
    private void saveCheckingDuplicate(Competition competition) {
        try {
            if (competition.getId() == null) {
                competitionRepository.save(competition);
            }
            competitionRepository.flush();
        } catch (DataIntegrityViolationException e) {
            if (!ConstraintViolationUtil.isUniqueViolation(e)) {
                throw e;
            }
            throw new ErrorException(ErrorCode.COMPETITION_DUPLICATED);
        }
    }

    private void publishRenamed(Competition competition) {
        eventPublisher.publishEvent(new CompetitionRenamedEvent(competition.getId(), competition.getName(), competition.getCompetitionYear()));
    }

    AdminCompetitionDetailResponse toDetail(Competition competition) {
        Long competitionId = competition.getId();
        long interestCount = competitionInterestService.getInterestCounts(List.of(competitionId)).getOrDefault(competitionId, 0L);
        LocalDateTime now = LocalDateTime.now(clock);
        List<AdminCompetitionLinkResponse> links = competitionLinkRepository.findAllByCompetitionId(competitionId).stream()
                .sorted(Comparator.comparing(CompetitionLink::getType))
                .map(link -> competitionMapper.toAdminLink(link, competition, now))
                .toList();
        return competitionMapper.toAdminDetail(competition, interestCount, links);
    }
}
