package com.kaung.uit_virtual_campus.repository;

import com.kaung.uit_virtual_campus.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    @Query("""
            SELECT n FROM Notification n
            JOIN FETCH n.user u
            WHERE u.userId = :userId
            ORDER BY n.createdAt DESC, n.notificationId DESC
            """)
    List<Notification> findByUserIdOrderByCreatedAtDesc(@Param("userId") UUID userId);

    @Query("""
            SELECT n FROM Notification n
            JOIN FETCH n.user u
            WHERE n.notificationId = :notificationId
              AND u.userId = :userId
            """)
    Optional<Notification> findByIdAndUserId(
            @Param("notificationId") Long notificationId,
            @Param("userId") UUID userId
    );

    long countByUserUserIdAndReadFalse(UUID userId);

    @Modifying
    @Query("""
            UPDATE Notification n
            SET n.read = true,
                n.readAt = :readAt
            WHERE n.user.userId = :userId
              AND n.read = false
            """)
    int markAllRead(@Param("userId") UUID userId, @Param("readAt") OffsetDateTime readAt);
}
