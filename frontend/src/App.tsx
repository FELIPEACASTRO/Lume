import { BrowserRouter } from 'react-router-dom';
import { Toaster } from 'react-hot-toast';
import { ThemeProvider } from './components/theme/ThemeProvider';
import AppRoutes from './routes/AppRoutes';

export default function App() {
  return (
    <ThemeProvider>
      <BrowserRouter>
        <Toaster
          position="top-right"
          toastOptions={{
            duration: 3200,
            style: {
              borderRadius: '20px',
              border: '1px solid var(--surface-border-main)',
              background: 'var(--surface-panel)',
              color: 'var(--text-primary)',
              boxShadow: '0 7px 16px 0 var(--shadow-surface)',
            },
            success: {
              iconTheme: {
                primary: '#FFBF36',
                secondary: 'var(--background-white-main)',
              },
            },
            error: {
              iconTheme: {
                primary: '#d24a43',
                secondary: 'var(--background-white-main)',
              },
            },
          }}
        />
        <AppRoutes />
      </BrowserRouter>
    </ThemeProvider>
  );
}
