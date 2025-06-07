/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  darkMode: 'class',
  theme: {
    extend: {
      colors: {
        // Light mode colors
        primary: {
          light: '#ffffff',
          DEFAULT: '#f3f4f6',
          dark: '#e5e7eb',
        },
        // Dark mode colors
        dark: {
          primary: '#1f2937',
          secondary: '#111827',
          accent: '#374151',
        },
      },
    },
  },
  plugins: [],
} 