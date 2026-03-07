import { Link } from 'react-router-dom';

export default function Header() {
  return (
    <header className="bg-white shadow-sm border-b border-dark-100">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex justify-between items-center h-16">
          <Link to="/" className="flex items-center gap-3">
            <img src="/lume.svg" alt="Lume" className="h-9 w-9" />
            <span className="text-xl font-bold text-dark-900">Lume</span>
          </Link>

          <nav className="flex items-center gap-6">
            <Link
              to="/"
              className="text-dark-600 hover:text-lume-500 font-medium transition-colors"
            >
              Home
            </Link>
            <Link
              to="/users"
              className="text-dark-600 hover:text-lume-500 font-medium transition-colors"
            >
              Usuários
            </Link>
          </nav>
        </div>
      </div>
    </header>
  );
}
