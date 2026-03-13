"use client";

import { FormEvent, KeyboardEvent, useCallback, useRef, useEffect } from "react";

type Props = {
  value: string;
  onChange: (value: string) => void;
  onSubmit: () => void;
  disabled?: boolean;
  isStreaming?: boolean;
  onStop?: () => void;
  modelLabel?: string;
};

export function Composer({ value, onChange, onSubmit, disabled, isStreaming, onStop, modelLabel }: Props) {
  const textareaRef = useRef<HTMLTextAreaElement>(null);

  useEffect(() => {
    const el = textareaRef.current;
    if (!el) return;
    el.style.height = "auto";
    el.style.height = `${Math.min(el.scrollHeight, 200)}px`;
  }, [value]);

  const handleSubmit = useCallback((e: FormEvent) => {
    e.preventDefault();
    if (!value.trim() || disabled) return;
    onSubmit();
  }, [value, disabled, onSubmit]);

  const handleKeyDown = useCallback((e: KeyboardEvent<HTMLTextAreaElement>) => {
    if (e.key === "Enter" && !e.shiftKey) {
      e.preventDefault();
      if (!value.trim() || disabled) return;
      onSubmit();
    }
  }, [value, disabled, onSubmit]);

  return (
    <div className="composer">
      <form className="composer-inner" onSubmit={handleSubmit}>
        <textarea
          ref={textareaRef}
          className="composer-input"
          value={value}
          onChange={(e) => onChange(e.target.value)}
          onKeyDown={handleKeyDown}
          placeholder="Envie uma mensagem..."
          disabled={disabled}
          rows={1}
        />
        <button
          type="submit"
          className="composer-send"
          disabled={!value.trim() || disabled}
          aria-label="Enviar mensagem"
        >
          <svg viewBox="0 0 16 16" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <path d="M8 12V4M4 8l4-4 4 4" />
          </svg>
        </button>
      </form>
      <div className="composer-footer" style={{ maxWidth: 720, margin: "0 auto" }}>
        {modelLabel ? (
          <span className="composer-model-label">{modelLabel}</span>
        ) : <span />}
        {isStreaming && onStop ? (
          <button type="button" className="composer-stop" onClick={onStop}>
            Parar
          </button>
        ) : null}
      </div>
    </div>
  );
}
