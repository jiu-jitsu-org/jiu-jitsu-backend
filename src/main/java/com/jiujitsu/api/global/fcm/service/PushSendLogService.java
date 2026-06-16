package com.jiujitsu.api.global.fcm.service;

import com.jiujitsu.api.global.fcm.entity.PushSendLog;
import com.jiujitsu.api.global.fcm.repository.PushSendLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PushSendLogService {
    private final PushSendLogRepository pushSendLogRepository;

    public void insertPushSendLog(PushSendLog pushSendLog) {
        pushSendLogRepository.save(pushSendLog);
    }
}
