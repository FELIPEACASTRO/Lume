import { Link } from 'react-router-dom';
import { FiUsers, FiDatabase, FiCode } from 'react-icons/fi';

export default function Home() {
  return (
    <div className="space-y-8">
      <div className="text-center py-12">
        <img src="/lume.svg" alt="Lume" className="h-20 w-20 mx-auto mb-6" />
        <h1 className="text-4xl font-bold text-dark-900 mb-4">
          Bem-vindo ao <span className="text-lume-500">Lume</span>
        </h1>
        <p className="text-lg text-dark-500 max-w-2xl mx-auto">
          Uma aplicação moderna construída com React, Java 21 (Spring Boot) e PostgreSQL.
        </p>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        <div className="card text-center">
          <div className="inline-flex items-center justify-center w-12 h-12 bg-lume-100 text-lume-600 rounded-lg mb-4">
            <FiCode size={24} />
          </div>
          <h3 className="text-lg font-semibold text-dark-900 mb-2">Frontend React</h3>
          <p className="text-dark-500 text-sm">
            Interface moderna com React 18, TypeScript, Tailwind CSS e Vite.
          </p>
        </div>

        <div className="card text-center">
          <div className="inline-flex items-center justify-center w-12 h-12 bg-green-100 text-green-600 rounded-lg mb-4">
            <FiDatabase size={24} />
          </div>
          <h3 className="text-lg font-semibold text-dark-900 mb-2">Backend Java 21</h3>
          <p className="text-dark-500 text-sm">
            API REST robusta com Spring Boot 3, JPA e Flyway para migrações.
          </p>
        </div>

        <div className="card text-center">
          <div className="inline-flex items-center justify-center w-12 h-12 bg-blue-100 text-blue-600 rounded-lg mb-4">
            <FiUsers size={24} />
          </div>
          <h3 className="text-lg font-semibold text-dark-900 mb-2">PostgreSQL</h3>
          <p className="text-dark-500 text-sm">
            Banco de dados relacional confiável com suporte a migrações automatizadas.
          </p>
        </div>
      </div>

      <div className="text-center">
        <Link to="/users" className="btn-primary inline-block">
          Gerenciar Usuários
        </Link>
      </div>
    </div>
  );
}
