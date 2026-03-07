import { Link } from 'react-router-dom';

export default function NotFound() {
  return (
    <div className="text-center py-20">
      <h1 className="text-6xl font-bold text-lume-500 mb-4">404</h1>
      <h2 className="text-2xl font-semibold text-dark-900 mb-2">Página não encontrada</h2>
      <p className="text-dark-500 mb-8">A página que você está procurando não existe.</p>
      <Link to="/" className="btn-primary">
        Voltar para Home
      </Link>
    </div>
  );
}
