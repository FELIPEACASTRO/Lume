import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ThemeProvider, useTheme } from './ThemeProvider';

const getPreferences = vi.fn();
const updatePreferences = vi.fn();

vi.mock('../../services/settingsService', () => ({
  settingsService: {
    getPreferences: () => getPreferences(),
    updatePreferences: (payload: unknown) => updatePreferences(payload),
  },
}));

function ThemeHarness() {
  const { theme, updatePreferences: persistPreferences } = useTheme();

  return (
    <div>
      <span data-testid="theme-value">{theme}</span>
      <button type="button" onClick={() => void persistPreferences({ appearance: theme === 'light' ? 'dark' : 'light' })}>
        alternar
      </button>
    </div>
  );
}

describe('ThemeProvider', () => {
  beforeEach(() => {
    window.localStorage.clear();
    document.documentElement.dataset.theme = 'light';
    getPreferences.mockResolvedValue({
      appearance: 'dark',
      languageCode: 'pt-BR',
      emailUpdates: true,
      productUpdates: true,
    });
    updatePreferences.mockImplementation(async (payload) => ({
      appearance: payload.appearance ?? 'dark',
      languageCode: 'pt-BR',
      emailUpdates: true,
      productUpdates: true,
    }));
  });

  it('applies the remote theme and persists updates', async () => {
    const user = userEvent.setup();

    render(
      <ThemeProvider>
        <ThemeHarness />
      </ThemeProvider>
    );

    await waitFor(() => {
      expect(screen.getByTestId('theme-value')).toHaveTextContent('dark');
      expect(document.documentElement.dataset.theme).toBe('dark');
    });

    await user.click(screen.getByRole('button', { name: 'alternar' }));

    await waitFor(() => {
      expect(updatePreferences).toHaveBeenCalledWith({ appearance: 'light' });
      expect(document.documentElement.dataset.theme).toBe('light');
      expect(window.localStorage.getItem('lume-theme')).toBe('light');
    });
  });
});
