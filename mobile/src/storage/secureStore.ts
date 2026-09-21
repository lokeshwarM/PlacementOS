import * as SecureStore from 'expo-secure-store';
import { AuthUser } from '../types';

const TOKEN_KEY = 'placementos_jwt_token';
const USER_KEY = 'placementos_user_data';

export async function getToken(): Promise<string | null> {
  try {
    return await SecureStore.getItemAsync(TOKEN_KEY);
  } catch (error) {
    console.error('Failed to get token from SecureStore:', error);
    return null;
  }
}

export async function setToken(token: string): Promise<void> {
  try {
    await SecureStore.setItemAsync(TOKEN_KEY, token);
  } catch (error) {
    console.error('Failed to set token in SecureStore:', error);
    throw error;
  }
}

export async function removeToken(): Promise<void> {
  try {
    await SecureStore.deleteItemAsync(TOKEN_KEY);
  } catch (error) {
    console.error('Failed to delete token from SecureStore:', error);
  }
}

export async function getStoredUser(): Promise<AuthUser | null> {
  try {
    const data = await SecureStore.getItemAsync(USER_KEY);
    if (!data) return null;
    return JSON.parse(data) as AuthUser;
  } catch (error) {
    console.error('Failed to get user from SecureStore:', error);
    return null;
  }
}

export async function setStoredUser(user: AuthUser): Promise<void> {
  try {
    await SecureStore.setItemAsync(USER_KEY, JSON.stringify(user));
  } catch (error) {
    console.error('Failed to set user in SecureStore:', error);
    throw error;
  }
}

export async function removeStoredUser(): Promise<void> {
  try {
    await SecureStore.deleteItemAsync(USER_KEY);
  } catch (error) {
    console.error('Failed to delete user from SecureStore:', error);
  }
}

export async function clearAuthStorage(): Promise<void> {
  await Promise.all([removeToken(), removeStoredUser()]);
}
