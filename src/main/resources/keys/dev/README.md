# Dev JWT keys

These keys are FOR LOCAL DEVELOPMENT ONLY. They are committed to the repo for
ergonomics (running locally without env-var setup).

In production, set `SECURED_X_JWT_PRIVATE_KEY` and `SECURED_X_JWT_PUBLIC_KEY`
env vars to override these defaults.

To regenerate:

```bash
openssl genpkey -algorithm RSA -out private.pem -pkeyopt rsa_keygen_bits:2048
openssl rsa -pubout -in private.pem -out public.pem
```
