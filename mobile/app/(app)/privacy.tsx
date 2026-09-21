import { useRouter } from 'expo-router';
import React from 'react';
import { ScrollView, StyleSheet, Text, TouchableOpacity, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

export default function PrivacyPolicyScreen() {
  const router = useRouter();

  return (
    <SafeAreaView style={styles.safeArea} edges={['bottom']}>
      <ScrollView contentContainerStyle={styles.scrollContent}>
        <View style={styles.header}>
          <Text style={styles.title}>Privacy Policy</Text>
          <Text style={styles.subtitle}>
            PlacementOS Data Handling & Retention Policy • Last updated: September 2026
          </Text>
        </View>

        <View style={styles.card}>
          <Text style={styles.sectionHeading}>1. Overview</Text>
          <Text style={styles.paragraph}>
            PlacementOS is an institutional placement management and notification system. This policy
            describes how we collect, use, and handle your data across both our web and mobile applications.
          </Text>
        </View>

        <View style={styles.card}>
          <Text style={styles.sectionHeading}>2. Information We Collect</Text>
          <Text style={styles.paragraph}>
            We collect and process only the data strictly necessary for placement tracking and notifications:
          </Text>
          <View style={styles.bulletList}>
            <Text style={styles.bulletItem}>
              • <Text style={styles.bold}>Authentication Credentials:</Text> College email address and
              cryptographically hashed passwords (using BCrypt). Plaintext passwords are never stored or logged.
            </Text>
            <Text style={styles.bulletItem}>
              • <Text style={styles.bold}>Academic Identifiers:</Text> Student registration number, NeoPAT ID,
              degree, branch, graduating batch year, CGPA, and standing arrears count.
            </Text>
            <Text style={styles.bulletItem}>
              • <Text style={styles.bold}>Contact Information:</Text> Phone number (optional) provided for
              direct recruiter communication.
            </Text>
            <Text style={styles.bulletItem}>
              • <Text style={styles.bold}>Placement & Application History:</Text> Eligible drives, application
              submissions, timestamps, and shortlist match results.
            </Text>
            <Text style={styles.bulletItem}>
              • <Text style={styles.bold}>Telegram Identity:</Text> Telegram numeric chat ID and handle,
              collected exclusively via explicit student authorization via deep-link tokens.
            </Text>
          </View>
        </View>

        <View style={styles.card}>
          <Text style={styles.sectionHeading}>3. How Your Data Is Used</Text>
          <Text style={styles.paragraph}>
            • Evaluating deterministic eligibility against company criteria (CGPA, branch, arrears).{'\n'}
            • Sending automated real-time alerts and deadline reminders via Telegram.{'\n'}
            • Matching official company shortlist documents with student records.{'\n'}
            • Tracking application submissions and interview schedules.
          </Text>
        </View>

        <View style={styles.card}>
          <Text style={styles.sectionHeading}>4. Account Deletion & Retention</Text>
          <Text style={styles.paragraph}>
            You may permanently delete your account at any time from your Profile or the Delete Account screen.
            Upon deletion:
          </Text>
          <View style={styles.bulletList}>
            <Text style={styles.bulletItem}>
              • <Text style={styles.bold}>Immediate Anonymisation:</Text> Personal name, phone number,
              gender, and specialization are cleared. Account email is replaced with an invalid placeholder.
            </Text>
            <Text style={styles.bulletItem}>
              • <Text style={styles.bold}>Telegram Unlinked:</Text> All Telegram identities and link tokens
              are permanently deleted from our database.
            </Text>
            <Text style={styles.bulletItem}>
              • <Text style={styles.bold}>Reminders Cancelled:</Text> All pending reminder tasks and outbox
              messages are cancelled.
            </Text>
            <Text style={styles.bulletItem}>
              • <Text style={styles.bold}>Institutional Records Retained:</Text> Registration number, NeoPAT
              ID, and historical placement records (drives, applications, and shortlist entries) are retained
              as university system of record.
            </Text>
          </View>
        </View>

        <View style={styles.card}>
          <Text style={styles.sectionHeading}>5. Security Controls</Text>
          <Text style={styles.paragraph}>
            • Stateless JWT authentication with HMAC-SHA256 signatures.{'\n'}
            • Mobile tokens stored exclusively in Android Keystore / iOS Keychain via Expo SecureStore.{'\n'}
            • No sensitive tokens logged or exposed in frontend storage.{'\n'}
            • All network communications encrypted over TLS/HTTPS.
          </Text>
        </View>

        <TouchableOpacity style={styles.backButton} onPress={() => router.back()}>
          <Text style={styles.backButtonText}>← Go Back</Text>
        </TouchableOpacity>
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
  header: {
    marginBottom: 16,
  },
  title: {
    fontSize: 24,
    fontWeight: '800',
    color: '#f4f4f5',
    marginBottom: 4,
  },
  subtitle: {
    fontSize: 12,
    color: '#a1a1aa',
    lineHeight: 16,
  },
  card: {
    backgroundColor: '#18181b',
    borderColor: '#27272a',
    borderWidth: 1,
    borderRadius: 18,
    padding: 18,
    marginBottom: 12,
  },
  sectionHeading: {
    fontSize: 15,
    fontWeight: '800',
    color: '#f4f4f5',
    marginBottom: 8,
  },
  paragraph: {
    fontSize: 13,
    color: '#d4d4d8',
    lineHeight: 20,
  },
  bulletList: {
    marginTop: 8,
    gap: 8,
  },
  bulletItem: {
    fontSize: 13,
    color: '#d4d4d8',
    lineHeight: 19,
  },
  bold: {
    fontWeight: '700',
    color: '#f4f4f5',
  },
  backButton: {
    backgroundColor: '#27272a',
    borderRadius: 12,
    paddingVertical: 14,
    alignItems: 'center',
    marginTop: 8,
  },
  backButtonText: {
    color: '#f4f4f5',
    fontSize: 14,
    fontWeight: '700',
  },
});
