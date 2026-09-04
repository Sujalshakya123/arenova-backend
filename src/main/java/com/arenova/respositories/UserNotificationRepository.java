package com.arenova.respositories;

import com.arenova.entities.User;
import com.arenova.entities.UserNotification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserNotificationRepository extends JpaRepository<UserNotification, Long> {

    List<UserNotification> findByUserOrderByCreatedAtDesc(User user);
}
