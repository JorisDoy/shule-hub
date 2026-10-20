"use client";

import { useEffect, useRef, useState, type ReactNode } from "react";
import { AuthProvider, hasAuthParams, useAuth } from "react-oidc-context";
import { LogIn } from "lucide-react";
import { Button } from "@/components/ui/Button";
import { readStorage, removeStorage, STORAGE_KEYS, writeStorage } from "@/lib/storage";
import { errorMessage } from "@/lib/api/problem";
import { FullScreen } from "@/features/session/FullScreen";

const AUTHORITY = process.env.NEXT_PUBLIC_OIDC_AUTHORITY || "http://localhost:8180/realms/shulehub";
const CLIENT_ID = process.env.NEXT_PUBLIC_OIDC_CLIENT_ID || "shulehub-web";

function origin(): string {
  return typeof window === "undefined" ? "" : window.location.origin;
}

/** Remove ?code=&state=&session_state= from the address bar once the code has been exchanged. */
function onSigninCallback(): void {
  window.history.replaceState(window.history.state, document.title, window.location.pathname);
}

/** OIDC (authorization code + PKCE, public client) against Keycloak. */
export function AuthRoot({ children }: { children: ReactNode }) {
  return (
    <AuthProvider
      authority={AUTHORITY}
      client_id={CLIENT_ID}
      redirect_uri={`${origin()}/app`}
      post_logout_redirect_uri={`${origin()}/`}
      response_type="code"
      scope="openid profile email"
      automaticSilentRenew
      onSigninCallback={onSigninCallback}
    >
      <AuthGate>{children}</AuthGate>
    </AuthProvider>
  );
}

/** Remembers the deep link the person opened so we can return there after Keycloak sends them to /app. */
export function rememberReturnTo(): void {
  const { pathname, search } = window.location;
  if (pathname.startsWith("/app") && pathname !== "/app") writeStorage(STORAGE_KEYS.returnTo, pathname + search, "session");
}

export function takeReturnTo(): string | null {
  const value = readStorage(STORAGE_KEYS.returnTo, "session");
  removeStorage(STORAGE_KEYS.returnTo, "session");
  return value && value.startsWith("/app") ? value : null;
}

function AuthGate({ children }: { children: ReactNode }) {
  const auth = useAuth();
  const attempted = useRef(false);
  const [slow, setSlow] = useState(false);

  const shouldRedirect = !auth.isLoading && !auth.isAuthenticated && !auth.activeNavigator && !auth.error && !hasAuthParams();

  useEffect(() => {
    if (!shouldRedirect || attempted.current) return;
    attempted.current = true;
    rememberReturnTo();
    void auth.signinRedirect();
  }, [shouldRedirect, auth]);

  // On slow connections the redirect can take a while; offer a manual button after a few seconds.
  useEffect(() => {
    if (!shouldRedirect) return;
    const t = setTimeout(() => setSlow(true), 5000);
    return () => clearTimeout(t);
  }, [shouldRedirect]);

  if (auth.error) {
    return (
      <FullScreen
        title="We couldn't sign you in"
        action={
          <Button
            icon={<LogIn className="size-4" aria-hidden />}
            onClick={() => {
              window.history.replaceState(null, "", window.location.pathname);
              void auth.removeUser().finally(() => auth.signinRedirect());
            }}
          >
            Sign in again
          </Button>
        }
      >
        <p>{friendlyAuthError(auth.error)}</p>
      </FullScreen>
    );
  }

  if (auth.isAuthenticated) return <>{children}</>;

  return (
    <FullScreen title={auth.activeNavigator === "signoutRedirect" ? "Signing you out" : "Opening Shule Hub"} busy>
      <p>{auth.isLoading ? "Checking your sign-in…" : "Taking you to the sign-in page…"}</p>
      {shouldRedirect && slow ? (
        <Button variant="secondary" className="mt-4" onClick={() => void auth.signinRedirect()}>
          Continue to sign in
        </Button>
      ) : null}
    </FullScreen>
  );
}

function friendlyAuthError(error: Error): string {
  const msg = errorMessage(error);
  if (/failed to fetch|networkerror|load failed/i.test(msg)) {
    return "The sign-in service isn't reachable right now. Check your connection and try again.";
  }
  if (/no matching state/i.test(msg)) {
    return "This sign-in link has already been used or has expired. Sign in again to continue.";
  }
  return msg;
}
