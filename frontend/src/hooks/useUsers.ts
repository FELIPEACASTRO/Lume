import { useState, useEffect, useCallback } from 'react';
import { userService } from '../services/userService';
import { User, UserRequest } from '../types';
import toast from 'react-hot-toast';

/**
 * Custom Hook para gerenciamento de estado e operações de usuários.
 *
 * Princípio SRP: encapsula toda a lógica de estado e comunicação com a API,
 * separando-a da camada de apresentação (componentes).
 */
export function useUsers() {
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
    await userService.create(data);
    toast.success('Usuário criado com sucesso!');
    setShowForm(false);
    loadUsers();
  };

  const handleUpdate = async (data: UserRequest) => {
    if (!editingUser) return;
    await userService.update(editingUser.id, data);
    toast.success('Usuário atualizado com sucesso!');
    setEditingUser(null);
    setShowForm(false);
    loadUsers();
  };

  const handleDelete = async (id: number) => {
    if (!window.confirm('Tem certeza que deseja desativar este usuário?')) return;
    await userService.delete(id);
    toast.success('Usuário desativado com sucesso!');
    loadUsers();
  };

  const startEditing = (user: User) => {
    setEditingUser(user);
    setShowForm(true);
  };

  const cancelForm = () => {
    setShowForm(false);
    setEditingUser(null);
  };

  const openCreateForm = () => {
    setEditingUser(null);
    setShowForm(true);
  };

  return {
    users,
    loading,
    showForm,
    editingUser,
    handleCreate,
    handleUpdate,
    handleDelete,
    startEditing,
    cancelForm,
    openCreateForm,
  };
}
