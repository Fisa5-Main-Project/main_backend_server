package com.know_who_how.main_server.auth.service;

import com.know_who_how.main_server.auth.dto.SmsCertificationConfirmDto;
import com.know_who_how.main_server.auth.dto.SmsCertificationRequestDto;
import com.know_who_how.main_server.global.config.CoolSmsProperties;
import com.know_who_how.main_server.global.exception.CustomException;
import com.know_who_how.main_server.global.exception.ErrorCode;
import com.solapi.sdk.SolapiClient;
import com.solapi.sdk.message.model.Message;
import com.solapi.sdk.message.service.DefaultMessageService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class SmsCertificationService {

    private final CoolSmsProperties coolSmsProperties;
    private DefaultMessageService messageService;

    // In-memory storage for certification codes (for demonstration purposes)
    // In a real application, consider using Redis for persistence and scalability
    private final Map<String, String> certificationCodes = new ConcurrentHashMap<>();
    private final Map<String, LocalDateTime> certificationCodeTimestamps = new ConcurrentHashMap<>();
    private static final Duration EXPIRATION_TIME = Duration.ofMinutes(5); // 5 minutes expiration

    @PostConstruct
    private void init() {
        this.messageService = SolapiClient.INSTANCE.createInstance(
                coolSmsProperties.getApiKey(),
                coolSmsProperties.getApiSecret()
        );
    }

    /**
     * SMS 인증 번호를 전송합니다.
     *
     * @param requestDto SMS 인증 요청 DTO
     * @return 전송 성공 여부
     */
    public boolean sendSmsCertification(SmsCertificationRequestDto requestDto) {
        // 전화번호 유효성 검사는 DTO @Pattern으로 처리됨

        String certificationCode = createCertificationCode();
        Message message = new Message();
        message.setFrom(coolSmsProperties.getFromNumber());
        message.setTo(requestDto.getPhoneNum());
        message.setText("[KnowWhoHow] 본인확인 인증번호는 [" + certificationCode + "] 입니다.");

        try {
            this.messageService.send(message); // Assume void return or success by no exception
            certificationCodes.put(requestDto.getPhoneNum(), certificationCode);
            certificationCodeTimestamps.put(requestDto.getPhoneNum(), LocalDateTime.now());
            return true;
        } catch (Exception e) {
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR); // SMS 전송 중 오류 발생
        }
    }

    /**
     * SMS 인증 번호를 확인합니다.
     *
     * @param confirmDto SMS 인증 확인 DTO
     * @return 인증 성공 여부
     */
    public boolean confirmSmsCertification(SmsCertificationConfirmDto confirmDto) {
        String storedCode = certificationCodes.get(confirmDto.getPhoneNum());
        LocalDateTime storedTimestamp = certificationCodeTimestamps.get(confirmDto.getPhoneNum());

        if (storedCode == null || storedTimestamp == null) {
            throw new CustomException(ErrorCode.CERTIFICATION_CODE_NOT_FOUND);
        }

        if (LocalDateTime.now().isAfter(storedTimestamp.plus(EXPIRATION_TIME))) {
            certificationCodes.remove(confirmDto.getPhoneNum());
            certificationCodeTimestamps.remove(confirmDto.getPhoneNum());
            throw new CustomException(ErrorCode.CERTIFICATION_CODE_EXPIRED);
        }

        if (!storedCode.equals(confirmDto.getCertificationCode())) {
            throw new CustomException(ErrorCode.INVALID_CERTIFICATION_CODE);
        }

        // 인증 성공 시, 코드 삭제 (재사용 방지)
        certificationCodes.remove(confirmDto.getPhoneNum());
        certificationCodeTimestamps.remove(confirmDto.getPhoneNum());
        return true;
    }

    /**
     * 6자리 랜덤 인증 코드를 생성합니다.
     *
     * @return 생성된 인증 코드
     */
    private String createCertificationCode() {
        SecureRandom random = new SecureRandom();
        int code = 100000 + random.nextInt(900000); // 100000 ~ 999999
        return String.valueOf(code);
    }
}
