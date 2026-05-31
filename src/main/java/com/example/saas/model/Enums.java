
package com.example.saas.model;

public final class Enums {

  private Enums() {
  }

  public enum TenantStatus {
    active, suspended, deleted
  }

  public enum UserRole {
    admin, manager, member
  }

  public enum UserStatus {
    active, disabled
  }

  public enum ProjectVisibility {
    PRIVATE("private"),
    INTERNAL("internal");

    private final String dbValue;

    ProjectVisibility(String dbValue) {
      this.dbValue = dbValue;
    }

    public String getDbValue() {
      return dbValue;
    }

    public static ProjectVisibility fromDb(String value) {
      for (ProjectVisibility v : values()) {
        if (v.dbValue.equals(value)) {
          return v;
        }
      }
      throw new IllegalArgumentException("Unknown visibility: " + value);
    }
  }

  public enum ProjectMemberRole {
    owner, editor, viewer
  }

  public enum TaskStatus {
    todo, in_progress, done
  }

  public enum TaskPriority {
    low, medium, high
  }
}
