import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useLocalSearchParams, useRouter } from 'expo-router';
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
import { api } from '../../../src/api/client';

export default function PlacementDetailScreen() {
  const { driveId } = useLocalSearchParams<{ driveId: string }>();
  const router = useRouter();
  const queryClient = useQueryClient();
  const [applyError, setApplyError] = useState<string | null>(null);

  const numericDriveId = driveId ? parseInt(driveId, 10) : 0;

  const { data: drive, isLoading, error, refetch } = useQuery({
    queryKey: ['placement-detail', numericDriveId],
    queryFn: () => api.getPlacementDetail(numericDriveId),
    enabled: numericDriveId > 0,
  });

  const applyMutation = useMutation({
    mutationFn: (applicationId: number) => api.apply(applicationId),
    onSuccess: () => {
      setApplyError(null);
      queryClient.invalidateQueries({ queryKey: ['placement-detail', numericDriveId] });
      queryClient.invalidateQueries({ queryKey: ['dashboard-placements'] });
      queryClient.invalidateQueries({ queryKey: ['dashboard-applications'] });
      queryClient.invalidateQueries({ queryKey: ['dashboard-reminders'] });
      queryClient.invalidateQueries({ queryKey: ['placements-list'] });
      queryClient.invalidateQueries({ queryKey: ['applications-list'] });
      Alert.alert('Application Submitted', 'Your application has been marked as APPLIED.');
    },
    onError: (err: Error) => {
      setApplyError(err.message || 'Failed to submit application.');
    },
  });

  if (isLoading) {
    return (
      <SafeAreaView style={styles.safeArea}>
        <View style={styles.centerContainer}>
          <ActivityIndicator size="large" color="#818cf8" />
          <Text style={styles.loadingText}>Loading drive details...</Text>
        </View>
      </SafeAreaView>
    );
  }

  if (error || !drive) {
    return (
      <SafeAreaView style={styles.safeArea}>
        <View style={styles.centerContainer}>
          <Text style={styles.errorIcon}>⚠️</Text>
          <Text style={styles.errorTitle}>Drive Not Found</Text>
          <Text style={styles.errorSubtitle}>Unable to load placement drive details.</Text>
          <TouchableOpacity style={styles.retryBtn} onPress={() => refetch()}>
            <Text style={styles.retryText}>Retry</Text>
          </TouchableOpacity>
        </View>
      </SafeAreaView>
    );
  }

  const isApplied = drive.applicationStatus === 'APPLIED';
  const isShortlisted = drive.shortlistStatus === 'MATCHED' || drive.applicationStatus === 'SHORTLISTED';
  const canApply = drive.overallEligibility === 'ELIGIBLE' && !isApplied && drive.applicationId && !drive.deadlinePassed;

  return (
    <SafeAreaView style={styles.safeArea} edges={['bottom']}>
      <ScrollView contentContainerStyle={styles.scrollContent}>
        {/* Company & Title Card */}
        <View style={styles.card}>
          <View style={styles.cardHeader}>
            <View style={styles.titleGroup}>
              <Text style={styles.companyName}>{drive.companyName}</Text>
              <Text style={styles.driveTitle}>{drive.title}</Text>
            </View>
            <View
              style={[
                styles.badge,
                drive.overallEligibility === 'ELIGIBLE' ? styles.badgeEligible : styles.badgeNotEligible,
              ]}
            >
              <Text
                style={[
                  styles.badgeText,
                  drive.overallEligibility === 'ELIGIBLE' ? styles.badgeEligibleText : styles.badgeNotEligibleText,
                ]}
              >
                {drive.overallEligibility}
              </Text>
            </View>
          </View>

          {drive.description && (
            <Text style={styles.descriptionText}>{drive.description}</Text>
          )}

          {/* Key Dates */}
          <View style={styles.datesContainer}>
            {drive.applicationDeadline && (
              <View style={styles.dateRow}>
                <Text style={styles.dateLabel}>⏰ Application Deadline:</Text>
                <Text style={styles.dateValue}>
                  {new Date(drive.applicationDeadline).toLocaleString()}
                </Text>
              </View>
            )}
            {drive.driveDate && (
              <View style={styles.dateRow}>
                <Text style={styles.dateLabel}>📅 Drive Date:</Text>
                <Text style={styles.dateValue}>
                  {new Date(drive.driveDate).toLocaleDateString()}
                </Text>
              </View>
            )}
            {drive.testDate && (
              <View style={styles.dateRow}>
                <Text style={styles.dateLabel}>📝 Test Date:</Text>
                <Text style={styles.dateValue}>
                  {new Date(drive.testDate).toLocaleDateString()}
                </Text>
              </View>
            )}
            {drive.interviewDate && (
              <View style={styles.dateRow}>
                <Text style={styles.dateLabel}>🎯 Interview Date:</Text>
                <Text style={styles.dateValue}>
                  {new Date(drive.interviewDate).toLocaleDateString()}
                </Text>
              </View>
            )}
          </View>
        </View>

        {/* Application Action Banner */}
        <View style={styles.statusCard}>
          <Text style={styles.statusSectionTitle}>Application Status</Text>

          {isShortlisted ? (
            <View style={styles.shortlistBanner}>
              <Text style={styles.shortlistBannerIcon}>🏆</Text>
              <View>
                <Text style={styles.shortlistBannerTitle}>You are Shortlisted!</Text>
                <Text style={styles.shortlistBannerText}>
                  Your name appears on the official shortlist for this drive.
                </Text>
              </View>
            </View>
          ) : isApplied ? (
            <View style={styles.appliedBanner}>
              <Text style={styles.appliedBannerIcon}>✓</Text>
              <View>
                <Text style={styles.appliedBannerTitle}>Application Submitted</Text>
                <Text style={styles.appliedBannerText}>
                  {drive.appliedAt ? `Applied on ${new Date(drive.appliedAt).toLocaleString()}` : 'Applied'}
                </Text>
              </View>
            </View>
          ) : drive.deadlinePassed ? (
            <View style={styles.deadlinePassedBanner}>
              <Text style={styles.deadlinePassedText}>
                ⚠️ Application deadline has passed for this drive.
              </Text>
            </View>
          ) : canApply ? (
            <View style={styles.applyActionBox}>
              <Text style={styles.applyPrompt}>
                You meet eligibility requirements for this drive.
              </Text>
              {applyError && (
                <Text style={styles.applyErrorText}>{applyError}</Text>
              )}
              <TouchableOpacity
                style={[styles.applyButton, applyMutation.isPending && styles.applyButtonDisabled]}
                onPress={() => {
                  if (drive.applicationId) {
                    applyMutation.mutate(drive.applicationId);
                  }
                }}
                disabled={applyMutation.isPending}
                activeOpacity={0.8}
              >
                {applyMutation.isPending ? (
                  <ActivityIndicator color="#ffffff" />
                ) : (
                  <Text style={styles.applyButtonText}>Submit Application (APPLY)</Text>
                )}
              </TouchableOpacity>
            </View>
          ) : (
            <View style={styles.notEligibleBox}>
              <Text style={styles.notEligiblePrompt}>
                {drive.actionRequired || 'You do not meet all criteria for this drive.'}
              </Text>
            </View>
          )}
        </View>

        {/* Roles Breakdown */}
        <View style={styles.section}>
          <Text style={styles.sectionHeading}>Placement Roles ({drive.roles.length})</Text>

          {drive.roles.map((role) => (
            <View key={role.roleId} style={styles.roleCard}>
              <View style={styles.roleHeader}>
                <Text style={styles.roleTitle}>{role.roleTitle}</Text>
                <View
                  style={[
                    styles.roleBadge,
                    role.decision === 'ELIGIBLE' ? styles.roleBadgeEligible : styles.roleBadgeNotEligible,
                  ]}
                >
                  <Text
                    style={[
                      styles.roleBadgeText,
                      role.decision === 'ELIGIBLE' ? styles.roleBadgeTextEligible : styles.roleBadgeTextNotEligible,
                    ]}
                  >
                    {role.decision}
                  </Text>
                </View>
              </View>

              {/* Criteria Explanations */}
              {role.criteriaExplanations.length > 0 && (
                <View style={styles.criteriaBox}>
                  {role.criteriaExplanations.map((exp, idx) => (
                    <Text key={idx} style={styles.criteriaText}>
                      • {exp}
                    </Text>
                  ))}
                </View>
              )}

              {/* Specific Requirements */}
              <View style={styles.specsGrid}>
                {role.minCgpa !== null && (
                  <View style={styles.specItem}>
                    <Text style={styles.specLabel}>Min CGPA</Text>
                    <Text style={styles.specValue}>{role.minCgpa.toFixed(2)}</Text>
                  </View>
                )}
                {role.maxStandingArrearsAllowed !== null && (
                  <View style={styles.specItem}>
                    <Text style={styles.specLabel}>Max Arrears</Text>
                    <Text style={styles.specValue}>{role.maxStandingArrearsAllowed}</Text>
                  </View>
                )}
                {role.genderAllowed && (
                  <View style={styles.specItem}>
                    <Text style={styles.specLabel}>Gender</Text>
                    <Text style={styles.specValue}>{role.genderAllowed}</Text>
                  </View>
                )}
              </View>

              {role.eligibleBranches.length > 0 && (
                <View style={styles.branchesBox}>
                  <Text style={styles.branchesLabel}>Eligible Branches:</Text>
                  <View style={styles.branchesRow}>
                    {role.eligibleBranches.map((b, i) => (
                      <View key={i} style={styles.branchTag}>
                        <Text style={styles.branchTagText}>{b}</Text>
                      </View>
                    ))}
                  </View>
                </View>
              )}
            </View>
          ))}
        </View>
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
    padding: 24,
  },
  loadingText: {
    color: '#a1a1aa',
    fontSize: 14,
    marginTop: 12,
  },
  errorIcon: {
    fontSize: 36,
    marginBottom: 12,
  },
  errorTitle: {
    fontSize: 18,
    fontWeight: '700',
    color: '#f4f4f5',
    marginBottom: 4,
  },
  errorSubtitle: {
    fontSize: 13,
    color: '#71717a',
    marginBottom: 16,
  },
  retryBtn: {
    backgroundColor: '#6366f1',
    paddingHorizontal: 20,
    paddingVertical: 10,
    borderRadius: 10,
  },
  retryText: {
    color: '#ffffff',
    fontWeight: '700',
    fontSize: 13,
  },
  card: {
    backgroundColor: '#18181b',
    borderColor: '#27272a',
    borderWidth: 1,
    borderRadius: 20,
    padding: 18,
    marginBottom: 14,
  },
  cardHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'flex-start',
    marginBottom: 10,
  },
  titleGroup: {
    flex: 1,
  },
  companyName: {
    fontSize: 20,
    fontWeight: '800',
    color: '#f4f4f5',
  },
  driveTitle: {
    fontSize: 14,
    color: '#a1a1aa',
    marginTop: 2,
  },
  badge: {
    paddingHorizontal: 10,
    paddingVertical: 4,
    borderRadius: 8,
  },
  badgeEligible: {
    backgroundColor: 'rgba(34, 197, 94, 0.15)',
    borderColor: 'rgba(34, 197, 94, 0.3)',
    borderWidth: 1,
  },
  badgeNotEligible: {
    backgroundColor: 'rgba(239, 68, 68, 0.15)',
    borderColor: 'rgba(239, 68, 68, 0.3)',
    borderWidth: 1,
  },
  badgeText: {
    fontSize: 12,
    fontWeight: '700',
  },
  badgeEligibleText: {
    color: '#4ade80',
  },
  badgeNotEligibleText: {
    color: '#f87171',
  },
  descriptionText: {
    fontSize: 13,
    color: '#d4d4d8',
    lineHeight: 20,
    marginBottom: 14,
  },
  datesContainer: {
    borderTopColor: '#27272a',
    borderTopWidth: 1,
    paddingTop: 12,
    gap: 6,
  },
  dateRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  dateLabel: {
    fontSize: 12,
    color: '#a1a1aa',
  },
  dateValue: {
    fontSize: 12,
    fontWeight: '600',
    color: '#f4f4f5',
  },
  statusCard: {
    backgroundColor: '#18181b',
    borderColor: '#27272a',
    borderWidth: 1,
    borderRadius: 20,
    padding: 18,
    marginBottom: 14,
  },
  statusSectionTitle: {
    fontSize: 14,
    fontWeight: '700',
    color: '#e4e4e7',
    marginBottom: 12,
  },
  shortlistBanner: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: 'rgba(234, 179, 8, 0.15)',
    borderColor: 'rgba(234, 179, 8, 0.3)',
    borderWidth: 1,
    borderRadius: 14,
    padding: 14,
  },
  shortlistBannerIcon: {
    fontSize: 24,
    marginRight: 12,
  },
  shortlistBannerTitle: {
    fontSize: 14,
    fontWeight: '800',
    color: '#eab308',
  },
  shortlistBannerText: {
    fontSize: 12,
    color: '#fde047',
    marginTop: 2,
  },
  appliedBanner: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: 'rgba(34, 197, 94, 0.12)',
    borderColor: 'rgba(34, 197, 94, 0.3)',
    borderWidth: 1,
    borderRadius: 14,
    padding: 14,
  },
  appliedBannerIcon: {
    fontSize: 20,
    fontWeight: '800',
    color: '#4ade80',
    marginRight: 12,
  },
  appliedBannerTitle: {
    fontSize: 14,
    fontWeight: '700',
    color: '#4ade80',
  },
  appliedBannerText: {
    fontSize: 12,
    color: '#86efac',
    marginTop: 2,
  },
  deadlinePassedBanner: {
    backgroundColor: 'rgba(239, 68, 68, 0.1)',
    borderRadius: 12,
    padding: 12,
  },
  deadlinePassedText: {
    color: '#f87171',
    fontSize: 13,
    fontWeight: '600',
  },
  applyActionBox: {
    gap: 10,
  },
  applyPrompt: {
    fontSize: 13,
    color: '#a1a1aa',
  },
  applyErrorText: {
    color: '#f87171',
    fontSize: 12,
    fontWeight: '600',
  },
  applyButton: {
    backgroundColor: '#22c55e',
    borderRadius: 12,
    paddingVertical: 14,
    alignItems: 'center',
  },
  applyButtonDisabled: {
    opacity: 0.6,
  },
  applyButtonText: {
    color: '#ffffff',
    fontSize: 15,
    fontWeight: '800',
  },
  notEligibleBox: {
    backgroundColor: '#27272a',
    borderRadius: 12,
    padding: 12,
  },
  notEligiblePrompt: {
    fontSize: 12,
    color: '#a1a1aa',
  },
  section: {
    marginTop: 8,
  },
  sectionHeading: {
    fontSize: 16,
    fontWeight: '800',
    color: '#f4f4f5',
    marginBottom: 12,
  },
  roleCard: {
    backgroundColor: '#18181b',
    borderColor: '#27272a',
    borderWidth: 1,
    borderRadius: 18,
    padding: 16,
    marginBottom: 12,
  },
  roleHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 10,
  },
  roleTitle: {
    fontSize: 16,
    fontWeight: '700',
    color: '#f4f4f5',
  },
  roleBadge: {
    paddingHorizontal: 8,
    paddingVertical: 3,
    borderRadius: 6,
  },
  roleBadgeEligible: {
    backgroundColor: 'rgba(34, 197, 94, 0.15)',
  },
  roleBadgeNotEligible: {
    backgroundColor: 'rgba(239, 68, 68, 0.15)',
  },
  roleBadgeText: {
    fontSize: 11,
    fontWeight: '700',
  },
  roleBadgeTextEligible: {
    color: '#4ade80',
  },
  roleBadgeTextNotEligible: {
    color: '#f87171',
  },
  criteriaBox: {
    backgroundColor: '#27272a',
    borderRadius: 10,
    padding: 10,
    marginBottom: 10,
    gap: 4,
  },
  criteriaText: {
    fontSize: 12,
    color: '#d4d4d8',
  },
  specsGrid: {
    flexDirection: 'row',
    gap: 10,
    marginBottom: 10,
  },
  specItem: {
    backgroundColor: '#27272a',
    paddingHorizontal: 10,
    paddingVertical: 6,
    borderRadius: 8,
  },
  specLabel: {
    fontSize: 10,
    color: '#71717a',
  },
  specValue: {
    fontSize: 13,
    fontWeight: '700',
    color: '#f4f4f5',
  },
  branchesBox: {
    borderTopColor: '#27272a',
    borderTopWidth: 1,
    paddingTop: 8,
  },
  branchesLabel: {
    fontSize: 11,
    color: '#71717a',
    marginBottom: 4,
  },
  branchesRow: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 6,
  },
  branchTag: {
    backgroundColor: '#27272a',
    paddingHorizontal: 8,
    paddingVertical: 2,
    borderRadius: 4,
  },
  branchTagText: {
    fontSize: 11,
    color: '#a1a1aa',
  },
});
