import {
  AuthResponse,
  AuthUser,
  PageResponse,
  StudentApplication,
  StudentNotification,
  StudentPlacementDetail,
  StudentPlacementDriveCard,
  StudentProfile,
  StudentReminder,
  TelegramLinkResponse,
  TelegramStatusResponse,
} from './types';

// NEXT_PUBLIC_API_URL must be set in production. Do NOT rely on localhost fallback in deployed builds.
const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080/api/v1';

class ApiClient {
  private getToken(): string | null {
    if (typeof window !== 'undefined') {
      return localStorage.getItem('placementos_token');
    }
    return null;
  }

  private async request<T>(endpoint: string, options: RequestInit = {}): Promise<T> {
    const token = this.getToken();
    const headers: Record<string, string> = {
      'Content-Type': 'application/json',
      Accept: 'application/json',
      ...(options.headers as Record<string, string>),
    };

    if (token) {
      headers['Authorization'] = `Bearer ${token}`;
    }

    const url = `${API_BASE_URL}${endpoint}`;
    const response = await fetch(url, {
      ...options,
      headers,
    });

    if (!response.ok) {
      let errorMessage = `HTTP Error ${response.status}: ${response.statusText}`;
      try {
        const errorBody = await response.json();
        errorMessage = errorBody.message || errorBody.error || errorMessage;
      } catch {
        // use fallback statusText
      }
      throw new Error(errorMessage);
    }

    // Return empty object for 204 No Content
    if (response.status === 204) {
      return {} as T;
    }

    return response.json();
  }

  // ---------------------------------------------------------------------------
  // Authentication
  // ---------------------------------------------------------------------------

  async register(data: { email: string; password: string }): Promise<AuthResponse> {
    return this.request<AuthResponse>('/auth/register', {
      method: 'POST',
      body: JSON.stringify(data),
    });
  }

  async login(data: { email: string; password: string }): Promise<AuthResponse> {
    return this.request<AuthResponse>('/auth/login', {
      method: 'POST',
      body: JSON.stringify(data),
    });
  }

  async getMe(): Promise<AuthUser> {
    return this.request<AuthUser>('/auth/me');
  }

  // ---------------------------------------------------------------------------
  // Student Profile & Onboarding
  // ---------------------------------------------------------------------------

  async getProfile(): Promise<StudentProfile> {
    return this.request<StudentProfile>('/student/profile');
  }

  async onboard(data: {
    name: string;
    registrationNumber: string;
    neopatId?: string;
    branch: string;
    batch: number;
    cgpa: number;
    phoneNumber?: string;
    degree: string;
    specialization?: string;
    standingArrears?: number;
    gender?: string;
  }): Promise<StudentProfile> {
    return this.request<StudentProfile>('/student/onboarding', {
      method: 'POST',
      body: JSON.stringify(data),
    });
  }

  async updateProfile(data: {
    name?: string;
    phoneNumber?: string;
    specialization?: string;
    standingArrears?: number;
    gender?: string;
  }): Promise<StudentProfile> {
    return this.request<StudentProfile>('/student/profile', {
      method: 'PUT',
      body: JSON.stringify(data),
    });
  }

  // ---------------------------------------------------------------------------
  // Placements & Role Breakdown
  // ---------------------------------------------------------------------------

  async getPlacements(page = 0, size = 10): Promise<PageResponse<StudentPlacementDriveCard>> {
    return this.request<PageResponse<StudentPlacementDriveCard>>(`/student/placements?page=${page}&size=${size}`);
  }

  async getPlacementDetail(driveId: number): Promise<StudentPlacementDetail> {
    return this.request<StudentPlacementDetail>(`/student/placements/${driveId}`);
  }

  // ---------------------------------------------------------------------------
  // Applications & Apply Action
  // ---------------------------------------------------------------------------

  async getApplications(page = 0, size = 10): Promise<PageResponse<StudentApplication>> {
    return this.request<PageResponse<StudentApplication>>(`/student/applications?page=${page}&size=${size}`);
  }

  async applyToApplication(applicationId: number): Promise<{ applicationId: number; status: string; appliedAt: string }> {
    return this.request<{ applicationId: number; status: string; appliedAt: string }>(
      `/student/applications/${applicationId}/apply`,
      {
        method: 'POST',
      }
    );
  }

  // ---------------------------------------------------------------------------
  // Notifications & Reminders
  // ---------------------------------------------------------------------------

  async getNotifications(page = 0, size = 10, type?: string): Promise<PageResponse<StudentNotification>> {
    const typeParam = type ? `&type=${type}` : '';
    return this.request<PageResponse<StudentNotification>>(`/student/notifications?page=${page}&size=${size}${typeParam}`);
  }

  async getReminders(page = 0, size = 10): Promise<PageResponse<StudentReminder>> {
    return this.request<PageResponse<StudentReminder>>(`/student/reminders?page=${page}&size=${size}`);
  }

  async stopReminder(reminderId: number, reason = 'STUDENT_PORTAL_STOP'): Promise<{ status: string; reason: string }> {
    return this.request<{ status: string; reason: string }>(`/student/reminders/${reminderId}/stop?reason=${reason}`, {
      method: 'POST',
    });
  }

  // ---------------------------------------------------------------------------
  // Telegram Integration
  // ---------------------------------------------------------------------------

  async getTelegramStatus(): Promise<TelegramStatusResponse> {
    return this.request<TelegramStatusResponse>('/student/telegram/status');
  }

  async generateTelegramLinkToken(): Promise<TelegramLinkResponse> {
    return this.request<TelegramLinkResponse>('/student/telegram/link-token', {
      method: 'POST',
    });
  }

  async unlinkTelegram(): Promise<{ status: string }> {
    return this.request<{ status: string }>('/student/telegram/unlink', {
      method: 'POST',
    });
  }

  // ---------------------------------------------------------------------------
  // Account Management
  // ---------------------------------------------------------------------------

  /**
   * Authenticated account deletion. Derives user identity from the JWT principal.
   * Does NOT accept userId/email from the client.
   */
  async deleteAccount(): Promise<void> {
    await this.request<void>('/auth/account', {
      method: 'DELETE',
    });
  }
}

export const api = new ApiClient();

