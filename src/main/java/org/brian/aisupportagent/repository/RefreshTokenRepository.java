package org.brian.aisupportagent.repository;

import jakarta.persistence.LockModeType;
import org.brian.aisupportagent.entity.RefreshToken;
import org.brian.aisupportagent.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<RefreshToken> findByTokenHash(String tokenHash);
    Optional<RefreshToken> findByUser(User user);

    // Automatically cleans up old tokens if a user logs in again
    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    void deleteByUser(User user);
}
