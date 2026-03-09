import { render, screen } from '@testing-library/react';
import StatusBadge from './StatusBadge';

describe('StatusBadge', () => {
  it('renders the disabled preview label', () => {
    render(<StatusBadge state="disabled-preview" />);
    expect(screen.getByText('Indisponivel')).toBeInTheDocument();
  });
});
