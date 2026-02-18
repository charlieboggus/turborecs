import { jwtVerify } from "jose"
import { NextResponse } from "next/server"
import type { NextRequest } from "next/server"

const secret = new TextEncoder().encode(process.env.TURBORECS_JWT_SECRET!)
const PUBLIC_PATHS = ["/login"]

export default async function proxy(request: NextRequest) {
  const { pathname } = request.nextUrl
  const isPublic = PUBLIC_PATHS.some((p) => pathname.startsWith(p))

  if (isPublic) {
    return NextResponse.next()
  }

  const token = request.cookies.get("auth_session")?.value

  if (!token) {
    return redirectToLogin(request)
  }

  try {
    await jwtVerify(token, secret)
    return NextResponse.next()
  } catch (e) {
    // expired or tampered
    console.log(e)
    return redirectToLogin(request)
  }
}

function redirectToLogin(request: NextRequest) {
  const loginUrl = new URL("/login", request.url)
  loginUrl.searchParams.set("redirect", request.nextUrl.pathname)
  return NextResponse.redirect(loginUrl)
}

export const config = {
  matcher: ["/((?!_next/static|_next/image|favicon.ico|.*\\..*$).*)"],
}
