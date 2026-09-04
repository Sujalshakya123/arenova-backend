package com.arenova.respositories;

import com.arenova.entities.SupportChatMessage;
import com.arenova.entities.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface SupportChatMessageRepository extends JpaRepository<SupportChatMessage, Long> {

    List<SupportChatMessage> findByUserOrderBySentAtDesc(User user, Pageable pageable);

    Optional<SupportChatMessage> findFirstByUserOrderBySentAtDesc(User user);

    long countByUser(User user);

    @Query("""
            SELECT m FROM SupportChatMessage m
            WHERE m.sentAt = (
                SELECT MAX(m2.sentAt) FROM SupportChatMessage m2 WHERE m2.user = m.user
            )
            ORDER BY m.sentAt DESC
            """)
    List<SupportChatMessage> findLatestMessagePerUser();
}
