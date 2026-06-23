
# Multi-Tenant Project Collaboration Platform

A secure, shared-database SaaS backend built with Spring Boot, PostgreSQL, Redis, and Row Level Security (RLS).

This project focuses on solving real SaaS problems that appear in production systems:

* Preventing one company from accessing another company's data
* Enforcing permissions across both organization-level and project-level roles
* Supporting revocable sessions while still using JWTs
* Preventing privilege escalation
* Ensuring a project never becomes ownerless and an organization never loses its last administrator

To solve these problems, the platform combines PostgreSQL Row Level Security (RLS), layered authorization, project-scoped permissions, session tracking, and tenant-aware business rules inside a shared-database architecture.

---

# Goal

This project explores a SaaS architecture where multiple organizations operate inside the same deployment and database while remaining completely isolated from one another.

The goal was to build something that demonstrates:

* Multi-tenancy
* Authorization
* Security boundaries
* Session management
* Ownership governance
* Real-world business rules

---

# Architecture Overview

The platform follows a shared-database multi-tenant architecture.Each company is represented as a tenant.A tenant owns:

* Users
* Projects
* Tasks
* Sessions
* Activity logs

All tenants share the same PostgreSQL database, but data isolation is enforced through multiple layers:

1. JWT-based identity
2. Tenant-aware application services
3. Authorization checks
4. PostgreSQL Row Level Security (RLS)


# Core Features

## Multi-Tenant Architecture

* Shared PostgreSQL database
* Tenant-aware request processing
* PostgreSQL Row Level Security
* Cross-tenant access protection
* Tenant-scoped resource ownership

---

## Authentication & Session Management

Authentication uses JWT access tokens together with persistent refresh-token-backed sessions. Login creates a server-side session that can be individually revoked.


* Login
* Logout
* Token refresh
* Active session tracking
* Session revocation

---

## Organization Management

Administrators can:
* Manage users
* Create projects
* Monitor activity
* Control tenant-wide resources

The system protects organizational integrity by preventing actions that would leave a tenant without an active administrator.

---

## Project Management

* Create projects
* Update projects
* Soft delete projects
* Membership management
* Activity tracking

Projects support visibility settings:

### Private

Only project members can access the project.

### Internal

Visible across the organization but still protected by project-level permissions.

---

## Project Roles

Every project maintains its own permission model.Roles include:

**Owner**: Responsible for project governance and continuity.
**Editor**: Can contribute and manage project content but cannot freely escalate privileges.
**Viewer**: Can participate in project work with limited permissions. 

Viewers assigned to a task can update its status without receiving broader project management permissions.

---

## Task Management

Tasks belong to projects and support:

* Creation
* Assignment
* Status updates
* Priority tracking
* Ownership validation

Task operations respect both project membership and tenant boundaries. The system validates that users belong to a project before participating in project work.

---

## Dashboard & Analytics

The dashboard adapts to the user's role.

**Administrators**: 
Receive tenant-wide metrics.

**Managers**: Receive project-scoped metrics.

**Members**: Receive personal productivity metrics.

Available insights include:
* Project counts
* User counts
* Open tasks
* Completed tasks

---

## PostgreSQL Row Level Security (RLS)

Tenant isolation is enforced at the database layer using PostgreSQL Row Level Security.

---

## Layered Authorization

Permissions are determined using multiple dimensions:

* Tenant role
* Project membership
* Project role
* Project visibility
* Resource-specific rules


---

## Privilege Escalation Protection

The system prevents users from granting permissions equal to or greater than their own. For example:

* Editors cannot create new owners.
* Editors cannot create other editors.
* Editors may only add viewers.

---

## Administrative Continuity

The platform prevents actions that would destabilize an organization, such as:

* The final active administrator cannot be removed.
* Projects cannot become ownerless.
* Ownership transfers are controlled operations.

---

## Session Revocation

JWT authentication is combined with server-side session tracking.

* Logout support
* Refresh token invalidation
* Active session management

---

## Learnings

**Shared Database Multi-Tenancy**: 
Safely operating multiple organizations inside one database.

**Database-Enforced Security**: Using PostgreSQL Row Level Security as an independent security layer.

**Multi-Dimensional Authorization**: Combining tenant roles, project roles, memberships, and visibility rules.

**Ownership Governance**: Preventing invalid states such as ownerless projects or admin-less organizations.

**Session Management**: Balancing stateless JWT authentication with revocable server-side sessions.

And..
* Multi-tenant SaaS architecture
* PostgreSQL Row Level Security
* Spring Security
* JWT authentication
* Redis-backed session management
* Authorization design
* Business rule enforcement
* Ownership and governance models
* Integration testing for security guarantees


---

# Key Business Rules

Several business rules significantly influence the system design:

* A tenant must always have an active administrator.
* A project must always have an owner.
* Editors cannot escalate permissions.
* Project visibility does not automatically grant modification rights.
* Users must belong to a project before participating in project work.
* Refresh tokens must be revocable.
* Cross-tenant access is forbidden.


