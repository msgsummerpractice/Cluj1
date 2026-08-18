export interface AttendanceRecord {
  id: string;
  checkInTime: string;
  user: {
    firstName: string;
    lastName: string;
  };
}
