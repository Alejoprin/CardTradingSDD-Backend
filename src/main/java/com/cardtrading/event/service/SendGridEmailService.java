package com.cardtrading.event.service;

import com.sendgrid.*;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
@Slf4j
public class SendGridEmailService {

    @Value("${SENDGRID_API_KEY}")
    private String sendGridApiKey;

    @Value("${app.mail.from}")
    private String fromEmail;

    @Value("${app.mail.from-name}")
    private String fromName;

    public void sendEmail(String to, String subject, String htmlContent) {
        Email from = new Email(fromEmail, fromName);
        Email toEmail = new Email(to);
        Content content = new Content("text/html", htmlContent);
        Mail mail = new Mail(from, subject, toEmail, content);

        SendGrid sg = new SendGrid(sendGridApiKey);
        Request request = new Request();

        try {
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());

            Response response = sg.api(request);

            if (response.getStatusCode() >= 200 && response.getStatusCode() < 300) {
                log.info("Email sent successfully to={} subject={}", to, subject);
            } else {
                log.error("Failed to send email to={} status={} body={}",
                        to, response.getStatusCode(), response.getBody());
            }
        } catch (IOException e) {
            log.error("Error sending email to={} subject={}", to, subject, e);
        }
    }
}