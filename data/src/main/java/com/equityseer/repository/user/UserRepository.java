package com.equityseer.repository.user;

import com.equityseer.entity.user.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<UserEntity, Long> {}
