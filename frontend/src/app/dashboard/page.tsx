'use client';

import React, { useEffect, useState } from 'react';
import Link from 'next/link';
import { useAuth } from '../../context/AuthContext';
import { api } from '../../lib/api';
import { StudentPlacementDriveCard, StudentNotification, StudentReminder } from '../../lib/types';
import PlacementCard from '../../components/PlacementCard';
import NotificationItem from '../../components/NotificationItem';
import ReminderItem from '../../components/ReminderItem';
import LoadingSpinner from '../../components/LoadingSpinner';
import EmptyState from '../../components/EmptyState';

export default function DashboardPage() {
  const { user, profile } = useAuth();
  const [placements, setPlacements] = useState<StudentPlacementDriveCard[]>([]);
  const [notifications, setNotifications] = useState<StudentNotification[]>([]);
  const [reminders, setReminders] = useState<StudentReminder[]>([]);
  const [loading, setLoading] = useState(true);
  const [applyingId, setApplyingId] = useState<number | null>(null);
  const [stoppingId, setStoppingId] = useState<number | null>(null);
  const [message, setMessage] = useState<string | null>(null);

  const loadData = async () => {
    try {
      const [placementsRes, notifsRes, remindersRes] = await Promise.all([
        api.getPlacements(0, 6),
        api.getNotifications(0, 4),
        api.getReminders(0, 4),
      ]);
      setPlacements(placementsRes.content || []);
      setNotifications(notifsRes.content || []);
      setReminders(remindersRes.content || []);
    } catch (err: unknown) {
      console.error('Failed to load dashboard data:', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  const handleApply = async (driveId: number, applicationId?: number) => {
    if (!applicationId) {
      setMessage('Application record not found. Please click Details to inspect.');
      return;
    }

    setApplyingId(applicationId);
    setMessage(null);
    try {
      await api.applyToApplication(applicationId);
      setMessage('🎉 Application marked as APPLIED! Reminders stopped.');
      await loadData();
    } catch (err: unknown) {
      if (err instanceof Error) {
        setMessage(`Failed to apply: ${err.message}`);
      }
    } finally {
      setApplyingId(null);
    }
  };

  const handleStopReminder = async (reminderId: number) => {
    setStoppingId(reminderId);
    try {
      await api.stopReminder(reminderId);
      await loadData();
    } catch (err: unknown) {
      console.error('Failed to stop reminder:', err);
    } finally {
      setStoppingId(null);
    }
  };

  if (loading) {
    return <LoadingSpinner text="Loading your placement dashboard..." />;
  }

  const eligibleDrives = placements.filter((p) => p.overallEligibility === 'ELIGIBLE' && p.applicationStatus !== 'APPLIED');
  const shortlistedDrives = placements.filter((p) => p.shortlistStatus === 'MATCHED' || p.applicationStatus === 'SHORTLISTED');
  const activeReminders = reminders.filter((r) => r.isActive);

  return (
    <div className="space-y-8">
      {/* Welcome Banner */}
      <div className="bg-linear-to-r from-indigo-900 via-indigo-800 to-indigo-950 text-white rounded-3xl p-6 sm:p-8 shadow-xl relative overflow-hidden">
        <div className="relative z-10 max-w-2xl">
          <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-semibold bg-white/10 text-indigo-200 backdrop-blur-md mb-3">
            🎓 {profile?.branch || 'Student'} • Batch {profile?.batch || 2026} • CGPA {profile?.cgpa ? Number(profile.cgpa).toFixed(2) : '-'}
          </span>
          <h1 className="text-2xl sm:text-3xl font-bold tracking-tight">
            Welcome back, {profile?.name || user?.email?.split('@')[0]}!
          </h1>
          <p className="text-sm text-indigo-200 mt-2">
            {eligibleDrives.length > 0
              ? `You have ${eligibleDrives.length} placement drive${eligibleDrives.length > 1 ? 's' : ''} eligible for application.`
              : 'Keep your academic profile updated to match new placement criteria.'}
          </p>
        </div>
      </div>

      {message && (
        <div className="p-4 rounded-2xl bg-indigo-50 border border-indigo-200 text-indigo-800 text-sm font-medium dark:bg-indigo-950/40 dark:border-indigo-900 dark:text-indigo-300">
          {message}
        </div>
      )}

      {/* Shortlist Alert (if any) */}
      {shortlistedDrives.length > 0 && (
        <div className="p-5 rounded-2xl bg-amber-50 border border-amber-300 text-amber-900 dark:bg-amber-950/40 dark:border-amber-700 dark:text-amber-200 shadow-sm flex items-center justify-between gap-4">
          <div className="flex items-center gap-3">
            <span className="text-2xl">⭐</span>
            <div>
              <h3 className="text-sm font-bold">Congratulations! You are shortlisted in {shortlistedDrives.length} drive(s).</h3>
              <p className="text-xs text-amber-800 dark:text-amber-300 mt-0.5">
                Companies: {shortlistedDrives.map((d) => d.companyName).join(', ')}
              </p>
            </div>
          </div>
          <Link
            href="/applications"
            className="px-3 py-1.5 text-xs font-bold text-amber-900 bg-amber-200 hover:bg-amber-300 rounded-xl transition-colors shrink-0"
          >
            View Details →
          </Link>
        </div>
      )}

      {/* Metrics Row */}
      <div className="grid grid-cols-2 sm:grid-cols-4 gap-4">
        <div className="bg-white dark:bg-zinc-900 border border-zinc-200 dark:border-zinc-800 rounded-2xl p-4 shadow-2xs">
          <span className="text-xs font-medium text-zinc-500">Active Drives</span>
          <p className="text-2xl font-bold text-zinc-900 dark:text-zinc-100 mt-1">{placements.length}</p>
        </div>
        <div className="bg-white dark:bg-zinc-900 border border-zinc-200 dark:border-zinc-800 rounded-2xl p-4 shadow-2xs">
          <span className="text-xs font-medium text-zinc-500">Eligible to Apply</span>
          <p className="text-2xl font-bold text-emerald-600 dark:text-emerald-400 mt-1">{eligibleDrives.length}</p>
        </div>
        <div className="bg-white dark:bg-zinc-900 border border-zinc-200 dark:border-zinc-800 rounded-2xl p-4 shadow-2xs">
          <span className="text-xs font-medium text-zinc-500">Shortlisted</span>
          <p className="text-2xl font-bold text-amber-600 dark:text-amber-400 mt-1">{shortlistedDrives.length}</p>
        </div>
        <div className="bg-white dark:bg-zinc-900 border border-zinc-200 dark:border-zinc-800 rounded-2xl p-4 shadow-2xs">
          <span className="text-xs font-medium text-zinc-500">Active Reminders</span>
          <p className="text-2xl font-bold text-indigo-600 dark:text-indigo-400 mt-1">{activeReminders.length}</p>
        </div>
      </div>

      {/* Main Content Layout */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        {/* Left 2 Cols: Placement Drives */}
        <div className="lg:col-span-2 space-y-4">
          <div className="flex items-center justify-between">
            <h2 className="text-lg font-bold text-zinc-900 dark:text-zinc-100">
              Placement Drives & Eligibility
            </h2>
            <Link
              href="/placements"
              className="text-xs font-semibold text-indigo-600 dark:text-indigo-400 hover:underline"
            >
              View All ({placements.length}) →
            </Link>
          </div>

          {placements.length === 0 ? (
            <EmptyState
              title="No Placement Drives Found"
              description="New placement drives will appear here automatically when university emails are processed."
            />
          ) : (
            <div className="grid grid-cols-1 gap-4">
              {placements.map((drive) => (
                <PlacementCard
                  key={drive.driveId}
                  drive={drive}
                  onApply={handleApply}
                  isApplying={applyingId === drive.applicationId}
                />
              ))}
            </div>
          )}
        </div>

        {/* Right 1 Col: Reminders & Recent Notifications */}
        <div className="space-y-6">
          {/* Active Reminders */}
          <div className="space-y-3">
            <div className="flex items-center justify-between">
              <h3 className="text-sm font-bold text-zinc-900 dark:text-zinc-100">
                Active Reminders ({activeReminders.length})
              </h3>
              <Link
                href="/reminders"
                className="text-xs font-semibold text-indigo-600 dark:text-indigo-400 hover:underline"
              >
                All →
              </Link>
            </div>

            {activeReminders.length === 0 ? (
              <p className="text-xs text-zinc-500 dark:text-zinc-400 p-4 border border-dashed border-zinc-200 dark:border-zinc-800 rounded-2xl text-center">
                No active deadline reminders.
              </p>
            ) : (
              <div className="space-y-2">
                {activeReminders.map((r) => (
                  <ReminderItem
                    key={r.id}
                    reminder={r}
                    onStop={handleStopReminder}
                    isStopping={stoppingId === r.id}
                  />
                ))}
              </div>
            )}
          </div>

          {/* Recent Notifications */}
          <div className="space-y-3">
            <div className="flex items-center justify-between">
              <h3 className="text-sm font-bold text-zinc-900 dark:text-zinc-100">
                Recent Notifications
              </h3>
              <Link
                href="/notifications"
                className="text-xs font-semibold text-indigo-600 dark:text-indigo-400 hover:underline"
              >
                All →
              </Link>
            </div>

            {notifications.length === 0 ? (
              <p className="text-xs text-zinc-500 dark:text-zinc-400 p-4 border border-dashed border-zinc-200 dark:border-zinc-800 rounded-2xl text-center">
                No notifications received yet.
              </p>
            ) : (
              <div className="space-y-2">
                {notifications.map((n) => (
                  <NotificationItem key={n.id} notif={n} />
                ))}
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
