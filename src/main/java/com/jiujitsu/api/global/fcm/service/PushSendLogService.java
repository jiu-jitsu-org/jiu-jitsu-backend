package com.jiujitsu.api.global.fcm.service;

import com.jiujitsu.api.global.fcm.entity.PushSendLog;
import com.jiujitsu.api.global.fcm.repository.PushSendLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PushSendLogService {
    private final PushSendLogRepository pushSendLogRepository;

    // todo: 실패/성공 둘다 적용해서 테스트 필요
    public void insertPushSendLog(PushSendLog pushSendLog) {
        pushSendLogRepository.save(pushSendLog);
    }
}
