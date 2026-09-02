import React from 'react';
import { StudentNotification } from '../lib/types';

export default function NotificationItem({ notif }: { notif: StudentNotification }) {
  const getIcon = (type: string) => {
    switch (type) {
      case 'ELIGIBILITY': return '🎯';
      case 'SHORTLIST': return '⭐';
      case 'DEADLINE': return '⏰';
      case 'REMINDER': return '🔔';
      default: return '📩';
    }
  };

  const getBadgeColor = (type: string) => {
    switch (type) {
      case 'SHORTLIST': return 'bg-amber-100 text-amber-800 dark:bg-amber-950/50 dark:text-amber-300';
      case 'ELIGIBILITY': return 'bg-emerald-100 text-emerald-800 dark:bg-emerald-950/50 dark:text-emerald-300';
      case 'DEADLINE': return 'bg-rose-100 text-rose-800 dark:bg-rose-950/50 dark:text-rose-300';
      default: return 'bg-indigo-100 text-indigo-800 dark:bg-indigo-950/50 dark:text-indigo-300';
    }
  };

  return (
    <div className="border border-zinc-200 dark:border-zinc-800 rounded-2xl p-4 bg-white dark:bg-zinc-900 shadow-2xs hover:border-zinc-300 dark:hover:border-zinc-700 transition-all flex items-start gap-3.5">
      <div className="text-2xl p-2 rounded-xl bg-zinc-100 dark:bg-zinc-800 flex items-center justify-center shrink-0">
        {getIcon(notif.notificationType)}
      </div>

      <div className="flex-1 min-w-0">
        <div className="flex items-center justify-between gap-2">
          <div className="flex items-center gap-2">
            <span className={`text-xs font-semibold px-2 py-0.5 rounded-md ${getBadgeColor(notif.notificationType)}`}>
              {notif.notificationType}
            </span>
            {notif.companyName && (
              <span className="text-xs font-bold text-zinc-900 dark:text-zinc-100">{notif.companyName}</span>
            )}
            {notif.roleTitle && (
              <span className="text-xs text-zinc-500 dark:text-zinc-400">({notif.roleTitle})</span>
            )}
          </div>
          <span className="text-xs text-zinc-400 dark:text-zinc-500 whitespace-nowrap">
            {new Date(notif.createdAt).toLocaleDateString(undefined, { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' })}
          </span>
        </div>

        <p className="text-sm text-zinc-700 dark:text-zinc-300 mt-2 whitespace-pre-line leading-relaxed">
          {notif.messagePayload || 'Notification received.'}
        </p>
      </div>
    </div>
  );
}
