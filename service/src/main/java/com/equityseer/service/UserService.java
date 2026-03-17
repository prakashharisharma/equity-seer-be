package com.equityseer.service;

import com.equityseer.data.UserEntity;
import com.equityseer.data.UserRepository;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class UserService {
  private final UserRepository userRepository;

  public UserService(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  @Transactional
  public UserEntity create(String name) {
    log.info("Creating user with name={}", name); // parameterized logging
    return userRepository.save(new UserEntity(name));
  }

  @Transactional(readOnly = true)
  public List<UserEntity> list() {
    return userRepository.findAll();
  }
}
