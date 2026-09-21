import React, { createContext, useContext, useEffect, useState } from 'react';
import { api } from '../api/client';
import { clearAuthStorage, getStoredUser, getToken, setStoredUser, setToken } from '../storage/secureStore';
import { AuthResponse, AuthUser, StudentProfile } from '../types';

interface AuthContextType {
  user: AuthUser | null;
  profile: StudentProfile | null;
  loading: boolean;
  login: (email: string, pass: string) => Promise<AuthResponse>;
  register: (email: string, pass: string) => Promise<AuthResponse>;
  logout: () => Promise<void>;
  refreshProfile: () => Promise<void>;
  deleteAccount: () => Promise<void>;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(null);
  const [profile, setProfile] = useState<StudentProfile | null>(null);
  const [loading, setLoading] = useState(true);

  // Restore session from SecureStore on startup
  useEffect(() => {
    async function restoreSession() {
      try {
        const token = await getToken();
        if (!token) {
          setLoading(false);
          return;
        }

        // Validate token with backend /auth/me
        try {
          const currentUser = await api.getCurrentUser();
          setUser(currentUser);
          await setStoredUser(currentUser);

          if (currentUser.profileStatus !== 'INCOMPLETE') {
            try {
              const prof = await api.getProfile();
              setProfile(prof);
            } catch (err) {
              console.warn('Could not fetch student profile during session restore:', err);
            }
          }
        } catch (err) {
          console.warn('Session restoration failed; token invalid or expired:', err);
          await clearAuthStorage();
          setUser(null);
          setProfile(null);
        }
      } catch (err) {
        console.error('Error reading SecureStore:', err);
      } finally {
        setLoading(false);
      }
    }

    restoreSession();
  }, []);

  const login = async (email: string, pass: string): Promise<AuthResponse> => {
    const res = await api.login(email, pass);
    await setToken(res.token);

    const authUser: AuthUser = {
      id: res.userId,
      email: res.email,
      role: res.role,
      profileStatus: res.profileStatus,
      studentId: res.studentId,
      studentName: res.studentName,
    };

    await setStoredUser(authUser);
    setUser(authUser);

    if (res.profileStatus !== 'INCOMPLETE') {
      try {
        const prof = await api.getProfile();
        setProfile(prof);
      } catch (err) {
        console.warn('Could not fetch profile on login:', err);
      }
    }

    return res;
  };

  const register = async (email: string, pass: string): Promise<AuthResponse> => {
    const res = await api.register(email, pass);
    await setToken(res.token);

    const authUser: AuthUser = {
      id: res.userId,
      email: res.email,
      role: res.role,
      profileStatus: res.profileStatus,
      studentId: res.studentId,
      studentName: res.studentName,
    };

    await setStoredUser(authUser);
    setUser(authUser);
    setProfile(null);
    return res;
  };

  const logout = async () => {
    await clearAuthStorage();
    setUser(null);
    setProfile(null);
  };

  const refreshProfile = async () => {
    try {
      const prof = await api.getProfile();
      setProfile(prof);
      if (user && user.profileStatus !== prof.profileStatus) {
        const updatedUser = { ...user, profileStatus: prof.profileStatus };
        setUser(updatedUser);
        await setStoredUser(updatedUser);
      }
    } catch (err) {
      console.error('Failed to refresh profile:', err);
    }
  };

  const deleteAccount = async () => {
    await api.deleteAccount();
    await logout();
  };

  return (
    <AuthContext.Provider
      value={{
        user,
        profile,
        loading,
        login,
        register,
        logout,
        refreshProfile,
        deleteAccount,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}
