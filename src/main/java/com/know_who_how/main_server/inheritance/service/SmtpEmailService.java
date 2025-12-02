package com.know_who_how.main_server.inheritance.service;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SmtpEmailService implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String senderEmail;


    @Override
    public void sendVideoLetterLink(String toEmail, String videoLink) {
        try{
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");

            helper.setFrom(senderEmail, "KnowHow 유언장 서비스"); // 발신자 이름 설정 가능
            helper.setTo(toEmail);
            helper.setSubject("💌[KnowHow] 당신을 위한 상속 영상 편지가 도착했습니다.");

            String content  = buildEmailContent(videoLink);
            helper.setText(content, true); // true: HTML 형식으로 전송

            mailSender.send(message);
            log.info("이메일 발송 성공: {}에게 링크 전송 완료", toEmail);

        } catch(Exception e){
            log.error("이메일 발송 중 오류 발생: 대상: {}", toEmail,e);
            throw new RuntimeException("이메일 전송 실패: "+e.getMessage(),e);
        }
    }

    private String buildEmailContent(String videoLink){
        return "<html><body>"
                + "<h1>안녕하세요, 수신자님.</h1>"
                + "<p>KnowWhoHow를 통해 당신에게 소중한 영상 편지가 도착했습니다.</p>"
                + "<p style='margin-top: 20px;'>아래 버튼을 눌러 영상을 확인하세요:</p>"
                + "<div style='margin: 30px 0;'>"
                + "<a href=\"" + videoLink + "\" "
                + "style=\"padding: 15px 30px; background-color: #007bff; color: white; text-decoration: none; border-radius: 8px; font-size: 16px; display: inline-block;\">"
                + "영상 편지 확인하기"
                + "</a>"
                + "</div>"
                + "<p style='color: #6c757d; font-size: 12px;'>이 링크는 임시 접근 토큰을 포함하고 있으며, 보안을 위해 사용 후 만료됩니다.</p>"
                + "</body></html>";
    }
}
