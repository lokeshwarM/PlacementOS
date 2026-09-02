import React, { useState } from 'react';
import { RoleEligibilityCard } from '../lib/types';
import { EligibilityDecisionBadge } from './ApplicationStatusBadge';

export default function RoleEligibilityRow({ role }: { role: RoleEligibilityCard }) {
  const [expanded, setExpanded] = useState(false);

  return (
    <div className="border border-zinc-200 dark:border-zinc-800 rounded-xl p-3.5 bg-white dark:bg-zinc-900/60 shadow-2xs hover:border-zinc-300 dark:hover:border-zinc-700 transition-all">
      <div className="flex items-center justify-between gap-3">
        <div className="min-w-0 flex-1">
          <div className="flex items-center gap-2">
            <h4 className="text-sm font-semibold text-zinc-900 dark:text-zinc-100 truncate">{role.roleTitle}</h4>
            <EligibilityDecisionBadge decision={role.decision} />
          </div>
          <div className="flex flex-wrap items-center gap-x-4 gap-y-1 text-xs text-zinc-500 dark:text-zinc-400 mt-1">
            {role.minCgpa !== null && <span>Min CGPA: <strong className="text-zinc-700 dark:text-zinc-200">{role.minCgpa.toFixed(2)}</strong></span>}
            {role.eligibleBranches && role.eligibleBranches.length > 0 && (
              <span>Branches: <strong className="text-zinc-700 dark:text-zinc-200">{role.eligibleBranches.join(', ')}</strong></span>
            )}
            {role.maxStandingArrearsAllowed !== null && (
              <span>Max Arrears: <strong className="text-zinc-700 dark:text-zinc-200">{role.maxStandingArrearsAllowed}</strong></span>
            )}
          </div>
        </div>

        <button
          type="button"
          onClick={() => setExpanded(!expanded)}
          className="text-xs font-medium text-indigo-600 dark:text-indigo-400 hover:text-indigo-700 dark:hover:text-indigo-300 p-1.5 rounded-lg hover:bg-indigo-50 dark:hover:bg-indigo-950/30 transition-colors"
        >
          {expanded ? 'Hide Reasons ▲' : 'View Reasons ▼'}
        </button>
      </div>

      {expanded && (
        <div className="mt-3 pt-3 border-t border-zinc-100 dark:border-zinc-800/80">
          <p className="text-xs font-semibold text-zinc-600 dark:text-zinc-300 mb-1.5">Evaluation Breakdown:</p>
          <ul className="space-y-1">
            {role.criteriaExplanations.map((reason, idx) => (
              <li key={idx} className="text-xs text-zinc-600 dark:text-zinc-400 flex items-start gap-1.5">
                <span>{reason}</span>
              </li>
            ))}
          </ul>
        </div>
      )}
    </div>
  );
}
