import { FiArrowLeft } from 'react-icons/fi';
import { Link } from 'react-router-dom';

export default function NotFound() {
  return (
    <div className="mx-auto flex min-h-[70vh] max-w-3xl items-center justify-center">
      <section className="shell-surface w-full px-6 py-10 text-center sm:px-10 sm:py-14">
        <p className="text-xs font-semibold uppercase tracking-[0.24em] text-[var(--ink-soft)]">404</p>
        <h1 className="mt-4 text-4xl font-semibold text-[var(--ink-strong)]" style={{ fontFamily: 'var(--font-display)' }}>
          Esta rota nao existe no workspace.
        </h1>
        <p className="mx-auto mt-4 max-w-xl text-sm leading-7 text-[var(--ink-soft)]">
          Use a navegacao lateral ou volte para a pagina inicial para continuar a operacao.
        </p>
        <Link to="/" className="btn-primary mt-8">
          <FiArrowLeft size={16} />
          Voltar ao inicio
        </Link>
      </section>
    </div>
  );
}
