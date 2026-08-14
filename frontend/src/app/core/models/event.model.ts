export interface Event {
  id: string;
  name: string;
  description?: string;
  startDate: string;
  endDate: string;
  type: 'INTERNAL' | 'EXTERNAL' | 'LOCAL';
  location: 'ALL' | 'CLUJ' | 'TIMISOARA' | 'MURES' | null;
  status: 'DRAFT' | 'PUBLISHED' | 'COMPLETED';
  foodProvided?: boolean;
}
