# PetStore Production Foundation

## Outcome

Evolve the existing Android prototype into a production marketplace without restarting from scratch. Preserve useful screens and domain flows, but replace the monolithic runtime, local-only data, simulated fulfillment, and client-authoritative payment logic incrementally.

## Confirmed Product Decisions

- PetStore is a marketplace for independent pet shops, groomers, and veterinary hospitals.
- Tirupati is the first launch city.
- New cities, services, payment options, COD limits, and delivery providers are activated by super-admin configuration without an app release.
- Customer browsing is available without login.
- Customer identity is collected at checkout. Name, mobile number, and address are required; email is optional. Checkout accepts either mobile OTP or email OTP; without an email, mobile OTP is used.
- Merchant and captain access requires mobile OTP plus admin approval. Captain access also requires KYC approval.
- Super-admin uses email/password plus TOTP MFA.
- Customer, merchant, and captain are separate Android apps. Super-admin is a web app.
- Delivery supports PetStore captains and third-party providers.
- Products, grooming, and veterinary bookings are in the first release.
- Online payments are collected by the platform. Merchant payable amounts are recorded in a ledger and settled after fulfillment is confirmed.
- COD is supported with a Tirupati launch default of ₹1,000, configurable by super-admin.
- Supabase is the first backend. Postgres ownership, stable application contracts, and server-side business operations preserve a later Spring Boot migration path.

## Architecture Decision

Do not restart. Use an incremental strangler migration:

1. Introduce stable domain contracts beside the existing app.
2. Build customer UI against repository interfaces and debug-only fake adapters.
3. Extract customer, merchant, and captain apps into separate Gradle application modules.
4. Add the super-admin web app against the same versioned contracts.
5. Replace fake adapters with Supabase-backed adapters and server-authoritative functions.
6. Retire prototype paths only after equivalent production flows are verified.

Target repository layout:

```text
apps/
  customer-android/
  merchant-android/
  captain-android/
  admin-web/
shared/
  domain/
  api-contract/
  design-system/
backend/
  supabase/
    migrations/
    functions/
    tests/
```

The current `app` module remains operational while code moves into this structure. A large package/module rewrite is intentionally not part of this first slice because the existing build cannot currently download its Gradle distribution in this environment.

## Non-Negotiable Boundaries

- UI code never imports Supabase or payment SDKs directly.
- Release source sets never include fake repositories or seeded sessions.
- Order totals, inventory reservations, coupons, COD eligibility, payment verification, delivery assignment, and settlements are server-authoritative.
- Money crosses contracts as integer paise, never `Double`.
- City launch behavior is data, not conditional code.
- Every write operation uses idempotency keys and auditable state transitions.
- Database tables use explicit grants, RLS policies, and migrations.
- Payment provider webhooks verify signatures before changing order state.
- Storage objects have independent backup and retention procedures.

## Delivery Plan

### Slice 1 — Contract foundation

- [x] Confirm marketplace, city, client, payment, delivery, service, and authentication decisions.
- [x] Add city, role-auth, checkout, delivery-provider, and settlement contracts.
- [x] Add debug-only frontend configuration adapter.
- [x] Add pure unit tests for launch and checkout policies.
- [x] Run repository checks and document environmental blockers.

### Slice 2 — Customer frontend

- [ ] Create the PetStore design system and adaptive navigation shell.
- [ ] Implement guest home, location, catalog, grooming, and veterinary discovery.
- [ ] Implement cart and checkout identity/OTP states.
- [ ] Implement loading, empty, error, offline, and retry states.
- [ ] Add deep-link routes for products, shops, bookings, cart, and orders.

### Slice 3 — Merchant and captain frontends

- [ ] Extract merchant onboarding, catalog, inventory, service slots, and order operations.
- [ ] Build captain mobile verification, KYC, availability, assignment, navigation, proof-of-delivery, and cash reconciliation.

### Slice 4 — Super-admin web

- [ ] Build city lifecycle and capability toggles.
- [ ] Build merchant/captain approval, COD policy, delivery-provider, commission, settlement, and audit controls.

### Slice 5 — Supabase backend

- [ ] Add versioned Postgres migrations, grants, RLS, storage policies, and seed fixtures.
- [ ] Add OTP/RBAC claims and privileged server functions.
- [ ] Add inventory reservation, payment order, webhook, ledger, settlement, and delivery workflows.
- [ ] Add daily backup, off-site logical dump, storage backup, restore drill, and PITR plan.

### Slice 6 — Release hardening

- [ ] Remove demo login, simulations, committed signing material, and fake integrations.
- [ ] Configure CI, static analysis, unit/integration/E2E tests, device testing, observability, Play Integrity, and Play Store release tracks.

## Slice 1 Acceptance Criteria

- A customer-visible city is determined only by its configuration status.
- Tirupati can enable products, grooming, veterinary services, online payments, COD, and both delivery-provider types.
- COD orders over ₹1,000 are rejected by the shared policy; online payments are not affected.
- Guest browsing remains allowed while checkout requires the configured identity and verification factors.
- Captain, merchant, and super-admin access policies represent the confirmed verification and approval requirements.
- Fake configuration exists only under the Android `debug` source set.
- New policy logic has focused unit tests and does not alter the existing runtime path.

## Slice 1 Verification

- `git diff --check`: passed for tracked changes; new files were also checked for trailing whitespace.
- Mobile audit: passed; the repository script reported no errors or warnings.
- Unified lint runner: completed, but it detected no configured Kotlin linter.
- `testDebugUnitTest`: not executed because the Gradle wrapper distribution is not cached and this environment cannot reach `services.gradle.org`. The tests must run in CI or a development environment with Gradle available before merge.
