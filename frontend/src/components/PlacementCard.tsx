import React from 'react';
import Link from 'next/link';
import { StudentPlacementDriveCard } from '../lib/types';
import { ApplicationStatusBadge, ShortlistBadge } from './ApplicationStatusBadge';
import RoleEligibilityRow from './RoleEligibilityRow';

export default function PlacementCard({
  drive,
  onApply,
  isApplying = false,
}: {
  drive: StudentPlacementDriveCard;
  onApply?: (driveId: number, appId?: number) => void;
  isApplying?: boolean;
}) {
  const deadlineDate = drive.applicationDeadline ? new Date(drive.applicationDeadline) : null;
  const isExpired = drive.deadlinePassed;

  return (
    <div className="border border-zinc-200 dark:border-zinc-800 rounded-2xl p-5 bg-white dark:bg-zinc-900 shadow-xs hover:shadow-md transition-all flex flex-col justify-between gap-4">
      <div>
        {/* Header */}
        <div className="flex items-start justify-between gap-3">
          <div>
            <span className="text-xs font-semibold tracking-wider text-indigo-600 dark:text-indigo-400 uppercase">
              {drive.companyName}
            </span>
            <h3 className="text-lg font-bold text-zinc-900 dark:text-zinc-100 mt-0.5">{drive.title}</h3>
          </div>
          <div className="flex flex-col items-end gap-1.5">
            <ApplicationStatusBadge status={drive.applicationStatus} />
            <ShortlistBadge status={drive.shortlistStatus} />
          </div>
        </div>

        {/* Description */}
        {drive.description && (
          <p className="text-sm text-zinc-600 dark:text-zinc-400 line-clamp-2 mt-2">
            {drive.description}
          </p>
        )}

        {/* Deadline & Dates */}
        <div className="flex flex-wrap items-center gap-4 text-xs text-zinc-500 dark:text-zinc-400 mt-3 pt-3 border-t border-zinc-100 dark:border-zinc-800">
          <div className="flex items-center gap-1.5">
            <span>📅 Deadline:</span>
            <span className={`font-semibold ${isExpired ? 'text-rose-600 dark:text-rose-400' : 'text-zinc-800 dark:text-zinc-200'}`}>
              {deadlineDate ? deadlineDate.toLocaleString(undefined, { dateStyle: 'medium', timeStyle: 'short' }) : 'Not specified'}
            </span>
          </div>
          {drive.driveDate && (
            <div className="flex items-center gap-1.5">
              <span>🏢 Drive:</span>
              <span className="font-semibold text-zinc-800 dark:text-zinc-200">
                {new Date(drive.driveDate).toLocaleDateString()}
              </span>
            </div>
          )}
        </div>

        {/* Role Eligibility Rows */}
        <div className="mt-4 space-y-2">
          <p className="text-xs font-semibold text-zinc-500 uppercase tracking-wider">Available Roles ({drive.roles.length})</p>
          {drive.roles.map((role) => (
            <RoleEligibilityRow key={role.roleId} role={role} />
          ))}
        </div>
      </div>

      {/* Footer / Actions */}
      <div className="flex items-center justify-between gap-3 pt-3 border-t border-zinc-100 dark:border-zinc-800 mt-2">
        <p className="text-xs font-medium text-zinc-600 dark:text-zinc-300">
          {drive.actionRequired}
        </p>

        <div className="flex items-center gap-2">
          <Link
            href={`/placements/${drive.driveId}`}
            className="px-3 py-1.5 text-xs font-medium text-zinc-700 dark:text-zinc-300 bg-zinc-100 hover:bg-zinc-200 dark:bg-zinc-800 dark:hover:bg-zinc-700 rounded-xl transition-colors"
          >
            Details →
          </Link>

          {drive.isActionable && onApply && (
            <button
              type="button"
              disabled={isApplying}
              onClick={() => onApply(drive.driveId, drive.applicationId || undefined)}
              className="px-4 py-1.5 text-xs font-semibold text-white bg-indigo-600 hover:bg-indigo-700 disabled:opacity-50 rounded-xl transition-all shadow-xs"
            >
              {isApplying ? 'Applying...' : 'Apply Now 🚀'}
            </button>
          )}
        </div>
      </div>
    </div>
  );
}
