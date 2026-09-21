import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import React from 'react';
import {
  ActivityIndicator,
  Alert,
  FlatList,
  RefreshControl,
  StyleSheet,
  Text,
  TouchableOpacity,
  View,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { api } from '../../src/api/client';
import BottomNav from '../../src/components/BottomNav';
import { StudentReminder } from '../../src/types';

export default function RemindersScreen() {
  const queryClient = useQueryClient();

  const { data, isLoading, refetch } = useQuery({
    queryKey: ['reminders-full-list'],
    queryFn: () => api.getReminders(0, 20),
  });

  const stopMutation = useMutation({
    mutationFn: (reminderId: number) => api.stopReminder(reminderId, 'STUDENT_PORTAL_STOP'),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['reminders-full-list'] });
      queryClient.invalidateQueries({ queryKey: ['dashboard-reminders'] });
      Alert.alert('Reminder Stopped', 'You will no longer receive deadline reminders for this drive.');
    },
    onError: (err: Error) => {
      Alert.alert('Error', err.message || 'Failed to stop reminder.');
    },
  });

  const reminders = data?.content || [];

  const renderItem = ({ item }: { item: StudentReminder }) => (
    <View style={styles.card}>
      <View style={styles.cardHeader}>
        <View style={styles.titleGroup}>
          <Text style={styles.companyName}>{item.companyName}</Text>
          <Text style={styles.roleTitle}>{item.roleTitle || 'Placement Drive'}</Text>
        </View>
        <View
          style={[
            styles.statusBadge,
            item.isActive ? styles.badgeActive : styles.badgeInactive,
          ]}
        >
          <Text
            style={[
              styles.statusText,
              item.isActive ? styles.badgeActiveText : styles.badgeInactiveText,
            ]}
          >
            {item.status}
          </Text>
        </View>
      </View>

      <View style={styles.detailsGrid}>
        <View style={styles.detailItem}>
          <Text style={styles.detailLabel}>Next Alert</Text>
          <Text style={styles.detailValue}>
            {new Date(item.scheduledFor).toLocaleString([], {
              month: 'short',
              day: 'numeric',
              hour: '2-digit',
              minute: '2-digit',
            })}
          </Text>
        </View>

        <View style={styles.detailItem}>
          <Text style={styles.detailLabel}>Frequency</Text>
          <Text style={styles.detailValue}>Every {item.intervalMinutes}m</Text>
        </View>

        <View style={styles.detailItem}>
          <Text style={styles.detailLabel}>Sent</Text>
          <Text style={styles.detailValue}>
            {item.remindersSent} / {item.maxReminders}
          </Text>
        </View>
      </View>

      {item.isActive && (
        <View style={styles.cardFooter}>
          <Text style={styles.footerNote}>Reminders deliver automatically via Telegram.</Text>
          <TouchableOpacity
            style={styles.stopButton}
            onPress={() => {
              Alert.alert(
                'Stop Reminders?',
                `Are you sure you want to stop reminders for ${item.companyName}?`,
                [
                  { text: 'Cancel', style: 'cancel' },
                  {
                    text: 'Stop Reminders',
                    style: 'destructive',
                    onPress: () => stopMutation.mutate(item.id),
                  },
                ]
              );
            }}
            disabled={stopMutation.isPending}
          >
            <Text style={styles.stopButtonText}>Stop Alerts</Text>
          </TouchableOpacity>
        </View>
      )}
    </View>
  );

  return (
    <SafeAreaView style={styles.safeArea} edges={['bottom']}>
      <View style={styles.container}>
        <View style={styles.header}>
          <Text style={styles.title}>Active Reminders</Text>
          <Text style={styles.subtitle}>
            Automated server-side reminder lifecycle for upcoming deadlines
          </Text>
        </View>

        {isLoading ? (
          <ActivityIndicator color="#818cf8" style={styles.loader} />
        ) : (
          <FlatList
            data={reminders}
            keyExtractor={(item) => item.id.toString()}
            renderItem={renderItem}
            contentContainerStyle={styles.listContent}
            refreshControl={
              <RefreshControl refreshing={isLoading} onRefresh={refetch} tintColor="#818cf8" />
            }
            ListEmptyComponent={
              <View style={styles.emptyContainer}>
                <Text style={styles.emptyIcon}>🔔</Text>
                <Text style={styles.emptyTitle}>No Active Reminders</Text>
                <Text style={styles.emptySubtitle}>
                  Reminders are scheduled automatically when you become eligible for a placement drive.
                </Text>
              </View>
            }
          />
        )}
      </View>
      <BottomNav />
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safeArea: {
    flex: 1,
    backgroundColor: '#09090b',
  },
  container: {
    flex: 1,
    paddingHorizontal: 16,
    paddingTop: 12,
  },
  header: {
    marginBottom: 16,
  },
  title: {
    fontSize: 22,
    fontWeight: '800',
    color: '#f4f4f5',
  },
  subtitle: {
    fontSize: 13,
    color: '#a1a1aa',
    marginTop: 2,
  },
  loader: {
    marginTop: 40,
  },
  listContent: {
    paddingBottom: 24,
  },
  card: {
    backgroundColor: '#18181b',
    borderColor: '#27272a',
    borderWidth: 1,
    borderRadius: 18,
    padding: 16,
    marginBottom: 12,
  },
  cardHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'flex-start',
    marginBottom: 12,
  },
  titleGroup: {
    flex: 1,
  },
  companyName: {
    fontSize: 16,
    fontWeight: '800',
    color: '#f4f4f5',
  },
  roleTitle: {
    fontSize: 13,
    color: '#a1a1aa',
    marginTop: 2,
  },
  statusBadge: {
    paddingHorizontal: 8,
    paddingVertical: 3,
    borderRadius: 6,
  },
  badgeActive: {
    backgroundColor: 'rgba(34, 197, 94, 0.15)',
    borderColor: 'rgba(34, 197, 94, 0.3)',
    borderWidth: 1,
  },
  badgeInactive: {
    backgroundColor: '#27272a',
  },
  statusText: {
    fontSize: 11,
    fontWeight: '700',
  },
  badgeActiveText: {
    color: '#4ade80',
  },
  badgeInactiveText: {
    color: '#71717a',
  },
  detailsGrid: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    backgroundColor: '#27272a',
    borderRadius: 12,
    padding: 12,
    marginBottom: 12,
  },
  detailItem: {
    alignItems: 'center',
  },
  detailLabel: {
    fontSize: 10,
    color: '#a1a1aa',
    marginBottom: 2,
  },
  detailValue: {
    fontSize: 12,
    fontWeight: '700',
    color: '#f4f4f5',
  },
  cardFooter: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    borderTopColor: '#27272a',
    borderTopWidth: 1,
    paddingTop: 10,
  },
  footerNote: {
    fontSize: 11,
    color: '#71717a',
    flex: 1,
    marginRight: 8,
  },
  stopButton: {
    backgroundColor: 'rgba(239, 68, 68, 0.15)',
    borderColor: 'rgba(239, 68, 68, 0.3)',
    borderWidth: 1,
    paddingHorizontal: 12,
    paddingVertical: 6,
    borderRadius: 8,
  },
  stopButtonText: {
    color: '#f87171',
    fontSize: 12,
    fontWeight: '700',
  },
  emptyContainer: {
    paddingVertical: 48,
    alignItems: 'center',
  },
  emptyIcon: {
    fontSize: 36,
    marginBottom: 12,
  },
  emptyTitle: {
    fontSize: 16,
    fontWeight: '700',
    color: '#f4f4f5',
    marginBottom: 4,
  },
  emptySubtitle: {
    fontSize: 13,
    color: '#71717a',
    textAlign: 'center',
  },
});
