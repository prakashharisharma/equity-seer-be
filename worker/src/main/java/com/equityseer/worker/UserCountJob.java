package com.equityseer.worker;

import com.equityseer.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class UserCountJob {
  private static final Logger LOG = LoggerFactory.getLogger(UserCountJob.class);
  private final UserService userService;

  public UserCountJob(UserService userService) {
    this.userService = userService;
  }

  @Scheduled(fixedDelayString = "${worker.userCount.fixedDelay:60000}")
  public void logUserCount() {
    var count = userService.list().size();
    LOG.info("Current user count: {}", count);
  }
}

