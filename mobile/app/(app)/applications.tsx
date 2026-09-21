import { useQuery } from '@tanstack/react-query';
import { useRouter } from 'expo-router';
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
import { StudentApplication } from '../../src/types';

export default function ApplicationsScreen() {
  const router = useRouter();
  const [page, setPage] = useState(0);

  const { data, isLoading, refetch } = useQuery({
    queryKey: ['applications-list', page],
    queryFn: () => api.getApplications(page, 20),
  });

  const applications = data?.content || [];

  const getStatusBadgeStyle = (status: string) => {
    switch (status) {
      case 'SHORTLISTED':
        return { bg: 'rgba(234, 179, 8, 0.2)', text: '#eab308' };
      case 'APPLIED':
        return { bg: 'rgba(34, 197, 94, 0.15)', text: '#4ade80' };
      case 'ELIGIBLE':
        return { bg: 'rgba(99, 102, 241, 0.15)', text: '#818cf8' };
      case 'REJECTED':
        return { bg: 'rgba(239, 68, 68, 0.15)', text: '#f87171' };
      default:
        return { bg: '#27272a', text: '#a1a1aa' };
    }
  };

  const renderItem = ({ item }: { item: StudentApplication }) => {
    const badgeStyle = getStatusBadgeStyle(item.status);
    return (
      <TouchableOpacity
        style={styles.card}
        onPress={() => router.push(`/placements/${item.driveId}`)}
        activeOpacity={0.8}
      >
        <View style={styles.cardHeader}>
          <View style={styles.companyInfo}>
            <Text style={styles.companyName}>{item.companyName}</Text>
            <Text style={styles.roleTitle}>{item.roleTitle || item.driveTitle}</Text>
          </View>
          <View style={[styles.statusBadge, { backgroundColor: badgeStyle.bg }]}>
            <Text style={[styles.statusText, { color: badgeStyle.text }]}>{item.status}</Text>
          </View>
        </View>

        <View style={styles.metaRow}>
          {item.appliedAt && (
            <Text style={styles.appliedDate}>
              Applied: {new Date(item.appliedAt).toLocaleDateString()}
            </Text>
          )}
          {item.deadline && (
            <Text style={styles.deadlineDate}>
              Deadline: {new Date(item.deadline).toLocaleDateString()}
            </Text>
          )}
        </View>

        {item.shortlistStatus === 'MATCHED' && (
          <View style={styles.shortlistTag}>
            <Text style={styles.shortlistTagText}>⭐ Shortlisted on Official List</Text>
          </View>
        )}
      </TouchableOpacity>
    );
  };

  return (
    <SafeAreaView style={styles.safeArea} edges={['bottom']}>
      <View style={styles.container}>
        <View style={styles.header}>
          <Text style={styles.title}>Application Tracker</Text>
          <Text style={styles.subtitle}>
            Monitor your placement submissions and shortlist results
          </Text>
        </View>

        {isLoading ? (
          <ActivityIndicator color="#818cf8" style={styles.loader} />
        ) : (
          <FlatList
            data={applications}
            keyExtractor={(item) => item.applicationId.toString()}
            renderItem={renderItem}
            contentContainerStyle={styles.listContent}
            refreshControl={
              <RefreshControl refreshing={isLoading} onRefresh={refetch} tintColor="#818cf8" />
            }
            ListEmptyComponent={
              <View style={styles.emptyContainer}>
                <Text style={styles.emptyIcon}>📝</Text>
                <Text style={styles.emptyTitle}>No Applications Yet</Text>
                <Text style={styles.emptySubtitle}>
                  You haven't submitted any placement applications yet.
                </Text>
                <TouchableOpacity
                  style={styles.browseBtn}
                  onPress={() => router.push('/placements')}
                >
                  <Text style={styles.browseBtnText}>Browse Eligible Drives</Text>
                </TouchableOpacity>
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
    marginBottom: 10,
  },
  companyInfo: {
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
    paddingHorizontal: 10,
    paddingVertical: 4,
    borderRadius: 6,
  },
  statusText: {
    fontSize: 11,
    fontWeight: '800',
  },
  metaRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    borderTopColor: '#27272a',
    borderTopWidth: 1,
    paddingTop: 10,
  },
  appliedDate: {
    fontSize: 11,
    color: '#71717a',
  },
  deadlineDate: {
    fontSize: 11,
    color: '#f59e0b',
  },
  shortlistTag: {
    backgroundColor: 'rgba(234, 179, 8, 0.15)',
    borderColor: 'rgba(234, 179, 8, 0.3)',
    borderWidth: 1,
    borderRadius: 8,
    paddingHorizontal: 10,
    paddingVertical: 6,
    marginTop: 10,
  },
  shortlistTagText: {
    color: '#eab308',
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
    marginBottom: 20,
  },
  browseBtn: {
    backgroundColor: '#6366f1',
    paddingHorizontal: 20,
    paddingVertical: 12,
    borderRadius: 12,
  },
  browseBtnText: {
    color: '#ffffff',
    fontSize: 14,
    fontWeight: '700',
  },
});
