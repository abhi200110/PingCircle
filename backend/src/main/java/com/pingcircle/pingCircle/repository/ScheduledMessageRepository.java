package com.pingcircle.pingCircle.repository;

import com.pingcircle.pingCircle.entity.ScheduledMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ScheduledMessageRepository extends JpaRepository<ScheduledMessage, Long> {

    /**
     * Find all scheduled messages that need to be sent (not sent yet and scheduled time has passed)
     */
    @Query("SELECT sm FROM ScheduledMessage sm WHERE sm.isSent = false AND sm.scheduledTime <= :currentTime ORDER BY sm.scheduledTime ASC")
    List<ScheduledMessage> findMessagesToSend(@Param("currentTime") Long currentTime);

    /**
     * Find all scheduled messages for a specific user (as sender)
     */
    @Query("SELECT sm FROM ScheduledMessage sm WHERE sm.senderName = :senderName ORDER BY sm.scheduledTime DESC")
    List<ScheduledMessage> findBySenderName(@Param("senderName") String senderName);

    /**
     * Find all scheduled messages for a specific user (as sender) that haven't been sent yet
     */
    @Query("SELECT sm FROM ScheduledMessage sm WHERE sm.senderName = :senderName AND sm.isSent = false ORDER BY sm.scheduledTime ASC")
    List<ScheduledMessage> findPendingBySenderName(@Param("senderName") String senderName);

    /**
     * Find scheduled messages by type
     */
    @Query("SELECT sm FROM ScheduledMessage sm WHERE sm.messageType = :messageType AND sm.senderName = :senderName ORDER BY sm.scheduledTime ASC")
    List<ScheduledMessage> findByMessageTypeAndSender(@Param("messageType") ScheduledMessage.MessageType messageType, 
                                                     @Param("senderName") String senderName);

    /**
     * Find reminder messages for a specific user
     */
    @Query("SELECT sm FROM ScheduledMessage sm WHERE sm.senderName = :senderName AND sm.messageType IN ('REMINDER', 'BIRTHDAY', 'ANNIVERSARY') ORDER BY sm.scheduledTime ASC")
    List<ScheduledMessage> findRemindersBySender(@Param("senderName") String senderName);

    /**
     * Delete scheduled messages by sender name
     */
    void deleteBySenderName(String senderName);
}
