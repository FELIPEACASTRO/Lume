"use client";

import { useEffect, useRef, ReactNode } from "react";

type Props = {
  children: ReactNode;
};

export function MessageList({ children }: Props) {
  const containerRef = useRef<HTMLDivElement>(null);
  const bottomRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: "smooth" });
  });

  return (
    <div className="chat-messages" ref={containerRef}>
      <div className="chat-messages-inner">
        {children}
        <div ref={bottomRef} />
      </div>
    </div>
  );
}
