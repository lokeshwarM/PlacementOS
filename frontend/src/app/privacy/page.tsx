import React from 'react';
import type { Metadata } from 'next';

export const metadata: Metadata = {
  title: 'Privacy Policy — PlacementOS',
  description: 'Privacy Policy for PlacementOS — a university placement workflow automation platform.',
};

export default function PrivacyPolicyPage() {
  return (
    <main className="max-w-3xl mx-auto py-10 px-4 space-y-8 text-zinc-800 dark:text-zinc-200">
      <div>
        <h1 className="text-3xl font-bold text-zinc-900 dark:text-zinc-100">Privacy Policy</h1>
        <p className="text-sm text-zinc-500 dark:text-zinc-400 mt-2">
          Last updated: September 2026
        </p>
      </div>

      <section className="space-y-3">
        <h2 className="text-lg font-bold text-zinc-900 dark:text-zinc-100">1. About PlacementOS</h2>
        <p className="text-sm leading-relaxed">
          PlacementOS is a university placement workflow automation platform operated on behalf of
          the placement cell. It automatically processes CDC placement emails, evaluates student
          eligibility against placement drives, detects shortlist announcements, and delivers
          personalised placement alerts to students via Telegram.
        </p>
        <p className="text-sm leading-relaxed">
          This policy explains what data we collect, how we use it, how long we retain it, and what
          controls you have.
        </p>
      </section>

      <section className="space-y-3">
        <h2 className="text-lg font-bold text-zinc-900 dark:text-zinc-100">2. Data We Collect</h2>

        <h3 className="text-sm font-semibold text-zinc-900 dark:text-zinc-100">2.1 Account Credentials</h3>
        <p className="text-sm leading-relaxed">
          We collect your email address and a BCrypt-hashed copy of your password. Plaintext
          passwords are never stored or logged. Authentication is issued as short-lived JWT tokens.
        </p>

        <h3 className="text-sm font-semibold text-zinc-900 dark:text-zinc-100">2.2 Student Academic Profile</h3>
        <p className="text-sm leading-relaxed">
          During onboarding you provide: full name, registration number, NeoPAT ID (optional),
          branch, batch year, CGPA, degree programme, specialisation (optional), standing arrears
          (optional), gender (optional), and phone number (optional). These fields are used solely
          to evaluate your eligibility against placement drives.
        </p>

        <h3 className="text-sm font-semibold text-zinc-900 dark:text-zinc-100">2.3 Placement and Application Data</h3>
        <p className="text-sm leading-relaxed">
          Placement drives are automatically sourced from university CDC emails. Your per-drive
          eligibility decisions, application status (ELIGIBLE, APPLIED, SHORTLISTED, etc.),
          application timestamps, and shortlist match results are stored.
        </p>

        <h3 className="text-sm font-semibold text-zinc-900 dark:text-zinc-100">2.4 Telegram Identity</h3>
        <p className="text-sm leading-relaxed">
          If you connect your Telegram account, we store your Telegram chat ID, Telegram user ID,
          and Telegram username. These are used exclusively to send placement alerts and receive
          application confirmation commands via Telegram. Your Telegram data is removed when you
          disconnect Telegram or delete your account.
        </p>

        <h3 className="text-sm font-semibold text-zinc-900 dark:text-zinc-100">2.5 Gmail Source Processing</h3>
        <p className="text-sm leading-relaxed">
          The system processes CDC Gmail messages to extract placement information. Email text and
          attachment content (e.g., Excel shortlists, PDFs) is processed to identify placement
          details. Processed email metadata is stored. Raw email content is not stored beyond what
          is necessary for placement extraction.
        </p>

        <h3 className="text-sm font-semibold text-zinc-900 dark:text-zinc-100">2.6 Notifications and Reminders</h3>
        <p className="text-sm leading-relaxed">
          Notification records (type, channel, delivery status, timestamps) and reminder tasks
          (schedule, status) are stored per student to track delivery and avoid duplicate alerts.
        </p>

        <h3 className="text-sm font-semibold text-zinc-900 dark:text-zinc-100">2.7 Technical and Log Data</h3>
        <p className="text-sm leading-relaxed">
          Application server logs may capture request timestamps, HTTP status codes, and error
          messages for operational purposes. JWT tokens and credentials are never written to logs.
        </p>
      </section>

      <section className="space-y-3">
        <h2 className="text-lg font-bold text-zinc-900 dark:text-zinc-100">3. How We Use Your Data</h2>
        <ul className="text-sm leading-relaxed list-disc list-inside space-y-1">
          <li>To evaluate your academic eligibility against current and future placement drives.</li>
          <li>To detect your presence on company shortlists from CDC-sourced documents.</li>
          <li>To deliver personalised placement eligibility, shortlist, deadline, and reminder alerts via Telegram.</li>
          <li>To track your application status and record explicit Apply actions.</li>
          <li>To allow you to use the student portal and mobile app.</li>
        </ul>
        <p className="text-sm leading-relaxed">
          We do not sell your personal data. We do not use your data for advertising.
        </p>
      </section>

      <section className="space-y-3">
        <h2 className="text-lg font-bold text-zinc-900 dark:text-zinc-100">4. Data Retention</h2>
        <p className="text-sm leading-relaxed">
          <strong>Authentication account:</strong> Your login credentials are removed when you delete
          your account. Your email address is anonymised in our database.
        </p>
        <p className="text-sm leading-relaxed">
          <strong>Personal profile fields:</strong> Name, phone number, gender, and specialisation
          are anonymised when you delete your account.
        </p>
        <p className="text-sm leading-relaxed">
          <strong>Institutional academic identifiers:</strong> Registration number and NeoPAT ID are
          retained for institutional shortlist history integrity. These identifiers form the
          university system of record and cannot be unilaterally deleted.
        </p>
        <p className="text-sm leading-relaxed">
          <strong>Placement and application history:</strong> Placement drives, shortlist entries,
          and your application records are retained as part of the university placement system of
          record. They are not deleted when you delete your account.
        </p>
        <p className="text-sm leading-relaxed">
          <strong>Telegram identity:</strong> Removed immediately on account deletion or Telegram disconnection.
        </p>
        <p className="text-sm leading-relaxed">
          <strong>Reminders and outbox:</strong> Cancelled on account deletion.
        </p>
      </section>

      <section className="space-y-3">
        <h2 className="text-lg font-bold text-zinc-900 dark:text-zinc-100">5. Your Rights</h2>
        <ul className="text-sm leading-relaxed list-disc list-inside space-y-1">
          <li><strong>Access:</strong> View your profile and academic data in the student portal at any time.</li>
          <li><strong>Update:</strong> Update editable profile fields (name, phone number, specialisation, gender, standing arrears) via Profile settings.</li>
          <li><strong>Telegram disconnection:</strong> Disconnect Telegram at any time from Profile settings. This removes all Telegram identity data immediately.</li>
          <li><strong>Account deletion:</strong> Delete your account from Profile → Delete Account. This anonymises personal data and removes your authentication credentials and Telegram identity. See Section 4 for institutional record retention.</li>
        </ul>
      </section>

      <section className="space-y-3">
        <h2 className="text-lg font-bold text-zinc-900 dark:text-zinc-100">6. Data Security</h2>
        <p className="text-sm leading-relaxed">
          Passwords are stored as BCrypt hashes and are never logged. Authentication tokens are
          short-lived JWTs. Database credentials, Gmail OAuth refresh tokens (stored with AES-256-GCM
          encryption), Telegram bot tokens, and all other service secrets are environment-injected
          and are never committed to source control or logged.
        </p>
      </section>

      <section className="space-y-3">
        <h2 className="text-lg font-bold text-zinc-900 dark:text-zinc-100">7. Third-Party Services</h2>
        <ul className="text-sm leading-relaxed list-disc list-inside space-y-1">
          <li><strong>Telegram:</strong> Placement alerts are delivered via the Telegram Bot API. Telegram's own privacy policy applies to your use of the Telegram application.</li>
          <li><strong>Google (Gmail):</strong> The platform's system Gmail account is used to receive university CDC placement emails. Student personal Gmail accounts are not accessed.</li>
          <li><strong>Neon PostgreSQL:</strong> Database hosted on Neon (PostgreSQL-compatible). Data is stored within the configured Neon region.</li>
          <li><strong>Upstash Redis:</strong> Used for event streaming and caching. No personally identifiable student data is permanently stored in Redis.</li>
        </ul>
      </section>

      <section className="space-y-3">
        <h2 className="text-lg font-bold text-zinc-900 dark:text-zinc-100">8. Contact</h2>
        <p className="text-sm leading-relaxed">
          For privacy-related questions or requests, contact your university Placement Cell
          administrator.
        </p>
      </section>

      <div className="pt-4 border-t border-zinc-200 dark:border-zinc-800">
        <a
          href="/dashboard"
          className="text-xs font-medium text-indigo-600 dark:text-indigo-400 hover:underline"
        >
          ← Return to Dashboard
        </a>
      </div>
    </main>
  );
}
