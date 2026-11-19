# Community Maps abuse-control boundary

Repository configuration declares one API service with one fixed published port;
no replica count, external limiter or Cloudflare Tunnel configuration is present.
This does not verify the live NAS topology, which was not accessed.

Fastify does not trust forwarded headers. Arbitrary X-Forwarded-For and
CF-Connecting-IP headers must not become identity sources. Behind a tunnel/proxy,
request.ip may identify that proxy: limits and guest counts conservatively group
its users. Do not enable blanket trustProxy. A production rollout must verify the
actual hop chain, restrict direct origin access and explicitly configure trusted
hops before claiming per-client limits. IPv4/IPv6 addresses represent network
addresses, not people; IPv6 privacy-address rotation and distributed IPs can evade
network-based uniqueness. Anonymous Firebase identities share guest IP counting.
Registered accounts remain Sybilable; this is not unique-human counting.

The existing IP limiter is process-local and resets on restart. It provides bounded
single-process burst protection, including authenticated calls, not durable quotas.
Publishing now additionally locks the owning user row and counts publications in
a rolling 24-hour window, capped at 100 per account. Unpublishing does not refund
the quota. The check and insert are atomic, survive API restart, and existing
idempotent retries remain retrievable at the quota. This requires no new service.

Multiple replicas need a shared limiter and verified proxy boundary before rollout;
this is a deployment blocker, not an implicit Redis dependency added by this branch.
Single-replica public rollout still requires signup-abuse monitoring, representative
load tests and actual tunnel configuration validation. Ratings are one per registered
eligible account/map; owner exclusion is not protection against alternate accounts.
Import actors are durably unique per map; process-local limits are a second layer.
The stable HMAC secret must not rotate routinely. Secret rotation changes actor
identities and may permit recounting; never store raw addresses to work around this.
