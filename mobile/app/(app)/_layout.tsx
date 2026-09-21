import { Stack } from 'expo-router';
import React from 'react';

export default function AppLayout() {
  return (
    <Stack
      screenOptions={{
        headerStyle: { backgroundColor: '#09090b' },
        headerTintColor: '#f4f4f5',
        headerTitleStyle: { fontWeight: '700' },
        contentStyle: { backgroundColor: '#09090b' },
        headerShadowVisible: false,
      }}
    >
      <Stack.Screen name="index" options={{ headerShown: false }} />
      <Stack.Screen name="dashboard" options={{ title: 'PlacementOS', headerLeft: () => null }} />
      <Stack.Screen name="onboarding" options={{ title: 'Complete Profile', headerLeft: () => null }} />
      <Stack.Screen name="placements/index" options={{ title: 'Placement Drives' }} />
      <Stack.Screen name="placements/[driveId]" options={{ title: 'Drive Details' }} />
      <Stack.Screen name="applications" options={{ title: 'Applications' }} />
      <Stack.Screen name="notifications" options={{ title: 'Notification Center' }} />
      <Stack.Screen name="reminders" options={{ title: 'Active Reminders' }} />
      <Stack.Screen name="profile" options={{ title: 'Student Profile' }} />
      <Stack.Screen name="telegram" options={{ title: 'Telegram Connection' }} />
      <Stack.Screen name="privacy" options={{ title: 'Privacy Policy' }} />
      <Stack.Screen name="delete-account" options={{ title: 'Delete Account' }} />
    </Stack>
  );
}
