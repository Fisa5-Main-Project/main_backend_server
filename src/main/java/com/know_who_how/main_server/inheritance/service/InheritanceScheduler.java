package com.know_who_how.main_server.inheritance.service;

import com.know_who_how.main_server.global.entity.Inheritance.InheritanceRecipient;
import com.know_who_how.main_server.inheritance.repository.InheritanceRecipientRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class InheritanceScheduler {

    private final InheritanceRecipientRepository recipientRepository;
    private final EmailService emailService;

    @Value("${app.service-base-url}")
    private String serviceBaseUrl;

    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void processScheduledVideoEmails(){
        LocalDateTime now = LocalDateTime.now();

        // 발송 대상 조회
        List<InheritanceRecipient> targets = recipientRepository.findRecipientsToSend(now);

        if(targets.isEmpty()){
            log.debug("예약 발송 대상 없음.");
            return;
        }

        log.info("예약된 영상 편지 {}건 발송 시작.", targets.size());

        // 이메일 발송 시도
        for(InheritanceRecipient recipient : targets){
            String token = recipient.getAccessLink();

            // 최종 URL 생성
            String videoLink = String.format("%s/api/v1/inheritance/video-letter?token=%s", serviceBaseUrl, token);

            try{
                // 이메일 발송 서비스 호출
                emailService.sendVideoLetterLink(recipient.getEmail(),  videoLink);

                // 발송 성공 시 상태 업데이트
                recipient.markSent(LocalDateTime.now());
                log.info("-> 발송 성공 및 DB 상태 업데이트: Recipient ID {} ({})", recipient.getRecipientId(), recipient.getEmail());
            } catch (Exception e){
                log.error("-> 발송 실패(다음 스케줄에 재시도 예정): Recipient ID{} ({})",  recipient.getRecipientId(), recipient.getEmail());
            }
        }
    }


}
