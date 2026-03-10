/* eslint-disable react-refresh/only-export-components */

import {
  ReactNode,
  createContext,
  useContext,
  useEffect,
  useMemo,
  useState,
} from 'react';
import { settingsService } from '../../services/settingsService';
import { toApiClientError } from '../../services/api';
import { SettingsPreferencesDto, ThemeMode } from '../../types';

const THEME_STORAGE_KEY = 'lume-theme';

const defaultPreferences: SettingsPreferencesDto = {
  appearance: 'light',
  languageCode: 'pt-BR',
  emailUpdates: true,
  productUpdates: true,
};

interface ThemeContextValue {
  theme: ThemeMode;
  preferences: SettingsPreferencesDto;
  preferencesLoading: boolean;
  preferencesError: string | null;
  updatePreferences: (payload: Partial<SettingsPreferencesDto>) => Promise<void>;
}

const ThemeContext = createContext<ThemeContextValue | null>(null);

function readStoredTheme(): ThemeMode {
  if (typeof window === 'undefined') {
    return 'light';
  }

  const storedTheme = window.localStorage.getItem(THEME_STORAGE_KEY);
  return storedTheme === 'dark' ? 'dark' : 'light';
}

export function bootstrapTheme() {
  if (typeof document === 'undefined') {
    return;
  }

  document.documentElement.dataset.theme = readStoredTheme();
}

interface ThemeProviderProps {
  children: ReactNode;
}

export function ThemeProvider({ children }: ThemeProviderProps) {
  const [preferences, setPreferences] = useState<SettingsPreferencesDto>(() => ({
    ...defaultPreferences,
    appearance: readStoredTheme(),
  }));
  const [preferencesLoading, setPreferencesLoading] = useState(true);
  const [preferencesError, setPreferencesError] = useState<string | null>(null);

  useEffect(() => {
    document.documentElement.dataset.theme = preferences.appearance;
    window.localStorage.setItem(THEME_STORAGE_KEY, preferences.appearance);
  }, [preferences.appearance]);

  useEffect(() => {
    void (async () => {
      try {
        setPreferencesLoading(true);
        setPreferencesError(null);
        const remotePreferences = await settingsService.getPreferences();
        setPreferences(remotePreferences);
      } catch (error) {
        setPreferencesError(toApiClientError(error).message);
      } finally {
        setPreferencesLoading(false);
      }
    })();
  }, []);

  const value = useMemo<ThemeContextValue>(() => ({
    theme: preferences.appearance,
    preferences,
    preferencesLoading,
    preferencesError,
    updatePreferences: async (payload) => {
      const previousPreferences = preferences;
      const optimisticPreferences = {
        ...preferences,
        ...payload,
      };
      setPreferences(optimisticPreferences);

      try {
        setPreferencesError(null);
        const nextPreferences = await settingsService.updatePreferences(payload);
        setPreferences(nextPreferences);
      } catch (error) {
        setPreferencesError(toApiClientError(error).message);
        setPreferences(previousPreferences);
        throw error;
      }
    },
  }), [preferences, preferencesError, preferencesLoading]);

  return <ThemeContext.Provider value={value}>{children}</ThemeContext.Provider>;
}

export function useTheme() {
  const context = useContext(ThemeContext);

  if (!context) {
    throw new Error('useTheme must be used within ThemeProvider');
  }

  return context;
}
