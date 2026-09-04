package com.arenova.respositories;

import com.arenova.entities.NotificationUserState;
import com.arenova.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NotificationUserStateRepository extends JpaRepository<NotificationUserState, Long> {

    List<NotificationUserState> findByUser(User user);

    Optional<NotificationUserState> findByUserAndNotificationKey(User user, String notificationKey);
}
