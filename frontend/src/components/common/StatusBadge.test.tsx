import { render, screen } from '@testing-library/react';
import StatusBadge from './StatusBadge';

describe('StatusBadge', () => {
  it('renders the unavailable label', () => {
    render(<StatusBadge state="unavailable" />);
    expect(screen.getByText('Indisponivel')).toBeInTheDocument();
  });

  it('renders the attention label', () => {
    render(<StatusBadge state="attention" />);
    expect(screen.getByText('Atencao')).toBeInTheDocument();
  });
});
