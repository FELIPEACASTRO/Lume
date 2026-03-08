import { FiEdit2, FiTrash2 } from 'react-icons/fi';
import { User } from '../../types';
import { formatDate } from '../../utils/formatters';

interface UserTableProps {
  users: User[];
  onEdit: (user: User) => void;
  onDelete: (user: User) => void;
  canManage?: boolean;
}

export default function UserTable({ users, onEdit, onDelete, canManage = true }: UserTableProps) {
  return (
    <div className="space-y-3">
      {users.map((user) => (
        <article
          key={user.id}
          className="rounded-[24px] border bg-white px-5 py-5 transition-all duration-200 hover:-translate-y-0.5 hover:shadow-panel"
          style={{ borderColor: 'var(--line-soft)' }}
        >
          <div className="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
            <div className="min-w-0 flex-1">
              <div className="flex flex-wrap items-center gap-3">
                <h3 className="text-lg font-semibold text-[var(--ink-strong)]">{user.name}</h3>
                <span
                  className={[
                    'rounded-full px-3 py-1 text-xs font-semibold',
                    user.active ? 'bg-[rgba(44,134,86,0.12)] text-[#2c8656]' : 'bg-[rgba(163,60,47,0.12)] text-[#a33c2f]',
                  ].join(' ')}
                >
                  {user.active ? 'Ativo' : 'Inativo'}
                </span>
                {user.roleLabel ? (
                  <span className="rounded-full bg-[var(--surface-muted)] px-3 py-1 text-xs font-semibold text-[var(--ink-strong)]">
                    {user.roleLabel}
                  </span>
                ) : null}
                {user.currentUser ? (
                  <span className="rounded-full border px-3 py-1 text-xs font-semibold text-[var(--ink-soft)]" style={{ borderColor: 'var(--line-soft)' }}>
                    Voce
                  </span>
                ) : null}
              </div>
              <p className="mt-2 text-sm text-[var(--ink-soft)]">{user.email}</p>
              <div className="mt-4 flex flex-wrap gap-x-6 gap-y-2 text-xs font-semibold uppercase tracking-[0.18em] text-[var(--ink-soft)]">
                <span>ID {user.id}</span>
                <span>Criado em {formatDate(user.createdAt)}</span>
              </div>
            </div>

            <div className="flex flex-wrap gap-3">
              <button type="button" className="btn-secondary" onClick={() => onEdit(user)} disabled={!canManage}>
                <FiEdit2 size={16} />
                Editar
              </button>
              <button
                type="button"
                className="btn-danger"
                onClick={() => onDelete(user)}
                disabled={!canManage || user.currentUser}
              >
                <FiTrash2 size={16} />
                Desativar
              </button>
            </div>
          </div>
        </article>
      ))}
    </div>
  );
}
