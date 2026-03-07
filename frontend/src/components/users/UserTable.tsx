import { User } from '../../types';
import { formatDate } from '../../utils/formatters';
import { FiEdit2, FiTrash2 } from 'react-icons/fi';

interface UserTableProps {
  users: User[];
  onEdit: (user: User) => void;
  onDelete: (id: number) => void;
}

/**
 * Componente de tabela de usuários.
 * Princípio SRP: responsável apenas pela renderização da tabela.
 * A lógica de estado e ações é gerenciada pelo hook useUsers.
 */
export default function UserTable({ users, onEdit, onDelete }: UserTableProps) {
  return (
    <div className="overflow-x-auto">
      <table className="w-full">
        <thead>
          <tr className="border-b border-dark-100">
            <th className="text-left py-3 px-4 text-sm font-semibold text-dark-600">ID</th>
            <th className="text-left py-3 px-4 text-sm font-semibold text-dark-600">Nome</th>
            <th className="text-left py-3 px-4 text-sm font-semibold text-dark-600">E-mail</th>
            <th className="text-left py-3 px-4 text-sm font-semibold text-dark-600">Status</th>
            <th className="text-left py-3 px-4 text-sm font-semibold text-dark-600">Criado em</th>
            <th className="text-right py-3 px-4 text-sm font-semibold text-dark-600">Ações</th>
          </tr>
        </thead>
        <tbody>
          {users.map((user) => (
            <tr key={user.id} className="border-b border-dark-50 hover:bg-dark-50 transition-colors">
              <td className="py-3 px-4 text-sm text-dark-500">{user.id}</td>
              <td className="py-3 px-4 text-sm font-medium text-dark-900">{user.name}</td>
              <td className="py-3 px-4 text-sm text-dark-600">{user.email}</td>
              <td className="py-3 px-4">
                <span
                  className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${
                    user.active
                      ? 'bg-green-100 text-green-800'
                      : 'bg-red-100 text-red-800'
                  }`}
                >
                  {user.active ? 'Ativo' : 'Inativo'}
                </span>
              </td>
              <td className="py-3 px-4 text-sm text-dark-500">{formatDate(user.createdAt)}</td>
              <td className="py-3 px-4">
                <div className="flex justify-end gap-2">
                  <button
                    onClick={() => onEdit(user)}
                    className="p-2 text-dark-400 hover:text-lume-500 hover:bg-lume-50 rounded-lg transition-colors"
                    title="Editar"
                  >
                    <FiEdit2 size={16} />
                  </button>
                  <button
                    onClick={() => onDelete(user.id)}
                    className="p-2 text-dark-400 hover:text-red-500 hover:bg-red-50 rounded-lg transition-colors"
                    title="Excluir"
                  >
                    <FiTrash2 size={16} />
                  </button>
                </div>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
