package com.jiujitsu.api.domain.competition.service;

import com.jiujitsu.api.domain.competition.dto.CompetitionDetailResponse;
import com.jiujitsu.api.domain.competition.dto.CompetitionListItem;
import com.jiujitsu.api.domain.competition.dto.CompetitionListRequest;
import com.jiujitsu.api.domain.competition.dto.CompetitionListResponse;
import com.jiujitsu.api.domain.competition.dto.CompetitionPickerResponse;
import com.jiujitsu.api.domain.competition.entity.Competition;
import com.jiujitsu.api.domain.competition.entity.CompetitionInterest;
import com.jiujitsu.api.domain.competition.mapper.CompetitionMapper;
import com.jiujitsu.api.domain.competition.repository.CompetitionRepository;
import com.jiujitsu.api.domain.user.entity.User;
import com.jiujitsu.api.domain.user.service.AuthenticationFacade;
import com.jiujitsu.api.global.exception.ErrorCode;
import com.jiujitsu.api.global.exception.ErrorException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.jiujitsu.api.domain.competition.repository.CompetitionSpecifications.*;

/**
 * 대회 탭 읽기 모델 (목록, 상세, 글쓰기 대회 고르기, 게시글 대회 카드). 대회 정보에 관심 수·내 관심 여부를 붙여 응답 하나로 화면을 채운다
 * 카탈로그·자동완성(CompetitionService), 관심 쓰기(CompetitionInterestService)와 분리
 * 커뮤니티(게시글)는 getPublished·getCards 로만 대회를 본다 — 대회 도메인은 커뮤니티를 모른다
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CompetitionQueryService {

    private static final Sort ASCENDING = Sort.by(Sort.Order.asc("openDate"), Sort.Order.asc("id"));
    private static final Sort DESCENDING = Sort.by(Sort.Order.desc("openDate"), Sort.Order.desc("id"));
    private static final int PICKER_LIMIT = 20;

    private final CompetitionRepository competitionRepository;
    private final CompetitionInterestService competitionInterestService;
    private final AuthenticationFacade authenticationFacade;
    private final CompetitionMapper competitionMapper;
    private final Clock clock;

    /**
     * 대회 목록 — 기준일 + 양방향 키셋 커서. 정렬 (openDate, id) 오름차순
     * 첫 요청은 기준일(기본 오늘) 이후, 이후 요청은 응답의 nextCursor(뒤)·previousCursor(앞, 과거)
     * offset 페이지를 쓰지 않는 이유: 기준이 오늘이라 날짜가 바뀌거나 대회가 추가되면 페이지가 밀리고, 기준일 앞쪽을 표현할 수 없다
     */
    public CompetitionListResponse getList(CompetitionListRequest request) {
        // 빈 cursor(`cursor=`)는 없는 것으로 본다 — 빈 date 가 null 로 바인딩되는 것과 맞춘다
        String cursor = request.cursor() == null || request.cursor().isBlank() ? null : request.cursor();
        if (request.date() != null && cursor != null) {
            throw new ErrorException(ErrorCode.WRONG_PARAMETER);
        }
        LocalDate today = LocalDate.now(clock);
        Long userId = authenticationFacade.getCurrentUserOptional().map(User::getId).orElse(null);
        if (request.interestedOnly() && userId == null) {
            throw new ErrorException(ErrorCode.LOGIN_NOT_ACCESS);
        }

        CompetitionCursor position = cursor != null
                ? CompetitionCursor.decode(cursor)
                : CompetitionCursor.startOf(request.date() != null ? request.date() : today);
        Specification<Competition> filter = filter(request, today, userId);
        int size = request.sizeOrDefault();

        // 한 개 더 가져와 같은 방향에 더 있는지 본다. 앞쪽은 내림차순으로 가져와 뒤집는다 (응답은 항상 오름차순)
        List<Competition> fetched = competitionRepository.findBy(filter.and(boundary(position)),
                q -> q.sortBy(position.isBefore() ? DESCENDING : ASCENDING).limit(size + 1).all());
        boolean hasMore = fetched.size() > size;
        List<Competition> page = new ArrayList<>(fetched.subList(0, Math.min(size, fetched.size())));
        if (position.isBefore()) {
            Collections.reverse(page);
        }

        String previousCursor;
        String nextCursor;
        if (position.isBefore()) {
            previousCursor = hasMore ? beforeOf(page.get(0)).encode() : null;
            nextCursor = oppositeCursor(filter, page.isEmpty() ? position.opposite() : afterOf(page.get(page.size() - 1)));
        } else {
            nextCursor = hasMore ? afterOf(page.get(page.size() - 1)).encode() : null;
            previousCursor = oppositeCursor(filter, page.isEmpty() ? position.opposite() : beforeOf(page.get(0)));
        }

        return new CompetitionListResponse(today, toItems(page, today, userId), previousCursor, nextCursor);
    }

    /**
     * 대회 상세 (비로그인 가능). 조회수는 올리지 않는다 — 공유 미리보기·웹 서버 렌더링도 이 조회를 불러서,
     * 여기서 올리면 사람이 보지 않아도 숫자가 오른다. 조회수는 CompetitionViewService(POST)
     * 없거나 미공개면 COMPETITION_NOT_FOUND
     */
    public CompetitionDetailResponse getDetail(Long competitionId) {
        Competition competition = getPublished(competitionId);
        LocalDate today = LocalDate.now(clock);
        Long userId = authenticationFacade.getCurrentUserOptional().map(User::getId).orElse(null);

        List<Long> ids = List.of(competitionId);
        long interestCount = competitionInterestService.getInterestCounts(ids).getOrDefault(competitionId, 0L);
        Optional<CompetitionInterest> mine = userId == null ? Optional.empty() : competitionInterestService.findInterest(userId, competitionId);
        return competitionMapper.toDetail(competition, today, interestCount,
                mine.isPresent(), mine.map(CompetitionInterest::isNoticeEnabled).orElse(false));
    }

    /**
     * 글쓰기 대회 고르기 (비로그인 가능). 공개 대회를 이름으로 찾아 다가오는 대회(오늘 포함, 가까운 순) → 끝난 대회(최근 순)로 최대 20개
     * 대회 전 질문과 대회 뒤 후기를 모두 고를 수 있어야 해서, 기준일 앞뒤로 나뉘는 목록(getList) 대신 오늘에 가까운 대회부터 준다
     * 검색어가 비면 이름 조건 없이 같은 순서
     */
    public CompetitionPickerResponse getPicker(String keyword) {
        LocalDate today = LocalDate.now(clock);
        Long userId = authenticationFacade.getCurrentUserOptional().map(User::getId).orElse(null);

        Specification<Competition> filter = published();
        String normalized = Competition.normalize(keyword);
        if (!normalized.isEmpty()) {
            filter = filter.and(searchNameLike("%" + CompetitionService.escapeLike(normalized) + "%"));
        }

        // 기준 (today, 0) 뒤 = 오늘 포함 이후, 앞 = 어제까지 (id 는 1부터)
        CompetitionCursor todayStart = CompetitionCursor.startOf(today);
        List<Competition> picked = new ArrayList<>(competitionRepository.findBy(filter.and(boundary(todayStart)),
                q -> q.sortBy(ASCENDING).limit(PICKER_LIMIT).all()));
        if (picked.size() < PICKER_LIMIT) {
            picked.addAll(competitionRepository.findBy(filter.and(boundary(todayStart.opposite())),
                    q -> q.sortBy(DESCENDING).limit(PICKER_LIMIT - picked.size()).all()));
        }
        return new CompetitionPickerResponse(today, toItems(picked, today, userId));
    }

    /**
     * 공개 대회 (게시글에 대회를 연결할 때). 없거나 미공개면 COMPETITION_NOT_FOUND
     */
    public Competition getPublished(Long competitionId) {
        return competitionRepository.findByIdAndPublishedTrue(competitionId)
                .orElseThrow(() -> new ErrorException(ErrorCode.COMPETITION_NOT_FOUND));
    }

    /**
     * 게시글 대회 카드 일괄 조회 (대회 탭 목록 항목과 같음). 미공개·없는 대회는 결과에 없다 (= 카드 없음)
     */
    public Map<Long, CompetitionListItem> getCards(Collection<Long> competitionIds) {
        if (competitionIds.isEmpty()) {
            return Map.of();
        }
        LocalDate today = LocalDate.now(clock);
        Long userId = authenticationFacade.getCurrentUserOptional().map(User::getId).orElse(null);
        return toItems(competitionRepository.findAllByIdInAndPublishedTrue(competitionIds), today, userId).stream()
                .collect(Collectors.toMap(CompetitionListItem::competitionId, Function.identity()));
    }

    private Specification<Competition> filter(CompetitionListRequest request, LocalDate today, Long userId) {
        Specification<Competition> filter = published();
        if (request.region() != null && !request.region().isEmpty()) {
            filter = filter.and(regionIn(request.region()));
        }
        if (request.status() != null && !request.status().isEmpty()) {
            filter = filter.and(statusIn(request.status(), today));
        }
        if (request.interestedOnly()) {
            filter = filter.and(interestedBy(userId));
        }
        String keyword = Competition.normalize(request.keyword());
        if (!keyword.isEmpty()) {
            filter = filter.and(searchNameLike("%" + CompetitionService.escapeLike(keyword) + "%"));
        }
        return filter;
    }

    // 반대 방향에 대회가 있을 때만 커서를 준다 (없는데 커서를 주면 클라가 빈 페이지를 한 번 더 부른다)
    private String oppositeCursor(Specification<Competition> filter, CompetitionCursor cursor) {
        return competitionRepository.exists(filter.and(boundary(cursor))) ? cursor.encode() : null;
    }

    private static Specification<Competition> boundary(CompetitionCursor cursor) {
        return cursor.isBefore()
                ? before(cursor.openDate(), cursor.id())
                : after(cursor.openDate(), cursor.id());
    }

    private static CompetitionCursor afterOf(Competition competition) {
        return CompetitionCursor.after(competition.getOpenDate(), competition.getId());
    }

    private static CompetitionCursor beforeOf(Competition competition) {
        return CompetitionCursor.before(competition.getOpenDate(), competition.getId());
    }

    private List<CompetitionListItem> toItems(List<Competition> page, LocalDate today, Long userId) {
        List<Long> ids = page.stream().map(Competition::getId).toList();
        Map<Long, Long> interestCounts = competitionInterestService.getInterestCounts(ids);
        Set<Long> interestedIds = userId == null ? Set.of() : competitionInterestService.getInterestedIds(userId, ids);
        return page.stream()
                .map(competition -> competitionMapper.toListItem(competition, today,
                        interestCounts.getOrDefault(competition.getId(), 0L),
                        interestedIds.contains(competition.getId())))
                .toList();
    }
}
