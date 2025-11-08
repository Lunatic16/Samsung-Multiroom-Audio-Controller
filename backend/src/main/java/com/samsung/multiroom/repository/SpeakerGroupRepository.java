package com.samsung.multiroom.repository;

import com.samsung.multiroom.model.SpeakerGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SpeakerGroupRepository extends JpaRepository<SpeakerGroup, Long> {
    Optional<SpeakerGroup> findByName(String name);
}