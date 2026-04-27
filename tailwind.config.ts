import type { Config } from "tailwindcss";

const config: Config = {
  content: ["./src/**/*.{ts,tsx}"],
  theme: {
    extend: {
      colors: {
        paper: "#f7f3ec",
        ink: "#1a1714",
        muted: "#7a7268",
        rule: "#e4ddd0",
        accent: "#5b5e8c",
      },
      fontFamily: {
        serif: [
          "Iowan Old Style",
          "Source Serif Pro",
          "Source Serif 4",
          "Charter",
          "Georgia",
          "serif",
        ],
        mono: [
          "ui-monospace",
          "SFMono-Regular",
          "Menlo",
          "Monaco",
          "Consolas",
          "monospace",
        ],
      },
      maxWidth: {
        page: "680px",
      },
    },
  },
  plugins: [],
};

export default config;
