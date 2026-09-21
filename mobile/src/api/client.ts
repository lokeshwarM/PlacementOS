import { getToken, clearAuthStorage } from '../storage/secureStore';
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
} from '../types';

const API_BASE_URL = process.env.EXPO_PUBLIC_API_URL || 'http://10.0.2.2:8080/api/v1';

class ApiClient {
  private async request<T>(endpoint: string, options: RequestInit = {}): Promise<T> {
    const url = `${API_BASE_URL}${endpoint}`;
    const token = await getToken();

    const headers: Record<string, string> = {
      'Content-Type': 'application/json',
      Accept: 'application/json',
      ...(options.headers as Record<string, string>),
    };

    if (token) {
      headers['Authorization'] = `Bearer ${token}`;
    }

    const response = await fetch(url, {
      ...options,
      headers,
    });

    if (response.status === 401) {
      // Unauthorized: clear auth token
      await clearAuthStorage();
      throw new Error('Session expired. Please log in again.');
    }

    if (!response.ok) {
      let errorMessage = `Request failed (${response.status})`;
      try {
        const errorData = await response.json();
        errorMessage = errorData.message || errorData.error || errorMessage;
      } catch {
        // use default error message
      }
      throw new Error(errorMessage);
    }

    // Return empty object for 204 or void responses
    if (response.status === 204) {
      return {} as T;
    }

    return response.json();
  }

  // ---------------------------------------------------------------------------
  // Auth
  // ---------------------------------------------------------------------------

  async login(email: string, password: string): Promise<AuthResponse> {
    return this.request<AuthResponse>('/auth/login', {
      method: 'POST',
      body: JSON.stringify({ email, password }),
    });
  }

  async register(email: string, password: string): Promise<AuthResponse> {
    return this.request<AuthResponse>('/auth/register', {
      method: 'POST',
      body: JSON.stringify({ email, password }),
    });
  }

  async getCurrentUser(): Promise<AuthUser> {
    return this.request<AuthUser>('/auth/me');
  }

  // ---------------------------------------------------------------------------
  // Profile & Onboarding
  // ---------------------------------------------------------------------------

  async getProfile(): Promise<StudentProfile> {
    return this.request<StudentProfile>('/student/profile');
  }

  async onboard(data: {
    registrationNumber: string;
    neopatId: string;
    branch: string;
    batch: number;
    cgpa: number;
    phoneNumber?: string;
    gender?: string;
    specialization?: string;
    degree?: string;
  }): Promise<StudentProfile> {
    return this.request<StudentProfile>('/student/onboarding', {
      method: 'POST',
      body: JSON.stringify(data),
    });
  }

  async updateProfile(data: {
    phoneNumber?: string;
    gender?: string;
    specialization?: string;
  }): Promise<StudentProfile> {
    return this.request<StudentProfile>('/student/profile', {
      method: 'PUT',
      body: JSON.stringify(data),
    });
  }

  // ---------------------------------------------------------------------------
  // Placements
  // ---------------------------------------------------------------------------

  async getPlacements(page = 0, size = 10): Promise<PageResponse<StudentPlacementDriveCard>> {
    return this.request<PageResponse<StudentPlacementDriveCard>>(
      `/student/placements?page=${page}&size=${size}`
    );
  }

  async getPlacementDetail(driveId: number): Promise<StudentPlacementDetail> {
    return this.request<StudentPlacementDetail>(`/student/placements/${driveId}`);
  }

  // ---------------------------------------------------------------------------
  // Applications
  // ---------------------------------------------------------------------------

  async getApplications(page = 0, size = 10): Promise<PageResponse<StudentApplication>> {
    return this.request<PageResponse<StudentApplication>>(
      `/student/applications?page=${page}&size=${size}`
    );
  }

  async getApplication(applicationId: number): Promise<StudentApplication> {
    return this.request<StudentApplication>(`/student/applications/${applicationId}`);
  }

  async apply(applicationId: number): Promise<{ applicationId: number; status: string; appliedAt: string }> {
    return this.request<{ applicationId: number; status: string; appliedAt: string }>(
      `/student/applications/${applicationId}/apply`,
      { method: 'POST' }
    );
  }

  // ---------------------------------------------------------------------------
  // Notifications & Reminders
  // ---------------------------------------------------------------------------

  async getNotifications(
    page = 0,
    size = 10,
    type?: string
  ): Promise<PageResponse<StudentNotification>> {
    const query = type ? `&type=${type}` : '';
    return this.request<PageResponse<StudentNotification>>(
      `/student/notifications?page=${page}&size=${size}${query}`
    );
  }

  async getReminders(page = 0, size = 10): Promise<PageResponse<StudentReminder>> {
    return this.request<PageResponse<StudentReminder>>(
      `/student/reminders?page=${page}&size=${size}`
    );
  }

  async stopReminder(reminderId: number, reason = 'STUDENT_PORTAL_STOP'): Promise<{ status: string; reason: string }> {
    return this.request<{ status: string; reason: string }>(
      `/student/reminders/${reminderId}/stop?reason=${encodeURIComponent(reason)}`,
      { method: 'POST' }
    );
  }

  // ---------------------------------------------------------------------------
  // Telegram
  // ---------------------------------------------------------------------------

  async getTelegramStatus(): Promise<TelegramStatusResponse> {
    return this.request<TelegramStatusResponse>('/student/telegram/status');
  }

  async createTelegramLinkToken(): Promise<TelegramLinkResponse> {
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

  async deleteAccount(): Promise<void> {
    await this.request<void>('/auth/account', {
      method: 'DELETE',
    });
  }
}

export const api = new ApiClient();
