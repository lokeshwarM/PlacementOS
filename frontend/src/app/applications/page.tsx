'use client';

import React, { useEffect, useState } from 'react';
import Link from 'next/link';
import { api } from '../../lib/api';
import { StudentApplication } from '../../lib/types';
import { ApplicationStatusBadge, ShortlistBadge } from '../../components/ApplicationStatusBadge';
import LoadingSpinner from '../../components/LoadingSpinner';
import EmptyState from '../../components/EmptyState';

export default function ApplicationsPage() {
  const [applications, setApplications] = useState<StudentApplication[]>([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const loadApps = async () => {
      setLoading(true);
      try {
        const res = await api.getApplications(page, 10);
        setApplications(res.content || []);
        setTotalPages(res.totalPages || 1);
      } catch (err) {
        console.error('Failed to load applications:', err);
      } finally {
        setLoading(false);
      }
    };
    loadApps();
  }, [page]);

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-zinc-900 dark:text-zinc-100">My Applications</h1>
        <p className="text-sm text-zinc-500 dark:text-zinc-400 mt-1">
          Track the status of your submitted and evaluated placement applications.
        </p>
      </div>

      {loading ? (
        <LoadingSpinner text="Loading your applications..." />
      ) : applications.length === 0 ? (
        <EmptyState
          title="No Applications Found"
          description="You have not applied to any placement drives yet."
        />
      ) : (
        <div className="bg-white dark:bg-zinc-900 border border-zinc-200 dark:border-zinc-800 rounded-2xl shadow-xs overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm text-zinc-600 dark:text-zinc-300">
              <thead className="text-xs uppercase bg-zinc-50 dark:bg-zinc-800/60 text-zinc-500 dark:text-zinc-400 border-b border-zinc-200 dark:border-zinc-800">
                <tr>
                  <th className="px-5 py-3.5 font-semibold">Company & Drive</th>
                  <th className="px-5 py-3.5 font-semibold">Role</th>
                  <th className="px-5 py-3.5 font-semibold">Status</th>
                  <th className="px-5 py-3.5 font-semibold">Shortlist</th>
                  <th className="px-5 py-3.5 font-semibold">Applied At</th>
                  <th className="px-5 py-3.5 font-semibold text-right">Action</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-zinc-100 dark:divide-zinc-800">
                {applications.map((app) => (
                  <tr key={app.applicationId} className="hover:bg-zinc-50/50 dark:hover:bg-zinc-800/30 transition-colors">
                    <td className="px-5 py-4">
                      <span className="text-xs font-bold text-indigo-600 dark:text-indigo-400 uppercase block">
                        {app.companyName}
                      </span>
                      <span className="font-semibold text-zinc-900 dark:text-zinc-100">
                        {app.driveTitle}
                      </span>
                    </td>
                    <td className="px-5 py-4 text-xs font-medium text-zinc-700 dark:text-zinc-300">
                      {app.roleTitle || 'General'}
                    </td>
                    <td className="px-5 py-4">
                      <ApplicationStatusBadge status={app.status} />
                    </td>
                    <td className="px-5 py-4">
                      <ShortlistBadge status={app.shortlistStatus} />
                    </td>
                    <td className="px-5 py-4 text-xs text-zinc-500">
                      {app.appliedAt ? new Date(app.appliedAt).toLocaleString() : '—'}
                    </td>
                    <td className="px-5 py-4 text-right">
                      <Link
                        href={`/placements/${app.driveId}`}
                        className="text-xs font-semibold text-indigo-600 dark:text-indigo-400 hover:underline"
                      >
                        View Drive →
                      </Link>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
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
