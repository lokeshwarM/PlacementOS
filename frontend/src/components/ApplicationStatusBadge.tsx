import React from 'react';
import { ApplicationStatus, EligibilityDecision, ShortlistMatchStatus } from '../lib/types';

export function ApplicationStatusBadge({ status }: { status: ApplicationStatus }) {
  switch (status) {
    case 'ELIGIBLE':
      return (
        <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-semibold bg-emerald-50 text-emerald-700 border border-emerald-200 dark:bg-emerald-950/40 dark:text-emerald-400 dark:border-emerald-800">
          <span className="w-1.5 h-1.5 rounded-full bg-emerald-500 animate-pulse" />
          Eligible
        </span>
      );
    case 'APPLIED':
      return (
        <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-semibold bg-blue-50 text-blue-700 border border-blue-200 dark:bg-blue-950/40 dark:text-blue-400 dark:border-blue-800">
          <span className="w-1.5 h-1.5 rounded-full bg-blue-500" />
          Applied
        </span>
      );
    case 'SHORTLISTED':
      return (
        <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-bold bg-amber-50 text-amber-700 border border-amber-300 dark:bg-amber-950/40 dark:text-amber-300 dark:border-amber-700 shadow-xs">
          ⭐ Shortlisted
        </span>
      );
    case 'NOT_ELIGIBLE':
      return (
        <span className="inline-flex items-center px-2.5 py-1 rounded-full text-xs font-medium bg-rose-50 text-rose-700 border border-rose-200 dark:bg-rose-950/40 dark:text-rose-400 dark:border-rose-800">
          Ineligible
        </span>
      );
    case 'COMPLETED':
      return (
        <span className="inline-flex items-center px-2.5 py-1 rounded-full text-xs font-medium bg-purple-50 text-purple-700 border border-purple-200 dark:bg-purple-950/40 dark:text-purple-400 dark:border-purple-800">
          Completed
        </span>
      );
    case 'REJECTED':
      return (
        <span className="inline-flex items-center px-2.5 py-1 rounded-full text-xs font-medium bg-zinc-100 text-zinc-700 border border-zinc-300 dark:bg-zinc-800 dark:text-zinc-300 dark:border-zinc-700">
          Rejected
        </span>
      );
    default:
      return (
        <span className="inline-flex items-center px-2.5 py-1 rounded-full text-xs font-medium bg-zinc-100 text-zinc-600 border border-zinc-200 dark:bg-zinc-800 dark:text-zinc-400 dark:border-zinc-700">
          Not Started
        </span>
      );
  }
}

export function EligibilityDecisionBadge({ decision }: { decision: EligibilityDecision }) {
  switch (decision) {
    case 'ELIGIBLE':
      return (
        <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-md text-xs font-medium bg-emerald-100/80 text-emerald-800 dark:bg-emerald-900/50 dark:text-emerald-300">
          ✓ Eligible
        </span>
      );
    case 'NOT_ELIGIBLE':
      return (
        <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-md text-xs font-medium bg-rose-100/80 text-rose-800 dark:bg-rose-900/50 dark:text-rose-300">
          ✗ Not Eligible
        </span>
      );
    default:
      return (
        <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-md text-xs font-medium bg-amber-100/80 text-amber-800 dark:bg-amber-900/50 dark:text-amber-300">
          ℹ Review Required
        </span>
      );
  }
}

export function ShortlistBadge({ status }: { status: ShortlistMatchStatus | null }) {
  if (status === 'MATCHED') {
    return (
      <span className="inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-xs font-bold bg-amber-100 text-amber-900 border border-amber-300 dark:bg-amber-900/40 dark:text-amber-200 dark:border-amber-700">
        🎉 Confirmed Shortlist
      </span>
    );
  }
  return null;
}
