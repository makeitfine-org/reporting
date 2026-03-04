package com.banking.notification.unit;

import com.banking.notification.application.EmailDispatchService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EmailDispatchServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private EmailDispatchService emailDispatchService;

    @Test
    void sendEmail_success_invokesMailSender() {
        emailDispatchService.sendEmail("user@bank.com", "Subject", "Body text");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());

        SimpleMailMessage sent = captor.getValue();
        assertThat(sent.getTo()).containsExactly("user@bank.com");
        assertThat(sent.getSubject()).isEqualTo("Subject");
        assertThat(sent.getText()).isEqualTo("Body text");
    }

    @Test
    void sendEmail_mailSenderThrows_propagatesException() {
        doThrow(new MailSendException("SMTP unavailable")).when(mailSender).send(any(SimpleMailMessage.class));

        assertThatThrownBy(() -> emailDispatchService.sendEmail("user@bank.com", "Subject", "Body"))
                .isInstanceOf(MailSendException.class)
                .hasMessageContaining("SMTP unavailable");
    }
}
