import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import * as Linking from 'expo-linking';
import React, { useState } from 'react';
import {
  ActivityIndicator,
  Alert,
  ScrollView,
  StyleSheet,
  Text,
  TouchableOpacity,
  View,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { api } from '../../src/api/client';

export default function TelegramScreen() {
  const queryClient = useQueryClient();
  const [linking, setLinking] = useState(false);
  const [linkError, setLinkError] = useState<string | null>(null);

  const { data: status, isLoading, refetch } = useQuery({
    queryKey: ['telegram-status'],
    queryFn: () => api.getTelegramStatus(),
  });

  const unlinkMutation = useMutation({
    mutationFn: () => api.unlinkTelegram(),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['telegram-status'] });
      queryClient.invalidateQueries({ queryKey: ['dashboard-telegram'] });
      Alert.alert('Unlinked', 'Your Telegram account has been disconnected.');
    },
    onError: (err: Error) => {
      Alert.alert('Error', err.message || 'Failed to disconnect Telegram.');
    },
  });

  const handleConnectTelegram = async () => {
    setLinking(true);
    setLinkError(null);
    try {
      const response = await api.createTelegramLinkToken();
      if (response.deepLink) {
        // Open Telegram app via deep link
        const supported = await Linking.canOpenURL(response.deepLink);
        if (supported) {
          await Linking.openURL(response.deepLink);
        } else {
          // Open web link fallback
          await Linking.openURL(response.deepLink);
        }
      }
    } catch (err: unknown) {
      if (err instanceof Error) {
        setLinkError(err.message);
      } else {
        setLinkError('Failed to generate Telegram connection link.');
      }
    } finally {
      setLinking(false);
    }
  };

  if (isLoading) {
    return (
      <SafeAreaView style={styles.safeArea}>
        <View style={styles.centerContainer}>
          <ActivityIndicator size="large" color="#38bdf8" />
          <Text style={styles.loadingText}>Checking Telegram connection...</Text>
        </View>
      </SafeAreaView>
    );
  }

  const isLinked = status?.linked;

  return (
    <SafeAreaView style={styles.safeArea} edges={['bottom']}>
      <ScrollView contentContainerStyle={styles.scrollContent}>
        {/* Header Card */}
        <View style={styles.card}>
          <View style={styles.iconContainer}>
            <Text style={styles.headerIcon}>✈️</Text>
          </View>
          <Text style={styles.title}>Telegram Notifications</Text>
          <Text style={styles.subtitle}>
            Connect your Telegram chat to receive immediate placement alerts, eligibility
            confirmations, and deadline reminders with 1-tap responses.
          </Text>

          {/* Connection Status Badge */}
          <View style={styles.statusRow}>
            <View
              style={[
                styles.statusIndicator,
                isLinked ? styles.indicatorConnected : styles.indicatorDisconnected,
              ]}
            />
            <Text style={styles.statusLabel}>
              {isLinked ? 'Connected to Telegram' : 'Not Connected'}
            </Text>
          </View>
        </View>

        {/* Content based on state */}
        {isLinked ? (
          <View style={styles.card}>
            <Text style={styles.sectionHeading}>Connection Details</Text>
            <View style={styles.infoBox}>
              <View style={styles.infoRow}>
                <Text style={styles.infoLabel}>Telegram Handle:</Text>
                <Text style={styles.infoValue}>
                  {status?.telegramUsername ? `@${status.telegramUsername}` : 'Linked'}
                </Text>
              </View>
              {status?.linkedAt && (
                <View style={styles.infoRow}>
                  <Text style={styles.infoLabel}>Connected On:</Text>
                  <Text style={styles.infoValue}>
                    {new Date(status.linkedAt).toLocaleDateString()}
                  </Text>
                </View>
              )}
            </View>

            <TouchableOpacity
              style={styles.unlinkButton}
              onPress={() => {
                Alert.alert(
                  'Disconnect Telegram?',
                  'You will no longer receive real-time placement alerts on Telegram.',
                  [
                    { text: 'Cancel', style: 'cancel' },
                    {
                      text: 'Disconnect',
                      style: 'destructive',
                      onPress: () => unlinkMutation.mutate(),
                    },
                  ]
                );
              }}
              disabled={unlinkMutation.isPending}
            >
              {unlinkMutation.isPending ? (
                <ActivityIndicator color="#f87171" />
              ) : (
                <Text style={styles.unlinkButtonText}>Disconnect Telegram</Text>
              )}
            </TouchableOpacity>
          </View>
        ) : (
          <View style={styles.card}>
            <Text style={styles.sectionHeading}>How to Connect</Text>
            <View style={styles.stepsList}>
              <View style={styles.stepItem}>
                <View style={styles.stepNumber}>
                  <Text style={styles.stepNumberText}>1</Text>
                </View>
                <Text style={styles.stepText}>Tap "Open in Telegram" below.</Text>
              </View>
              <View style={styles.stepItem}>
                <View style={styles.stepNumber}>
                  <Text style={styles.stepNumberText}>2</Text>
                </View>
                <Text style={styles.stepText}>The Telegram app opens with PlacementOS bot.</Text>
              </View>
              <View style={styles.stepItem}>
                <View style={styles.stepNumber}>
                  <Text style={styles.stepNumberText}>3</Text>
                </View>
                <Text style={styles.stepText}>Tap START to securely link your student account.</Text>
              </View>
              <View style={styles.stepItem}>
                <View style={styles.stepNumber}>
                  <Text style={styles.stepNumberText}>4</Text>
                </View>
                <Text style={styles.stepText}>Return here and tap "Refresh Status".</Text>
              </View>
            </View>

            {linkError && (
              <View style={styles.errorBox}>
                <Text style={styles.errorText}>{linkError}</Text>
              </View>
            )}

            <TouchableOpacity
              style={[styles.connectButton, linking && styles.buttonDisabled]}
              onPress={handleConnectTelegram}
              disabled={linking}
              activeOpacity={0.8}
            >
              {linking ? (
                <ActivityIndicator color="#ffffff" />
              ) : (
                <Text style={styles.connectButtonText}>Open in Telegram ✈️</Text>
              )}
            </TouchableOpacity>

            <TouchableOpacity style={styles.refreshButton} onPress={() => refetch()}>
              <Text style={styles.refreshButtonText}>Refresh Status 🔄</Text>
            </TouchableOpacity>
          </View>
        )}
      </ScrollView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safeArea: {
    flex: 1,
    backgroundColor: '#09090b',
  },
  scrollContent: {
    padding: 16,
    paddingBottom: 32,
  },
  centerContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
  },
  loadingText: {
    color: '#a1a1aa',
    fontSize: 14,
    marginTop: 12,
  },
  card: {
    backgroundColor: '#18181b',
    borderColor: '#27272a',
    borderWidth: 1,
    borderRadius: 20,
    padding: 20,
    marginBottom: 14,
  },
  iconContainer: {
    width: 60,
    height: 60,
    borderRadius: 30,
    backgroundColor: 'rgba(56, 189, 248, 0.15)',
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: 14,
  },
  headerIcon: {
    fontSize: 28,
  },
  title: {
    fontSize: 22,
    fontWeight: '800',
    color: '#f4f4f5',
    marginBottom: 6,
  },
  subtitle: {
    fontSize: 13,
    color: '#a1a1aa',
    lineHeight: 18,
    marginBottom: 16,
  },
  statusRow: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: '#27272a',
    borderRadius: 12,
    paddingHorizontal: 14,
    paddingVertical: 10,
    gap: 8,
  },
  statusIndicator: {
    width: 10,
    height: 10,
    borderRadius: 5,
  },
  indicatorConnected: {
    backgroundColor: '#4ade80',
  },
  indicatorDisconnected: {
    backgroundColor: '#f87171',
  },
  statusLabel: {
    fontSize: 13,
    fontWeight: '700',
    color: '#f4f4f5',
  },
  sectionHeading: {
    fontSize: 16,
    fontWeight: '700',
    color: '#f4f4f5',
    marginBottom: 14,
  },
  infoBox: {
    backgroundColor: '#27272a',
    borderRadius: 12,
    padding: 14,
    gap: 8,
    marginBottom: 16,
  },
  infoRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
  },
  infoLabel: {
    fontSize: 12,
    color: '#a1a1aa',
  },
  infoValue: {
    fontSize: 12,
    fontWeight: '700',
    color: '#f4f4f5',
  },
  unlinkButton: {
    backgroundColor: 'rgba(239, 68, 68, 0.15)',
    borderColor: 'rgba(239, 68, 68, 0.3)',
    borderWidth: 1,
    borderRadius: 12,
    paddingVertical: 12,
    alignItems: 'center',
  },
  unlinkButtonText: {
    color: '#f87171',
    fontSize: 14,
    fontWeight: '700',
  },
  stepsList: {
    gap: 12,
    marginBottom: 18,
  },
  stepItem: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  stepNumber: {
    width: 24,
    height: 24,
    borderRadius: 12,
    backgroundColor: '#27272a',
    alignItems: 'center',
    justifyContent: 'center',
    marginRight: 10,
  },
  stepNumberText: {
    color: '#818cf8',
    fontSize: 12,
    fontWeight: '700',
  },
  stepText: {
    fontSize: 13,
    color: '#d4d4d8',
    flex: 1,
  },
  errorBox: {
    backgroundColor: 'rgba(239, 68, 68, 0.15)',
    borderColor: 'rgba(239, 68, 68, 0.3)',
    borderWidth: 1,
    borderRadius: 12,
    padding: 12,
    marginBottom: 14,
  },
  errorText: {
    color: '#f87171',
    fontSize: 12,
  },
  connectButton: {
    backgroundColor: '#0284c7',
    borderRadius: 12,
    paddingVertical: 14,
    alignItems: 'center',
    marginBottom: 10,
  },
  buttonDisabled: {
    opacity: 0.6,
  },
  connectButtonText: {
    color: '#ffffff',
    fontSize: 15,
    fontWeight: '700',
  },
  refreshButton: {
    backgroundColor: '#27272a',
    borderRadius: 12,
    paddingVertical: 12,
    alignItems: 'center',
  },
  refreshButtonText: {
    color: '#d4d4d8',
    fontSize: 13,
    fontWeight: '600',
  },
});
