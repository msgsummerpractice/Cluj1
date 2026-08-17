export interface UserProfile {
  firstName: string | null;
  lastName: string | null;
  email: string;
  role: string;
  userLocation: string | null;
  profilePicture: Blob | null;

}
