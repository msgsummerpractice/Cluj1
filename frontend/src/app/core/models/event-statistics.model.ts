import { ParticipantDetail } from './participant-details.model';

export interface EventStatistics {
  invitedCount: number;
  registrationCount: number;
  participantCount: number;
  registrationTimeDistribution: { [key: string]: number };
  foodPreferencePercentages: { [key: string]: number };
  accommodationPercentage: number;
  transportPercentage: number;
  photoConsentPercentage: number;
  participants: ParticipantDetail[];
}
