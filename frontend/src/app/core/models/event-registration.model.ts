export type FoodPreference = 'NONE' | 'VEGETARIAN' | 'VEGAN';

export interface EventRegistrationRequest {
  gdprConsent?: boolean;
  photoConsent?: boolean;
  foodPreference?: FoodPreference | null;
  transportationNeeded?: boolean;
  accommodationNeeded?: boolean;
  accommodationDays?: number;
  driverName?: string;
  driverPhone?: string;
}
