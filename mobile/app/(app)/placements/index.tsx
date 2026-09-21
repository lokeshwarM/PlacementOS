import { useQuery } from '@tanstack/react-query';
import { useRouter } from 'expo-router';
import React, { useState } from 'react';
import {
  ActivityIndicator,
  FlatList,
  RefreshControl,
  StyleSheet,
  Text,
  TextInput,
  TouchableOpacity,
  View,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { api } from '../../../src/api/client';
import BottomNav from '../../../src/components/BottomNav';
import { StudentPlacementDriveCard } from '../../../src/types';

export default function PlacementsListScreen() {
  const router = useRouter();
  const [searchQuery, setSearchQuery] = useState('');
  const [filterEligibility, setFilterEligibility] = useState<'ALL' | 'ELIGIBLE' | 'APPLIED' | 'SHORTLISTED'>('ALL');
  const [page, setPage] = useState(0);

  const { data, isLoading, refetch } = useQuery({
    queryKey: ['placements-list', page],
    queryFn: () => api.getPlacements(page, 20),
  });

  const drives = data?.content || [];

  const filteredDrives = drives.filter((drive) => {
    const matchesSearch =
      drive.companyName.toLowerCase().includes(searchQuery.toLowerCase()) ||
      drive.title.toLowerCase().includes(searchQuery.toLowerCase());

    if (!matchesSearch) return false;

    if (filterEligibility === 'ELIGIBLE') {
      return drive.overallEligibility === 'ELIGIBLE' && drive.applicationStatus !== 'APPLIED';
    }
    if (filterEligibility === 'APPLIED') {
      return drive.applicationStatus === 'APPLIED';
    }
    if (filterEligibility === 'SHORTLISTED') {
      return drive.shortlistStatus === 'MATCHED' || drive.applicationStatus === 'SHORTLISTED';
    }

    return true;
  });

  const renderItem = ({ item }: { item: StudentPlacementDriveCard }) => (
    <TouchableOpacity
      style={styles.card}
      onPress={() => router.push(`/placements/${item.driveId}`)}
      activeOpacity={0.8}
    >
      <View style={styles.cardHeader}>
        <View style={styles.titleGroup}>
          <Text style={styles.companyName}>{item.companyName}</Text>
          <Text style={styles.driveTitle}>{item.title}</Text>
        </View>
        <View
          style={[
            styles.statusBadge,
            item.overallEligibility === 'ELIGIBLE'
              ? styles.eligibleBadge
              : styles.notEligibleBadge,
          ]}
        >
          <Text
            style={[
              styles.statusText,
              item.overallEligibility === 'ELIGIBLE'
                ? styles.eligibleText
                : styles.notEligibleText,
            ]}
          >
            {item.overallEligibility}
          </Text>
        </View>
      </View>

      {item.applicationDeadline && (
        <View style={styles.deadlineContainer}>
          <Text style={styles.deadlineIcon}>⏰</Text>
          <Text style={styles.deadlineText}>
            Deadline: {new Date(item.applicationDeadline).toLocaleDateString()}
          </Text>
        </View>
      )}

      {/* Role-specific breakdown */}
      <View style={styles.rolesContainer}>
        <Text style={styles.rolesLabel}>Roles & Eligibility:</Text>
        <View style={styles.rolesGrid}>
          {item.roles.map((r) => (
            <View
              key={r.roleId}
              style={[
                styles.roleChip,
                r.decision === 'ELIGIBLE' ? styles.chipEligible : styles.chipNotEligible,
              ]}
            >
              <Text
                style={[
                  styles.roleChipText,
                  r.decision === 'ELIGIBLE' ? styles.chipEligibleText : styles.chipNotEligibleText,
                ]}
              >
                {r.roleTitle}: {r.decision}
              </Text>
            </View>
          ))}
        </View>
      </View>

      {/* Footer / Status */}
      <View style={styles.cardFooter}>
        <View style={styles.appStatusGroup}>
          <Text style={styles.appStatusLabel}>Status: </Text>
          <Text style={styles.appStatusValue}>{item.applicationStatus}</Text>
        </View>
        <Text style={styles.detailsLink}>Details →</Text>
      </View>
    </TouchableOpacity>
  );

  return (
    <SafeAreaView style={styles.safeArea} edges={['bottom']}>
      <View style={styles.container}>
        {/* Search Bar */}
        <View style={styles.searchContainer}>
          <TextInput
            style={styles.searchInput}
            placeholder="Search company or drive..."
            placeholderTextColor="#71717a"
            value={searchQuery}
            onChangeText={setSearchQuery}
          />
        </View>

        {/* Filter Pills */}
        <View style={styles.filterRow}>
          {(['ALL', 'ELIGIBLE', 'APPLIED', 'SHORTLISTED'] as const).map((filter) => (
            <TouchableOpacity
              key={filter}
              style={[
                styles.filterPill,
                filterEligibility === filter && styles.filterPillActive,
              ]}
              onPress={() => setFilterEligibility(filter)}
            >
              <Text
                style={[
                  styles.filterPillText,
                  filterEligibility === filter && styles.filterPillTextActive,
                ]}
              >
                {filter}
              </Text>
            </TouchableOpacity>
          ))}
        </View>

        {/* Drives List */}
        {isLoading ? (
          <ActivityIndicator color="#818cf8" style={styles.loader} />
        ) : (
          <FlatList
            data={filteredDrives}
            keyExtractor={(item) => item.driveId.toString()}
            renderItem={renderItem}
            contentContainerStyle={styles.listContent}
            refreshControl={
              <RefreshControl refreshing={isLoading} onRefresh={refetch} tintColor="#818cf8" />
            }
            ListEmptyComponent={
              <View style={styles.emptyContainer}>
                <Text style={styles.emptyIcon}>🔍</Text>
                <Text style={styles.emptyText}>No placement drives match your filters</Text>
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
  searchContainer: {
    marginBottom: 12,
  },
  searchInput: {
    backgroundColor: '#18181b',
    borderColor: '#27272a',
    borderWidth: 1,
    borderRadius: 12,
    paddingHorizontal: 16,
    paddingVertical: 10,
    color: '#f4f4f5',
    fontSize: 14,
  },
  filterRow: {
    flexDirection: 'row',
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
  listContent: {
    paddingBottom: 24,
  },
  loader: {
    marginTop: 40,
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
    marginBottom: 8,
  },
  titleGroup: {
    flex: 1,
  },
  companyName: {
    fontSize: 16,
    fontWeight: '800',
    color: '#f4f4f5',
  },
  driveTitle: {
    fontSize: 13,
    color: '#a1a1aa',
    marginTop: 2,
  },
  statusBadge: {
    paddingHorizontal: 8,
    paddingVertical: 3,
    borderRadius: 6,
  },
  eligibleBadge: {
    backgroundColor: 'rgba(34, 197, 94, 0.15)',
    borderColor: 'rgba(34, 197, 94, 0.3)',
    borderWidth: 1,
  },
  notEligibleBadge: {
    backgroundColor: 'rgba(239, 68, 68, 0.15)',
    borderColor: 'rgba(239, 68, 68, 0.3)',
    borderWidth: 1,
  },
  statusText: {
    fontSize: 11,
    fontWeight: '700',
  },
  eligibleText: {
    color: '#4ade80',
  },
  notEligibleText: {
    color: '#f87171',
  },
  deadlineContainer: {
    flexDirection: 'row',
    alignItems: 'center',
    marginBottom: 10,
  },
  deadlineIcon: {
    fontSize: 12,
    marginRight: 6,
  },
  deadlineText: {
    color: '#f59e0b',
    fontSize: 12,
    fontWeight: '600',
  },
  rolesContainer: {
    marginBottom: 10,
  },
  rolesLabel: {
    fontSize: 11,
    fontWeight: '600',
    color: '#71717a',
    marginBottom: 4,
  },
  rolesGrid: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 6,
  },
  roleChip: {
    paddingHorizontal: 8,
    paddingVertical: 4,
    borderRadius: 6,
  },
  chipEligible: {
    backgroundColor: 'rgba(34, 197, 94, 0.1)',
  },
  chipNotEligible: {
    backgroundColor: 'rgba(239, 68, 68, 0.1)',
  },
  roleChipText: {
    fontSize: 11,
    fontWeight: '600',
  },
  chipEligibleText: {
    color: '#4ade80',
  },
  chipNotEligibleText: {
    color: '#f87171',
  },
  cardFooter: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    borderTopColor: '#27272a',
    borderTopWidth: 1,
    paddingTop: 10,
  },
  appStatusGroup: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  appStatusLabel: {
    fontSize: 11,
    color: '#71717a',
  },
  appStatusValue: {
    fontSize: 11,
    fontWeight: '700',
    color: '#d4d4d8',
  },
  detailsLink: {
    fontSize: 12,
    fontWeight: '700',
    color: '#818cf8',
  },
  emptyContainer: {
    paddingVertical: 48,
    alignItems: 'center',
  },
  emptyIcon: {
    fontSize: 32,
    marginBottom: 10,
  },
  emptyText: {
    color: '#71717a',
    fontSize: 14,
  },
});
