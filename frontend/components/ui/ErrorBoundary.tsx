"use client";

import { Component, ErrorInfo, ReactNode } from "react";

type Props = {
  children: ReactNode;
  fallback?: ReactNode;
};

type State = {
  hasError: boolean;
};

export class ErrorBoundary extends Component<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = { hasError: false };
  }

  static getDerivedStateFromError(): State {
    return { hasError: true };
  }

  componentDidCatch(error: Error, info: ErrorInfo) {
    console.error("[ErrorBoundary]", error, info.componentStack);
  }

  render() {
    if (this.state.hasError) {
      return (
        this.props.fallback ?? (
          <div className="screen-center">
            <div style={{ textAlign: "center" }}>
              <h2 style={{ marginBottom: "0.5rem" }}>Algo deu errado</h2>
              <p className="muted">Recarregue a pagina para tentar novamente.</p>
            </div>
          </div>
        )
      );
    }
    return this.props.children;
  }
}
