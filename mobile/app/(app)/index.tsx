import { Redirect } from 'expo-router';
import React from 'react';
import { useAuth } from '../../src/auth/AuthContext';

export default function Index() {
  const { user } = useAuth();

  if (!user) {
    return <Redirect href="/(auth)/login" />;
  }

  if (user.profileStatus === 'INCOMPLETE') {
    return <Redirect href="/(app)/onboarding" />;
  }

  return <Redirect href="/(app)/dashboard" />;
}
