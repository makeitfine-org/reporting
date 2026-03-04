package com.banking.notification.infrastructure.postgres.repository;

import com.banking.notification.domain.NotificationStatus;
import com.banking.notification.infrastructure.postgres.entity.NotificationJob;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationJobRepository extends JpaRepository<NotificationJob, String> {
    List<NotificationJob> findByStatus(NotificationStatus status);
}
