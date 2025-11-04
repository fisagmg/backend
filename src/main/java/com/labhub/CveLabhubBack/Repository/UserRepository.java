package com.labhub.CveLabhubBack.Repository;

import com.labhub.CveLabhubBack.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<UserEntity, Long> {
    Optional<UserEntity> findByKcUserId(String kcUserId);
    Optional<UserEntity> findByEmail(String email);
}
