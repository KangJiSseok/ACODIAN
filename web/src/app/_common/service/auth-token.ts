// 백엔드는 Authorization header를 "Bearer <token>" 형태로 내려줍니다.
// axios/auth service에서는 실제 access token 문자열만 필요하므로 prefix를 제거합니다.
export function extractAccessToken(authorizationHeader?: string | null) {
  if (!authorizationHeader) {
    return null;
  }

  const trimmedHeader = authorizationHeader.trim();
  if (!trimmedHeader) {
    return null;
  }

  return trimmedHeader.startsWith("Bearer ")
    ? trimmedHeader.slice("Bearer ".length)
    : trimmedHeader;
}
