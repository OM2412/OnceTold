package com.oncetold.oncetold.repository;

import com.oncetold.oncetold.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {
    List<Message> findByTicketIdOrderByCreatedAtAsc(Long ticketId);
}
