# Dev Profile - Swagger UI Test Guideline

## Prerequisites

### 1. Start Application with Dev Profile

```bash
./gradlew bootRun --args='--spring.profiles.active=dev'
```

Console output should show:
```
=== Dev test data initialized ===
Accounts: alice(manager), bob(member), charlie(observer)
Hobbies: photography-club, coding-lab, draft-hobby
Use POST /api/v1/dev/token/{nickname} to get JWT tokens
```

### 2. Open Swagger UI

```
http://localhost:8080/swagger-ui.html
```

---

## Test Data Overview

### Accounts

| Nickname | Role | Email | Description |
|----------|------|-------|-------------|
| alice | Manager | alice@dev.local | Hobby creator/manager |
| bob | Member | bob@dev.local | Joined hobbies, enrolled in events |
| charlie | Observer | charlie@dev.local | Clean state, no associations |

### Hobbies

| Path | Title | Published | Recruiting | Manager | Members |
|------|-------|-----------|------------|---------|---------|
| photography-club | Photography Club | O | O | alice | alice, bob |
| coding-lab | Coding Lab | O | X | alice | alice |
| draft-hobby | Draft Hobby | X | X | alice | - |

### Events

| Hobby | Title | Type | Limit | End Enrollment |
|-------|-------|------|-------|----------------|
| photography-club | Weekend Photo Walk | FCFS | 10 | +7 days |
| photography-club | Photo Contest | CONFIRMATIVE | 5 | +14 days |
| coding-lab | Code Meetup | FCFS | 20 | +3 days |
| coding-lab | Hackathon | CONFIRMATIVE | 8 | +10 days |

### Enrollments (bob)

| Event | Accepted | Attended | Test Purpose |
|-------|----------|----------|--------------|
| Weekend Photo Walk (FCFS) | true | false | checkin / cancel-checkin |
| Photo Contest (CONFIRMATIVE) | false | false | accept / reject |
| Code Meetup (FCFS) | true | false | disenroll |

### Notifications

| Account | Type | Checked | Test Purpose |
|---------|------|---------|--------------|
| alice | HOBBY_CREATED | false | unread-count, mark-as-read |
| alice | HOBBY_UPDATED | false | list query |
| bob | EVENT_ENROLLMENT | false | checked=false filter |
| bob | HOBBY_CREATED | true | checked=true filter, delete |

---

## Token Workflow

A token must be issued before testing any authenticated API endpoint.

### Token Issue

1. **Dev** tag > `GET /api/v1/dev/accounts` > Try it out > Execute
2. **Dev** tag > `POST /api/v1/dev/token/{nickname}` > Enter nickname > Execute
3. Copy `accessToken` value from response

### Token Register

1. Click **Authorize** button at the top of Swagger UI
2. Enter `Bearer {accessToken}` in the Value field
3. Click **Authorize**

### Token Switch

1. **Authorize** > **Logout** click
2. Issue a new token for a different account and register it

---

## Test Scenarios

### Phase 1: alice (Manager)

> Token: `POST /api/v1/dev/token/alice`

#### 1-1. Account API

| # | Endpoint | Expected | Check |
|---|----------|----------|-------|
| 1 | `GET /api/v1/accounts/me` | alice profile returned | [ ] |
| 2 | `GET /api/v1/accounts/alice` | public profile | [ ] |
| 3 | `GET /api/v1/accounts/bob` | bob's public profile | [ ] |

#### 1-2. Hobby Read API

> **Sort Parameter Usage**
>
> The Pageable `sort` parameter uses the format `property,direction`.
>
> | Value | Description |
> |-------|-------------|
> | `publishedDateTime,desc` | Published date descending (default) |
> | `title,asc` | Title ascending |
> | `memberCount,desc` | Member count descending |
>
> - Leave empty to use the default: `publishedDateTime,desc`
> - Multiple sort: add one entry per sort field (e.g. `title,asc` + `memberCount,desc`)

| # | Endpoint | Expected | Check |
|---|----------|----------|-------|
| 1 | `GET /api/v1/hobbies` | published hobbies (2 items) | [ ] |
| 2 | `GET /api/v1/hobbies/photography-club` | detail with tags, zones | [ ] |
| 3 | `GET /api/v1/hobbies/coding-lab` | detail | [ ] |
| 4 | `GET /api/v1/hobbies/draft-hobby` | detail (unpublished) | [ ] |
| 5 | `GET /api/v1/hobbies/photography-club/members` | 2 members (alice, bob) | [ ] |
| 6 | `GET /api/v1/hobbies/coding-lab/members` | 1 member (alice) | [ ] |

#### 1-3. Hobby Management API (Manager Only)

| # | Endpoint | Expected | Check |
|---|----------|----------|-------|
| 1 | `POST` photography-club recruiting stop | recruiting=false | [ ] |
| 2 | `POST` photography-club recruiting start | recruiting=true (cooldown cleared) | [ ] |
| 3 | `POST` draft-hobby publish | published=true | [ ] |

#### 1-4. Event API

| # | Endpoint | Expected | Check |
|---|----------|----------|-------|
| 1 | `GET /api/v1/hobbies/photography-club/events` | 2 events | [ ] |
| 2 | `GET /api/v1/hobbies/coding-lab/events` | 2 events | [ ] |
| 3 | Event detail for each | enrollments list visible | [ ] |

#### 1-5. Notification API

| # | Endpoint | Expected | Check |
|---|----------|----------|-------|
| 1 | Unread count | 2 (HOBBY_CREATED, HOBBY_UPDATED) | [ ] |
| 2 | Notification list (unchecked) | 2 items | [ ] |
| 3 | Mark as read | checked=true | [ ] |
| 4 | Unread count again | 0 | [ ] |

---

### Phase 2: bob (Member)

> Token: `POST /api/v1/dev/token/bob`

#### 2-1. Account API

| # | Endpoint | Expected | Check |
|---|----------|----------|-------|
| 1 | `GET /api/v1/accounts/me` | bob profile | [ ] |

#### 2-2. Enrollment - Photo Walk (FCFS, accepted=true)

| # | Action | Expected | Check |
|---|--------|----------|-------|
| 1 | Check-in | attended=true | [ ] |
| 2 | Cancel check-in | attended=false | [ ] |

#### 2-3. Enrollment - Photo Contest (CONFIRMATIVE, accepted=false)

> alice token required for accept/reject

| # | Action | Token | Expected | Check |
|---|--------|-------|----------|-------|
| 1 | Accept enrollment | alice | accepted=true | [ ] |
| 2 | Reject enrollment | alice | accepted=false | [ ] |

#### 2-4. Enrollment - Code Meetup (FCFS, accepted=true)

| # | Action | Expected | Check |
|---|--------|----------|-------|
| 1 | Disenroll | enrollment removed | [ ] |

#### 2-5. New Enrollment

| # | Action | Expected | Check |
|---|--------|----------|-------|
| 1 | Enroll in Hackathon | new enrollment created | [ ] |

#### 2-6. Notification API

| # | Endpoint | Expected | Check |
|---|----------|----------|-------|
| 1 | Notification list (unchecked) | 1 item (EVENT_ENROLLMENT) | [ ] |
| 2 | Notification list (checked) | 1 item (HOBBY_CREATED) | [ ] |
| 3 | Delete checked notifications | checked notifications removed | [ ] |

---

### Phase 3: charlie (Observer)

> Token: `POST /api/v1/dev/token/charlie`

#### 3-1. Clean State Verification

| # | Endpoint | Expected | Check |
|---|----------|----------|-------|
| 1 | `GET /api/v1/accounts/me` | charlie profile, no associations | [ ] |
| 2 | Unread notification count | 0 | [ ] |

#### 3-2. Hobby Join

| # | Action | Expected | Check |
|---|--------|----------|-------|
| 1 | Join photography-club | member added (recruiting=true) | [ ] |
| 2 | `GET /api/v1/hobbies/photography-club/members` | 3 members | [ ] |

#### 3-3. Event Enrollment

| # | Action | Expected | Check |
|---|--------|----------|-------|
| 1 | Enroll in Weekend Photo Walk | enrollment created | [ ] |

#### 3-4. Hobby Leave

| # | Action | Expected | Check |
|---|--------|----------|-------|
| 1 | Leave photography-club | member removed | [ ] |

---

### Phase 4: Permission Denied Cases

#### 4-1. Unauthorized Access (No Token)

| # | Action | Expected | Check |
|---|--------|----------|-------|
| 1 | Authorize > Logout (remove token) | - | [ ] |
| 2 | `GET /api/v1/accounts/me` | 401 Unauthorized | [ ] |
| 3 | `GET /api/v1/hobbies` (public) | 200 OK | [ ] |
| 4 | `GET /api/v1/hobbies/photography-club` (public) | 200 OK | [ ] |

#### 4-2. Forbidden Access (Wrong Role)

| # | Action | Token | Expected | Check |
|---|--------|-------|----------|-------|
| 1 | Manage coding-lab settings | bob | 403 Forbidden | [ ] |
| 2 | Accept/reject enrollment | bob | 403 Forbidden | [ ] |

---

## Checklist Summary

- [ ] Phase 1: alice (Manager) - all passed
- [ ] Phase 2: bob (Member) - all passed
- [ ] Phase 3: charlie (Observer) - all passed
- [ ] Phase 4: Permission Denied - all passed
