import { useUsers } from '../hooks/useUsers';
import UserTable from '../components/users/UserTable';
import UserForm from '../components/users/UserForm';
import Loading from '../components/common/Loading';
import EmptyState from '../components/common/EmptyState';
import { FiPlus } from 'react-icons/fi';

/**
 * Página de gerenciamento de usuários.
 *
 * Princípio SRP: responsável apenas pela composição e layout.
 * A lógica de estado é delegada ao hook useUsers (separação de concerns).
 */
export default function Users() {
  const {
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
  } = useUsers();

  return (
    <div className="space-y-6">
      <div className="flex justify-between items-center">
        <div>
          <h1 className="text-2xl font-bold text-dark-900">Usuários</h1>
          <p className="text-dark-500 mt-1">Gerencie os usuários do sistema</p>
        </div>
        {!showForm && (
          <button onClick={openCreateForm} className="btn-primary flex items-center gap-2">
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
            onCancel={cancelForm}
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
          <UserTable users={users} onEdit={startEditing} onDelete={handleDelete} />
        )}
      </div>
    </div>
  );
}
