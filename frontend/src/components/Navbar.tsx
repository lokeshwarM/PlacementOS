'use client';

import React from 'react';
import Link from 'next/link';
import { usePathname } from 'next/navigation';
import { useAuth } from '../context/AuthContext';

export default function Navbar() {
  const { user, profile, logout } = useAuth();
  const pathname = usePathname();

  if (!user || pathname === '/login' || pathname === '/onboarding') {
    return null;
  }

  const links = [
    { href: '/dashboard', label: 'Dashboard', icon: '📊' },
    { href: '/placements', label: 'Placements', icon: '🎯' },
    { href: '/applications', label: 'Applications', icon: '📝' },
    { href: '/notifications', label: 'Notifications', icon: '🔔' },
    { href: '/reminders', label: 'Reminders', icon: '⏰' },
    { href: '/profile', label: 'Profile', icon: '👤' },
  ];

  return (
    <header className="sticky top-0 z-40 w-full border-b border-zinc-200/80 dark:border-zinc-800/80 bg-white/80 dark:bg-zinc-950/80 backdrop-blur-md">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 h-16 flex items-center justify-between">
        {/* Brand */}
        <div className="flex items-center gap-8">
          <Link href="/dashboard" className="flex items-center gap-2 group">
            <div className="w-8 h-8 rounded-xl bg-indigo-600 flex items-center justify-center text-white font-bold text-lg shadow-sm group-hover:scale-105 transition-transform">
              P
            </div>
            <span className="font-bold text-lg tracking-tight text-zinc-900 dark:text-zinc-50">
              Placement<span className="text-indigo-600 dark:text-indigo-400">OS</span>
            </span>
          </Link>

          {/* Nav links */}
          <nav className="hidden md:flex items-center gap-1">
            {links.map((link) => {
              const active = pathname === link.href || (link.href !== '/dashboard' && pathname.startsWith(link.href));
              return (
                <Link
                  key={link.href}
                  href={link.href}
                  className={`flex items-center gap-1.5 px-3 py-1.5 text-sm font-medium rounded-xl transition-all ${
                    active
                      ? 'bg-zinc-100 dark:bg-zinc-800 text-zinc-900 dark:text-zinc-100 font-semibold'
                      : 'text-zinc-600 dark:text-zinc-400 hover:text-zinc-900 dark:hover:text-zinc-100 hover:bg-zinc-50 dark:hover:bg-zinc-900'
                  }`}
                >
                  <span>{link.icon}</span>
                  <span>{link.label}</span>
                </Link>
              );
            })}
          </nav>
        </div>

        {/* User / Profile & Logout */}
        <div className="flex items-center gap-4">
          <div className="hidden sm:flex flex-col items-end text-right">
            <span className="text-xs font-semibold text-zinc-900 dark:text-zinc-100">
              {profile?.name || user.email}
            </span>
            <span className="text-[10px] text-zinc-500 dark:text-zinc-400">
              {profile?.registrationNumber || user.role}
            </span>
          </div>

          <button
            type="button"
            onClick={logout}
            className="px-3 py-1.5 text-xs font-medium text-zinc-600 dark:text-zinc-400 hover:text-rose-600 dark:hover:text-rose-400 hover:bg-rose-50 dark:hover:bg-rose-950/30 rounded-xl transition-colors border border-zinc-200 dark:border-zinc-800"
          >
            Logout ↪
          </button>
        </div>
      </div>
    </header>
  );
}
