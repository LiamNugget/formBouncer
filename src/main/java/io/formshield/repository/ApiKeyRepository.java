package io.formshield.repository;

import io.formshield.model.ApiKey;
import io.formshield.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ApiKeyRepository extends JpaRepository<ApiKey, Long> {
    Optional<ApiKey> findByKeyHashAndIsActiveTrue(String keyHash);
    List<ApiKey> findByUser(User user);
    int countByUserAndIsActiveTrue(User user);
}
