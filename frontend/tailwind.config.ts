import type { Config } from "tailwindcss";

// Config simples: Tailwind analisa seus arquivos para gerar apenas o CSS usado.
export default {
  content: ["./index.html", "./src/**/*.{ts,tsx}"],
  theme: {
    extend: {},
  },
  plugins: [],
} satisfies Config;

