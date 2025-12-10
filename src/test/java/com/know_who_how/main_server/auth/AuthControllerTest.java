package com.know_who_how.main_server.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.know_who_how.main_server.auth.controller.AuthController;
import com.know_who_how.main_server.auth.dto.*;
import com.know_who_how.main_server.auth.service.AuthService;
import com.know_who_how.main_server.auth.service.SmsCertificationService;
import com.know_who_how.main_server.global.exception.CustomException;
import com.know_who_how.main_server.global.exception.ErrorCode;
import com.know_who_how.main_server.global.jwt.JwtAuthFilter;
import com.know_who_how.main_server.global.jwt.JwtProperties;
import com.know_who_how.main_server.global.util.CookieUtil;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.filter.CharacterEncodingFilter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @MockBean
        private AuthService authService;

        @MockBean
        private SmsCertificationService smsCertificationService;

        @SpyBean
        private CookieUtil cookieUtil;

        /**
         * 기능 ID: AUTH-01
         * 테스트 시나리오: 사용 가능한 아이디로 중복 확인 요청
         * 테스트 조건: DB에 존재하지 않는 아이디("newuser")로 GET /api/v1/auth/signup/check-login-id 요청
         * 예상 결과:
         * - 200 OK 상태 코드 반환
         * - 응답 본문에 "사용 가능한 아이디입니다." 메시지 포함
         */
        @Test
        @DisplayName("아이디 중복 확인 - 사용 가능")
        void checkLoginIdDuplicate_should_ReturnSuccess_when_IdIsAvailable() throws Exception {
                // given
                String loginId = "newuser";
                String successMessage = "사용 가능한 아이디입니다.";
                given(authService.checkLoginIdDuplicate(loginId)).willReturn(successMessage);

                // when
                ResultActions resultActions = mockMvc.perform(get("/api/v1/auth/signup/check-login-id")
                                .param("loginId", loginId));

                // then
                resultActions.andExpect(status().isOk())
                                .andExpect(jsonPath("$.isSuccess").value(true))
                                .andExpect(jsonPath("$.data").value(successMessage));
        }

        /**
         * 기능 ID: AUTH-01
         * 테스트 시나리오: 이미 사용 중인 아이디로 중복 확인 요청
         * 테스트 조건: DB에 존재하는 아이디("existinguser")로 GET /api/v1/auth/signup/check-login-id
         * 요청
         * 예상 결과:
         * - 409 Conflict 상태 코드 반환
         * - 응답 본문에 에러 코드 "AUTH_004" 포함
         */
        @Test
        @DisplayName("아이디 중복 확인 - 이미 사용 중")
        void checkLoginIdDuplicate_should_ThrowException_when_IdIsTaken() throws Exception {
                // given
                String loginId = "existinguser";
                given(authService.checkLoginIdDuplicate(loginId))
                                .willThrow(new CustomException(ErrorCode.LOGIN_ID_DUPLICATE));

                // when
                ResultActions resultActions = mockMvc.perform(get("/api/v1/auth/signup/check-login-id")
                                .param("loginId", loginId));

                // then
                resultActions.andExpect(status().isConflict()) // 409 Conflict
                                .andExpect(jsonPath("$.isSuccess").value(false))
                                .andExpect(jsonPath("$.error.code").value(ErrorCode.LOGIN_ID_DUPLICATE.getCode()));
        }

        /**
         * 기능 ID: AUTH-02
         * 테스트 시나리오: 사용 가능한 전화번호로 중복 확인 요청
         * 테스트 조건: DB에 존재하지 않는 전화번호("01012345678")로 GET
         * /api/v1/auth/signup/check-phone-num 요청
         * 예상 결과:
         * - 200 OK 상태 코드 반환
         * - 응답 본문에 "사용 가능한 전화번호입니다." 메시지 포함
         */
        @Test
        @DisplayName("전화번호 중복 확인 - 사용 가능")
        void checkPhoneNumDuplicate_should_ReturnSuccess_when_PhoneNumIsAvailable() throws Exception {
                // given
                String phoneNum = "01012345678";
                String successMessage = "사용 가능한 전화번호입니다.";
                given(authService.checkPhoneNumDuplicate(phoneNum)).willReturn(successMessage);

                // when
                ResultActions resultActions = mockMvc.perform(get("/api/v1/auth/signup/check-phone-num")
                                .param("phoneNum", phoneNum));

                // then
                resultActions.andExpect(status().isOk())
                                .andExpect(jsonPath("$.isSuccess").value(true))
                                .andExpect(jsonPath("$.data").value(successMessage));
        }

        /**
         * 기능 ID: AUTH-02
         * 테스트 시나리오: 이미 사용 중인 전화번호로 중복 확인 요청
         * 테스트 조건: DB에 존재하는 전화번호("01087654321")로 GET /api/v1/auth/signup/check-phone-num
         * 요청
         * 예상 결과:
         * - 409 Conflict 상태 코드 반환
         * - 응답 본문에 에러 코드 "AUTH_005" 포함
         */
        @Test
        @DisplayName("전화번호 중복 확인 - 이미 사용 중")
        void checkPhoneNumDuplicate_should_ThrowException_when_PhoneNumIsTaken() throws Exception {
                // given
                String phoneNum = "01087654321";
                given(authService.checkPhoneNumDuplicate(phoneNum))
                                .willThrow(new CustomException(ErrorCode.PHONE_NUM_DUPLICATE));

                // when
                ResultActions resultActions = mockMvc.perform(get("/api/v1/auth/signup/check-phone-num")
                                .param("phoneNum", phoneNum));

                // then
                resultActions.andExpect(status().isConflict()) // 409 Conflict
                                .andExpect(jsonPath("$.isSuccess").value(false))
                                .andExpect(jsonPath("$.error.code").value(ErrorCode.PHONE_NUM_DUPLICATE.getCode()));
        }

        /**
         * 기능 ID: AUTH-03
         * 테스트 시나리오: 본인인증 SMS 문자 발송 요청 성공
         * 테스트 조건: 유효한 사용자 정보(이름, 생년월일, 성별, 전화번호)로 POST /api/v1/auth/signup/send-sms 요청
         * 예상 결과:
         * - 200 OK 상태 코드 반환
         * - 응답 본문에 생성된 verificationId 포함
         */
        @Test
        @DisplayName("본인인증 SMS 발송 - 성공")
        void sendSms_should_ReturnVerificationId_when_RequestIsValid() throws Exception {
                // given
                SmsCertificationRequestDto requestDto = new SmsCertificationRequestDto("홍길동", "1990-01-01", "MALE",
                                "01011112222");
                String verificationId = "some-verification-id";
                String requestJson = objectMapper.writeValueAsString(requestDto);

                given(smsCertificationService.sendSmsCertification(any(SmsCertificationRequestDto.class)))
                                .willReturn(verificationId);

                // when
                ResultActions resultActions = mockMvc.perform(post("/api/v1/auth/signup/send-sms")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestJson));

                // then
                resultActions.andExpect(status().isOk())
                                .andExpect(jsonPath("$.isSuccess").value(true))
                                .andExpect(jsonPath("$.data.verificationId").value(verificationId));
        }

        /**
         * 기능 ID: AUTH-03
         * 테스트 시나리오: 이미 가입된 전화번호로 본인인증 SMS 문자 발송 요청
         * 테스트 조건: 중복된 전화번호로 POST /api/v1/auth/signup/send-sms 요청
         * 예상 결과:
         * - 409 Conflict 상태 코드 반환
         * - 응답 본문에 에러 코드 "AUTH_005" 포함
         */
        @Test
        @DisplayName("본인인증 SMS 발송 - 전화번호 중복")
        void sendSms_should_ThrowException_when_PhoneNumIsDuplicate() throws Exception {
                // given
                SmsCertificationRequestDto requestDto = new SmsCertificationRequestDto("홍길동", "1990-01-01", "MALE",
                                "01011112222");
                String requestJson = objectMapper.writeValueAsString(requestDto);

                given(smsCertificationService.sendSmsCertification(any(SmsCertificationRequestDto.class)))
                                .willThrow(new CustomException(ErrorCode.PHONE_NUM_DUPLICATE));

                // when
                ResultActions resultActions = mockMvc.perform(post("/api/v1/auth/signup/send-sms")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestJson));

                // then
                resultActions.andExpect(status().isConflict())
                                .andExpect(jsonPath("$.isSuccess").value(false))
                                .andExpect(jsonPath("$.error.code").value(ErrorCode.PHONE_NUM_DUPLICATE.getCode()));
        }

        /**
         * 기능 ID: AUTH-03
         * 테스트 시나리오: SMS 서버 문제로 발송 실패
         * 테스트 조건: SMS 서비스 내부 오류 발생 상황을 가정
         * 예상 결과:
         * - 500 Internal Server Error 상태 코드 반환
         * - 응답 본문에 에러 코드 "AUTH_012" 포함
         */
        @Test
        @DisplayName("본인인증 SMS 발송 - SMS 전송 실패")
        void sendSms_should_ThrowException_when_SmsFailsToSend() throws Exception {
                // given
                SmsCertificationRequestDto requestDto = new SmsCertificationRequestDto("홍길동", "1990-01-01", "MALE",
                                "01011112222");
                String requestJson = objectMapper.writeValueAsString(requestDto);

                // AUTH_012는 SMS_SEND_FAILURE에 해당합니다.
                given(smsCertificationService.sendSmsCertification(any(SmsCertificationRequestDto.class)))
                                .willThrow(new CustomException(ErrorCode.SMS_SEND_FAILURE));

                // when
                ResultActions resultActions = mockMvc.perform(post("/api/v1/auth/signup/send-sms")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestJson));

                // then
                resultActions.andExpect(status().isInternalServerError())
                                .andExpect(jsonPath("$.isSuccess").value(false))
                                .andExpect(jsonPath("$.error.code").value(ErrorCode.SMS_SEND_FAILURE.getCode()));
        }

        /**
         * 기능 ID: AUTH-04
         * 테스트 시나리오: 올바른 인증번호로 인증 확인 요청
         * 테스트 조건: 유효한 verificationId와 authCode로 POST /api/v1/auth/signup/check-code 요청
         * 예상 결과:
         * - 200 OK 상태 코드 반환
         * - 응답 본문에 "인증이 완료되었습니다." 메시지 포함
         */
        @Test
        @DisplayName("SMS 인증번호 확인 - 성공")
        void confirmSms_should_ReturnSuccess_when_CodeIsValid() throws Exception {
                // given
                SmsCertificationConfirmDto confirmDto = new SmsCertificationConfirmDto("some-verification-id",
                                "123456");
                String successMessage = "인증이 완료되었습니다.";
                String requestJson = objectMapper.writeValueAsString(confirmDto);

                given(smsCertificationService.confirmSmsCertification(any(SmsCertificationConfirmDto.class)))
                                .willReturn(successMessage);

                // when
                ResultActions resultActions = mockMvc.perform(post("/api/v1/auth/signup/check-code")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestJson));

                // then
                resultActions.andExpect(status().isOk())
                                .andExpect(jsonPath("$.isSuccess").value(true))
                                .andExpect(jsonPath("$.data").value(successMessage));
        }

        /**
         * 기능 ID: AUTH-04
         * 테스트 시나리오: 잘못된 인증번호로 인증 확인 요청
         * 테스트 조건: 유효하지 않은 authCode로 POST /api/v1/auth/signup/check-code 요청
         * 예상 결과:
         * - 400 Bad Request 상태 코드 반환
         * - 응답 본문에 에러 코드 "AUTH_007" 포함
         */
        @Test
        @DisplayName("SMS 인증번호 확인 - 잘못된 인증번호")
        void confirmSms_should_ThrowException_when_CodeIsInvalid() throws Exception {
                // given
                SmsCertificationConfirmDto confirmDto = new SmsCertificationConfirmDto("some-verification-id",
                                "wrong-code");
                String requestJson = objectMapper.writeValueAsString(confirmDto);

                given(smsCertificationService.confirmSmsCertification(any(SmsCertificationConfirmDto.class)))
                                .willThrow(new CustomException(ErrorCode.INVALID_CERTIFICATION_CODE));

                // when
                ResultActions resultActions = mockMvc.perform(post("/api/v1/auth/signup/check-code")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestJson));

                // then
                resultActions.andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.isSuccess").value(false))
                                .andExpect(jsonPath("$.error.code")
                                                .value(ErrorCode.INVALID_CERTIFICATION_CODE.getCode()));
        }

        /**
         * 기능 ID: AUTH-04
         * 테스트 시나리오: 만료된 인증번호로 인증 확인 요청
         * 테스트 조건: 만료된 인증번호 상황을 가정한 후 POST /api/v1/auth/signup/check-code 요청
         * 예상 결과:
         * - 400 Bad Request 상태 코드 반환
         * - 응답 본문에 에러 코드 "AUTH_008" 포함
         */
        @Test
        @DisplayName("SMS 인증번호 확인 - 만료된 인증번호")
        void confirmSms_should_ThrowException_when_CodeIsExpired() throws Exception {
                // given
                SmsCertificationConfirmDto confirmDto = new SmsCertificationConfirmDto("some-verification-id",
                                "123456");
                String requestJson = objectMapper.writeValueAsString(confirmDto);

                given(smsCertificationService.confirmSmsCertification(any(SmsCertificationConfirmDto.class)))
                                .willThrow(new CustomException(ErrorCode.CERTIFICATION_CODE_EXPIRED));

                // when
                ResultActions resultActions = mockMvc.perform(post("/api/v1/auth/signup/check-code")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestJson));

                // then
                resultActions.andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.isSuccess").value(false))
                                .andExpect(jsonPath("$.error.code")
                                                .value(ErrorCode.CERTIFICATION_CODE_EXPIRED.getCode()));
        }

        /**
         * 기능 ID: AUTH-05
         * 테스트 시나리오: 유효한 정보로 최종 회원가입 요청
         * 테스트 조건: 모든 필수 정보(인증 ID, 약관 동의, 아이디, 비밀번호 등)를 포함하여 POST
         * /api/v1/auth/signup/submit 요청
         * 예상 결과:
         * - 200 OK 상태 코드 반환
         * - 응답 본문에 "회원가입이 완료되었습니다." 메시지 포함
         */
        @Test
        @DisplayName("최종 회원가입 - 성공")
        void signup_should_ReturnSuccess_when_ValidRequest() throws Exception {
                // given
                String requestJson = """
                                {
                                  "verificationId": "c78a8a1d-2c4f-4f3a-b5f7-5a9a8e3a4b2f",
                                  "termAgreements": [
                                    { "termId": 1, "isAgreed": true },
                                    { "termId": 2, "isAgreed": true }
                                  ],
                                  "loginId": "newuser123",
                                  "password": "Password123!",
                                  "financialPropensity": "STABLE",
                                  "keywordIds": [1, 2, 3]
                                }
                                """;

                willDoNothing().given(authService).signup(any(UserSignupRequestDto.class));

                // when
                ResultActions resultActions = mockMvc.perform(post("/api/v1/auth/signup/submit")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestJson));

                // then
                resultActions.andExpect(status().isOk())
                                .andExpect(jsonPath("$.isSuccess").value(true))
                                .andExpect(jsonPath("$.data").value("회원가입이 완료되었습니다."));
        }

        /**
         * 기능 ID: AUTH-05
         * 테스트 시나리오: 중복된 아이디로 최종 회원가입 요청
         * 테스트 조건: 이미 존재하는 아이디로 POST /api/v1/auth/signup/submit 요청
         * 예상 결과:
         * - 409 Conflict 상태 코드 반환
         * - 응답 본문에 에러 코드 "AUTH_004" 포함
         */
        @Test
        @DisplayName("최종 회원가입 - 아이디 중복")
        void signup_should_ThrowException_when_LoginIdIsDuplicate() throws Exception {
                // given
                String requestJson = """
                                {
                                  "verificationId": "c78a8a1d-2c4f-4f3a-b5f7-5a9a8e3a4b2f",
                                  "termAgreements": [ { "termId": 1, "isAgreed": true } ],
                                  "loginId": "existinguser",
                                  "password": "Password123!",
                                  "financialPropensity": "STABLE"
                                }
                                """;

                willThrow(new CustomException(ErrorCode.LOGIN_ID_DUPLICATE)).given(authService)
                                .signup(any(UserSignupRequestDto.class));

                // when
                ResultActions resultActions = mockMvc.perform(post("/api/v1/auth/signup/submit")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestJson));

                // then
                resultActions.andExpect(status().isConflict())
                                .andExpect(jsonPath("$.isSuccess").value(false))
                                .andExpect(jsonPath("$.error.code").value(ErrorCode.LOGIN_ID_DUPLICATE.getCode()));
        }

        /**
         * 기능 ID: AUTH-05
         * 테스트 시나리오: 필수 약관 미동의하여 최종 회원가입 요청
         * 테스트 조건: 필수 약관에 동의하지 않은 상태로 POST /api/v1/auth/signup/submit 요청
         * 예상 결과:
         * - 400 Bad Request 상태 코드 반환
         * - 응답 본문에 에러 코드 "AUTH_003" 포함
         */
        @Test
        @DisplayName("최종 회원가입 - 필수 약관 미동의")
        void signup_should_ThrowException_when_RequiredTermNotAgreed() throws Exception {
                // given
                String requestJson = """
                                {
                                  "verificationId": "c78a8a1d-2c4f-4f3a-b5f7-5a9a8e3a4b2f",
                                  "termAgreements": [ { "termId": 1, "isAgreed": false } ],
                                  "loginId": "newuser123",
                                  "password": "Password123!",
                                  "financialPropensity": "STABLE"
                                }
                                """;

                willThrow(new CustomException(ErrorCode.REQUIRED_TERM_NOT_AGREED)).given(authService)
                                .signup(any(UserSignupRequestDto.class));

                // when
                ResultActions resultActions = mockMvc.perform(post("/api/v1/auth/signup/submit")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestJson));

                // then
                resultActions.andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.isSuccess").value(false))
                                .andExpect(jsonPath("$.error.code")
                                                .value(ErrorCode.REQUIRED_TERM_NOT_AGREED.getCode()));
        }

        /**
         * 기능 ID: AUTH-07
         * 테스트 시나리오: 유효한 토큰으로 로그아웃 요청
         * 테스트 조건: 유효한 Access Token(헤더)과 Refresh Token(쿠키)을 포함하여 POST
         * /api/v1/auth/logout 요청
         * 예상 결과:
         * - 200 OK 상태 코드 반환
         * - 응답 본문에 "로그아웃이 완료되었습니다." 메시지 포함
         * - Refresh Token 쿠키의 max-age가 0으로 설정되어 삭제됨
         */
        @Test
        @DisplayName("로그아웃 - 성공")
        @WithMockUser
        void logout_should_ReturnSuccess_when_ValidRequest() throws Exception {
                // given
                String accessToken = "dummy-access-token";
                String refreshToken = "dummy-refresh-token";

                willDoNothing().given(authService).logout(any(String.class), any(String.class));
                willDoNothing().given(cookieUtil).deleteRefreshTokenCookie(any());

                // when
                ResultActions resultActions = mockMvc.perform(post("/api/v1/auth/logout")
                                .header("Authorization", "Bearer " + accessToken)
                                .cookie(new Cookie("refresh_token", refreshToken)));

                // then
                resultActions
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.isSuccess").value(true))
                                .andExpect(jsonPath("$.data").value("로그아웃이 완료되었습니다."));
        }

        /**
         * 기능 ID: AUTH-07
         * 테스트 시나리오: 이미 로그아웃된 토큰으로 다시 로그아웃 요청
         * 테스트 조건: 블랙리스트에 등록된 Access Token으로 POST /api/v1/auth/logout 요청
         * 예상 결과:
         * - 400 Bad Request 상태 코드 반환
         * - 응답 본문에 에러 코드 "AUTH_011" 포함
         */
        @Test
        @DisplayName("로그아웃 - 이미 로그아웃된 토큰")
        void logout_should_ThrowException_when_TokenIsAlreadyLoggedOut() throws Exception {
                // given
                String accessToken = "already-logged-out-token";
                String refreshToken = "some-refresh-token";

                willThrow(new CustomException(ErrorCode.ALREADY_LOGGED_OUT))
                                .given(authService).logout(any(String.class), any(String.class));

                // when
                ResultActions resultActions = mockMvc.perform(post("/api/v1/auth/logout")
                                .header("Authorization", "Bearer " + accessToken)
                                .cookie(new Cookie("refresh_token", refreshToken)));

                // then
                resultActions.andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.isSuccess").value(false))
                                .andExpect(jsonPath("$.error.code").value(ErrorCode.ALREADY_LOGGED_OUT.getCode()));
        }

        /**
         * 기능 ID: AUTH-07
         * 테스트 시나리오: 유효하지 않은 리프레시 토큰으로 로그아웃 요청
         * 테스트 조건: DB에 저장된 값과 다른 Refresh Token으로 POST /api/v1/auth/logout 요청
         * 예상 결과:
         * - 401 Unauthorized 상태 코드 반환
         * - 응답 본문에 에러 코드 "AUTH_013" 포함
         */
        @Test
        @DisplayName("로그아웃 - 유효하지 않은 리프레시 토큰")
        void logout_should_ThrowException_when_RefreshTokenIsInvalid() throws Exception {
                // given
                String accessToken = "valid-access-token";
                String invalidRefreshToken = "invalid-refresh-token";

                willThrow(new CustomException(ErrorCode.INVALID_REFRESH_TOKEN))
                                .given(authService).logout(any(String.class), any(String.class));

                // when
                ResultActions resultActions = mockMvc.perform(post("/api/v1/auth/logout")
                                .header("Authorization", "Bearer " + accessToken)
                                .cookie(new Cookie("refresh_token", invalidRefreshToken)));

                // then
                resultActions.andExpect(status().isUnauthorized())
                                .andExpect(jsonPath("$.isSuccess").value(false))
                                .andExpect(jsonPath("$.error.code").value(ErrorCode.INVALID_REFRESH_TOKEN.getCode()));
        }

        /**
         * 기능 ID: AUTH-08
         * 테스트 시나리오: 유효한 리프레시 토큰으로 Access Token 재발급 요청
         * 테스트 조건: 유효한 Refresh Token(쿠키)을 포함하여 POST /api/v1/auth/reissue 요청
         * 예상 결과:
         * - 200 OK 상태 코드 반환
         * - 응답 본문에 새로 발급된 Access Token 포함
         * - 응답 헤더에 새로 발급된 Refresh Token을 담은 HttpOnly 쿠키 설정
         */
        @Test
        @DisplayName("Access Token 재발급 - 성공")
        void reissue_should_ReturnNewAccessToken_when_RefreshTokenIsValid() throws Exception {
                // given
                String validRefreshToken = "valid-refresh-token";
                TokenResponseDto newTokenDto = TokenResponseDto.builder()
                                .grantType("Bearer")
                                .accessToken("new-access-token")
                                .refreshToken("new-refresh-token")
                                .build();

                given(authService.reissue(validRefreshToken)).willReturn(newTokenDto);
                willDoNothing().given(cookieUtil).setRefreshTokenCookie(any(), any());

                // when
                ResultActions resultActions = mockMvc.perform(post("/api/v1/auth/reissue")
                                .cookie(new Cookie("refresh_token", validRefreshToken)));

                // then
                resultActions.andExpect(status().isOk())
                                .andExpect(jsonPath("$.isSuccess").value(true))
                                .andExpect(jsonPath("$.data.accessToken").value("new-access-token"));
        }

        /**
         * 기능 ID: AUTH-08
         * 테스트 시나리오: 유효하지 않은 리프레시 토큰으로 Access Token 재발급 요청
         * 테스트 조건: 만료되었거나 DB에 없는 Refresh Token(쿠키)으로 POST /api/v1/auth/reissue 요청
         * 예상 결과:
         * - 401 Unauthorized 상태 코드 반환
         * - 응답 본문에 에러 코드 "AUTH_013" 포함
         */
        @Test
        @DisplayName("Access Token 재발급 - 유효하지 않은 리프레시 토큰")
        void reissue_should_ThrowException_when_RefreshTokenIsInvalid() throws Exception {
                // given
                String invalidRefreshToken = "invalid-refresh-token";
                given(authService.reissue(invalidRefreshToken))
                                .willThrow(new CustomException(ErrorCode.INVALID_REFRESH_TOKEN));

                // when
                ResultActions resultActions = mockMvc.perform(post("/api/v1/auth/reissue")
                                .cookie(new Cookie("refresh_token", invalidRefreshToken)));

                // then
                resultActions.andExpect(status().isUnauthorized())
                                .andExpect(jsonPath("$.isSuccess").value(false))
                                .andExpect(jsonPath("$.error.code").value(ErrorCode.INVALID_REFRESH_TOKEN.getCode()));
        }

        /**
         * 기능 ID: AUTH-08
         * 테스트 시나리오: 리프레시 토큰 없이 Access Token 재발급 요청
         * 테스트 조건: Refresh Token 쿠키를 포함하지 않고 POST /api/v1/auth/reissue 요청
         * 예상 결과:
         * - 400 Bad Request 상태 코드 반환 (Spring의 @CookieValue 필수 값 누락 처리)
         */
        @Test
        @DisplayName("Access Token 재발급 - 리프레시 토큰 누락")
        void reissue_should_ThrowException_when_RefreshTokenIsMissing() throws Exception {
                // when
                ResultActions resultActions = mockMvc.perform(post("/api/v1/auth/reissue"));

                // then
                resultActions.andExpect(status().isBadRequest());
        }

}
