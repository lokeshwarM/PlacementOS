import { useRouter } from 'expo-router';
import React, { useState } from 'react';
import {
  ActivityIndicator,
  Alert,
  ScrollView,
  StyleSheet,
  Text,
  TextInput,
  TouchableOpacity,
  View,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { api } from '../../src/api/client';
import { useAuth } from '../../src/auth/AuthContext';
import BottomNav from '../../src/components/BottomNav';

export default function ProfileScreen() {
  const { user, profile, refreshProfile, logout } = useAuth();
  const router = useRouter();

  const [phoneNumber, setPhoneNumber] = useState(profile?.phoneNumber || '');
  const [gender, setGender] = useState(profile?.gender || '');
  const [specialization, setSpecialization] = useState(profile?.specialization || '');
  const [saving, setSaving] = useState(false);
  const [saveSuccess, setSaveSuccess] = useState(false);

  const handleSave = async () => {
    setSaving(true);
    setSaveSuccess(false);
    try {
      await api.updateProfile({
        phoneNumber: phoneNumber.trim() || undefined,
        gender: gender.trim() || undefined,
        specialization: specialization.trim() || undefined,
      });
      await refreshProfile();
      setSaveSuccess(true);
      setTimeout(() => setSaveSuccess(false), 3000);
    } catch (err: unknown) {
      if (err instanceof Error) {
        Alert.alert('Update Failed', err.message);
      } else {
        Alert.alert('Update Failed', 'Failed to update profile.');
      }
    } finally {
      setSaving(false);
    }
  };

  return (
    <SafeAreaView style={styles.safeArea} edges={['bottom']}>
      <ScrollView contentContainerStyle={styles.scrollContent}>
        {/* Header / Identity */}
        <View style={styles.card}>
          <View style={styles.identityRow}>
            <View style={styles.avatar}>
              <Text style={styles.avatarText}>
                {(profile?.name || user?.email || 'S').charAt(0).toUpperCase()}
              </Text>
            </View>
            <View style={styles.identityInfo}>
              <Text style={styles.userName}>{profile?.name || 'Student Profile'}</Text>
              <Text style={styles.userEmail}>{user?.email}</Text>
              <View style={styles.badgeRow}>
                <View style={styles.roleBadge}>
                  <Text style={styles.roleBadgeText}>{user?.role || 'STUDENT'}</Text>
                </View>
                <View
                  style={[
                    styles.statusBadge,
                    user?.profileStatus === 'VERIFIED'
                      ? styles.badgeVerified
                      : user?.profileStatus === 'COMPLETE'
                      ? styles.badgeComplete
                      : styles.badgeIncomplete,
                  ]}
                >
                  <Text style={styles.statusBadgeText}>{user?.profileStatus || 'INCOMPLETE'}</Text>
                </View>
              </View>
            </View>
          </View>
        </View>

        {/* Academic System of Record (Read-Only) */}
        <View style={styles.card}>
          <Text style={styles.sectionTitle}>Institutional Record</Text>
          <Text style={styles.sectionSubtitle}>
            Verified university identifiers used for placement eligibility.
          </Text>

          <View style={styles.grid2}>
            <View style={styles.infoField}>
              <Text style={styles.fieldLabel}>Registration Number</Text>
              <Text style={styles.fieldValue}>{profile?.registrationNumber || '—'}</Text>
            </View>
            <View style={styles.infoField}>
              <Text style={styles.fieldLabel}>NeoPAT ID</Text>
              <Text style={styles.fieldValue}>{profile?.neopatId || '—'}</Text>
            </View>
            <View style={styles.infoField}>
              <Text style={styles.fieldLabel}>Branch</Text>
              <Text style={styles.fieldValue}>{profile?.branch || '—'}</Text>
            </View>
            <View style={styles.infoField}>
              <Text style={styles.fieldLabel}>Batch Year</Text>
              <Text style={styles.fieldValue}>{profile?.batch || '—'}</Text>
            </View>
            <View style={styles.infoField}>
              <Text style={styles.fieldLabel}>CGPA</Text>
              <Text style={styles.fieldValueHighlight}>
                {profile?.cgpa ? Number(profile.cgpa).toFixed(2) : '—'}
              </Text>
            </View>
            <View style={styles.infoField}>
              <Text style={styles.fieldLabel}>Standing Arrears</Text>
              <Text style={styles.fieldValue}>{profile?.standingArrears ?? 0}</Text>
            </View>
          </View>
        </View>

        {/* Editable Personal Information */}
        <View style={styles.card}>
          <Text style={styles.sectionTitle}>Personal Details</Text>
          <Text style={styles.sectionSubtitle}>Update your contact and specialization info.</Text>

          {saveSuccess && (
            <View style={styles.successBanner}>
              <Text style={styles.successText}>✓ Profile updated successfully</Text>
            </View>
          )}

          <View style={styles.inputGroup}>
            <Text style={styles.inputLabel}>Phone Number</Text>
            <TextInput
              style={styles.input}
              placeholder="+919876543210"
              placeholderTextColor="#71717a"
              keyboardType="phone-pad"
              value={phoneNumber}
              onChangeText={setPhoneNumber}
            />
          </View>

          <View style={styles.inputGroup}>
            <Text style={styles.inputLabel}>Gender</Text>
            <TextInput
              style={styles.input}
              placeholder="e.g. MALE / FEMALE"
              placeholderTextColor="#71717a"
              value={gender}
              onChangeText={setGender}
            />
          </View>

          <View style={styles.inputGroup}>
            <Text style={styles.inputLabel}>Specialization</Text>
            <TextInput
              style={styles.input}
              placeholder="e.g. Data Science"
              placeholderTextColor="#71717a"
              value={specialization}
              onChangeText={setSpecialization}
            />
          </View>

          <TouchableOpacity
            style={[styles.saveButton, saving && styles.buttonDisabled]}
            onPress={handleSave}
            disabled={saving}
          >
            {saving ? (
              <ActivityIndicator color="#ffffff" />
            ) : (
              <Text style={styles.saveButtonText}>Save Changes</Text>
            )}
          </TouchableOpacity>
        </View>

        {/* Telegram Integration Entry */}
        <TouchableOpacity
          style={styles.telegramCard}
          onPress={() => router.push('/telegram')}
          activeOpacity={0.8}
        >
          <View style={styles.telegramIconContainer}>
            <Text style={styles.telegramIcon}>✈️</Text>
          </View>
          <View style={styles.telegramContent}>
            <Text style={styles.telegramCardTitle}>Telegram Integration</Text>
            <Text style={styles.telegramCardSubtitle}>
              Manage your linked Telegram account for instant deadline alerts.
            </Text>
          </View>
          <Text style={styles.chevron}>›</Text>
        </TouchableOpacity>

        {/* Privacy Policy Link */}
        <TouchableOpacity
          style={styles.privacyRow}
          onPress={() => router.push('/privacy')}
          activeOpacity={0.7}
        >
          <Text style={styles.privacyLinkText}>📄 View Privacy Policy</Text>
          <Text style={styles.chevron}>›</Text>
        </TouchableOpacity>

        {/* Sign Out */}
        <TouchableOpacity
          style={styles.logoutButton}
          onPress={() => {
            Alert.alert('Sign Out', 'Are you sure you want to sign out?', [
              { text: 'Cancel', style: 'cancel' },
              { text: 'Sign Out', onPress: logout },
            ]);
          }}
        >
          <Text style={styles.logoutButtonText}>Sign Out</Text>
        </TouchableOpacity>

        {/* Danger Zone: Account Deletion */}
        <View style={styles.dangerCard}>
          <Text style={styles.dangerTitle}>Danger Zone</Text>
          <Text style={styles.dangerSubtitle}>
            Permanently delete your account. This anonymises personal data and unlinks Telegram while
            retaining institutional placement history.
          </Text>
          <TouchableOpacity
            style={styles.deleteButton}
            onPress={() => router.push('/delete-account')}
          >
            <Text style={styles.deleteButtonText}>Delete My Account</Text>
          </TouchableOpacity>
        </View>
      </ScrollView>
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
    paddingBottom: 32,
  },
  card: {
    backgroundColor: '#18181b',
    borderColor: '#27272a',
    borderWidth: 1,
    borderRadius: 20,
    padding: 18,
    marginBottom: 14,
  },
  identityRow: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  avatar: {
    width: 56,
    height: 56,
    borderRadius: 28,
    backgroundColor: '#6366f1',
    alignItems: 'center',
    justifyContent: 'center',
    marginRight: 16,
  },
  avatarText: {
    fontSize: 24,
    fontWeight: '800',
    color: '#ffffff',
  },
  identityInfo: {
    flex: 1,
  },
  userName: {
    fontSize: 18,
    fontWeight: '800',
    color: '#f4f4f5',
  },
  userEmail: {
    fontSize: 13,
    color: '#a1a1aa',
    marginTop: 2,
  },
  badgeRow: {
    flexDirection: 'row',
    gap: 6,
    marginTop: 8,
  },
  roleBadge: {
    backgroundColor: '#27272a',
    paddingHorizontal: 8,
    paddingVertical: 2,
    borderRadius: 4,
  },
  roleBadgeText: {
    fontSize: 10,
    fontWeight: '700',
    color: '#d4d4d8',
  },
  statusBadge: {
    paddingHorizontal: 8,
    paddingVertical: 2,
    borderRadius: 4,
  },
  badgeVerified: {
    backgroundColor: 'rgba(34, 197, 94, 0.2)',
  },
  badgeComplete: {
    backgroundColor: 'rgba(99, 102, 241, 0.2)',
  },
  badgeIncomplete: {
    backgroundColor: 'rgba(239, 68, 68, 0.2)',
  },
  statusBadgeText: {
    fontSize: 10,
    fontWeight: '700',
    color: '#f4f4f5',
  },
  sectionTitle: {
    fontSize: 15,
    fontWeight: '800',
    color: '#f4f4f5',
    marginBottom: 4,
  },
  sectionSubtitle: {
    fontSize: 12,
    color: '#71717a',
    marginBottom: 14,
  },
  grid2: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 12,
  },
  infoField: {
    width: '47%',
    backgroundColor: '#27272a',
    borderRadius: 12,
    padding: 12,
  },
  fieldLabel: {
    fontSize: 10,
    fontWeight: '600',
    color: '#71717a',
    marginBottom: 4,
  },
  fieldValue: {
    fontSize: 13,
    fontWeight: '700',
    color: '#f4f4f5',
  },
  fieldValueHighlight: {
    fontSize: 15,
    fontWeight: '800',
    color: '#818cf8',
  },
  successBanner: {
    backgroundColor: 'rgba(34, 197, 94, 0.15)',
    borderColor: 'rgba(34, 197, 94, 0.3)',
    borderWidth: 1,
    borderRadius: 10,
    padding: 10,
    marginBottom: 12,
  },
  successText: {
    color: '#4ade80',
    fontSize: 12,
    fontWeight: '600',
  },
  inputGroup: {
    marginBottom: 12,
  },
  inputLabel: {
    fontSize: 12,
    fontWeight: '600',
    color: '#d4d4d8',
    marginBottom: 6,
  },
  input: {
    backgroundColor: '#27272a',
    borderColor: '#3f3f46',
    borderWidth: 1,
    borderRadius: 10,
    paddingHorizontal: 14,
    paddingVertical: 10,
    fontSize: 14,
    color: '#f4f4f5',
  },
  saveButton: {
    backgroundColor: '#6366f1',
    borderRadius: 10,
    paddingVertical: 12,
    alignItems: 'center',
    marginTop: 6,
  },
  buttonDisabled: {
    opacity: 0.6,
  },
  saveButtonText: {
    color: '#ffffff',
    fontSize: 14,
    fontWeight: '700',
  },
  telegramCard: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: '#18181b',
    borderColor: 'rgba(56, 189, 248, 0.3)',
    borderWidth: 1,
    borderRadius: 18,
    padding: 16,
    marginBottom: 14,
  },
  telegramIconContainer: {
    width: 40,
    height: 40,
    borderRadius: 20,
    backgroundColor: 'rgba(56, 189, 248, 0.15)',
    alignItems: 'center',
    justifyContent: 'center',
    marginRight: 12,
  },
  telegramIcon: {
    fontSize: 20,
  },
  telegramContent: {
    flex: 1,
  },
  telegramCardTitle: {
    fontSize: 14,
    fontWeight: '700',
    color: '#38bdf8',
  },
  telegramCardSubtitle: {
    fontSize: 11,
    color: '#94a3b8',
    marginTop: 2,
  },
  privacyRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    backgroundColor: '#18181b',
    borderColor: '#27272a',
    borderWidth: 1,
    borderRadius: 14,
    padding: 16,
    marginBottom: 14,
  },
  privacyLinkText: {
    fontSize: 13,
    color: '#d4d4d8',
    fontWeight: '600',
  },
  chevron: {
    fontSize: 18,
    color: '#71717a',
  },
  logoutButton: {
    backgroundColor: '#27272a',
    borderRadius: 12,
    paddingVertical: 14,
    alignItems: 'center',
    marginBottom: 16,
  },
  logoutButtonText: {
    color: '#f4f4f5',
    fontSize: 14,
    fontWeight: '700',
  },
  dangerCard: {
    backgroundColor: 'rgba(239, 68, 68, 0.08)',
    borderColor: 'rgba(239, 68, 68, 0.3)',
    borderWidth: 1,
    borderRadius: 18,
    padding: 16,
    marginBottom: 16,
  },
  dangerTitle: {
    fontSize: 14,
    fontWeight: '800',
    color: '#f87171',
    marginBottom: 4,
  },
  dangerSubtitle: {
    fontSize: 12,
    color: '#fca5a5',
    lineHeight: 16,
    marginBottom: 12,
  },
  deleteButton: {
    backgroundColor: 'rgba(239, 68, 68, 0.2)',
    borderColor: '#ef4444',
    borderWidth: 1,
    borderRadius: 10,
    paddingVertical: 10,
    alignItems: 'center',
  },
  deleteButtonText: {
    color: '#f87171',
    fontSize: 13,
    fontWeight: '700',
  },
});
