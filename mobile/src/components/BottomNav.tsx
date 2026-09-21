import { usePathname, useRouter } from 'expo-router';
import React from 'react';
import { StyleSheet, Text, TouchableOpacity, View } from 'react-native';

interface NavItem {
  name: string;
  route: string;
  icon: string;
}

const NAV_ITEMS: NavItem[] = [
  { name: 'Dashboard', route: '/dashboard', icon: '📊' },
  { name: 'Drives', route: '/placements', icon: '💼' },
  { name: 'Applied', route: '/applications', icon: '📝' },
  { name: 'Alerts', route: '/notifications', icon: '🔔' },
  { name: 'Profile', route: '/profile', icon: '👤' },
];

export default function BottomNav() {
  const router = useRouter();
  const pathname = usePathname();

  return (
    <View style={styles.container}>
      {NAV_ITEMS.map((item) => {
        const isActive = pathname.includes(item.route);
        return (
          <TouchableOpacity
            key={item.route}
            style={[styles.tab, isActive && styles.activeTab]}
            onPress={() => router.push(item.route as any)}
            activeOpacity={0.7}
          >
            <Text style={styles.icon}>{item.icon}</Text>
            <Text style={[styles.label, isActive && styles.activeLabel]}>{item.name}</Text>
          </TouchableOpacity>
        );
      })}
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flexDirection: 'row',
    backgroundColor: '#121215',
    borderTopColor: '#27272a',
    borderTopWidth: 1,
    paddingVertical: 8,
    paddingHorizontal: 12,
    justifyContent: 'space-around',
  },
  tab: {
    alignItems: 'center',
    justifyContent: 'center',
    paddingVertical: 4,
    paddingHorizontal: 8,
    borderRadius: 8,
  },
  activeTab: {
    backgroundColor: 'rgba(99, 102, 241, 0.12)',
  },
  icon: {
    fontSize: 18,
    marginBottom: 2,
  },
  label: {
    fontSize: 11,
    fontWeight: '500',
    color: '#71717a',
  },
  activeLabel: {
    color: '#818cf8',
    fontWeight: '700',
  },
});
