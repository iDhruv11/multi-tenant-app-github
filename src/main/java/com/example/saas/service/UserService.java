package com.example.saas.service;

import com.example.saas.dto.Dtos.UserResponse;
import com.example.saas.exception.BadRequestException;
import com.example.saas.exception.ForbiddenException;
import com.example.saas.exception.NotFoundException;
import com.example.saas.model.Enums.ProjectMemberRole;
import com.example.saas.model.Enums.UserRole;
import com.example.saas.model.Enums.UserStatus;
import com.example.saas.model.User;
import com.example.saas.repository.ProjectMemberRepository;
import com.example.saas.repository.UserRepository;
import com.example.saas.security.LoggedInUser;
import com.example.saas.util.TenantCtx;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

  private final UserRepository userRepository;
  private final ProjectMemberRepository memberRepository;

  public UserService(UserRepository userRepository, ProjectMemberRepository memberRepository) {
    this.userRepository = userRepository;
    this.memberRepository = memberRepository;
  }

  @Transactional(readOnly = true)
  public List<UserResponse> list() {
    UUID me = LoggedInUser.current().getUserId();
    UserRole role = LoggedInUser.current().getRole();
    if (role == UserRole.member && !canMemberListUsers(me)) {
      throw new ForbiddenException("Members cannot view users");
    }
    return userRepository.findByTenantId(TenantCtx.get()).stream()
        .map(UserResponse::from)
        .toList();
  }

  @Transactional(readOnly = true)
  public UserResponse me() {
    return UserResponse.from(findInTenant(LoggedInUser.current().getUserId()));
  }

  public void ensureActive(UUID userId) {
    User user = findInTenant(userId);
    if (user.getStatus() != UserStatus.active) {
      throw new BadRequestException("User is not active");
    }
  }

  public String displayName(UUID userId) {
    User user = findInTenant(userId);
    return user.getFirstName() + " " + user.getLastName();
  }

  User findInTenant(UUID userId) {
    return userRepository
        .findByIdAndTenantId(userId, TenantCtx.get())
        .orElseThrow(() -> new NotFoundException("User not found"));
  }

  private boolean canMemberListUsers(UUID userId) {
    return memberRepository.existsByUserIdAndTenantIdAndRoleIn(
        userId, TenantCtx.get(), List.of(ProjectMemberRole.owner, ProjectMemberRole.editor));
  }
}
