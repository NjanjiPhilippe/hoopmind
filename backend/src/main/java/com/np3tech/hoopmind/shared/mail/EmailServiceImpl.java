package com.np3tech.hoopmind.shared.mail;

import com.np3tech.hoopmind.shared.mail.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Service for sending emails
 */
@Service
@ConditionalOnProperty(prefix = "spring.mail", name = "host")
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.from:noreply@hoopmind.com}")
    private String fromEmail;

    /**
     * Send simple email
     */
    @Async
    public void sendEmail(String to, String subject, String text) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);

            mailSender.send(message);
            log.info("Email sent to: {}", to);
        } catch (Exception e) {
            log.error("Error sending email to: {}", to, e);
        }
    }

    /**
     * Send welcome email to new user
     */
    @Async
    public void sendWelcomeEmail(String to, String username) {
        String subject = "Welcome to HoopMind!";
        String text = String.format(
                "Hello %s,\n\n" +
                        "Welcome to HoopMind! We're excited to have you on board.\n\n" +
                        "Start making predictions, compete with friends, and climb the leaderboard!\n\n" +
                        "Best regards,\n" +
                        "The HoopMind Team",
                username
        );

        sendEmail(to, subject, text);
    }

    /**
     * Send password reset email
     */
    @Async
    public void sendPasswordResetEmail(String to, String resetToken) {
        String subject = "Password Reset Request";
        String text = String.format(
                "Hello,\n\n" +
                        "You requested a password reset. Use the following token to reset your password:\n\n" +
                        "%s\n\n" +
                        "This token will expire in 1 hour.\n\n" +
                        "If you didn't request this, please ignore this email.\n\n" +
                        "Best regards,\n" +
                        "The HoopMind Team",
                resetToken
        );

        sendEmail(to, subject, text);
    }

    /**
     * Send account verification email
     */
    @Async
    public void sendVerificationEmail(String to, String verificationToken) {
        String subject = "Verify Your Email Address";
        String text = String.format(
                "Hello,\n\n" +
                        "Please verify your email address by using the following token:\n\n" +
                        "%s\n\n" +
                        "Best regards,\n" +
                        "The HoopMind Team",
                verificationToken
        );

        sendEmail(to, subject, text);
    }
}
