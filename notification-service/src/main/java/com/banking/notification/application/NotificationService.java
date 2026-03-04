package com.banking.notification.application;

import com.banking.notification.domain.NotificationChannel;
import com.banking.notification.domain.NotificationStatus;
import com.banking.notification.infrastructure.feign.CustomerServiceClient;
import com.banking.notification.infrastructure.feign.dto.ContactResponse;
import com.banking.notification.infrastructure.postgres.entity.NotificationJob;
import com.banking.notification.infrastructure.postgres.repository.NotificationJobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationJobRepository jobRepository;
    private final CustomerServiceClient customerServiceClient;
    private final EmailDispatchService emailDispatchService;
    private final SmsDispatchService smsDispatchService;

    @Transactional
    public void processTransactionNotification(String clientId, String transactionId,
                                               String amount, String currency) {
        ContactResponse contact = customerServiceClient.getContact(clientId);

        NotificationJob job = NotificationJob.builder()
                .clientId(clientId)
                .transactionId(transactionId)
                .channel(NotificationChannel.EMAIL_AND_SMS)
                .status(NotificationStatus.PENDING)
                .payload("Transaction " + transactionId + " " + amount + " " + currency)
                .build();
        job = jobRepository.save(job);

        try {
            String subject = "Transaction Confirmation";
            String body = "Your transaction of " + amount + " " + currency + " has been processed.";
            emailDispatchService.sendEmail(contact.getEmail(), subject, body);

            if (contact.getPhone() != null) {
                smsDispatchService.sendSms(contact.getPhone(), body);
            }

            job.setStatus(NotificationStatus.SENT);
            job.setSentAt(Instant.now());
        } catch (Exception e) {
            log.error("Failed to dispatch notification for clientId={} txId={}", clientId, transactionId, e);
            job.setStatus(NotificationStatus.FAILED);
            job.setFailedAt(Instant.now());
        }
        jobRepository.save(job);
    }

    @Transactional
    public void processCustomerNotification(String clientId, String eventType) {
        NotificationJob job = NotificationJob.builder()
                .clientId(clientId)
                .channel(NotificationChannel.EMAIL)
                .status(NotificationStatus.PENDING)
                .payload("Customer event: " + eventType)
                .build();
        job = jobRepository.save(job);

        try {
            ContactResponse contact = customerServiceClient.getContact(clientId);
            String subject = "Account Update";
            String body = "Your account has been updated: " + eventType;
            emailDispatchService.sendEmail(contact.getEmail(), subject, body);
            job.setStatus(NotificationStatus.SENT);
            job.setSentAt(Instant.now());
        } catch (Exception e) {
            log.error("Failed to dispatch customer notification for clientId={}", clientId, e);
            job.setStatus(NotificationStatus.FAILED);
            job.setFailedAt(Instant.now());
        }
        jobRepository.save(job);
    }
}
