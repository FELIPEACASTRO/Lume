import { describe, it, expect, afterEach } from "vitest";
import { render, cleanup } from "@testing-library/react";
import { MarkdownRenderer } from "@/components/chat/MarkdownRenderer";

afterEach(() => cleanup());

describe("MarkdownRenderer", () => {
  it("renders bold text", () => {
    const { container } = render(<MarkdownRenderer content="hello **world**" />);
    const strong = container.querySelector("strong");
    expect(strong).not.toBeNull();
    expect(strong?.textContent).toBe("world");
  });

  it("renders italic text", () => {
    const { container } = render(<MarkdownRenderer content="hello *world*" />);
    const em = container.querySelector("em");
    expect(em).not.toBeNull();
    expect(em?.textContent).toBe("world");
  });

  it("renders inline code", () => {
    const { container } = render(<MarkdownRenderer content="use `npm install`" />);
    const code = container.querySelector("code");
    expect(code).not.toBeNull();
    expect(code?.textContent).toBe("npm install");
  });

  it("renders headings", () => {
    const { container } = render(<MarkdownRenderer content="# Title" />);
    expect(container.querySelector("h1")).not.toBeNull();
    expect(container.querySelector("h1")?.textContent).toBe("Title");
  });

  it("renders h2 and h3", () => {
    const { container } = render(
      <MarkdownRenderer content={"## Subtitle\n\n### Section"} />
    );
    expect(container.querySelector("h2")?.textContent).toBe("Subtitle");
    expect(container.querySelector("h3")?.textContent).toBe("Section");
  });

  it("renders links with safe protocols", () => {
    const { container } = render(
      <MarkdownRenderer content="[click](https://example.com)" />
    );
    const link = container.querySelector("a");
    expect(link).not.toBeNull();
    expect(link?.getAttribute("href")).toBe("https://example.com");
    expect(link?.getAttribute("rel")).toBe("noopener");
  });

  it("strips javascript: protocol links (XSS prevention)", () => {
    const { container } = render(
      <MarkdownRenderer content="[xss](javascript:void)" />
    );
    const link = container.querySelector("a");
    expect(link).toBeNull();
    expect(container.textContent).toContain("xss");
  });

  it("strips data: protocol links (XSS prevention)", () => {
    const { container } = render(
      <MarkdownRenderer content="[xss](data:text/html,<script>alert(1)</script>)" />
    );
    const link = container.querySelector("a");
    expect(link).toBeNull();
  });

  it("strips vbscript: protocol links (XSS prevention)", () => {
    const { container } = render(
      <MarkdownRenderer content='[xss](vbscript:MsgBox("xss"))' />
    );
    const link = container.querySelector("a");
    expect(link).toBeNull();
  });

  it("renders fenced code blocks", () => {
    const { container } = render(
      <MarkdownRenderer content={'```js\nconsole.log("hi")\n```'} />
    );
    expect(container.querySelector(".code-block")).not.toBeNull();
    expect(container.querySelector("pre")).not.toBeNull();
  });

  it("renders unordered lists", () => {
    const { container } = render(
      <MarkdownRenderer content={"- item one\n- item two"} />
    );
    expect(container.querySelector("ul")).not.toBeNull();
    expect(container.querySelectorAll("li")).toHaveLength(2);
  });

  it("renders empty string without error", () => {
    const { container } = render(<MarkdownRenderer content="" />);
    expect(container.querySelector(".markdown-content")).not.toBeNull();
  });

  it("escapes HTML tags in content", () => {
    const { container } = render(
      <MarkdownRenderer content='<script>alert("xss")</script>' />
    );
    expect(container.querySelector("script")).toBeNull();
    expect(container.textContent).toContain("<script>");
  });
});
