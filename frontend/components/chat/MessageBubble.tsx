import clsx from "clsx";
import { MarkdownRenderer } from "./MarkdownRenderer";

type Props = {
  role: "user" | "assistant";
  body: string;
  timestamp?: string;
  userInitials?: string;
};

export function MessageBubble({ role, body, timestamp, userInitials }: Props) {
  return (
    <div className={clsx("message", role === "user" ? "message-user" : "message-assistant")}>
      <div className={clsx("message-avatar", role === "user" ? "message-avatar-user" : "message-avatar-assistant")}>
        {role === "user" ? (userInitials ?? "U") : "L"}
      </div>
      <div className="message-content">
        <div className="message-bubble">
          {role === "assistant" ? (
            <MarkdownRenderer content={body} />
          ) : (
            body
          )}
        </div>
        {timestamp ? <div className="message-time">{timestamp}</div> : null}
      </div>
    </div>
  );
}
