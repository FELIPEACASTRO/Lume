import { useState, useEffect, useCallback } from 'react';
import { userService } from '../services/userService';
import { User, UserRequest } from '../types';
import UserTable from '../components/users/UserTable';
import UserForm from '../components/users/UserForm';
import Loading from '../components/common/Loading';
import EmptyState from '../components/common/EmptyState';
import toast from 'react-hot-toast';
import { FiPlus } from 'react-icons/fi';

export default function Users() {
  const [users, setUsers] = useState<User[]>([]);
  const [loading, setLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [editingUser, setEditingUser] = useState<User | null>(null);

  const loadUsers = useCallback(async () => {
    try {
      setLoading(true);
      const page = await userService.findAll();
      setUsers(page.content);
    } catch {
      toast.error('Erro ao carregar usuários');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadUsers();
  }, [loadUsers]);

  const handleCreate = async (data: UserRequest) => {
    try {
      await userService.create(data);
      toast.success('Usuário criado com sucesso!');
      setShowForm(false);
      loadUsers();
    } catch {
      toast.error('Erro ao criar usuário');
    }
  };

  const handleUpdate = async (data: UserRequest) => {
    if (!editingUser) return;
    try {
      await userService.update(editingUser.id, data);
      toast.success('Usuário atualizado com sucesso!');
      setEditingUser(null);
      setShowForm(false);
      loadUsers();
    } catch {
      toast.error('Erro ao atualizar usuário');
    }
  };

  const handleDelete = async (id: number) => {
    if (!window.confirm('Tem certeza que deseja desativar este usuário?')) return;
    try {
      await userService.delete(id);
      toast.success('Usuário desativado com sucesso!');
      loadUsers();
    } catch {
      toast.error('Erro ao desativar usuário');
    }
  };

  const handleEdit = (user: User) => {
    setEditingUser(user);
    setShowForm(true);
  };

  const handleCancel = () => {
    setShowForm(false);
    setEditingUser(null);
  };

  return (
    <div className="space-y-6">
      <div className="flex justify-between items-center">
        <div>
          <h1 className="text-2xl font-bold text-dark-900">Usuários</h1>
          <p className="text-dark-500 mt-1">Gerencie os usuários do sistema</p>
        </div>
        {!showForm && (
          <button onClick={() => setShowForm(true)} className="btn-primary flex items-center gap-2">
            <FiPlus size={18} />
            Novo Usuário
          </button>
        )}
      </div>

      {showForm && (
        <div className="card">
          <h2 className="text-lg font-semibold text-dark-900 mb-4">
            {editingUser ? 'Editar Usuário' : 'Novo Usuário'}
          </h2>
          <UserForm
            initialData={editingUser ? { name: editingUser.name, email: editingUser.email, password: '' } : undefined}
            onSubmit={editingUser ? handleUpdate : handleCreate}
            onCancel={handleCancel}
            isEditing={!!editingUser}
          />
        </div>
      )}

      <div className="card">
        {loading ? (
          <Loading />
        ) : users.length === 0 ? (
          <EmptyState
            title="Nenhum usuário encontrado"
            description="Clique em 'Novo Usuário' para adicionar o primeiro."
          />
        ) : (
          <UserTable users={users} onEdit={handleEdit} onDelete={handleDelete} />
        )}
      </div>
    </div>
  );
}
