'use client';

import React, { useEffect, useState, useCallback } from 'react';
import { api } from '../../lib/api';
import { StudentReminder } from '../../lib/types';
import ReminderItem from '../../components/ReminderItem';
import LoadingSpinner from '../../components/LoadingSpinner';
import EmptyState from '../../components/EmptyState';

export default function RemindersPage() {
  const [reminders, setReminders] = useState<StudentReminder[]>([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [loading, setLoading] = useState(true);
  const [stoppingId, setStoppingId] = useState<number | null>(null);
  const [message, setMessage] = useState<string | null>(null);

  const loadReminders = useCallback(async () => {
    setLoading(true);
    try {
      const res = await api.getReminders(page, 10);
      setReminders(res.content || []);
      setTotalPages(res.totalPages || 1);
    } catch (err) {
      console.error('Failed to load reminders:', err);
    } finally {
      setLoading(false);
    }
  }, [page]);

  useEffect(() => {
    loadReminders();
  }, [loadReminders]);

  const handleStop = async (id: number) => {
    setStoppingId(id);
    setMessage(null);
    try {
      await api.stopReminder(id);
      setMessage('Reminder cancelled successfully.');
      await loadReminders();
    } catch (err: unknown) {
      if (err instanceof Error) {
        setMessage(`Failed to stop reminder: ${err.message}`);
      }
    } finally {
      setStoppingId(null);
    }
  };

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-zinc-900 dark:text-zinc-100">Reminder Center</h1>
        <p className="text-sm text-zinc-500 dark:text-zinc-400 mt-1">
          Active deadline reminders automatically stop when you apply or complete the drive requirement.
        </p>
      </div>

      <div className="flex items-center justify-between p-4 bg-sky-50 dark:bg-sky-950/30 rounded-2xl border border-sky-200 dark:border-sky-900">
        <div className="flex items-center space-x-3">
          <span className="text-xl">✈️</span>
          <div>
            <p className="text-xs font-bold text-sky-900 dark:text-sky-200">
              Get Real-Time Alerts & 1-Tap Actions on Telegram
            </p>
            <p className="text-xs text-sky-700 dark:text-sky-400 mt-0.5">
              Connect Telegram to receive deadline countdowns and mark applications APPLIED straight from your chat.
            </p>
          </div>
        </div>
        <a
          href="/profile"
          className="px-3.5 py-1.5 text-xs font-bold text-sky-700 hover:text-sky-800 bg-white hover:bg-sky-50 border border-sky-300 rounded-xl transition-all whitespace-nowrap"
        >
          Connect in Profile →
        </a>
      </div>

      {message && (
        <div className="p-4 rounded-2xl bg-indigo-50 border border-indigo-200 text-indigo-800 text-sm font-medium dark:bg-indigo-950/40 dark:border-indigo-900 dark:text-indigo-300">
          {message}
        </div>
      )}


      {loading ? (
        <LoadingSpinner text="Loading reminders..." />
      ) : reminders.length === 0 ? (
        <EmptyState
          title="No Scheduled Reminders"
          description="Reminders are automatically scheduled when eligible placement opportunities are detected."
          icon="⏰"
        />
      ) : (
        <div className="space-y-3">
          {reminders.map((r) => (
            <ReminderItem
              key={r.id}
              reminder={r}
              onStop={handleStop}
              isStopping={stoppingId === r.id}
            />
          ))}
        </div>
      )}

      {totalPages > 1 && (
        <div className="flex items-center justify-center gap-2 pt-4">
          <button
            type="button"
            disabled={page === 0}
            onClick={() => setPage((p) => Math.max(0, p - 1))}
            className="px-3 py-1.5 text-xs font-medium rounded-lg border border-zinc-300 dark:border-zinc-700 disabled:opacity-40"
          >
            ← Previous
          </button>
          <span className="text-xs text-zinc-500">
            Page {page + 1} of {totalPages}
          </span>
          <button
            type="button"
            disabled={page >= totalPages - 1}
            onClick={() => setPage((p) => p + 1)}
            className="px-3 py-1.5 text-xs font-medium rounded-lg border border-zinc-300 dark:border-zinc-700 disabled:opacity-40"
          >
            Next →
          </button>
        </div>
      )}
    </div>
  );
}
