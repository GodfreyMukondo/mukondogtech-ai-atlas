/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        navy: {
          DEFAULT: "#0B1F3A",
          dark: "#050E1A",
          light: "#1F314A",
          accent: "#3C4C61",
        },
        gold: {
          DEFAULT: "#C6A15B",
          dark: "#A8894D",
          light: "#D4B984",
          pale: "#E5D5B5",
          faint: "#F6F1E6",
        },
      },
    },
  },
  plugins: [],
}