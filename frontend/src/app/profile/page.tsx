'use client';

import React, { useEffect, useState } from 'react';
import { useAuth } from '../../context/AuthContext';
import { api } from '../../lib/api';
import { TelegramStatusResponse } from '../../lib/types';
import LoadingSpinner from '../../components/LoadingSpinner';

export default function ProfilePage() {
  const { user, profile, refreshProfile } = useAuth();

  const [name, setName] = useState('');
  const [phoneNumber, setPhoneNumber] = useState('');
  const [specialization, setSpecialization] = useState('');
  const [standingArrears, setStandingArrears] = useState('0');
  const [gender, setGender] = useState('FEMALE');

  const [saving, setSaving] = useState(false);
  const [message, setMessage] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  const [telegramStatus, setTelegramStatus] = useState<TelegramStatusResponse | null>(null);
  const [connectingTelegram, setConnectingTelegram] = useState(false);
  const [disconnectingTelegram, setDisconnectingTelegram] = useState(false);
  const [deepLinkUrl, setDeepLinkUrl] = useState<string | null>(null);

  useEffect(() => {
    api.getTelegramStatus().then(setTelegramStatus).catch(console.error);
  }, []);

  useEffect(() => {
    if (profile) {
      setName(profile.name || '');
      setPhoneNumber(profile.phoneNumber || '');
      setSpecialization(profile.specialization || '');
      setStandingArrears(profile.standingArrears !== null ? String(profile.standingArrears) : '0');
      setGender(profile.gender || 'FEMALE');
    }
  }, [profile]);

  const handleSave = async (e: React.FormEvent) => {
    e.preventDefault();
    setSaving(true);
    setMessage(null);
    setError(null);

    try {
      const arrears = standingArrears !== '' ? parseInt(standingArrears, 10) : 0;
      await api.updateProfile({
        name,
        phoneNumber: phoneNumber || undefined,
        specialization: specialization || undefined,
        standingArrears: arrears,
        gender,
      });
      await refreshProfile();
      setMessage('Profile updated successfully!');
    } catch (err: unknown) {
      if (err instanceof Error) {
        setError(err.message);
      } else {
        setError('Failed to update profile');
      }
    } finally {
      setSaving(false);
    }
  };

  const handleConnectTelegram = async () => {
    setConnectingTelegram(true);
    setError(null);
    try {
      const res = await api.generateTelegramLinkToken();
      setDeepLinkUrl(res.deepLink);
      window.open(res.deepLink, '_blank', 'noopener,noreferrer');
    } catch (err: unknown) {
      if (err instanceof Error) setError(err.message);
      else setError('Failed to generate Telegram connection link');
    } finally {
      setConnectingTelegram(false);
    }
  };

  const handleUnlinkTelegram = async () => {
    setDisconnectingTelegram(true);
    setError(null);
    try {
      await api.unlinkTelegram();
      const status = await api.getTelegramStatus();
      setTelegramStatus(status);
      setDeepLinkUrl(null);
      setMessage('Telegram disconnected successfully.');
    } catch (err: unknown) {
      if (err instanceof Error) setError(err.message);
      else setError('Failed to disconnect Telegram');
    } finally {
      setDisconnectingTelegram(false);
    }
  };


  if (!profile) {
    return <LoadingSpinner text="Loading your profile..." />;
  }

  const getStatusBadge = (status: string) => {
    switch (status) {
      case 'VERIFIED':
        return <span className="px-2.5 py-1 text-xs font-bold rounded-full bg-emerald-100 text-emerald-800 border border-emerald-300">✓ Verified Institutional Profile</span>;
      case 'COMPLETE':
        return <span className="px-2.5 py-1 text-xs font-bold rounded-full bg-blue-100 text-blue-800 border border-blue-300">✓ Complete</span>;
      default:
        return <span className="px-2.5 py-1 text-xs font-bold rounded-full bg-amber-100 text-amber-800 border border-amber-300">⚠ Incomplete</span>;
    }
  };

  return (
    <div className="max-w-3xl mx-auto space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-zinc-900 dark:text-zinc-100">Academic Profile</h1>
          <p className="text-sm text-zinc-500 dark:text-zinc-400 mt-1">
            Manage your student credentials and placement profile details.
          </p>
        </div>
        {getStatusBadge(profile.profileStatus)}
      </div>

      {message && (
        <div className="p-4 rounded-2xl bg-emerald-50 border border-emerald-200 text-emerald-800 text-xs font-medium dark:bg-emerald-950/40 dark:border-emerald-900 dark:text-emerald-300">
          {message}
        </div>
      )}

      {error && (
        <div className="p-4 rounded-2xl bg-rose-50 border border-rose-200 text-rose-700 text-xs font-medium dark:bg-rose-950/40 dark:border-rose-900 dark:text-rose-400">
          {error}
        </div>
      )}

      {/* Authoritative Institutional Identifiers (Read-only) */}
      <div className="bg-white dark:bg-zinc-900 border border-zinc-200 dark:border-zinc-800 rounded-3xl p-6 shadow-xs space-y-4">
        <h2 className="text-sm font-bold text-zinc-900 dark:text-zinc-100 uppercase tracking-wider">
          Institutional Identifiers & Academic Record
        </h2>
        <div className="grid grid-cols-2 sm:grid-cols-3 gap-4 text-xs">
          <div className="p-3 bg-zinc-50 dark:bg-zinc-800/50 rounded-xl">
            <span className="text-zinc-500">Registration Number</span>
            <p className="font-bold text-zinc-900 dark:text-zinc-100 mt-0.5">{profile.registrationNumber || '—'}</p>
          </div>
          <div className="p-3 bg-zinc-50 dark:bg-zinc-800/50 rounded-xl">
            <span className="text-zinc-500">NeoPAT ID</span>
            <p className="font-bold text-zinc-900 dark:text-zinc-100 mt-0.5">{profile.neopatId || '—'}</p>
          </div>
          <div className="p-3 bg-zinc-50 dark:bg-zinc-800/50 rounded-xl">
            <span className="text-zinc-500">Branch</span>
            <p className="font-bold text-zinc-900 dark:text-zinc-100 mt-0.5">{profile.branch || '—'}</p>
          </div>
          <div className="p-3 bg-zinc-50 dark:bg-zinc-800/50 rounded-xl">
            <span className="text-zinc-500">Batch</span>
            <p className="font-bold text-zinc-900 dark:text-zinc-100 mt-0.5">{profile.batch || '—'}</p>
          </div>
          <div className="p-3 bg-zinc-50 dark:bg-zinc-800/50 rounded-xl">
            <span className="text-zinc-500">CGPA</span>
            <p className="font-bold text-zinc-900 dark:text-zinc-100 mt-0.5">{profile.cgpa ? Number(profile.cgpa).toFixed(2) : '—'}</p>
          </div>
          <div className="p-3 bg-zinc-50 dark:bg-zinc-800/50 rounded-xl">
            <span className="text-zinc-500">Degree</span>
            <p className="font-bold text-zinc-900 dark:text-zinc-100 mt-0.5">{profile.degree || '—'}</p>
          </div>
        </div>
      </div>

      {/* Telegram Notifications Integration Card */}
      <div className="bg-white dark:bg-zinc-900 border border-zinc-200 dark:border-zinc-800 rounded-3xl p-6 shadow-xs space-y-4">
        <div className="flex items-center justify-between">
          <div className="flex items-center space-x-3">
            <div className="w-10 h-10 rounded-2xl bg-sky-50 dark:bg-sky-950/50 flex items-center justify-center text-sky-600">
              <svg className="w-5 h-5 fill-current" viewBox="0 0 24 24">
                <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm4.64 6.8c-.15 1.58-.8 5.42-1.13 7.19-.14.75-.42 1-.68 1.03-.58.05-1.02-.38-1.58-.75-.88-.58-1.38-.94-2.23-1.5-.99-.65-.35-1.01.22-1.59.15-.15 2.71-2.48 2.76-2.69a.2.2 0 00-.05-.18c-.06-.05-.14-.03-.21-.02-.09.02-1.49.95-4.22 2.79-.4.27-.76.41-1.08.4-.36-.01-1.04-.2-1.55-.37-.63-.2-1.12-.31-1.08-.66.02-.18.27-.36.74-.55 2.92-1.27 4.86-2.11 5.83-2.51 2.78-1.16 3.35-1.36 3.73-1.36.08 0 .27.02.39.12.1.08.13.19.14.27-.01.06.01.24 0 .38z"/>
              </svg>
            </div>
            <div>
              <h2 className="text-sm font-bold text-zinc-900 dark:text-zinc-100 uppercase tracking-wider">
                Telegram Notifications
              </h2>
              <p className="text-xs text-zinc-500 dark:text-zinc-400">
                Receive real-time placement alerts, eligibility notices, and 1-tap deadline reminders.
              </p>
            </div>
          </div>
          {telegramStatus?.linked ? (
            <span className="px-2.5 py-1 text-xs font-bold rounded-full bg-emerald-100 text-emerald-800 border border-emerald-300">
              ✓ Connected
            </span>
          ) : (
            <span className="px-2.5 py-1 text-xs font-bold rounded-full bg-zinc-100 text-zinc-700 border border-zinc-300 dark:bg-zinc-800 dark:text-zinc-300">
              Not Connected
            </span>
          )}
        </div>

        {telegramStatus?.linked ? (
          <div className="flex items-center justify-between p-4 bg-emerald-50/50 dark:bg-emerald-950/20 rounded-2xl border border-emerald-200 dark:border-emerald-900">
            <div>
              <p className="text-xs font-semibold text-emerald-900 dark:text-emerald-200">
                Connected Telegram Account: <span className="font-bold">@{telegramStatus.telegramUsername || 'Telegram User'}</span>
              </p>
              {telegramStatus.linkedAt && (
                <p className="text-xs text-emerald-700 dark:text-emerald-400 mt-0.5">
                  Linked on {new Date(telegramStatus.linkedAt).toLocaleDateString()}
                </p>
              )}
            </div>
            <button
              type="button"
              onClick={handleUnlinkTelegram}
              disabled={disconnectingTelegram}
              className="px-3.5 py-1.5 text-xs font-bold text-rose-700 hover:text-rose-800 bg-white hover:bg-rose-50 border border-rose-200 rounded-xl transition-all"
            >
              {disconnectingTelegram ? 'Disconnecting...' : 'Disconnect'}
            </button>
          </div>
        ) : (
          <div className="p-4 bg-zinc-50 dark:bg-zinc-800/40 rounded-2xl border border-zinc-200 dark:border-zinc-800 space-y-3">
            <p className="text-xs text-zinc-600 dark:text-zinc-400">
              Connect Telegram to receive placement eligibility, shortlist, deadline and reminder alerts directly to your phone.
            </p>
            <div className="flex items-center space-x-3">
              <button
                type="button"
                onClick={handleConnectTelegram}
                disabled={connectingTelegram}
                className="inline-flex items-center space-x-2 px-4 py-2 text-xs font-bold text-white bg-sky-600 hover:bg-sky-700 rounded-xl transition-all shadow-xs"
              >
                <span>{connectingTelegram ? 'Generating Link...' : 'Connect Telegram'}</span>
                <svg className="w-3.5 h-3.5 fill-current" viewBox="0 0 24 24">
                  <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm4.64 6.8c-.15 1.58-.8 5.42-1.13 7.19-.14.75-.42 1-.68 1.03-.58.05-1.02-.38-1.58-.75-.88-.58-1.38-.94-2.23-1.5-.99-.65-.35-1.01.22-1.59.15-.15 2.71-2.48 2.76-2.69a.2.2 0 00-.05-.18c-.06-.05-.14-.03-.21-.02-.09.02-1.49.95-4.22 2.79-.4.27-.76.41-1.08.4-.36-.01-1.04-.2-1.55-.37-.63-.2-1.12-.31-1.08-.66.02-.18.27-.36.74-.55 2.92-1.27 4.86-2.11 5.83-2.51 2.78-1.16 3.35-1.36 3.73-1.36.08 0 .27.02.39.12.1.08.13.19.14.27-.01.06.01.24 0 .38z"/>
                </svg>
              </button>
              {deepLinkUrl && (
                <a
                  href={deepLinkUrl}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="text-xs text-sky-600 underline font-medium hover:text-sky-700"
                >
                  Click here if Telegram didn't open automatically ↗
                </a>
              )}
            </div>
          </div>
        )}
      </div>

      {/* Editable Student Details Form */}
      <div className="bg-white dark:bg-zinc-900 border border-zinc-200 dark:border-zinc-800 rounded-3xl p-6 shadow-xs">

        <h2 className="text-sm font-bold text-zinc-900 dark:text-zinc-100 uppercase tracking-wider mb-4">
          Editable Profile Information
        </h2>

        <form onSubmit={handleSave} className="space-y-4">
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-semibold text-zinc-700 dark:text-zinc-300 mb-1">
                Full Name
              </label>
              <input
                type="text"
                required
                value={name}
                onChange={(e) => setName(e.target.value)}
                className="w-full px-3.5 py-2.5 text-sm rounded-xl border border-zinc-300 dark:border-zinc-700 bg-white dark:bg-zinc-800 text-zinc-900 dark:text-zinc-100 focus:ring-2 focus:ring-indigo-500"
              />
            </div>

            <div>
              <label className="block text-xs font-semibold text-zinc-700 dark:text-zinc-300 mb-1">
                Phone Number (WhatsApp)
              </label>
              <input
                type="tel"
                value={phoneNumber}
                onChange={(e) => setPhoneNumber(e.target.value)}
                placeholder="+91 98765 43210"
                className="w-full px-3.5 py-2.5 text-sm rounded-xl border border-zinc-300 dark:border-zinc-700 bg-white dark:bg-zinc-800 text-zinc-900 dark:text-zinc-100 focus:ring-2 focus:ring-indigo-500"
              />
            </div>

            <div>
              <label className="block text-xs font-semibold text-zinc-700 dark:text-zinc-300 mb-1">
                Specialization / Domain
              </label>
              <input
                type="text"
                value={specialization}
                onChange={(e) => setSpecialization(e.target.value)}
                placeholder="e.g. Artificial Intelligence"
                className="w-full px-3.5 py-2.5 text-sm rounded-xl border border-zinc-300 dark:border-zinc-700 bg-white dark:bg-zinc-800 text-zinc-900 dark:text-zinc-100 focus:ring-2 focus:ring-indigo-500"
              />
            </div>

            <div>
              <label className="block text-xs font-semibold text-zinc-700 dark:text-zinc-300 mb-1">
                Standing Arrears
              </label>
              <input
                type="number"
                min="0"
                value={standingArrears}
                onChange={(e) => setStandingArrears(e.target.value)}
                className="w-full px-3.5 py-2.5 text-sm rounded-xl border border-zinc-300 dark:border-zinc-700 bg-white dark:bg-zinc-800 text-zinc-900 dark:text-zinc-100 focus:ring-2 focus:ring-indigo-500"
              />
            </div>

            <div>
              <label className="block text-xs font-semibold text-zinc-700 dark:text-zinc-300 mb-1">
                Gender
              </label>
              <select
                value={gender}
                onChange={(e) => setGender(e.target.value)}
                className="w-full px-3.5 py-2.5 text-sm rounded-xl border border-zinc-300 dark:border-zinc-700 bg-white dark:bg-zinc-800 text-zinc-900 dark:text-zinc-100 focus:ring-2 focus:ring-indigo-500"
              >
                <option value="FEMALE">Female</option>
                <option value="MALE">Male</option>
                <option value="OTHER">Other</option>
              </select>
            </div>
          </div>

          <div className="flex justify-end pt-4">
            <button
              type="submit"
              disabled={saving}
              className="px-6 py-2.5 text-sm font-bold text-white bg-indigo-600 hover:bg-indigo-700 disabled:opacity-50 rounded-xl transition-all shadow-sm"
            >
              {saving ? 'Saving...' : 'Save Changes'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
