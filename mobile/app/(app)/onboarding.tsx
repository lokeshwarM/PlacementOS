import { useRouter } from 'expo-router';
import React, { useState } from 'react';
import {
  ActivityIndicator,
  KeyboardAvoidingView,
  Platform,
  ScrollView,
  StyleSheet,
  Text,
  TextInput,
  TouchableOpacity,
  View,
} from 'react-native';
import { api } from '../../src/api/client';
import { useAuth } from '../../src/auth/AuthContext';

export default function OnboardingScreen() {
  const { refreshProfile, logout } = useAuth();
  const router = useRouter();

  const [registrationNumber, setRegistrationNumber] = useState('');
  const [neopatId, setNeopatId] = useState('');
  const [branch, setBranch] = useState('');
  const [batch, setBatch] = useState('');
  const [cgpa, setCgpa] = useState('');
  const [phoneNumber, setPhoneNumber] = useState('');
  const [gender, setGender] = useState('');
  const [degree, setDegree] = useState('B.Tech');
  const [specialization, setSpecialization] = useState('');

  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = async () => {
    if (!registrationNumber.trim() || !neopatId.trim() || !branch.trim() || !batch.trim() || !cgpa.trim()) {
      setError('Please fill in all required academic fields (*).');
      return;
    }

    const batchNum = parseInt(batch.trim(), 10);
    const cgpaNum = parseFloat(cgpa.trim());

    if (isNaN(batchNum) || batchNum < 2000 || batchNum > 2100) {
      setError('Please enter a valid 4-digit graduating batch year.');
      return;
    }

    if (isNaN(cgpaNum) || cgpaNum < 0 || cgpaNum > 10) {
      setError('CGPA must be a number between 0.00 and 10.00.');
      return;
    }

    setSubmitting(true);
    setError(null);

    try {
      await api.onboard({
        registrationNumber: registrationNumber.trim().toUpperCase(),
        neopatId: neopatId.trim().toUpperCase(),
        branch: branch.trim(),
        batch: batchNum,
        cgpa: cgpaNum,
        phoneNumber: phoneNumber.trim() || undefined,
        gender: gender.trim() || undefined,
        degree: degree.trim() || undefined,
        specialization: specialization.trim() || undefined,
      });

      await refreshProfile();
      router.replace('/(app)/dashboard');
    } catch (err: unknown) {
      if (err instanceof Error) {
        setError(err.message);
      } else {
        setError('Failed to complete onboarding. Please check your details.');
      }
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <KeyboardAvoidingView
      behavior={Platform.OS === 'ios' ? 'padding' : 'height'}
      style={styles.container}
    >
      <ScrollView contentContainerStyle={styles.scrollContent} keyboardShouldPersistTaps="handled">
        <View style={styles.header}>
          <Text style={styles.title}>Complete Your Profile</Text>
          <Text style={styles.subtitle}>
            Link your institutional student record to evaluate placement eligibility.
          </Text>
        </View>

        <View style={styles.card}>
          {error && (
            <View style={styles.errorBox}>
              <Text style={styles.errorText}>{error}</Text>
            </View>
          )}

          <View style={styles.inputGroup}>
            <Text style={styles.label}>Registration Number *</Text>
            <TextInput
              style={styles.input}
              placeholder="e.g. 21BCE1001"
              placeholderTextColor="#71717a"
              autoCapitalize="characters"
              value={registrationNumber}
              onChangeText={setRegistrationNumber}
            />
          </View>

          <View style={styles.inputGroup}>
            <Text style={styles.label}>NeoPAT ID *</Text>
            <TextInput
              style={styles.input}
              placeholder="e.g. NP21BCE1001"
              placeholderTextColor="#71717a"
              autoCapitalize="characters"
              value={neopatId}
              onChangeText={setNeopatId}
            />
          </View>

          <View style={styles.row}>
            <View style={[styles.inputGroup, styles.flex1]}>
              <Text style={styles.label}>Branch / Dept *</Text>
              <TextInput
                style={styles.input}
                placeholder="e.g. CSE"
                placeholderTextColor="#71717a"
                value={branch}
                onChangeText={setBranch}
              />
            </View>
            <View style={styles.spacer} />
            <View style={[styles.inputGroup, styles.flex1]}>
              <Text style={styles.label}>Batch Year *</Text>
              <TextInput
                style={styles.input}
                placeholder="e.g. 2025"
                placeholderTextColor="#71717a"
                keyboardType="numeric"
                value={batch}
                onChangeText={setBatch}
              />
            </View>
          </View>

          <View style={styles.row}>
            <View style={[styles.inputGroup, styles.flex1]}>
              <Text style={styles.label}>CGPA (0-10) *</Text>
              <TextInput
                style={styles.input}
                placeholder="e.g. 8.75"
                placeholderTextColor="#71717a"
                keyboardType="decimal-pad"
                value={cgpa}
                onChangeText={setCgpa}
              />
            </View>
            <View style={styles.spacer} />
            <View style={[styles.inputGroup, styles.flex1]}>
              <Text style={styles.label}>Phone Number</Text>
              <TextInput
                style={styles.input}
                placeholder="e.g. +919876543210"
                placeholderTextColor="#71717a"
                keyboardType="phone-pad"
                value={phoneNumber}
                onChangeText={setPhoneNumber}
              />
            </View>
          </View>

          <View style={styles.inputGroup}>
            <Text style={styles.label}>Degree</Text>
            <TextInput
              style={styles.input}
              placeholder="e.g. B.Tech"
              placeholderTextColor="#71717a"
              value={degree}
              onChangeText={setDegree}
            />
          </View>

          <View style={styles.inputGroup}>
            <Text style={styles.label}>Specialization</Text>
            <TextInput
              style={styles.input}
              placeholder="e.g. Artificial Intelligence"
              placeholderTextColor="#71717a"
              value={specialization}
              onChangeText={setSpecialization}
            />
          </View>

          <TouchableOpacity
            style={[styles.button, submitting && styles.buttonDisabled]}
            onPress={handleSubmit}
            disabled={submitting}
            activeOpacity={0.8}
          >
            {submitting ? (
              <ActivityIndicator color="#ffffff" />
            ) : (
              <Text style={styles.buttonText}>Submit & Link Profile</Text>
            )}
          </TouchableOpacity>

          <TouchableOpacity style={styles.logoutBtn} onPress={logout}>
            <Text style={styles.logoutText}>Sign out</Text>
          </TouchableOpacity>
        </View>
      </ScrollView>
    </KeyboardAvoidingView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#09090b',
  },
  scrollContent: {
    padding: 20,
  },
  header: {
    marginBottom: 20,
  },
  title: {
    fontSize: 24,
    fontWeight: '800',
    color: '#f4f4f5',
    marginBottom: 6,
  },
  subtitle: {
    fontSize: 13,
    color: '#a1a1aa',
    lineHeight: 18,
  },
  card: {
    backgroundColor: '#18181b',
    borderColor: '#27272a',
    borderWidth: 1,
    borderRadius: 20,
    padding: 20,
  },
  errorBox: {
    backgroundColor: 'rgba(239, 68, 68, 0.15)',
    borderColor: 'rgba(239, 68, 68, 0.3)',
    borderWidth: 1,
    borderRadius: 12,
    padding: 12,
    marginBottom: 16,
  },
  errorText: {
    color: '#f87171',
    fontSize: 13,
    fontWeight: '500',
  },
  inputGroup: {
    marginBottom: 14,
  },
  label: {
    fontSize: 12,
    fontWeight: '600',
    color: '#d4d4d8',
    marginBottom: 6,
  },
  input: {
    backgroundColor: '#27272a',
    borderColor: '#3f3f46',
    borderWidth: 1,
    borderRadius: 12,
    paddingHorizontal: 14,
    paddingVertical: 10,
    fontSize: 14,
    color: '#f4f4f5',
  },
  row: {
    flexDirection: 'row',
  },
  flex1: {
    flex: 1,
  },
  spacer: {
    width: 12,
  },
  button: {
    backgroundColor: '#6366f1',
    borderRadius: 12,
    paddingVertical: 14,
    alignItems: 'center',
    marginTop: 10,
  },
  buttonDisabled: {
    opacity: 0.6,
  },
  buttonText: {
    color: '#ffffff',
    fontSize: 15,
    fontWeight: '700',
  },
  logoutBtn: {
    marginTop: 16,
    alignItems: 'center',
  },
  logoutText: {
    color: '#71717a',
    fontSize: 13,
  },
});
