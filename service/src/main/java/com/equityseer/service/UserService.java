package com.equityseer.service;

import com.equityseer.data.UserEntity;
import com.equityseer.data.UserRepository;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {
  private static final Logger LOG = LoggerFactory.getLogger(UserService.class);
  private final UserRepository userRepository;

  public UserService(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  @Transactional
  public UserEntity create(String name) {
    LOG.info("Creating user with name={}", name); // parameterized logging
    return userRepository.save(new UserEntity(name));
  }

  @Transactional(readOnly = true)
  public List<UserEntity> list() {
    return userRepository.findAll();
  }
}

