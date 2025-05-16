package com.jokahobby.infra.mail;

public interface EmailService {

    void sendEmail(EmailMessage emailMessage);
}
