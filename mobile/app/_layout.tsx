import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { Slot, useRouter, useSegments } from 'expo-router';
import { StatusBar } from 'expo-status-bar';
import React, { useEffect } from 'react';
import { ActivityIndicator, StyleSheet, View } from 'react-native';
import { SafeAreaProvider } from 'react-native-safe-area-context';
import { AuthProvider, useAuth } from '../src/auth/AuthContext';

const queryClient = new QueryClient();

function NavigationProtector() {
  const { user, loading } = useAuth();
  const segments = useSegments();
  const router = useRouter();

  useEffect(() => {
    if (loading) return;

    const inAuthGroup = segments[0] === '(auth)';
    const currentRoute = segments.join('/');
    const isPrivacy = currentRoute.includes('privacy');

    if (isPrivacy) {
      // Privacy policy is accessible to everyone
      return;
    }

    if (!user) {
      if (!inAuthGroup) {
        router.replace('/(auth)/login');
      }
    } else {
      if (user.profileStatus === 'INCOMPLETE') {
        if (!currentRoute.includes('onboarding') && !currentRoute.includes('delete-account')) {
          router.replace('/(app)/onboarding');
        }
      } else if (inAuthGroup) {
        router.replace('/(app)/dashboard');
      }
    }
  }, [user, loading, segments, router]);

  if (loading) {
    return (
      <View style={styles.loadingContainer}>
        <ActivityIndicator size="large" color="#6366f1" />
      </View>
    );
  }

  return <Slot />;
}

export default function RootLayout() {
  return (
    <SafeAreaProvider>
      <QueryClientProvider client={queryClient}>
        <AuthProvider>
          <StatusBar style="light" />
          <NavigationProtector />
        </AuthProvider>
      </QueryClientProvider>
    </SafeAreaProvider>
  );
}

const styles = StyleSheet.create({
  loadingContainer: {
    flex: 1,
    backgroundColor: '#09090b',
    alignItems: 'center',
    justifyContent: 'center',
  },
});
