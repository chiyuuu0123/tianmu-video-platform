package com.shanyangcode.tianmu.utils;

import com.shanyangcode.tianmu.common.ErrorCode;
import com.shanyangcode.tianmu.constants.EmailConstant;
import com.shanyangcode.tianmu.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.SimpleEmail;

@Slf4j
public class SendMailUtil {
    public static void sendEmailCode(String targetEmail, String authCode) {
        if (EmailConstant.EMAIL_PASSWORD.isBlank()) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "未配置 QQ 邮箱 SMTP 授权码");
        }

        try {
            // 设置 TLS 协议
            System.setProperty(EmailConstant.EMAIL_PROTOCOL, EmailConstant.TLS_VERSION);
            SimpleEmail mail = new SimpleEmail();
            mail.setHostName(EmailConstant.EMAIL_HOST_NAME);
            mail.setAuthentication(EmailConstant.EMAIL_USER_NAME, EmailConstant.EMAIL_PASSWORD);
            mail.setFrom(EmailConstant.EMAIL_USER_NAME, EmailConstant.EMAIL_NAME);
            mail.setSSLOnConnect(true);
            mail.addTo(targetEmail);
            mail.setSubject(EmailConstant.EMAIL_SUBJECT);
            mail.setMsg(String.format(EmailConstant.VERIFICATION_CODE_TEMPLATE, authCode));
            mail.send();
        } catch (EmailException e) {
            log.error(EmailConstant.EMAIL_EXCEPTION_LOG_TEMPLATE, targetEmail, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "验证码邮件发送失败，请检查发件邮箱配置");
        }
    }
}
