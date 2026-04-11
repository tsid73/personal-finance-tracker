import type { Config } from "tailwindcss";

export default {
  darkMode: "class",
  content: ["./index.html", "./src/**/*.{ts,tsx}"],
  theme: {
    extend: {
      colors: {
        ink: "#102a43",
        mist: "#f4f7fb",
        accent: "#0f766e",
        coral: "#e76f51",
        gold: "#e9c46a"
      },
      boxShadow: {
        card: "0 18px 50px rgba(16, 42, 67, 0.08)"
      }
    }
  },
  plugins: []
} satisfies Config;
