"use client";

import { useCallback, useRef, useState } from "react";
import { streamInference, StreamInferenceRequest } from "@/lib/api/inference";
import { SSEEvent } from "@/lib/utils/parseSSE";

type UseStreamingChatReturn = {
  streamingContent: string;
  isStreaming: boolean;
  streamError: string | null;
  startStream: (request: StreamInferenceRequest) => void;
  stopStream: () => void;
};

export function useStreamingChat(onComplete?: (fullContent: string) => void): UseStreamingChatReturn {
  const [streamingContent, setStreamingContent] = useState("");
  const [isStreaming, setIsStreaming] = useState(false);
  const [streamError, setStreamError] = useState<string | null>(null);
  const abortControllerRef = useRef<AbortController | null>(null);
  const contentRef = useRef("");

  const startStream = useCallback((request: StreamInferenceRequest) => {
    // Abort any existing stream
    abortControllerRef.current?.abort();

    const controller = new AbortController();
    abortControllerRef.current = controller;
    contentRef.current = "";
    setStreamingContent("");
    setIsStreaming(true);
    setStreamError(null);

    streamInference(
      request,
      {
        onEvent: (event: SSEEvent) => {
          if (event.type === "delta" || event.type === "message") {
            contentRef.current += event.data;
            setStreamingContent(contentRef.current);
          } else if (event.type === "error") {
            setStreamError(event.data);
          }
        },
        onDone: () => {
          setIsStreaming(false);
          if (onComplete && contentRef.current) {
            onComplete(contentRef.current);
          }
        },
        onError: (error) => {
          setIsStreaming(false);
          setStreamError(error.message);
        }
      },
      controller.signal
    );
  }, [onComplete]);

  const stopStream = useCallback(() => {
    abortControllerRef.current?.abort();
    setIsStreaming(false);
  }, []);

  return {
    streamingContent,
    isStreaming,
    streamError,
    startStream,
    stopStream
  };
}
