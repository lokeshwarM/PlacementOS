'use client';

import React, { useEffect, useState, useCallback } from 'react';
import { api } from '../../lib/api';
import { StudentNotification } from '../../lib/types';
import NotificationItem from '../../components/NotificationItem';
import LoadingSpinner from '../../components/LoadingSpinner';
import EmptyState from '../../components/EmptyState';

export default function NotificationsPage() {
  const [notifications, setNotifications] = useState<StudentNotification[]>([]);
  const [filter, setFilter] = useState<'ALL' | 'ELIGIBILITY' | 'SHORTLIST' | 'DEADLINE' | 'REMINDER'>('ALL');
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [loading, setLoading] = useState(true);

  const loadNotifications = useCallback(async () => {
    setLoading(true);
    try {
      const typeParam = filter === 'ALL' ? undefined : filter;
      const res = await api.getNotifications(page, 10, typeParam);
      setNotifications(res.content || []);
      setTotalPages(res.totalPages || 1);
    } catch (err) {
      console.error('Failed to load notifications:', err);
    } finally {
      setLoading(false);
    }
  }, [page, filter]);

  useEffect(() => {
    loadNotifications();
  }, [loadNotifications]);

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-zinc-900 dark:text-zinc-100">Notification Center</h1>
        <p className="text-sm text-zinc-500 dark:text-zinc-400 mt-1">
          Historical record of placement alerts, eligibility evaluations, and shortlist announcements.
        </p>
      </div>

      {/* Filter Tabs */}
      <div className="flex items-center gap-1.5 overflow-x-auto p-1 bg-zinc-100 dark:bg-zinc-800 rounded-xl w-fit">
        {(['ALL', 'ELIGIBILITY', 'SHORTLIST', 'DEADLINE', 'REMINDER'] as const).map((tab) => (
          <button
            key={tab}
            type="button"
            onClick={() => { setFilter(tab); setPage(0); }}
            className={`px-3 py-1.5 text-xs font-semibold rounded-lg transition-all ${
              filter === tab
                ? 'bg-white dark:bg-zinc-900 text-zinc-900 dark:text-zinc-100 shadow-xs'
                : 'text-zinc-500 hover:text-zinc-900 dark:hover:text-zinc-200'
            }`}
          >
            {tab === 'ALL' ? 'All Alerts' : tab.charAt(0) + tab.slice(1).toLowerCase()}
          </button>
        ))}
      </div>

      {loading ? (
        <LoadingSpinner text="Loading notifications..." />
      ) : notifications.length === 0 ? (
        <EmptyState
          title="No Notifications Found"
          description={filter === 'ALL' ? 'You have no notifications yet.' : `No notifications under ${filter}.`}
          icon="🔔"
        />
      ) : (
        <div className="space-y-3">
          {notifications.map((notif) => (
            <NotificationItem key={notif.id} notif={notif} />
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
