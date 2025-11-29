package com.know_who_how.main_server.auth.service;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.know_who_how.main_server.auth.dto.MypageSmsSendRequestDto;
import com.know_who_how.main_server.auth.dto.SmsCertificationConfirmDto;
import com.know_who_how.main_server.auth.dto.SmsCertificationRequestDto;
import com.know_who_how.main_server.auth.dto.TestSmsResponseDto;
import com.know_who_how.main_server.global.config.CoolSmsProperties;
import com.know_who_how.main_server.global.util.RedisUtil; // New import
import com.know_who_how.main_server.global.exception.CustomException;
import com.know_who_how.main_server.global.exception.ErrorCode;
import com.solapi.sdk.SolapiClient;
import com.solapi.sdk.message.exception.SolapiMessageNotReceivedException;
import com.solapi.sdk.message.model.Message;
import com.solapi.sdk.message.service.DefaultMessageService;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SmsCertificationService {

    private final CoolSmsProperties coolSmsProperties;
    private final RedisUtil redisUtil; // Replaced RedisTemplate with RedisUtil
    private final com.know_who_how.main_server.user.repository.UserRepository userRepository; // UserRepository 주입
    private DefaultMessageService messageService;

    private static final Duration EXPIRATION_TIME = Duration.ofMinutes(5); // 5 minutes expiration

    @PostConstruct
    private void init() {
        this.messageService = SolapiClient.INSTANCE.createInstance(
                coolSmsProperties.getApiKey(),
                coolSmsProperties.getApiSecret()
        );
    }

    /**
     * SMS 인증 번호를 전송하고, 인증 정보를 Redis에 저장합니다.
     *
     * @param requestDto SMS 인증 요청 DTO
     * @return verificationId
     */
    public String sendSmsCertification(SmsCertificationRequestDto requestDto) {
        // [추가] 전화번호 중복 확인
        if (userRepository.findByPhoneNum(requestDto.getPhoneNum()).isPresent()) {
            throw new CustomException(ErrorCode.PHONE_NUM_DUPLICATE);
        }

        String verificationId = UUID.randomUUID().toString();
        String certificationCode = createCertificationCode();
        Message message = new Message();
        message.setFrom(coolSmsProperties.getFromNumber());
        message.setTo(requestDto.getPhoneNum());
        message.setText("[KnowWhoHow] 본인확인 인증번호는 [" + certificationCode + "] 입니다.");

        try {
            this.messageService.send(message);
            SmsVerificationData data = new SmsVerificationData(certificationCode, requestDto, false);
            redisUtil.save(verificationId, data, EXPIRATION_TIME); // Use redisUtil.save
            return verificationId;
        } catch (SolapiMessageNotReceivedException e) {
            log.error("SMS 전송 실패, 상세 정보: {}", e.getFailedMessageList());
            log.error("에러 메시지: {}", e.getMessage());
            throw new CustomException(ErrorCode.SMS_SEND_FAILURE);
        } catch (Exception e) {
            log.error("SMS 인증 처리 중 알 수 없는 오류 발생", e);
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 테스트용 SMS 인증 번호를 발급하고, 인증 정보를 Redis에 저장합니다.
     * 실제 SMS는 발송하지 않습니다.
     *
     * @param requestDto SMS 인증 요청 DTO
     * @return TestSmsResponseDto containing "verificationId" and "authCode"
     */
    public TestSmsResponseDto sendTestSmsCertification(SmsCertificationRequestDto requestDto) {
        // [추가] 전화번호 중복 확인
        if (userRepository.findByPhoneNum(requestDto.getPhoneNum()).isPresent()) {
            throw new CustomException(ErrorCode.PHONE_NUM_DUPLICATE);
        }

        String verificationId = UUID.randomUUID().toString();
        String certificationCode = "123456"; // /test-sms에서 쓰이는 테스트 코드(고정)

        try {
            SmsVerificationData data = new SmsVerificationData(certificationCode, requestDto, false);
            redisUtil.save(verificationId, data, EXPIRATION_TIME); // Use redisUtil.save
            log.info("테스트용 SMS 인증 정보 생성 완료. verificationId: {}, authCode: {}", verificationId, certificationCode);
            return new TestSmsResponseDto(verificationId, certificationCode);
        } catch (Exception e) {
            log.error("테스트용 SMS 인증 정보 생성 중 오류 발생", e);
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * SMS 인증 번호를 확인합니다.
     *
     * @param confirmDto SMS 인증 확인 DTO
     * @return 인증 성공 메시지
     */
    public String confirmSmsCertification(SmsCertificationConfirmDto confirmDto) {
        String verificationId = confirmDto.getVerificationId();
        SmsVerificationData data = (SmsVerificationData) redisUtil.get(verificationId); // Use redisUtil.get

        if (data == null) {
            throw new CustomException(ErrorCode.CERTIFICATION_CODE_NOT_FOUND);
        }

        if (!data.getCertificationCode().equals(confirmDto.getAuthCode())) {
            throw new CustomException(ErrorCode.INVALID_CERTIFICATION_CODE);
        }

        // 인증 성공 시, Redis에 저장된 데이터의 isConfirmed 상태를 true로 업데이트
        SmsVerificationData updatedData = new SmsVerificationData(data.getCertificationCode(), data.getUserData(), true);
        redisUtil.save(verificationId, updatedData, EXPIRATION_TIME); // Use redisUtil.save

        return "인증번호가 일치합니다.";
    }

    public SmsCertificationRequestDto getUserVerificationData(String verificationId) {
        SmsVerificationData data = (SmsVerificationData) redisUtil.get(verificationId); // Use redisUtil.get
        if (data == null || !data.isConfirmed()) {
            throw new CustomException(ErrorCode.CERTIFICATION_CODE_NOT_FOUND); // 인증되지 않았거나 만료됨
        }
        return data.getUserData();
    }

    public void removeUserVerificationData(String verificationId) {
        redisUtil.delete(verificationId); // Use redisUtil.delete
    }

    /**
     * 마이페이지 프로필 수정 본인 확인을 위한 SMS 인증 번호를 전송하고, 인증 정보를 Redis에 저장합니다.
     * 전화번호 중복 확인 로직은 포함하지 않습니다.
     *
     * @param requestDto SMS 인증 요청 DTO (이름, 전화번호)
     * @return verificationId
     */
    public TestSmsResponseDto sendSmsCertificationForMypage(MypageSmsSendRequestDto requestDto) {
        String verificationId = UUID.randomUUID().toString();
        String certificationCode = createCertificationCode();
        Message message = new Message();
        message.setFrom(coolSmsProperties.getFromNumber());
        message.setTo(requestDto.getPhoneNum());
        message.setText("[KnowWhoHow] 본인확인 인증번호는 [" + certificationCode + "] 입니다.");

        // TODO: 실제 SMS 발송 로직('this.messageService.send(message)') 활성화 시, 아래 catch 블록에 SolapiMessageNotReceivedException 처리 로직 추가 필요
        try {
            // 실제 운영 환경에서는 SMS 발송 로직을 활성화해야 합니다.
            // this.messageService.send(message);

            // MypageSmsSendRequestDto를 SmsCertificationRequestDto로 변환 (birth, gender는 null)
            SmsCertificationRequestDto smsRequestForRedis = new SmsCertificationRequestDto(
                    requestDto.getName(),
                    null, // birth는 null
                    null, // gender는 null
                    requestDto.getPhoneNum()
            );
            SmsVerificationData data = new SmsVerificationData(certificationCode, smsRequestForRedis, false);
            redisUtil.save(verificationId, data, EXPIRATION_TIME);

            // verificationId와 authCode를 함께 반환
            log.info("마이페이지 인증용 SMS 정보 생성 완료. verificationId: {}, authCode: {}", verificationId, certificationCode);
            return new TestSmsResponseDto(verificationId, certificationCode);
        } catch (Exception e) {
            log.error("SMS 인증 처리 중 알 수 없는 오류 발생", e);
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 마이페이지 프로필 수정 SMS 인증 번호를 확인합니다.
     *
     * @param confirmDto SMS 인증 확인 DTO
     * @return 인증 성공 메시지
     */
    public String confirmSmsCertificationForMypage(SmsCertificationConfirmDto confirmDto) {
        String verificationId = confirmDto.getVerificationId();
        SmsVerificationData data = (SmsVerificationData) redisUtil.get(verificationId);

        if (data == null) {
            throw new CustomException(ErrorCode.CERTIFICATION_CODE_NOT_FOUND);
        }

        if (!data.getCertificationCode().equals(confirmDto.getAuthCode())) {
            throw new CustomException(ErrorCode.INVALID_CERTIFICATION_CODE);
        }

        // 인증 성공 시, Redis에 저장된 데이터의 isConfirmed 상태를 true로 업데이트
        SmsVerificationData updatedData = new SmsVerificationData(data.getCertificationCode(), data.getUserData(), true);
        redisUtil.save(verificationId, updatedData, EXPIRATION_TIME);

        return "인증번호가 일치합니다.";
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

    @Getter
    private static class SmsVerificationData implements Serializable {
        private final String certificationCode;
        private final SmsCertificationRequestDto userData;
        @JsonProperty("confirmed")
        private final boolean isConfirmed;

        @JsonCreator
        public SmsVerificationData(
                @JsonProperty("certificationCode") String certificationCode,
                @JsonProperty("userData") SmsCertificationRequestDto userData,
                @JsonProperty("confirmed") boolean isConfirmed) {
            this.certificationCode = certificationCode;
            this.userData = userData;
            this.isConfirmed = isConfirmed;
        }
    }
}
