export type UserRole = 'STUDENT' | 'ADMIN';
export type ProfileStatus = 'INCOMPLETE' | 'COMPLETE' | 'VERIFIED';
export type EligibilityDecision = 'ELIGIBLE' | 'NOT_ELIGIBLE' | 'REVIEW_REQUIRED';
export type ApplicationStatus = 'NOT_STARTED' | 'ELIGIBLE' | 'NOT_ELIGIBLE' | 'APPLIED' | 'SHORTLISTED' | 'REJECTED' | 'COMPLETED';
export type ShortlistMatchStatus = 'MATCHED' | 'AMBIGUOUS' | 'UNMATCHED' | 'REVIEW_REQUIRED';
export type NotificationType = 'ELIGIBILITY' | 'SHORTLIST' | 'DEADLINE' | 'REMINDER';
export type NotificationChannel = 'WHATSAPP' | 'TELEGRAM' | 'EMAIL' | 'IN_APP';
export type NotificationStatus = 'PENDING' | 'SENT' | 'FAILED' | 'SKIPPED';
export type ReminderStatus = 'PENDING' | 'COMPLETED' | 'CANCELLED' | 'FAILED';

export interface AuthUser {
  id: number;
  email: string;
  role: UserRole;
  profileStatus: ProfileStatus;
  studentId: number | null;
  studentName: string | null;
  registrationNumber?: string | null;
  branch?: string | null;
  batch?: number | null;
}

export interface AuthResponse {
  token: string;
  tokenType: string;
  userId: number;
  email: string;
  role: UserRole;
  profileStatus: ProfileStatus;
  studentId: number | null;
  studentName: string | null;
}

export interface StudentProfile {
  studentId: number | null;
  userId: number;
  email: string;
  name: string | null;
  registrationNumber: string | null;
  neopatId: string | null;
  branch: string | null;
  batch: number | null;
  cgpa: number | null;
  phoneNumber: string | null;
  degree: string | null;
  specialization: string | null;
  standingArrears: number | null;
  gender: string | null;
  profileStatus: ProfileStatus;
  isProfileComplete: boolean;
}

export interface RoleEligibilityCard {
  roleId: number;
  roleTitle: string;
  decision: EligibilityDecision;
  criteriaExplanations: string[];
  minCgpa: number | null;
  eligibleBranches: string[];
  maxStandingArrearsAllowed: number | null;
  genderAllowed: string | null;
}

export interface StudentPlacementDriveCard {
  driveId: number;
  companyName: string;
  title: string;
  description: string | null;
  applicationDeadline: string | null;
  driveDate: string | null;
  overallEligibility: EligibilityDecision;
  applicationStatus: ApplicationStatus;
  applicationId: number | null;
  shortlistStatus: ShortlistMatchStatus | null;
  roles: RoleEligibilityCard[];
  actionRequired: string;
  isActionable: boolean;
  deadlinePassed: boolean;
}

export interface StudentPlacementDetail extends StudentPlacementDriveCard {
  testDate: string | null;
  interviewDate: string | null;
  appliedAt: string | null;
  shortlistCandidateName: string | null;
}

export interface StudentApplication {
  applicationId: number;
  driveId: number;
  companyName: string;
  driveTitle: string;
  roleId: number | null;
  roleTitle: string | null;
  status: ApplicationStatus;
  appliedAt: string | null;
  shortlistStatus: ShortlistMatchStatus | null;
  deadline: string | null;
  createdAt: string;
}

export interface StudentNotification {
  id: number;
  idempotencyKey: string;
  notificationType: NotificationType;
  channel: NotificationChannel;
  status: NotificationStatus;
  driveId: number | null;
  companyName: string | null;
  roleId: number | null;
  roleTitle: string | null;
  messagePayload: string | null;
  sentAt: string | null;
  createdAt: string;
}

export interface StudentReminder {
  id: number;
  driveId: number;
  companyName: string;
  roleId: number | null;
  roleTitle: string | null;
  scheduledFor: string;
  intervalMinutes: number;
  remindersSent: number;
  maxReminders: number;
  cancelReason: string | null;
  lastReminderAt: string | null;
  status: ReminderStatus;
  isActive: boolean;
  completedAt: string | null;
}

export interface PageResponse<T> {
  content: T[];
  pageNumber: number;
  pageSize: number;
  totalElements: number;
  totalPages: number;
  isLast: boolean;
}

export interface TelegramStatusResponse {
  linked: boolean;
  telegramUsername: string | null;
  linkedAt: string | null;
}

export interface TelegramLinkResponse {
  token: string;
  deepLink: string;
  expiresAt: string;
}

