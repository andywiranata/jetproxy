/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{js,ts,jsx,tsx}'],
  theme: {
    extend: {
      colors: {
        primary: {
          50: '#faf4f2',
          100: '#f5e9e4',
          200: '#ebd3ca',
          300: '#ddb7a7',
          400: '#cc917b',
          500: '#c17a61',
          600: '#b36952',
          700: '#955445',
          800: '#7a463c',
          900: '#653d35',
          950: '#351e1a',
        },
        copper: '#B36952',
      }
    },
  },
  plugins: [],
};