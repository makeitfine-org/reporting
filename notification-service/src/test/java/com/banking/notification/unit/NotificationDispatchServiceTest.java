package com.banking.notification.unit;

import com.banking.notification.application.EmailDispatchService;
import com.banking.notification.application.NotificationService;
import com.banking.notification.application.SmsDispatchService;
import com.banking.notification.domain.NotificationStatus;
import com.banking.notification.infrastructure.feign.CustomerServiceClient;
import com.banking.notification.infrastructure.feign.dto.ContactResponse;
import com.banking.notification.infrastructure.postgres.entity.NotificationJob;
import com.banking.notification.infrastructure.postgres.repository.NotificationJobRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationDispatchServiceTest {

    @Mock
    private NotificationJobRepository jobRepository;
    @Mock
    private CustomerServiceClient customerServiceClient;
    @Mock
    private EmailDispatchService emailDispatchService;
    @Mock
    private SmsDispatchService smsDispatchService;

    @InjectMocks
    private NotificationService notificationService;

    @Test
    void processTransactionNotification_success_emailAndSms() {
        ContactResponse contact = new ContactResponse();
        contact.setClientId("cli-001");
        contact.setEmail("alice@bank.com");
        contact.setPhone("+49123456789");

        when(customerServiceClient.getContact("cli-001")).thenReturn(contact);
        when(jobRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        notificationService.processTransactionNotification("cli-001", "tx-001", "5000", "EUR");

        verify(emailDispatchService).sendEmail(eq("alice@bank.com"), anyString(), anyString());
        verify(smsDispatchService).sendSms(eq("+49123456789"), anyString());

        ArgumentCaptor<NotificationJob> captor = ArgumentCaptor.forClass(NotificationJob.class);
        verify(jobRepository, times(2)).save(captor.capture());
        NotificationJob saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo(NotificationStatus.SENT);
    }

    @Test
    void processTransactionNotification_emailFails_markedFailed() {
        ContactResponse contact = new ContactResponse();
        contact.setClientId("cli-002");
        contact.setEmail("bob@bank.com");

        when(customerServiceClient.getContact("cli-002")).thenReturn(contact);
        when(jobRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        doThrow(new RuntimeException("SMTP down")).when(emailDispatchService)
                .sendEmail(anyString(), anyString(), anyString());

        notificationService.processTransactionNotification("cli-002", "tx-002", "1000", "USD");

        ArgumentCaptor<NotificationJob> captor = ArgumentCaptor.forClass(NotificationJob.class);
        verify(jobRepository, times(2)).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(NotificationStatus.FAILED);
    }

    @Test
    void processTransactionNotification_noPhone_smsNotSent() {
        ContactResponse contact = new ContactResponse();
        contact.setClientId("cli-003");
        contact.setEmail("carol@bank.com");
        // phone is null

        when(customerServiceClient.getContact("cli-003")).thenReturn(contact);
        when(jobRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        notificationService.processTransactionNotification("cli-003", "tx-003", "200", "GBP");

        verify(smsDispatchService, never()).sendSms(anyString(), anyString());

        ArgumentCaptor<NotificationJob> captor = ArgumentCaptor.forClass(NotificationJob.class);
        verify(jobRepository, times(2)).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(NotificationStatus.SENT);
    }

    @Test
    void processCustomerNotification_success_emailSent() {
        ContactResponse contact = new ContactResponse();
        contact.setClientId("cli-004");
        contact.setEmail("dave@bank.com");

        when(customerServiceClient.getContact("cli-004")).thenReturn(contact);
        when(jobRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        notificationService.processCustomerNotification("cli-004", "KYC_APPROVED");

        verify(emailDispatchService).sendEmail(eq("dave@bank.com"), anyString(), anyString());

        ArgumentCaptor<NotificationJob> captor = ArgumentCaptor.forClass(NotificationJob.class);
        verify(jobRepository, times(2)).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(NotificationStatus.SENT);
    }

    @Test
    void processCustomerNotification_emailFails_markedFailed() {
        ContactResponse contact = new ContactResponse();
        contact.setClientId("cli-005");
        contact.setEmail("eve@bank.com");

        when(customerServiceClient.getContact("cli-005")).thenReturn(contact);
        when(jobRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        doThrow(new RuntimeException("Mail server down")).when(emailDispatchService)
                .sendEmail(anyString(), anyString(), anyString());

        notificationService.processCustomerNotification("cli-005", "RISK_UPDATED");

        ArgumentCaptor<NotificationJob> captor = ArgumentCaptor.forClass(NotificationJob.class);
        verify(jobRepository, times(2)).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(NotificationStatus.FAILED);
    }
}
