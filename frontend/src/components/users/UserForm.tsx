import { useState, FormEvent } from 'react';
import { UserRequest } from '../../types';

interface UserFormProps {
  initialData?: Partial<UserRequest>;
  onSubmit: (data: UserRequest) => Promise<void>;
  onCancel: () => void;
  isEditing?: boolean;
}

export default function UserForm({ initialData, onSubmit, onCancel, isEditing = false }: UserFormProps) {
  const [formData, setFormData] = useState<UserRequest>({
    name: initialData?.name || '',
    email: initialData?.email || '',
    password: initialData?.password || '',
  });
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setLoading(true);
    try {
      await onSubmit(formData);
    } finally {
      setLoading(false);
    }
  };

  return (
    <form onSubmit={handleSubmit} className="space-y-4">
      <div>
        <label htmlFor="name" className="block text-sm font-medium text-dark-700 mb-1">
          Nome
        </label>
        <input
          id="name"
          type="text"
          className="input-field"
          value={formData.name}
          onChange={(e) => setFormData({ ...formData, name: e.target.value })}
          required
          minLength={2}
          maxLength={150}
        />
      </div>

      <div>
        <label htmlFor="email" className="block text-sm font-medium text-dark-700 mb-1">
          E-mail
        </label>
        <input
          id="email"
          type="email"
          className="input-field"
          value={formData.email}
          onChange={(e) => setFormData({ ...formData, email: e.target.value })}
          required
        />
      </div>

      <div>
        <label htmlFor="password" className="block text-sm font-medium text-dark-700 mb-1">
          Senha {isEditing && <span className="text-dark-400">(deixe em branco para manter)</span>}
        </label>
        <input
          id="password"
          type="password"
          className="input-field"
          value={formData.password}
          onChange={(e) => setFormData({ ...formData, password: e.target.value })}
          required={!isEditing}
          minLength={6}
          maxLength={100}
        />
      </div>

      <div className="flex gap-3 pt-4">
        <button type="submit" className="btn-primary" disabled={loading}>
          {loading ? 'Salvando...' : isEditing ? 'Atualizar' : 'Criar'}
        </button>
        <button type="button" className="btn-secondary" onClick={onCancel}>
          Cancelar
        </button>
      </div>
    </form>
  );
}
