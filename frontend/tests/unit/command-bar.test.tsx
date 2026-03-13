import { describe, it, expect, vi, afterEach } from "vitest";
import { render, screen, fireEvent, cleanup } from "@testing-library/react";
import { CommandBar } from "@/components/mission-control/CommandBar";

afterEach(() => {
  cleanup();
});

describe("CommandBar", () => {
  const defaultProps = {
    prompt: "",
    rankingPolicy: "quality-first",
    routingPolicy: "balanced",
    disabled: false,
    isRunning: false,
    costPreview: null,
    onPromptChange: vi.fn(),
    onRankingPolicyChange: vi.fn(),
    onRoutingPolicyChange: vi.fn(),
    onRun: vi.fn()
  };

  it("should render prompt input and run button", () => {
    render(<CommandBar {...defaultProps} />);
    expect(screen.getByPlaceholderText(/solicitação/i)).toBeDefined();
    expect(screen.getByText("Run")).toBeDefined();
  });

  it("should disable run button when disabled prop is true", () => {
    render(<CommandBar {...defaultProps} disabled />);
    const runButton = screen.getByText("Run");
    expect(runButton).toBeDisabled();
  });

  it("should show 'Executando...' when running", () => {
    render(<CommandBar {...defaultProps} isRunning />);
    expect(screen.getByText("Executando...")).toBeDefined();
  });

  it("should display cost preview when provided", () => {
    render(
      <CommandBar
        {...defaultProps}
        costPreview={{
          totalEstimatedCostUsd: 0.0042,
          averageEstimatedCostUsd: 0.0021,
          completedRuns: 2,
          failedRuns: 0
        }}
      />
    );
    expect(screen.getByText("US$ 0.0042")).toBeDefined();
  });

  it("should call onRun when form is submitted with prompt", () => {
    const onRun = vi.fn();
    render(<CommandBar {...defaultProps} prompt="test prompt" onRun={onRun} />);
    const form = screen.getByText("Run").closest("form")!;
    fireEvent.submit(form);
    expect(onRun).toHaveBeenCalledOnce();
  });
});
