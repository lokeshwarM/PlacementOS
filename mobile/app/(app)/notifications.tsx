import { useQuery } from '@tanstack/react-query';
import React, { useState } from 'react';
import {
  ActivityIndicator,
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
import { NotificationType, StudentNotification } from '../../src/types';

const FILTER_TYPES: { label: string; value?: NotificationType }[] = [
  { label: 'All' },
  { label: 'Eligibility', value: 'ELIGIBILITY' },
  { label: 'Shortlist', value: 'SHORTLIST' },
  { label: 'Deadline', value: 'DEADLINE' },
  { label: 'Reminder', value: 'REMINDER' },
];

export default function NotificationsScreen() {
  const [selectedType, setSelectedType] = useState<NotificationType | undefined>(undefined);
  const [page, setPage] = useState(0);

  const { data, isLoading, refetch } = useQuery({
    queryKey: ['notifications-list', page, selectedType],
    queryFn: () => api.getNotifications(page, 20, selectedType),
  });

  const notifications = data?.content || [];

  const getTypeIcon = (type: NotificationType) => {
    switch (type) {
      case 'ELIGIBILITY':
        return '🎯';
      case 'SHORTLIST':
        return '🏆';
      case 'DEADLINE':
        return '⏰';
      case 'REMINDER':
        return '🔔';
      default:
        return '📬';
    }
  };

  const renderItem = ({ item }: { item: StudentNotification }) => (
    <View style={styles.card}>
      <View style={styles.cardHeader}>
        <View style={styles.typeBadge}>
          <Text style={styles.typeIcon}>{getTypeIcon(item.notificationType)}</Text>
          <Text style={styles.typeText}>{item.notificationType}</Text>
        </View>
        <Text style={styles.timeText}>
          {new Date(item.createdAt).toLocaleDateString([], {
            month: 'short',
            day: 'numeric',
            hour: '2-digit',
            minute: '2-digit',
          })}
        </Text>
      </View>

      {item.companyName && (
        <Text style={styles.companyName}>
          {item.companyName} {item.roleTitle ? `• ${item.roleTitle}` : ''}
        </Text>
      )}

      {item.messagePayload && (
        <Text style={styles.messageText}>{item.messagePayload}</Text>
      )}

      <View style={styles.cardFooter}>
        <View style={styles.channelBadge}>
          <Text style={styles.channelText}>Via {item.channel}</Text>
        </View>
        <Text style={styles.statusText}>{item.status}</Text>
      </View>
    </View>
  );

  return (
    <SafeAreaView style={styles.safeArea} edges={['bottom']}>
      <View style={styles.container}>
        <View style={styles.header}>
          <Text style={styles.title}>Notification Center</Text>
          <Text style={styles.subtitle}>
            Placement updates, eligibility alerts, and shortlist notifications
          </Text>
        </View>

        {/* Filter Pills */}
        <View style={styles.filterRow}>
          {FILTER_TYPES.map((f) => {
            const isActive = selectedType === f.value;
            return (
              <TouchableOpacity
                key={f.label}
                style={[styles.filterPill, isActive && styles.filterPillActive]}
                onPress={() => setSelectedType(f.value)}
              >
                <Text style={[styles.filterPillText, isActive && styles.filterPillTextActive]}>
                  {f.label}
                </Text>
              </TouchableOpacity>
            );
          })}
        </View>

        {isLoading ? (
          <ActivityIndicator color="#818cf8" style={styles.loader} />
        ) : (
          <FlatList
            data={notifications}
            keyExtractor={(item) => item.id.toString()}
            renderItem={renderItem}
            contentContainerStyle={styles.listContent}
            refreshControl={
              <RefreshControl refreshing={isLoading} onRefresh={refetch} tintColor="#818cf8" />
            }
            ListEmptyComponent={
              <View style={styles.emptyContainer}>
                <Text style={styles.emptyIcon}>📭</Text>
                <Text style={styles.emptyTitle}>No Notifications</Text>
                <Text style={styles.emptySubtitle}>
                  You don't have any notifications in this category.
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
    marginBottom: 14,
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
  filterRow: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 8,
    marginBottom: 14,
  },
  filterPill: {
    paddingHorizontal: 12,
    paddingVertical: 6,
    borderRadius: 999,
    backgroundColor: '#18181b',
    borderColor: '#27272a',
    borderWidth: 1,
  },
  filterPillActive: {
    backgroundColor: 'rgba(99, 102, 241, 0.2)',
    borderColor: '#6366f1',
  },
  filterPillText: {
    fontSize: 11,
    fontWeight: '600',
    color: '#71717a',
  },
  filterPillTextActive: {
    color: '#818cf8',
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
    alignItems: 'center',
    marginBottom: 8,
  },
  typeBadge: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: '#27272a',
    paddingHorizontal: 8,
    paddingVertical: 4,
    borderRadius: 6,
    gap: 6,
  },
  typeIcon: {
    fontSize: 12,
  },
  typeText: {
    fontSize: 11,
    fontWeight: '700',
    color: '#d4d4d8',
  },
  timeText: {
    fontSize: 11,
    color: '#71717a',
  },
  companyName: {
    fontSize: 15,
    fontWeight: '700',
    color: '#f4f4f5',
    marginBottom: 4,
  },
  messageText: {
    fontSize: 13,
    color: '#a1a1aa',
    lineHeight: 18,
    marginBottom: 10,
  },
  cardFooter: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    borderTopColor: '#27272a',
    borderTopWidth: 1,
    paddingTop: 8,
  },
  channelBadge: {
    backgroundColor: 'rgba(99, 102, 241, 0.1)',
    paddingHorizontal: 6,
    paddingVertical: 2,
    borderRadius: 4,
  },
  channelText: {
    fontSize: 10,
    color: '#818cf8',
    fontWeight: '600',
  },
  statusText: {
    fontSize: 11,
    color: '#71717a',
    fontWeight: '500',
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
