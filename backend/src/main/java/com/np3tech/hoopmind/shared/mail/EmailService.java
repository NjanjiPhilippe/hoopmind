package com.np3tech.hoopmind.shared.mail;

public interface EmailService {
    void sendEmail(String to, String subject, String text);
    void sendWelcomeEmail(String to, String username);
    void sendPasswordResetEmail(String to, String resetToken);
    void sendVerificationEmail(String to, String verificationToken);
}
