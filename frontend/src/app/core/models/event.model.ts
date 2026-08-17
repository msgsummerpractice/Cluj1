export interface Event {
  id: string;
  name: string;
  description?: string | null;
  startDate: string;
  endDate: string;
  registrationEndDate?: string | null;
  type: 'INTERNAL' | 'EXTERNAL' | 'LOCAL';
  location: 'ALL' | 'CLUJ' | 'TIMISOARA' | 'MURES' | null;
  status: 'DRAFT' | 'PUBLISHED' | 'COMPLETED';
  foodProvided?: boolean | null;
}
