'use client';

import React, { createContext, useContext, useEffect, useState, useCallback } from 'react';
import { useRouter, usePathname } from 'next/navigation';
import { AuthUser, StudentProfile } from '../lib/types';
import { api } from '../lib/api';

interface AuthContextType {
  user: AuthUser | null;
  profile: StudentProfile | null;
  loading: boolean;
  login: (email: string, pass: string) => Promise<void>;
  register: (email: string, pass: string) => Promise<void>;
  logout: () => void;
  refreshProfile: () => Promise<void>;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(null);
  const [profile, setProfile] = useState<StudentProfile | null>(null);
  const [loading, setLoading] = useState(true);
  const router = useRouter();
  const pathname = usePathname();

  const refreshProfile = useCallback(async () => {
    try {
      const p = await api.getProfile();
      setProfile(p);
      if (user) {
        setUser((prev) => (prev ? { ...prev, profileStatus: p.profileStatus, studentId: p.studentId } : prev));
      }
    } catch {
      // profile might not exist yet if incomplete
    }
  }, [user]);

  const checkAuth = useCallback(async () => {
    const token = localStorage.getItem('placementos_token');
    if (!token) {
      setUser(null);
      setProfile(null);
      setLoading(false);
      return;
    }

    try {
      const u = await api.getMe();
      setUser(u);
      try {
        const p = await api.getProfile();
        setProfile(p);
      } catch {
        // onboarding needed
      }
    } catch {
      localStorage.removeItem('placementos_token');
      setUser(null);
      setProfile(null);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    checkAuth();
  }, [checkAuth]);

  // Route protection
  useEffect(() => {
    if (loading) return;

    const isAuthRoute = pathname === '/login';
    const isOnboardingRoute = pathname === '/onboarding';
    const isPublicRoute = pathname === '/privacy';

    if (!user && !isAuthRoute && !isPublicRoute) {
      router.replace('/login');
    } else if (user && isAuthRoute) {
      if (user.profileStatus === 'INCOMPLETE') {
        router.replace('/onboarding');
      } else {
        router.replace('/dashboard');
      }
    } else if (user && !isOnboardingRoute && !isPublicRoute && user.profileStatus === 'INCOMPLETE') {
      router.replace('/onboarding');
    }
  }, [user, loading, pathname, router]);

  const login = async (email: string, pass: string) => {
    setLoading(true);
    try {
      const res = await api.login({ email, password: pass });
      localStorage.setItem('placementos_token', res.token);
      const u: AuthUser = {
        id: res.userId,
        email: res.email,
        role: res.role,
        profileStatus: res.profileStatus,
        studentId: res.studentId,
        studentName: res.studentName,
      };
      setUser(u);
      if (u.profileStatus === 'INCOMPLETE') {
        router.replace('/onboarding');
      } else {
        router.replace('/dashboard');
      }
    } finally {
      setLoading(false);
    }
  };

  const register = async (email: string, pass: string) => {
    setLoading(true);
    try {
      const res = await api.register({ email, password: pass });
      localStorage.setItem('placementos_token', res.token);
      const u: AuthUser = {
        id: res.userId,
        email: res.email,
        role: res.role,
        profileStatus: res.profileStatus,
        studentId: res.studentId,
        studentName: res.studentName,
      };
      setUser(u);
      router.replace('/onboarding');
    } finally {
      setLoading(false);
    }
  };

  const logout = () => {
    localStorage.removeItem('placementos_token');
    setUser(null);
    setProfile(null);
    router.replace('/login');
  };

  return (
    <AuthContext.Provider value={{ user, profile, loading, login, register, logout, refreshProfile }}>
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
