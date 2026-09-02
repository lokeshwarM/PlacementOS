import React from 'react';

export default function LoadingSpinner({ text = 'Loading...' }: { text?: string }) {
  return (
    <div className="flex flex-col items-center justify-center p-12 space-y-4">
      <div className="w-10 h-10 border-4 border-indigo-500/20 border-t-indigo-600 rounded-full animate-spin" />
      <p className="text-sm font-medium text-zinc-500 dark:text-zinc-400">{text}</p>
    </div>
  );
}
