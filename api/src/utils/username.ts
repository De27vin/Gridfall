export class UsernameValidationError extends Error {
  constructor(message: string) {
    super(message);
    this.name = "UsernameValidationError";
  }
}

export interface NormalizedUsername {
  username: string;
  usernameNormalized: string;
}

const allowedUsernamePattern = /^[A-Za-z0-9 _-]{3,16}$/;

export function normalizeUsername(input: string): NormalizedUsername {
  const username = input.trim().replace(/\s+/g, " ");
  if (!allowedUsernamePattern.test(username)) {
    throw new UsernameValidationError(
      "Username must be 3-16 characters and contain only letters, numbers, spaces, underscore, or minus"
    );
  }

  return {
    username,
    usernameNormalized: username.toLowerCase()
  };
}

