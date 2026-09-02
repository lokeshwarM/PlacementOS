'use client';

import { useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { useAuth } from '../context/AuthContext';
import LoadingSpinner from '../components/LoadingSpinner';

export default function Home() {
  const { user, loading } = useAuth();
  const router = useRouter();

  useEffect(() => {
    if (!loading) {
      if (!user) {
        router.replace('/login');
      } else if (user.profileStatus === 'INCOMPLETE') {
        router.replace('/onboarding');
      } else {
        router.replace('/dashboard');
      }
    }
  }, [user, loading, router]);

  return (
    <div className="flex flex-1 items-center justify-center min-h-[60vh]">
      <LoadingSpinner text="Redirecting to PlacementOS..." />
    </div>
  );
}
