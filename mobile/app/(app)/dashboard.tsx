import { useQuery } from '@tanstack/react-query';
import { useRouter } from 'expo-router';
import React from 'react';
import {
  ActivityIndicator,
  RefreshControl,
  ScrollView,
  StyleSheet,
  Text,
  TouchableOpacity,
  View,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { api } from '../../src/api/client';
import { useAuth } from '../../src/auth/AuthContext';
import BottomNav from '../../src/components/BottomNav';
import { StudentPlacementDriveCard } from '../../src/types';

export default function DashboardScreen() {
  const { user, profile } = useAuth();
  const router = useRouter();

  // Queries
  const {
    data: placementsData,
    isLoading: loadingPlacements,
    refetch: refetchPlacements,
  } = useQuery({
    queryKey: ['dashboard-placements'],
    queryFn: () => api.getPlacements(0, 10),
  });

  const {
    data: applicationsData,
    isLoading: loadingApplications,
    refetch: refetchApplications,
  } = useQuery({
    queryKey: ['dashboard-applications'],
    queryFn: () => api.getApplications(0, 5),
  });

  const {
    data: remindersData,
    isLoading: loadingReminders,
    refetch: refetchReminders,
  } = useQuery({
    queryKey: ['dashboard-reminders'],
    queryFn: () => api.getReminders(0, 5),
  });

  const {
    data: telegramData,
    refetch: refetchTelegram,
  } = useQuery({
    queryKey: ['dashboard-telegram'],
    queryFn: () => api.getTelegramStatus(),
  });

  const refreshing = loadingPlacements || loadingApplications || loadingReminders;

  const onRefresh = () => {
    refetchPlacements();
    refetchApplications();
    refetchReminders();
    refetchTelegram();
  };

  const drives = placementsData?.content || [];
  const actionRequiredDrives = drives.filter(
    (d) => d.isActionable && !d.deadlinePassed && d.applicationStatus !== 'APPLIED'
  );
  const shortlistedDrives = drives.filter(
    (d) => d.shortlistStatus === 'MATCHED' || d.applicationStatus === 'SHORTLISTED'
  );
  const activeReminders = (remindersData?.content || []).filter((r) => r.isActive);

  return (
    <SafeAreaView style={styles.safeArea} edges={['bottom']}>
      <ScrollView
        contentContainerStyle={styles.scrollContent}
        refreshControl={<RefreshControl refreshing={refreshing} onRefresh={onRefresh} tintColor="#818cf8" />}
      >
        {/* Welcome Banner */}
        <View style={styles.welcomeBanner}>
          <View style={styles.welcomeTextGroup}>
            <Text style={styles.greeting}>
              Hello, {profile?.name || user?.studentName || 'Student'} 👋
            </Text>
            <Text style={styles.subgreeting}>
              {profile?.registrationNumber || user?.registrationNumber || 'Student ID'} • {profile?.branch || 'Branch'}
            </Text>
          </View>
          <View style={styles.cgpaBadge}>
            <Text style={styles.cgpaLabel}>CGPA</Text>
            <Text style={styles.cgpaValue}>{profile?.cgpa ? Number(profile.cgpa).toFixed(2) : '—'}</Text>
          </View>
        </View>

        {/* Telegram Connection Alert (if not linked) */}
        {telegramData && !telegramData.linked && (
          <TouchableOpacity
            style={styles.telegramBanner}
            onPress={() => router.push('/telegram')}
            activeOpacity={0.8}
          >
            <Text style={styles.telegramIcon}>✈️</Text>
            <View style={styles.telegramTextGroup}>
              <Text style={styles.telegramTitle}>Connect Telegram for Live Alerts</Text>
              <Text style={styles.telegramSubtitle}>
                Receive real-time drive notifications & 1-tap reminders on your phone
              </Text>
            </View>
            <Text style={styles.chevron}>›</Text>
          </TouchableOpacity>
        )}

        {/* Action Required Section */}
        <View style={styles.sectionHeader}>
          <Text style={styles.sectionTitle}>⚡ Action Required Now</Text>
          <Text style={styles.badgeCount}>{actionRequiredDrives.length}</Text>
        </View>

        {loadingPlacements ? (
          <ActivityIndicator color="#818cf8" style={styles.loader} />
        ) : actionRequiredDrives.length === 0 ? (
          <View style={styles.emptyCard}>
            <Text style={styles.emptyIcon}>🎉</Text>
            <Text style={styles.emptyTitle}>You're all caught up!</Text>
            <Text style={styles.emptySubtitle}>No pending placement applications require your immediate action.</Text>
          </View>
        ) : (
          actionRequiredDrives.map((drive) => (
            <TouchableOpacity
              key={drive.driveId}
              style={styles.actionCard}
              onPress={() => router.push(`/placements/${drive.driveId}`)}
              activeOpacity={0.8}
            >
              <View style={styles.cardHeader}>
                <View style={styles.companyGroup}>
                  <Text style={styles.companyName}>{drive.companyName}</Text>
                  <Text style={styles.driveTitle}>{drive.title}</Text>
                </View>
                <View style={styles.eligibleBadge}>
                  <Text style={styles.eligibleText}>ELIGIBLE</Text>
                </View>
              </View>

              {drive.applicationDeadline && (
                <View style={styles.deadlineContainer}>
                  <Text style={styles.deadlineIcon}>⏰</Text>
                  <Text style={styles.deadlineText}>
                    Deadline: {new Date(drive.applicationDeadline).toLocaleDateString()}
                  </Text>
                </View>
              )}

              <View style={styles.rolesRow}>
                {drive.roles.map((r) => (
                  <View
                    key={r.roleId}
                    style={[
                      styles.roleChip,
                      r.decision === 'ELIGIBLE' ? styles.roleEligible : styles.roleNotEligible,
                    ]}
                  >
                    <Text
                      style={[
                        styles.roleChipText,
                        r.decision === 'ELIGIBLE' ? styles.roleEligibleText : styles.roleNotEligibleText,
                      ]}
                    >
                      {r.roleTitle}: {r.decision}
                    </Text>
                  </View>
                ))}
              </View>

              <View style={styles.cardFooter}>
                <Text style={styles.actionPrompt}>{drive.actionRequired}</Text>
                <Text style={styles.viewDetailLink}>View Details →</Text>
              </View>
            </TouchableOpacity>
          ))
        )}

        {/* Shortlisted Section */}
        {shortlistedDrives.length > 0 && (
          <>
            <View style={styles.sectionHeader}>
              <Text style={styles.sectionTitle}>🏆 Shortlisted Drives</Text>
              <Text style={[styles.badgeCount, styles.badgeGold]}>{shortlistedDrives.length}</Text>
            </View>
            {shortlistedDrives.map((drive) => (
              <TouchableOpacity
                key={drive.driveId}
                style={styles.shortlistCard}
                onPress={() => router.push(`/placements/${drive.driveId}`)}
                activeOpacity={0.8}
              >
                <View style={styles.cardHeader}>
                  <View>
                    <Text style={styles.companyName}>{drive.companyName}</Text>
                    <Text style={styles.driveTitle}>{drive.title}</Text>
                  </View>
                  <View style={styles.shortlistBadge}>
                    <Text style={styles.shortlistText}>SHORTLISTED</Text>
                  </View>
                </View>
              </TouchableOpacity>
            ))}
          </>
        )}

        {/* Active Reminders */}
        {activeReminders.length > 0 && (
          <>
            <View style={styles.sectionHeader}>
              <Text style={styles.sectionTitle}>🔔 Active Reminders</Text>
              <TouchableOpacity onPress={() => router.push('/reminders')}>
                <Text style={styles.seeAllLink}>See all ({activeReminders.length})</Text>
              </TouchableOpacity>
            </View>
            {activeReminders.slice(0, 3).map((r) => (
              <View key={r.id} style={styles.reminderCard}>
                <View style={styles.reminderInfo}>
                  <Text style={styles.reminderCompany}>{r.companyName}</Text>
                  <Text style={styles.reminderTime}>
                    Next alert: {new Date(r.scheduledFor).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                  </Text>
                </View>
                <TouchableOpacity
                  style={styles.stopBtn}
                  onPress={async () => {
                    await api.stopReminder(r.id, 'STUDENT_STOPPED');
                    refetchReminders();
                  }}
                >
                  <Text style={styles.stopBtnText}>Stop</Text>
                </TouchableOpacity>
              </View>
            ))}
          </>
        )}

        {/* Quick Nav Cards */}
        <View style={styles.sectionHeader}>
          <Text style={styles.sectionTitle}>Navigation</Text>
        </View>
        <View style={styles.grid}>
          <TouchableOpacity
            style={styles.gridCard}
            onPress={() => router.push('/placements')}
            activeOpacity={0.8}
          >
            <Text style={styles.gridIcon}>💼</Text>
            <Text style={styles.gridTitle}>All Drives</Text>
            <Text style={styles.gridSubtitle}>Browse placement catalog</Text>
          </TouchableOpacity>

          <TouchableOpacity
            style={styles.gridCard}
            onPress={() => router.push('/applications')}
            activeOpacity={0.8}
          >
            <Text style={styles.gridIcon}>📝</Text>
            <Text style={styles.gridTitle}>Applications</Text>
            <Text style={styles.gridSubtitle}>Track submission status</Text>
          </TouchableOpacity>

          <TouchableOpacity
            style={styles.gridCard}
            onPress={() => router.push('/notifications')}
            activeOpacity={0.8}
          >
            <Text style={styles.gridIcon}>🔔</Text>
            <Text style={styles.gridTitle}>Alerts</Text>
            <Text style={styles.gridSubtitle}>View notification history</Text>
          </TouchableOpacity>

          <TouchableOpacity
            style={styles.gridCard}
            onPress={() => router.push('/telegram')}
            activeOpacity={0.8}
          >
            <Text style={styles.gridIcon}>✈️</Text>
            <Text style={styles.gridTitle}>Telegram</Text>
            <Text style={styles.gridSubtitle}>
              {telegramData?.linked ? 'Connected' : 'Not linked'}
            </Text>
          </TouchableOpacity>
        </View>
      </ScrollView>

      {/* Bottom Nav */}
      <BottomNav />
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
    paddingBottom: 24,
  },
  welcomeBanner: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    backgroundColor: '#18181b',
    borderColor: '#27272a',
    borderWidth: 1,
    borderRadius: 20,
    padding: 18,
    marginBottom: 16,
  },
  welcomeTextGroup: {
    flex: 1,
  },
  greeting: {
    fontSize: 20,
    fontWeight: '800',
    color: '#f4f4f5',
    marginBottom: 4,
  },
  subgreeting: {
    fontSize: 13,
    color: '#a1a1aa',
  },
  cgpaBadge: {
    backgroundColor: 'rgba(99, 102, 241, 0.15)',
    borderColor: 'rgba(99, 102, 241, 0.3)',
    borderWidth: 1,
    borderRadius: 14,
    paddingHorizontal: 12,
    paddingVertical: 8,
    alignItems: 'center',
  },
  cgpaLabel: {
    fontSize: 10,
    fontWeight: '700',
    color: '#818cf8',
  },
  cgpaValue: {
    fontSize: 18,
    fontWeight: '800',
    color: '#f4f4f5',
  },
  telegramBanner: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: 'rgba(56, 189, 248, 0.1)',
    borderColor: 'rgba(56, 189, 248, 0.3)',
    borderWidth: 1,
    borderRadius: 16,
    padding: 14,
    marginBottom: 16,
  },
  telegramIcon: {
    fontSize: 22,
    marginRight: 12,
  },
  telegramTextGroup: {
    flex: 1,
  },
  telegramTitle: {
    fontSize: 13,
    fontWeight: '700',
    color: '#38bdf8',
  },
  telegramSubtitle: {
    fontSize: 11,
    color: '#94a3b8',
    marginTop: 2,
  },
  chevron: {
    fontSize: 20,
    color: '#38bdf8',
    marginLeft: 8,
  },
  sectionHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    marginTop: 12,
    marginBottom: 12,
  },
  sectionTitle: {
    fontSize: 15,
    fontWeight: '700',
    color: '#e4e4e7',
  },
  badgeCount: {
    backgroundColor: '#27272a',
    color: '#a1a1aa',
    fontSize: 12,
    fontWeight: '700',
    paddingHorizontal: 8,
    paddingVertical: 2,
    borderRadius: 999,
  },
  badgeGold: {
    backgroundColor: 'rgba(234, 179, 8, 0.2)',
    color: '#eab308',
  },
  loader: {
    marginVertical: 24,
  },
  emptyCard: {
    backgroundColor: '#18181b',
    borderColor: '#27272a',
    borderWidth: 1,
    borderRadius: 16,
    padding: 24,
    alignItems: 'center',
    marginBottom: 16,
  },
  emptyIcon: {
    fontSize: 28,
    marginBottom: 8,
  },
  emptyTitle: {
    fontSize: 14,
    fontWeight: '700',
    color: '#f4f4f5',
    marginBottom: 4,
  },
  emptySubtitle: {
    fontSize: 12,
    color: '#71717a',
    textAlign: 'center',
  },
  actionCard: {
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
  companyGroup: {
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
  eligibleBadge: {
    backgroundColor: 'rgba(34, 197, 94, 0.15)',
    borderColor: 'rgba(34, 197, 94, 0.3)',
    borderWidth: 1,
    paddingHorizontal: 8,
    paddingVertical: 3,
    borderRadius: 6,
  },
  eligibleText: {
    color: '#4ade80',
    fontSize: 11,
    fontWeight: '700',
  },
  deadlineContainer: {
    flexDirection: 'row',
    alignItems: 'center',
    marginBottom: 10,
  },
  deadlineIcon: {
    fontSize: 13,
    marginRight: 6,
  },
  deadlineText: {
    color: '#f59e0b',
    fontSize: 12,
    fontWeight: '600',
  },
  rolesRow: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 6,
    marginBottom: 12,
  },
  roleChip: {
    paddingHorizontal: 8,
    paddingVertical: 4,
    borderRadius: 6,
  },
  roleEligible: {
    backgroundColor: 'rgba(34, 197, 94, 0.1)',
  },
  roleNotEligible: {
    backgroundColor: 'rgba(239, 68, 68, 0.1)',
  },
  roleChipText: {
    fontSize: 11,
    fontWeight: '600',
  },
  roleEligibleText: {
    color: '#4ade80',
  },
  roleNotEligibleText: {
    color: '#f87171',
  },
  cardFooter: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    borderTopColor: '#27272a',
    borderTopWidth: 1,
    paddingTop: 10,
    marginTop: 2,
  },
  actionPrompt: {
    fontSize: 12,
    color: '#818cf8',
    fontWeight: '600',
    flex: 1,
  },
  viewDetailLink: {
    fontSize: 12,
    color: '#818cf8',
    fontWeight: '700',
  },
  shortlistCard: {
    backgroundColor: 'rgba(234, 179, 8, 0.08)',
    borderColor: 'rgba(234, 179, 8, 0.25)',
    borderWidth: 1,
    borderRadius: 16,
    padding: 14,
    marginBottom: 10,
  },
  shortlistBadge: {
    backgroundColor: 'rgba(234, 179, 8, 0.2)',
    paddingHorizontal: 8,
    paddingVertical: 4,
    borderRadius: 6,
  },
  shortlistText: {
    color: '#eab308',
    fontSize: 11,
    fontWeight: '800',
  },
  seeAllLink: {
    color: '#818cf8',
    fontSize: 12,
    fontWeight: '600',
  },
  reminderCard: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    backgroundColor: '#18181b',
    borderColor: '#27272a',
    borderWidth: 1,
    borderRadius: 14,
    padding: 12,
    marginBottom: 8,
  },
  reminderInfo: {
    flex: 1,
  },
  reminderCompany: {
    fontSize: 13,
    fontWeight: '700',
    color: '#f4f4f5',
  },
  reminderTime: {
    fontSize: 11,
    color: '#71717a',
    marginTop: 2,
  },
  stopBtn: {
    backgroundColor: '#27272a',
    paddingHorizontal: 12,
    paddingVertical: 6,
    borderRadius: 8,
  },
  stopBtnText: {
    color: '#f87171',
    fontSize: 12,
    fontWeight: '600',
  },
  grid: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 12,
  },
  gridCard: {
    flex: 1,
    minWidth: '45%',
    backgroundColor: '#18181b',
    borderColor: '#27272a',
    borderWidth: 1,
    borderRadius: 16,
    padding: 16,
  },
  gridIcon: {
    fontSize: 24,
    marginBottom: 8,
  },
  gridTitle: {
    fontSize: 14,
    fontWeight: '700',
    color: '#f4f4f5',
    marginBottom: 2,
  },
  gridSubtitle: {
    fontSize: 11,
    color: '#71717a',
  },
});
