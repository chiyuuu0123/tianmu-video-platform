package com.shanyangcode.tianmu.constants;

/**
 * 邮件常量
 */
public class EmailConstant {
    public static final Integer CODE_EXPIRE_MINUTES = 5;

    public static final String CODE_SEND_SUCCESS_MSG = "邮箱验证码发送成功";

    public static final String EMAIL_HOST_NAME = getConfig("TIANMU_MAIL_HOST", "smtp.qq.com");

    public static final String EMAIL_PROTOCOL = "mail.smtp.ssl.protocols";

    public static final String TLS_VERSION = "TLSv1.2";

    public static final String VERIFICATION_CODE_TEMPLATE = "您的验证码为: %s (五分钟内有效)";

    public static final String EMAIL_USER_NAME = getConfig("TIANMU_MAIL_USERNAME", "");

    /**
     * QQ 邮箱 SMTP 授权码。只从本机配置读取，禁止提交到代码仓库。
     */
    public static final String EMAIL_PASSWORD = getConfig("TIANMU_MAIL_AUTH_CODE", "");

    public static final String EMAIL_NAME = getConfig("TIANMU_MAIL_SENDER_NAME", "天幕");

    public static final String EMAIL_SUBJECT = "注册验证码";

    public static final String EMAIL_EXCEPTION_LOG_TEMPLATE = "发送验证码到 {} 失败";

    public static final String EMAIL_PROPERTY_KEY = "mail.smtp.ssl.protocols";

    public static final String EMAIL_PROPERTY_VALUE = "TLSv1.2";

    private static String getConfig(String key, String defaultValue) {
        String value = System.getProperty(key);
        if (value == null || value.isBlank()) {
            value = System.getenv(key);
        }
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }
}
