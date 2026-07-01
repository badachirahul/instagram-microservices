import { SignJWT } from 'jose'

// ---------------------------------------------------------------------------
// DEV-ONLY login. Real auth lives in Divya's user-service (/api/auth/login),
// which isn't built yet — so for now we mint a JWT in the browser with the
// shared HS256 secret (see docs/jwt-contract.md). Same shape user-service will
// issue: sub = userId (uuid), username, exp (24h). Swap this for a real login
// call once user-service exists.
// ---------------------------------------------------------------------------
const SECRET = new TextEncoder().encode(
  import.meta.env.VITE_JWT_SECRET ||
    'I0KQjwMx0u/vwwt0/UQ2bu9uePtTQm8aysfPmd/OucvzNt7sDrNwVLeTcJPyNuMu',
)

export async function devLogin(username) {
  const userId = crypto.randomUUID()
  const token = await new SignJWT({ username })
    .setProtectedHeader({ alg: 'HS256', typ: 'JWT' })
    .setSubject(userId)
    .setIssuedAt()
    .setExpirationTime('24h')
    .sign(SECRET)
  return { userId, username, token }
}
