'use client';

import React, { useEffect, useState } from 'react';
import { useAuth } from '../../context/AuthContext';
import { api } from '../../lib/api';
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
