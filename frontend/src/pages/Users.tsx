import { useMemo, useState } from 'react';
import { FiBarChart2, FiPlus, FiSearch, FiShield, FiUsers } from 'react-icons/fi';
import AsyncState from '../components/common/AsyncState';
import ConfirmDialog from '../components/common/ConfirmDialog';
import MetricCard from '../components/common/MetricCard';
import StatusBadge from '../components/common/StatusBadge';
import WorkspaceNotice from '../components/common/WorkspaceNotice';
import { useShell } from '../components/shell/ShellContext';
import UserForm from '../components/users/UserForm';
import UserTable from '../components/users/UserTable';
import { useUsers } from '../hooks/useUsers';

export default function Users() {
  const { refreshSummary, session } = useShell();
  const [query, setQuery] = useState('');
  const canManageMembers = session?.role.permissions.includes('members.manage') ?? false;
  const {
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
    reloadUsers,
  } = useUsers({ onUsersChanged: refreshSummary });

  const filteredUsers = useMemo(() => {
    const normalizedQuery = query.trim().toLowerCase();

    if (!normalizedQuery) {
      return users;
    }

    return users.filter((user) => {
      return [user.name, user.email, String(user.id), user.roleLabel ?? '', user.roleCode ?? '']
        .join(' ')
        .toLowerCase()
        .includes(normalizedQuery);
    });
  }, [query, users]);

  const activeUsers = users.filter((user) => user.active).length;
  const inactiveUsers = users.length - activeUsers;
  const metricsState = loading ? 'loading' : error ? 'error' : 'live';
  const metricsDetail = loading
    ? 'Aguardando resposta do backend.'
    : error
      ? 'API indisponivel para consolidar os totais.'
      : 'Contagem baseada nas memberships do workspace ativo.';
  const listState = loading ? 'loading' : error ? 'error' : filteredUsers.length === 0 ? 'empty' : 'live';

  return (
    <>
      <div className="space-y-6">
        <WorkspaceNotice
          title="Membros do workspace com tenancy real."
          description="A listagem agora depende da membership ativa do workspace selecionado. Roles e acesso de gestao respeitam RBAC no backend."
          state="live"
          detail={canManageMembers
            ? 'Voce possui permissao para convidar, editar role e desativar memberships neste workspace.'
            : 'Seu role atual permite leitura operacional, mas nao gestao de memberships neste workspace.'}
        />

        <section className="shell-surface p-6 sm:p-7">
          <div className="flex flex-col gap-6 xl:flex-row xl:items-end xl:justify-between">
            <div className="max-w-2xl">
              <div className="flex flex-wrap items-center gap-3">
                <p className="text-xs font-semibold uppercase tracking-[0.24em] text-[var(--ink-soft)]">Membros</p>
                <StatusBadge state="live" />
              </div>
              <h1 className="mt-3 text-3xl font-semibold text-[var(--ink-strong)]">Gestao de memberships no workspace ativo</h1>
              <p className="mt-3 text-sm leading-7 text-[var(--ink-soft)]">
                O modulo abaixo cruza usuario, membership, role e workspace ativo. A troca de workspace altera imediatamente o escopo da tela.
              </p>
            </div>

            <div className="flex flex-wrap gap-3">
              {!showForm && canManageMembers ? (
                <button type="button" className="btn-primary" onClick={openCreateForm}>
                  <FiPlus size={16} />
                  Novo membro
                </button>
              ) : null}
            </div>
          </div>

          <div className="mt-6 grid gap-4 md:grid-cols-3">
            <MetricCard
              icon={FiUsers}
              label="Membros"
              value={loading || error ? '--' : users.length}
              detail={metricsDetail}
              state={metricsState}
            />
            <MetricCard
              icon={FiBarChart2}
              label="Ativos"
              value={loading || error ? '--' : activeUsers}
              detail={metricsDetail}
              state={metricsState}
            />
            <MetricCard
              icon={FiShield}
              label="Inativos"
              value={loading || error ? '--' : inactiveUsers}
              detail={metricsDetail}
              state={metricsState}
            />
          </div>
        </section>

        <section className="grid gap-6 xl:grid-cols-[minmax(0,1.45fr)_380px]">
          <div className="shell-surface p-5 sm:p-6">
            <div className="flex flex-col gap-4 border-b pb-5 sm:flex-row sm:items-center sm:justify-between" style={{ borderColor: 'var(--line-soft)' }}>
              <div>
                <h2 className="text-2xl font-semibold text-[var(--ink-strong)]">Base de membros</h2>
                <p className="mt-2 text-sm text-[var(--ink-soft)]">Busque, revise roles e acione alteracoes sem sair do shell principal.</p>
              </div>

              <label className="shell-input flex min-w-[240px] items-center gap-3">
                <FiSearch size={16} className="text-[var(--ink-soft)]" />
                <input
                  aria-label="Buscar membros"
                  autoComplete="off"
                  className="w-full border-none bg-transparent outline-none placeholder:text-[var(--ink-soft)]"
                  placeholder="Buscar por nome, email, role ou ID"
                  type="search"
                  value={query}
                  onChange={(event) => setQuery(event.target.value)}
                />
              </label>
            </div>

            <div className="mt-5">
              <AsyncState
                state={listState}
                loadingLabel="Carregando membros..."
                errorTitle="Nao foi possivel carregar os membros."
                errorDescription={error ?? undefined}
                errorDetail="Verifique se o backend esta online, se o workspace foi carregado e se seu role possui acesso."
                onRetry={() => void reloadUsers()}
                emptyTitle="Nenhum membro encontrado"
                emptyDescription={
                  query
                    ? 'O filtro atual nao retornou resultados. Tente outro termo.'
                    : 'Convide o primeiro membro para operar neste workspace.'
                }
                emptyActionLabel={query || !canManageMembers ? undefined : 'Criar membro'}
                onEmptyAction={query || !canManageMembers ? undefined : openCreateForm}
              >
                <UserTable users={filteredUsers} onEdit={startEditing} onDelete={requestDelete} canManage={canManageMembers} />
              </AsyncState>
            </div>
          </div>

          <aside className="shell-surface p-5 sm:p-6">
            {showForm ? (
              <div>
                <p className="text-xs font-semibold uppercase tracking-[0.24em] text-[var(--ink-soft)]">
                  {editingUser ? 'Editar membro' : 'Novo membro'}
                </p>
                <h2 className="mt-3 text-2xl font-semibold text-[var(--ink-strong)]">
                  {editingUser ? 'Atualize a membership selecionada' : 'Convide um novo membro para este workspace'}
                </h2>
                <p className="mt-3 text-sm leading-6 text-[var(--ink-soft)]">
                  O form continua enxuto, mas agora carrega role junto com os dados basicos de acesso.
                </p>
                <div className="mt-6">
                  <UserForm
                    initialData={editingUser ? {
                      name: editingUser.name,
                      email: editingUser.email,
                      password: '',
                      roleCode: editingUser.roleCode ?? 'workspace_member',
                    } : undefined}
                    isEditing={!!editingUser}
                    onSubmit={editingUser ? handleUpdate : handleCreate}
                    onCancel={cancelForm}
                  />
                </div>
              </div>
            ) : (
              <div className="space-y-5">
                <div>
                  <p className="text-xs font-semibold uppercase tracking-[0.24em] text-[var(--ink-soft)]">Painel lateral</p>
                  <h2 className="mt-3 text-2xl font-semibold text-[var(--ink-strong)]">Sem ruido visual</h2>
                  <p className="mt-3 text-sm leading-6 text-[var(--ink-soft)]">
                    O formulario so entra em cena quando voce realmente precisa convidar ou ajustar uma membership. O resto da tela fica livre para diagnosticar estado, role e operacao.
                  </p>
                </div>

                <div className="rounded-[24px] bg-[var(--surface-muted)] p-5">
                  <p className="text-sm font-semibold text-[var(--ink-strong)]">Boas praticas rapidas</p>
                  <ul className="mt-3 space-y-3 text-sm leading-6 text-[var(--ink-soft)]">
                    <li>Use nomes claros para facilitar auditoria e busca.</li>
                    <li>Roles controlam a capacidade de gerenciar memberships no workspace atual.</li>
                    <li>Ao editar, senha vazia preserva a senha atual e nao e enviada no payload.</li>
                  </ul>
                </div>

                <button type="button" className="btn-primary w-full" onClick={openCreateForm} disabled={!canManageMembers}>
                  <FiPlus size={16} />
                  Abrir formulario
                </button>
              </div>
            )}
          </aside>
        </section>
      </div>

      <ConfirmDialog
        open={!!pendingDeletionUser}
        title={pendingDeletionUser ? `Desativar ${pendingDeletionUser.name}?` : 'Desativar membership?'}
        description="A membership sera marcada como inativa neste workspace, preservando historico operacional e auditoria."
        detail={
          pendingDeletionUser
            ? `${pendingDeletionUser.email} | ID ${pendingDeletionUser.id}`
            : undefined
        }
        confirmLabel={deletePending ? 'Desativando...' : 'Desativar membership'}
        tone="danger"
        busy={deletePending}
        onConfirm={() => void confirmDelete()}
        onCancel={cancelDelete}
      />
    </>
  );
}
