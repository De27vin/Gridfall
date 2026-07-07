import { applicationDefault, getApps, initializeApp } from "firebase-admin/app";
import { getAuth } from "firebase-admin/auth";
import { env } from "./env";

export interface AuthenticatedUser {
  firebaseUid: string;
  email?: string;
  isAnonymous: boolean;
}

function ensureFirebaseInitialized(): void {
  if (getApps().length > 0) return;
  if (!env.firebaseProjectId) {
    throw new Error("FIREBASE_PROJECT_ID is required to verify Firebase tokens");
  }
  if (!env.googleApplicationCredentials) {
    throw new Error("GOOGLE_APPLICATION_CREDENTIALS is required to verify Firebase tokens");
  }

  initializeApp({
    credential: applicationDefault(),
    projectId: env.firebaseProjectId
  });
}

export async function verifyFirebaseToken(idToken: string): Promise<AuthenticatedUser> {
  ensureFirebaseInitialized();
  const decoded = await getAuth().verifyIdToken(idToken);
  const signInProvider = decoded.firebase?.sign_in_provider;

  return {
    firebaseUid: decoded.uid,
    email: decoded.email,
    isAnonymous: signInProvider === "anonymous"
  };
}

