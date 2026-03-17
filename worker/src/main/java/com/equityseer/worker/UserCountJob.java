package com.equityseer.worker;

import com.equityseer.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class UserCountJob {
  private final UserService userService;

  public UserCountJob(UserService userService) {
    this.userService = userService;
  }

  @Scheduled(fixedDelayString = "${worker.userCount.fixedDelay:60000}")
  public void logUserCount() {
    var count = userService.list().size();
    log.info("Current user count: {}", count);
  }
}
