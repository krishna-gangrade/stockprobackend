package com.stockpro.auth.repository;

import com.stockpro.auth.entity.AuthAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuthAuditLog, Long> {

    List<AuthAuditLog> findByActorIdOrderByCreatedAtDesc(Long actorId);

    List<AuthAuditLog> findByActionOrderByCreatedAtDesc(String action);
}
