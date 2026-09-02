import React from 'react';
import { StudentReminder } from '../lib/types';

export default function ReminderItem({
  reminder,
  onStop,
  isStopping = false,
}: {
  reminder: StudentReminder;
  onStop: (id: number) => void;
  isStopping?: boolean;
}) {
  const scheduledDate = new Date(reminder.scheduledFor);

  return (
    <div className="border border-zinc-200 dark:border-zinc-800 rounded-2xl p-4 bg-white dark:bg-zinc-900 shadow-2xs hover:border-zinc-300 dark:hover:border-zinc-700 transition-all flex flex-col sm:flex-row sm:items-center justify-between gap-4">
      <div className="flex items-start gap-3">
        <div className="text-xl p-2 rounded-xl bg-amber-50 dark:bg-amber-950/40 text-amber-600 dark:text-amber-400 shrink-0">
          🔔
        </div>
        <div>
          <div className="flex items-center gap-2">
            <h4 className="text-sm font-bold text-zinc-900 dark:text-zinc-100">{reminder.companyName}</h4>
            {reminder.roleTitle && (
              <span className="text-xs text-zinc-500 dark:text-zinc-400">({reminder.roleTitle})</span>
            )}
            <span className={`text-[10px] font-semibold px-2 py-0.5 rounded-full ${reminder.isActive ? 'bg-emerald-100 text-emerald-800 dark:bg-emerald-950 dark:text-emerald-300' : 'bg-zinc-100 text-zinc-600 dark:bg-zinc-800 dark:text-zinc-400'}`}>
              {reminder.isActive ? 'Active' : reminder.status}
            </span>
          </div>

          <div className="flex flex-wrap items-center gap-x-4 gap-y-1 text-xs text-zinc-500 dark:text-zinc-400 mt-1">
            <span>Next schedule: <strong className="text-zinc-700 dark:text-zinc-200">{scheduledDate.toLocaleString()}</strong></span>
            <span>Interval: <strong className="text-zinc-700 dark:text-zinc-200">{reminder.intervalMinutes}m</strong></span>
            <span>Sent: <strong className="text-zinc-700 dark:text-zinc-200">{reminder.remindersSent} / {reminder.maxReminders}</strong></span>
          </div>
        </div>
      </div>

      {reminder.isActive && (
        <button
          type="button"
          disabled={isStopping}
          onClick={() => onStop(reminder.id)}
          className="px-3 py-1.5 text-xs font-semibold text-rose-600 dark:text-rose-400 bg-rose-50 hover:bg-rose-100 dark:bg-rose-950/30 dark:hover:bg-rose-900/50 rounded-xl transition-colors shrink-0 self-end sm:self-auto"
        >
          {isStopping ? 'Stopping...' : 'Stop Reminders 🛑'}
        </button>
      )}
    </div>
  );
}
