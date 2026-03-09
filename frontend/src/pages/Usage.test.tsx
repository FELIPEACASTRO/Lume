import { render, screen } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import Usage from './Usage';

const mockUseShell = vi.fn();

vi.mock('../components/shell/ShellContext', () => ({
  useShell: () => mockUseShell(),
}));

describe('Usage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockUseShell.mockReturnValue({
      session: {
        role: {
          permissions: ['budgets.read', 'budgets.manage'],
        },
      },
      usage: {
        dailyCredits: 300,
        consumedCredits: 120,
        remainingCredits: 180,
        activeTasks: 6,
        scheduledTasks: 1,
        unreadNotifications: 3,
        note: 'Usage operacional.',
        budget: {
          costCenter: 'core_now',
          chargebackMode: 'showback',
          softLimitCredits: 300,
          hardLimitCredits: 450,
          consumedCredits: 120,
          remainingSoftCredits: 180,
          remainingHardCredits: 330,
          softLimitUtilizationPercent: 40,
          hardLimitUtilizationPercent: 27,
          softLimitReached: false,
          hardLimitReached: false,
          budgetStatus: 'healthy',
          note: 'Budget operacional.',
        },
      },
    });
  });

  it('renders budget and finops metrics from shell usage summary', () => {
    render(<Usage />);

    expect(screen.getByText('Cost center')).toBeInTheDocument();
    expect(screen.getByText('core_now')).toBeInTheDocument();
    expect(screen.getByText('showback | healthy')).toBeInTheDocument();
    expect(screen.getByText('40%')).toBeInTheDocument();
    expect(screen.getByText('27%')).toBeInTheDocument();
  });
});
