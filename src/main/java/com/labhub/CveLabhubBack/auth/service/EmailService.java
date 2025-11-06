package com.labhub.CveLabhubBack.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {
    private final JavaMailSender mailSender;

    public void sendOtp(String to, String code) {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo(to);
        msg.setSubject("[CVE LabHub] 이메일 인증번호");
        msg.setText("인증번호는 " + code + " 입니다. 5분 안에 입력해주세요.");
        mailSender.send(msg);
    }
}
