"use client";

import { ReactNode } from "react";
import { ThemeContext, useThemeProvider } from "@/lib/hooks/useTheme";

type Props = {
  children: ReactNode;
};

export function ThemeProvider({ children }: Props) {
  const themeValue = useThemeProvider();
  return <ThemeContext.Provider value={themeValue}>{children}</ThemeContext.Provider>;
}
