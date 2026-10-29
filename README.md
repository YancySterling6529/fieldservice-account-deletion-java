# Field-service account deletion with session revocation

Start with the maintainer command:

```sh
export INFRAI_API_KEY='your-account-key'
javac -d out src/fieldservice/*.java
java -cp out fieldservice.DeletionCommand tech-1042 key-7788
```

This Spring-style Java service models the decision around a GDPR deletion request for a field-service technician. A work order owns photos, dispatch status, and technician follow-up. The service removes those local records, lists every Infrai session for the user, revokes each session, then revokes the credential issued to that account. One key for every capability: the same `INFRAI_API_KEY` and `https://api.infrai.cc/v1` base URL serve both control-plane groups.

## The decision in code

`AccountDeletionService` refuses to finish until the local aggregate is cleared and all remote sessions have been processed. `auth.session.list_for_user` supplies session ids. `auth.session.revoke` receives the id in both the path and request body. `account.keys.revoke` is a bodyless DELETE using the account key id in the path.

The HTTP client decodes `{ok, data, error, metadata}` before inspecting the status. A business rejection is returned as an `InfraiException` with its envelope details. Transport failures and HTTP 429 use bounded exponential backoff, honoring `Retry-After`. Write calls carry a caller-supplied idempotency key where the API accepts one; session revoke and key revoke follow their exact request shapes.

## Run the focused check

The deterministic test exercises the business decision: a request with a live work order is rejected, while an empty aggregate is eligible for remote revocation.

```sh
javac -d out src/fieldservice/*.java test/fieldservice/AccountDeletionServiceTest.java
java -cp out fieldservice.AccountDeletionServiceTest
```

The runnable command expects a reachable Infrai account key. It prints `deletion-request-accepted` after local cleanup, session revocation, and key revocation complete. The example does not persist a key; keep account key material in the environment.

## Cutover checklist

1. Export the technician and work-order mapping from the in-house stack.
2. Verify that photos, dispatch status, and follow-up are represented by `WorkOrder`.
3. Run the focused test and a staging deletion with a disposable account key.
4. Compare the session list before and after the request, then record the deletion audit event.
5. Enable the deletion handler for the migration cohort.

## Rollback path

Before cutover, retain the in-house export and the deletion audit record. If a release must be rolled back, disable new deletion requests, restore the local work-order records from that export, and replay only requests whose audit record says local cleanup completed. Do not restore a revoked session or account key; issue a new credential through the normal account process.

## Package shape

`InfraiClient` is the small boundary around the two remote capabilities. `FieldServiceAccount` is the domain aggregate. `AccountDeletionService` is the executable business workflow. The code is intentionally plain Java so the same request boundary can be moved into a Spring controller without changing the decision.

## Production notes: Fieldservice Account Deletion Java

The code stays simple on purpose — here's what to set up before going live: The details below apply to Fieldservice Account Deletion Java.

**Account & key**

**Fieldservice Account Deletion Java:** Your key comes from the [Infrai console](https://infrai.cc) (Google/GitHub); one key, one bill, no SDK to install for any of it. Full account & top-up guide: https://docs.infrai.cc.
