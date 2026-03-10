/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        lume: {
          50: '#fef9ec',
          100: '#fcefc9',
          200: '#f9dd8e',
          300: '#f5c54e',
          400: '#f2b127',
          500: '#ec930e',
          600: '#d06e09',
          700: '#ad4e0c',
          800: '#8d3d10',
          900: '#743310',
          950: '#431905',
        },
        dark: {
          50: '#f6f6f6',
          100: '#e7e7e7',
          200: '#d1d1d1',
          300: '#b0b0b0',
          400: '#888888',
          500: '#6d6d6d',
          600: '#5d5d5d',
          700: '#4f4f4f',
          800: '#454545',
          900: '#3d3d3d',
          950: '#1a1a1a',
        },
      },
      fontFamily: {
        sans: ['-apple-system', 'BlinkMacSystemFont', '"Segoe UI Variable Display"', '"Segoe UI"', 'Helvetica', '"Apple Color Emoji"', 'Arial', 'sans-serif'],
        display: ['"Libre Baskerville"', 'Georgia', 'serif'],
      },
      boxShadow: {
        panel: '0 5px 16px 0 var(--shadow-surface), 0 0 1.25px 0 var(--shadow-surface)',
        shell: '0 7px 16px 0 var(--shadow-surface)',
      },
    },
  },
  plugins: [],
}
