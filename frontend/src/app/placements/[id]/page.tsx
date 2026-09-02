'use client';

import React, { useEffect, useState, use } from 'react';
import Link from 'next/link';
import { api } from '../../../lib/api';
import { StudentPlacementDetail } from '../../../lib/types';
import { ApplicationStatusBadge, ShortlistBadge } from '../../../components/ApplicationStatusBadge';
import RoleEligibilityRow from '../../../components/RoleEligibilityRow';
import LoadingSpinner from '../../../components/LoadingSpinner';

export default function PlacementDetailPage({ params }: { params: Promise<{ id: string }> }) {
  const resolvedParams = use(params);
  const driveId = parseInt(resolvedParams.id, 10);

  const [drive, setDrive] = useState<StudentPlacementDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [applying, setApplying] = useState(false);
  const [message, setMessage] = useState<string | null>(null);

  const loadDetail = async () => {
    try {
      const res = await api.getPlacementDetail(driveId);
      setDrive(res);
    } catch (err: unknown) {
      console.error('Failed to load detail:', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadDetail();
  }, [driveId]);

  const handleApply = async () => {
    if (!drive || !drive.applicationId) {
      setMessage('Application ID not available.');
      return;
    }

    setApplying(true);
    setMessage(null);
    try {
      await api.applyToApplication(drive.applicationId);
      setMessage('🎉 Application successfully marked as APPLIED! Active reminders have been stopped.');
      await loadDetail();
    } catch (err: unknown) {
      if (err instanceof Error) {
        setMessage(`Apply failed: ${err.message}`);
      }
    } finally {
      setApplying(false);
    }
  };

  if (loading) {
    return <LoadingSpinner text="Loading placement details..." />;
  }

  if (!drive) {
    return (
      <div className="text-center py-12">
        <h2 className="text-xl font-bold text-zinc-900 dark:text-zinc-100">Placement Drive Not Found</h2>
        <Link href="/placements" className="text-indigo-600 dark:text-indigo-400 text-sm mt-2 inline-block">
          ← Back to Placements
        </Link>
      </div>
    );
  }

  const deadlineDate = drive.applicationDeadline ? new Date(drive.applicationDeadline) : null;

  return (
    <div className="max-w-4xl mx-auto space-y-6">
      {/* Back Link */}
      <Link
        href="/placements"
        className="text-xs font-semibold text-zinc-500 hover:text-zinc-900 dark:hover:text-zinc-200 transition-colors inline-flex items-center gap-1"
      >
        ← Back to Catalog
      </Link>

      {message && (
        <div className="p-4 rounded-2xl bg-indigo-50 border border-indigo-200 text-indigo-800 text-sm font-medium dark:bg-indigo-950/40 dark:border-indigo-900 dark:text-indigo-300">
          {message}
        </div>
      )}

      {/* Main Drive Header Card */}
      <div className="bg-white dark:bg-zinc-900 border border-zinc-200 dark:border-zinc-800 rounded-3xl p-6 sm:p-8 shadow-md">
        <div className="flex flex-col sm:flex-row sm:items-start justify-between gap-4">
          <div>
            <span className="text-xs font-bold tracking-widest text-indigo-600 dark:text-indigo-400 uppercase">
              {drive.companyName}
            </span>
            <h1 className="text-2xl sm:text-3xl font-bold text-zinc-900 dark:text-zinc-100 mt-1">
              {drive.title}
            </h1>
          </div>
          <div className="flex flex-wrap items-center gap-2">
            <ApplicationStatusBadge status={drive.applicationStatus} />
            <ShortlistBadge status={drive.shortlistStatus} />
          </div>
        </div>

        {drive.description && (
          <p className="text-sm text-zinc-600 dark:text-zinc-400 mt-4 leading-relaxed">
            {drive.description}
          </p>
        )}

        {/* Key Dates Grid */}
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 mt-6 pt-6 border-t border-zinc-100 dark:border-zinc-800 text-xs">
          <div className="p-3 bg-zinc-50 dark:bg-zinc-800/50 rounded-xl">
            <span className="text-zinc-500 font-medium">Application Deadline</span>
            <p className={`font-bold mt-0.5 ${drive.deadlinePassed ? 'text-rose-600 dark:text-rose-400' : 'text-zinc-900 dark:text-zinc-100'}`}>
              {deadlineDate ? deadlineDate.toLocaleString() : 'Not specified'}
            </p>
          </div>
          <div className="p-3 bg-zinc-50 dark:bg-zinc-800/50 rounded-xl">
            <span className="text-zinc-500 font-medium">Drive / Assessment Date</span>
            <p className="font-bold text-zinc-900 dark:text-zinc-100 mt-0.5">
              {drive.driveDate ? new Date(drive.driveDate).toLocaleDateString() : 'TBA'}
            </p>
          </div>
          <div className="p-3 bg-zinc-50 dark:bg-zinc-800/50 rounded-xl">
            <span className="text-zinc-500 font-medium">Applied At</span>
            <p className="font-bold text-zinc-900 dark:text-zinc-100 mt-0.5">
              {drive.appliedAt ? new Date(drive.appliedAt).toLocaleString() : 'Not applied'}
            </p>
          </div>
        </div>

        {/* Action Banner */}
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 mt-6 pt-6 border-t border-zinc-100 dark:border-zinc-800">
          <p className="text-sm font-semibold text-zinc-800 dark:text-zinc-200">
            {drive.actionRequired}
          </p>

          {drive.isActionable && (
            <button
              type="button"
              disabled={applying}
              onClick={handleApply}
              className="px-6 py-2.5 text-sm font-bold text-white bg-indigo-600 hover:bg-indigo-700 disabled:opacity-50 rounded-xl transition-all shadow-md"
            >
              {applying ? 'Submitting Application...' : 'Apply Now 🚀'}
            </button>
          )}
        </div>
      </div>

      {/* Role-Specific Eligibility Breakdown */}
      <div className="space-y-4">
        <h2 className="text-lg font-bold text-zinc-900 dark:text-zinc-100">
          Role-Specific Eligibility Breakdown ({drive.roles.length} Roles)
        </h2>
        <div className="space-y-3">
          {drive.roles.map((role) => (
            <RoleEligibilityRow key={role.roleId} role={role} />
          ))}
        </div>
      </div>
    </div>
  );
}
