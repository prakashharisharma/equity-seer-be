package com.equityseer.api;

import com.equityseer.data.UserEntity;
import com.equityseer.service.UserService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserController {
  private final UserService userService;

  public UserController(UserService userService) {
    this.userService = userService;
  }

  @GetMapping("/users")
  public List<UserEntity> list() {
    return userService.list();
  }

  @PostMapping("/users")
  @ResponseStatus(HttpStatus.CREATED)
  public UserEntity create(@RequestBody CreateUserRequest request) {
    return userService.create(request.name());
  }

  public record CreateUserRequest(String name) { }
}

