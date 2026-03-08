import { useCallback, useEffect, useState } from 'react';
import toast from 'react-hot-toast';
import { memberService } from '../services/memberService';
import { toApiClientError } from '../services/api';
import { User, UserCreateRequest, UserFormData, UserUpdateRequest } from '../types';

interface UseUsersOptions {
  onUsersChanged?: () => Promise<void> | void;
}

export function useUsers(options: UseUsersOptions = {}) {
  const [users, setUsers] = useState<User[]>([]);
  const [loading, setLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [editingUser, setEditingUser] = useState<User | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [pendingDeletionUser, setPendingDeletionUser] = useState<User | null>(null);
  const [deletePending, setDeletePending] = useState(false);

  const loadUsers = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      const members = await memberService.findAll();
      setUsers(members);
    } catch (loadError) {
      const apiError = toApiClientError(loadError);
      setError(apiError.message);
      setUsers([]);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void loadUsers();
  }, [loadUsers]);

  const handleCreate = async (data: UserFormData) => {
    const payload: UserCreateRequest = {
      name: data.name,
      email: data.email,
      password: data.password,
      roleCode: data.roleCode,
    };

    try {
      await memberService.create(payload);
      toast.success('Membro criado com sucesso.');
      setShowForm(false);
      await loadUsers();
      await options.onUsersChanged?.();
    } catch (createError) {
      toast.error(toApiClientError(createError).message);
    }
  };

  const handleUpdate = async (data: UserFormData) => {
    if (!editingUser) {
      return;
    }

    const payload: UserUpdateRequest = {
      name: data.name,
      email: data.email,
      roleCode: data.roleCode,
      active: editingUser.active,
    };

    if (data.password.trim()) {
      payload.password = data.password.trim();
    }

    try {
      await memberService.update(editingUser.membershipId ?? editingUser.id, payload);
      toast.success('Membro atualizado com sucesso.');
      setEditingUser(null);
      setShowForm(false);
      await loadUsers();
      await options.onUsersChanged?.();
    } catch (updateError) {
      toast.error(toApiClientError(updateError).message);
    }
  };

  const requestDelete = (user: User) => {
    setPendingDeletionUser(user);
  };

  const confirmDelete = async () => {
    if (!pendingDeletionUser) {
      return;
    }

    try {
      setDeletePending(true);
      await memberService.update(pendingDeletionUser.membershipId ?? pendingDeletionUser.id, {
        name: pendingDeletionUser.name,
        email: pendingDeletionUser.email,
        roleCode: pendingDeletionUser.roleCode ?? 'workspace_member',
        active: false,
      });
      toast.success('Membership desativada com sucesso.');
      setPendingDeletionUser(null);
      await loadUsers();
      await options.onUsersChanged?.();
    } catch (deleteError) {
      toast.error(toApiClientError(deleteError).message);
    } finally {
      setDeletePending(false);
    }
  };

  const cancelDelete = () => {
    if (deletePending) {
      return;
    }

    setPendingDeletionUser(null);
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
    error,
    showForm,
    editingUser,
    pendingDeletionUser,
    deletePending,
    handleCreate,
    handleUpdate,
    requestDelete,
    confirmDelete,
    cancelDelete,
    startEditing,
    cancelForm,
    openCreateForm,
    reloadUsers: loadUsers,
  };
}
