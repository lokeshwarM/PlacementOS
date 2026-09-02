'use client';

import React, { useEffect, useState, useCallback } from 'react';
import { api } from '../../lib/api';
import { StudentPlacementDriveCard } from '../../lib/types';
import PlacementCard from '../../components/PlacementCard';
import LoadingSpinner from '../../components/LoadingSpinner';
import EmptyState from '../../components/EmptyState';

export default function PlacementsCatalogPage() {
  const [placements, setPlacements] = useState<StudentPlacementDriveCard[]>([]);
  const [filter, setFilter] = useState<'ALL' | 'ELIGIBLE' | 'APPLIED' | 'SHORTLISTED'>('ALL');
  const [search, setSearch] = useState('');
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [loading, setLoading] = useState(true);
  const [applyingId, setApplyingId] = useState<number | null>(null);
  const [message, setMessage] = useState<string | null>(null);

  const loadPlacements = useCallback(async () => {
    setLoading(true);
    try {
      const res = await api.getPlacements(page, 10);
      setPlacements(res.content || []);
      setTotalPages(res.totalPages || 1);
    } catch (err) {
      console.error('Failed to load placements:', err);
    } finally {
      setLoading(false);
    }
  }, [page]);

  useEffect(() => {
    loadPlacements();
  }, [loadPlacements]);

  const handleApply = async (driveId: number, applicationId?: number) => {
    if (!applicationId) {
      setMessage('Application record not found for this drive.');
      return;
    }

    setApplyingId(applicationId);
    setMessage(null);
    try {
      await api.applyToApplication(applicationId);
      setMessage('🎉 Application submitted successfully!');
      await loadPlacements();
    } catch (err: unknown) {
      if (err instanceof Error) {
        setMessage(`Apply failed: ${err.message}`);
      }
    } finally {
      setApplyingId(null);
    }
  };

  const filteredPlacements = placements.filter((p) => {
    const matchesSearch =
      p.companyName.toLowerCase().includes(search.toLowerCase()) ||
      p.title.toLowerCase().includes(search.toLowerCase());

    if (!matchesSearch) return false;

    if (filter === 'ELIGIBLE') return p.overallEligibility === 'ELIGIBLE';
    if (filter === 'APPLIED') return p.applicationStatus === 'APPLIED';
    if (filter === 'SHORTLISTED') return p.shortlistStatus === 'MATCHED' || p.applicationStatus === 'SHORTLISTED';
    return true;
  });

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-zinc-900 dark:text-zinc-100">Placement Opportunities</h1>
        <p className="text-sm text-zinc-500 dark:text-zinc-400 mt-1">
          Explore university placement drives evaluated against your academic profile.
        </p>
      </div>

      {message && (
        <div className="p-4 rounded-2xl bg-indigo-50 border border-indigo-200 text-indigo-800 text-sm font-medium dark:bg-indigo-950/40 dark:border-indigo-900 dark:text-indigo-300">
          {message}
        </div>
      )}

      {/* Filter & Search Bar */}
      <div className="flex flex-col sm:flex-row items-center justify-between gap-4">
        {/* Search */}
        <input
          type="text"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          placeholder="Search by company or role..."
          className="w-full sm:w-72 px-3.5 py-2 text-sm rounded-xl border border-zinc-300 dark:border-zinc-700 bg-white dark:bg-zinc-800 text-zinc-900 dark:text-zinc-100 focus:ring-2 focus:ring-indigo-500"
        />

        {/* Filter Pills */}
        <div className="flex items-center gap-1.5 overflow-x-auto w-full sm:w-auto p-1 bg-zinc-100 dark:bg-zinc-800 rounded-xl">
          {(['ALL', 'ELIGIBLE', 'APPLIED', 'SHORTLISTED'] as const).map((tab) => (
            <button
              key={tab}
              type="button"
              onClick={() => setFilter(tab)}
              className={`px-3 py-1.5 text-xs font-semibold rounded-lg transition-all ${
                filter === tab
                  ? 'bg-white dark:bg-zinc-900 text-zinc-900 dark:text-zinc-100 shadow-xs'
                  : 'text-zinc-500 hover:text-zinc-900 dark:hover:text-zinc-200'
              }`}
            >
              {tab === 'ALL' ? 'All Drives' : tab.charAt(0) + tab.slice(1).toLowerCase()}
            </button>
          ))}
        </div>
      </div>

      {loading ? (
        <LoadingSpinner text="Loading placement drives..." />
      ) : filteredPlacements.length === 0 ? (
        <EmptyState
          title="No Matching Placements"
          description="Try adjusting your filter or search query."
        />
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
          {filteredPlacements.map((drive) => (
            <PlacementCard
              key={drive.driveId}
              drive={drive}
              onApply={handleApply}
              isApplying={applyingId === drive.applicationId}
            />
          ))}
        </div>
      )}

      {/* Pagination Controls */}
      {totalPages > 1 && (
        <div className="flex items-center justify-center gap-2 pt-4">
          <button
            type="button"
            disabled={page === 0}
            onClick={() => setPage((p) => Math.max(0, p - 1))}
            className="px-3 py-1.5 text-xs font-medium rounded-lg border border-zinc-300 dark:border-zinc-700 disabled:opacity-40"
          >
            ← Previous
          </button>
          <span className="text-xs text-zinc-500">
            Page {page + 1} of {totalPages}
          </span>
          <button
            type="button"
            disabled={page >= totalPages - 1}
            onClick={() => setPage((p) => p + 1)}
            className="px-3 py-1.5 text-xs font-medium rounded-lg border border-zinc-300 dark:border-zinc-700 disabled:opacity-40"
          >
            Next →
          </button>
        </div>
      )}
    </div>
  );
}
