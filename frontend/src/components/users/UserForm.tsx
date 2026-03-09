import { FormEvent, useEffect, useState } from 'react';
import { FiLock, FiMail, FiShield, FiUser } from 'react-icons/fi';
import { UserFormData } from '../../types';

interface UserFormProps {
  initialData?: Partial<UserFormData>;
  onSubmit: (data: UserFormData) => Promise<void>;
  onCancel: () => void;
  isEditing?: boolean;
}

export default function UserForm({ initialData, onSubmit, onCancel, isEditing = false }: UserFormProps) {
  const [formData, setFormData] = useState<UserFormData>({
    name: initialData?.name || '',
    email: initialData?.email || '',
    password: initialData?.password || '',
    roleCode: initialData?.roleCode || 'workspace_member',
  });
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    setFormData({
      name: initialData?.name || '',
      email: initialData?.email || '',
      password: initialData?.password || '',
      roleCode: initialData?.roleCode || 'workspace_member',
    });
  }, [initialData]);

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setLoading(true);

    try {
      await onSubmit(formData);
    } finally {
      setLoading(false);
    }
  };

  return (
    <form className="space-y-5" onSubmit={handleSubmit}>
      <div className="space-y-2">
        <label htmlFor="name" className="text-xs font-semibold uppercase tracking-[0.24em] text-[var(--ink-soft)]">
          Nome
        </label>
        <div className="shell-input flex items-center gap-3">
          <FiUser size={16} className="text-[var(--ink-soft)]" />
          <input
            id="name"
            type="text"
            autoComplete="name"
            className="w-full border-none bg-transparent outline-none"
            value={formData.name}
            onChange={(event) => setFormData({ ...formData, name: event.target.value })}
            minLength={2}
            maxLength={150}
            required
          />
        </div>
      </div>

      <div className="space-y-2">
        <label htmlFor="roleCode" className="text-xs font-semibold uppercase tracking-[0.24em] text-[var(--ink-soft)]">
          Funcao
        </label>
        <div className="shell-input flex items-center gap-3">
          <FiShield size={16} className="text-[var(--ink-soft)]" />
          <select
            id="roleCode"
            className="w-full border-none bg-transparent outline-none"
            value={formData.roleCode}
            onChange={(event) => setFormData({ ...formData, roleCode: event.target.value })}
          >
            <option value="workspace_member">Membro</option>
            <option value="workspace_admin">Administrador</option>
          </select>
        </div>
      </div>

      <div className="space-y-2">
        <label htmlFor="email" className="text-xs font-semibold uppercase tracking-[0.24em] text-[var(--ink-soft)]">
          E-mail
        </label>
        <div className="shell-input flex items-center gap-3">
          <FiMail size={16} className="text-[var(--ink-soft)]" />
          <input
            id="email"
            type="email"
            autoComplete="email"
            className="w-full border-none bg-transparent outline-none"
            value={formData.email}
            onChange={(event) => setFormData({ ...formData, email: event.target.value })}
            required
          />
        </div>
      </div>

      <div className="space-y-2">
        <label htmlFor="password" className="text-xs font-semibold uppercase tracking-[0.24em] text-[var(--ink-soft)]">
          Senha {isEditing ? '(opcional)' : ''}
        </label>
        <div className="shell-input flex items-center gap-3">
          <FiLock size={16} className="text-[var(--ink-soft)]" />
          <input
            id="password"
            type="password"
            autoComplete="new-password"
            aria-describedby={isEditing ? 'password-help' : undefined}
            className="w-full border-none bg-transparent outline-none"
            value={formData.password}
            onChange={(event) => setFormData({ ...formData, password: event.target.value })}
            minLength={6}
            maxLength={100}
            required={!isEditing}
          />
        </div>
        {isEditing ? (
          <p id="password-help" className="text-sm text-[var(--ink-soft)]">Deixe em branco para manter a senha atual. O frontend envia a senha apenas quando ela for preenchida.</p>
        ) : null}
      </div>

      <div className="flex flex-wrap gap-3 pt-2">
        <button type="submit" className="btn-primary" disabled={loading}>
          {loading ? 'Salvando...' : isEditing ? 'Salvar alteracoes' : 'Convidar pessoa'}
        </button>
        <button type="button" className="btn-secondary" onClick={onCancel}>
          Cancelar
        </button>
      </div>
    </form>
  );
}
