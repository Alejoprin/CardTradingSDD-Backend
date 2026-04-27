package com.cardtrading.event.service;

import com.cardtrading.auth.entity.User;
import com.cardtrading.auth.repository.UserRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final UserRepository userRepository;

    public void sendRegistrationConfirmation(UUID userId, String username, String email) {
        Context context = new Context();
        context.setVariable("username", username);
        sendEmail(email, "Welcome to Card Trading Platform!", "registration-confirmation", context);
    }

    public void sendTradeCreatedNotification(UUID tradeId, UUID offererId, UUID receiverId) {
        User offerer = userRepository.findById(offererId).orElse(null);
        User receiver = userRepository.findById(receiverId).orElse(null);
        if (receiver == null || offerer == null) return;

        Context context = new Context();
        context.setVariable("tradeId", tradeId.toString());
        context.setVariable("offererUsername", offerer.getUsername());
        context.setVariable("receiverUsername", receiver.getUsername());
        sendEmail(receiver.getEmail(), "New Trade Offer Received", "trade-created", context);
    }

    public void sendTradeAcceptedNotification(UUID tradeId, UUID offererId, UUID receiverId) {
        User offerer = userRepository.findById(offererId).orElse(null);
        User receiver = userRepository.findById(receiverId).orElse(null);
        if (offerer == null) return;

        Context context = new Context();
        context.setVariable("tradeId", tradeId.toString());
        context.setVariable("recipientUsername", offerer.getUsername());
        sendEmail(offerer.getEmail(), "Your Trade Offer Was Accepted", "trade-accepted", context);

        if (receiver != null) {
            context.setVariable("recipientUsername", receiver.getUsername());
            sendEmail(receiver.getEmail(), "Trade Accepted", "trade-accepted", context);
        }
    }

    public void sendTradeRejectedNotification(UUID tradeId, UUID offererId, String reason) {
        User offerer = userRepository.findById(offererId).orElse(null);
        if (offerer == null) return;

        Context context = new Context();
        context.setVariable("tradeId", tradeId.toString());
        context.setVariable("offererUsername", offerer.getUsername());
        context.setVariable("reason", reason);
        sendEmail(offerer.getEmail(), "Your Trade Offer Was Rejected", "trade-rejected", context);
    }

    public void sendTradeCompletedNotification(UUID tradeId, UUID offererId, UUID receiverId) {
        Map.of(offererId, "offerer", receiverId, "receiver").forEach((userId, role) -> {
            User user = userRepository.findById(userId).orElse(null);
            if (user == null) return;

            Context context = new Context();
            context.setVariable("tradeId", tradeId.toString());
            context.setVariable("recipientUsername", user.getUsername());
            sendEmail(user.getEmail(), "Trade Completed Successfully!", "trade-completed", context);
        });
    }

    private void sendEmail(String to, String subject, String template, Context context) {
        try {
            String html = templateEngine.process(template, context);
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            helper.setFrom("noreply@cardtrading.com");
            mailSender.send(message);
            log.info("Email sent to={} subject={} traceId={}", to, subject, MDC.get("traceId"));
        } catch (MessagingException e) {
            log.error("Failed to send email to={} subject={} traceId={}", to, subject, MDC.get("traceId"), e);
        }
    }
}
