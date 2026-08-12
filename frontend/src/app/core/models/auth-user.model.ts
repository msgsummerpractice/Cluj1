export type UserRole = 'PARTICIPANT' | 'MARKETING_ORGANIZER' | 'HR_USER' | 'ADMIN';

export interface AuthUser {
  id: string;
  email: string;
  role: UserRole;
  exp: number;
  iat: number;
}
