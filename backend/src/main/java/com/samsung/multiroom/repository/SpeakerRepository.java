package com.samsung.multiroom.repository;

import com.samsung.multiroom.model.Speaker;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SpeakerRepository extends JpaRepository<Speaker, Long> {
    Optional<Speaker> findByMacAddress(String macAddress);
    Optional<Speaker> findByIpAddress(String ipAddress);
}