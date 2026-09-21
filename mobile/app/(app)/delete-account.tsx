import { useRouter } from 'expo-router';
import React, { useState } from 'react';
import {
  ActivityIndicator,
  Alert,
  KeyboardAvoidingView,
  Platform,
  ScrollView,
  StyleSheet,
  Text,
  TextInput,
  TouchableOpacity,
  View,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useAuth } from '../../src/auth/AuthContext';

export default function DeleteAccountScreen() {
  const { deleteAccount } = useAuth();
  const router = useRouter();

  const [confirmText, setConfirmText] = useState('');
  const [deleting, setDeleting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleDelete = async () => {
    if (confirmText.trim() !== 'DELETE') {
      setError('Please type DELETE exactly to confirm.');
      return;
    }

    setDeleting(true);
    setError(null);

    try {
      await deleteAccount();
      Alert.alert('Account Deleted', 'Your PlacementOS account has been deleted and anonymised.', [
        {
          text: 'OK',
          onPress: () => router.replace('/(auth)/login'),
        },
      ]);
    } catch (err: unknown) {
      if (err instanceof Error) {
        setError(err.message);
      } else {
        setError('Failed to delete account. Please try again.');
      }
      setDeleting(false);
    }
  };

  return (
    <SafeAreaView style={styles.safeArea} edges={['bottom']}>
      <KeyboardAvoidingView
        behavior={Platform.OS === 'ios' ? 'padding' : 'height'}
        style={styles.keyboardView}
      >
        <ScrollView contentContainerStyle={styles.scrollContent} keyboardShouldPersistTaps="handled">
          <View style={styles.header}>
            <View style={styles.dangerIconContainer}>
              <Text style={styles.dangerIcon}>⚠️</Text>
            </View>
            <Text style={styles.title}>Delete Account</Text>
            <Text style={styles.subtitle}>
              This action is permanent and cannot be undone.
            </Text>
          </View>

          {/* Warning Card */}
          <View style={styles.warningCard}>
            <Text style={styles.warningHeading}>What happens when you delete:</Text>
            <View style={styles.consequencesList}>
              <Text style={styles.consequenceItem}>
                • <Text style={styles.bold}>Personal PII Anonymised:</Text> Your name, phone number,
                gender, and specialization will be cleared immediately.
              </Text>
              <Text style={styles.consequenceItem}>
                • <Text style={styles.bold}>Credentials Removed:</Text> Your login credentials will be
                permanently disabled. You will not be able to log back in.
              </Text>
              <Text style={styles.consequenceItem}>
                • <Text style={styles.bold}>Telegram Unlinked:</Text> Your Telegram chat linkage and link
                tokens will be deleted. No further messages will be sent.
              </Text>
              <Text style={styles.consequenceItem}>
                • <Text style={styles.bold}>Reminders Cancelled:</Text> All active deadline reminders and
                outbox tasks will be cancelled.
              </Text>
              <Text style={styles.consequenceItem}>
                • <Text style={styles.bold}>Institutional Records Preserved:</Text> Placement drives,
                applications, and shortlist entries are retained as university system of record.
              </Text>
            </View>
          </View>

          {/* Confirmation Input */}
          <View style={styles.confirmCard}>
            <Text style={styles.confirmLabel}>
              Type <Text style={styles.confirmHighlight}>DELETE</Text> below to confirm:
            </Text>
            <TextInput
              style={styles.input}
              placeholder="DELETE"
              placeholderTextColor="#71717a"
              autoCapitalize="characters"
              autoCorrect={false}
              value={confirmText}
              onChangeText={setConfirmText}
            />

            {error && (
              <View style={styles.errorBox}>
                <Text style={styles.errorText}>{error}</Text>
              </View>
            )}

            <TouchableOpacity
              style={[
                styles.deleteButton,
                (confirmText.trim() !== 'DELETE' || deleting) && styles.buttonDisabled,
              ]}
              onPress={handleDelete}
              disabled={confirmText.trim() !== 'DELETE' || deleting}
            >
              {deleting ? (
                <ActivityIndicator color="#ffffff" />
              ) : (
                <Text style={styles.deleteButtonText}>Permanently Delete Account</Text>
              )}
            </TouchableOpacity>

            <TouchableOpacity
              style={styles.cancelButton}
              onPress={() => router.back()}
              disabled={deleting}
            >
              <Text style={styles.cancelButtonText}>Cancel and Keep Account</Text>
            </TouchableOpacity>
          </View>
        </ScrollView>
      </KeyboardAvoidingView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safeArea: {
    flex: 1,
    backgroundColor: '#09090b',
  },
  keyboardView: {
    flex: 1,
  },
  scrollContent: {
    padding: 16,
    paddingBottom: 32,
  },
  header: {
    alignItems: 'center',
    marginBottom: 20,
  },
  dangerIconContainer: {
    width: 60,
    height: 60,
    borderRadius: 30,
    backgroundColor: 'rgba(239, 68, 68, 0.15)',
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: 12,
  },
  dangerIcon: {
    fontSize: 28,
  },
  title: {
    fontSize: 24,
    fontWeight: '800',
    color: '#f87171',
    marginBottom: 4,
  },
  subtitle: {
    fontSize: 13,
    color: '#a1a1aa',
  },
  warningCard: {
    backgroundColor: 'rgba(239, 68, 68, 0.08)',
    borderColor: 'rgba(239, 68, 68, 0.3)',
    borderWidth: 1,
    borderRadius: 18,
    padding: 18,
    marginBottom: 16,
  },
  warningHeading: {
    fontSize: 14,
    fontWeight: '800',
    color: '#fca5a5',
    marginBottom: 10,
  },
  consequencesList: {
    gap: 8,
  },
  consequenceItem: {
    fontSize: 12,
    color: '#fecaca',
    lineHeight: 18,
  },
  bold: {
    fontWeight: '700',
    color: '#ffffff',
  },
  confirmCard: {
    backgroundColor: '#18181b',
    borderColor: '#27272a',
    borderWidth: 1,
    borderRadius: 18,
    padding: 18,
  },
  confirmLabel: {
    fontSize: 13,
    color: '#d4d4d8',
    marginBottom: 10,
  },
  confirmHighlight: {
    fontWeight: '800',
    color: '#f87171',
  },
  input: {
    backgroundColor: '#27272a',
    borderColor: '#3f3f46',
    borderWidth: 1,
    borderRadius: 12,
    paddingHorizontal: 16,
    paddingVertical: 12,
    fontSize: 16,
    fontWeight: '700',
    color: '#f4f4f5',
    letterSpacing: 1,
    marginBottom: 14,
  },
  errorBox: {
    backgroundColor: 'rgba(239, 68, 68, 0.15)',
    borderColor: 'rgba(239, 68, 68, 0.3)',
    borderWidth: 1,
    borderRadius: 10,
    padding: 10,
    marginBottom: 14,
  },
  errorText: {
    color: '#f87171',
    fontSize: 12,
  },
  deleteButton: {
    backgroundColor: '#dc2626',
    borderRadius: 12,
    paddingVertical: 14,
    alignItems: 'center',
    marginBottom: 10,
  },
  buttonDisabled: {
    opacity: 0.4,
  },
  deleteButtonText: {
    color: '#ffffff',
    fontSize: 15,
    fontWeight: '800',
  },
  cancelButton: {
    backgroundColor: '#27272a',
    borderRadius: 12,
    paddingVertical: 12,
    alignItems: 'center',
  },
  cancelButtonText: {
    color: '#a1a1aa',
    fontSize: 13,
    fontWeight: '600',
  },
});
