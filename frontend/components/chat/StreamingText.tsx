"use client";

import { MarkdownRenderer } from "./MarkdownRenderer";

type Props = {
  content: string;
  isStreaming: boolean;
};

export function StreamingText({ content, isStreaming }: Props) {
  return (
    <div className="message message-assistant">
      <div className="message-avatar message-avatar-assistant">L</div>
      <div className="message-content">
        <div className="message-bubble">
          {content ? <MarkdownRenderer content={content} /> : null}
          {isStreaming ? <span className="streaming-cursor" /> : null}
        </div>
      </div>
    </div>
  );
}
