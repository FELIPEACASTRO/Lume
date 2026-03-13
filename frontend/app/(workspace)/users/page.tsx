"use client";

import { FormEvent, useEffect, useMemo, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { EmptyState } from "@/components/ui/EmptyState";
import { PageHeader } from "@/components/ui/PageHeader";
import { Panel } from "@/components/ui/Panel";
import { StatusBadge } from "@/components/ui/StatusBadge";
import { createMember, getSettingsUiOptions, listMembers, updateMember } from "@/lib/api/workspace";

export default function UsersPage() {
  const queryClient = useQueryClient();
  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [roleCode, setRoleCode] = useState("");
  const [feedback, setFeedback] = useState<string | null>(null);

  const membersQuery = useQuery({
    queryKey: ["members"],
    queryFn: listMembers
  });

  const uiOptionsQuery = useQuery({
    queryKey: ["settings-ui-options"],
    queryFn: getSettingsUiOptions
  });

  const roleOptions = useMemo(
    () => uiOptionsQuery.data?.memberRoles ?? [],
    [uiOptionsQuery.data]
  );

  useEffect(() => {
    if (roleCode || roleOptions.length === 0) {
      return;
    }
    const defaultRole = roleOptions.find((item) => item.defaultOption) ?? roleOptions[0];
    setRoleCode(defaultRole.code);
  }, [roleCode, roleOptions]);

  const createMemberMutation = useMutation({
    mutationFn: createMember,
    onSuccess: () => {
      setName("");
      setEmail("");
      setPassword("");
      const defaultRole = roleOptions.find((item) => item.defaultOption) ?? roleOptions[0];
      setRoleCode(defaultRole?.code ?? "");
      setFeedback("Membro adicionado com sucesso.");
      queryClient.invalidateQueries({ queryKey: ["members"] });
      queryClient.invalidateQueries({ queryKey: ["home-overview"] });
    },
    onError: (error) => {
      setFeedback(error instanceof Error ? error.message : "Falha ao adicionar membro.");
    }
  });

  const updateMemberMutation = useMutation({
    mutationFn: ({ memberId, payload }: { memberId: number; payload: { roleCode?: string; active?: boolean } }) =>
      updateMember(memberId, payload),
    onSuccess: () => {
      setFeedback("Equipe atualizada com sucesso.");
      queryClient.invalidateQueries({ queryKey: ["members"] });
      queryClient.invalidateQueries({ queryKey: ["home-overview"] });
    },
    onError: (error) => {
      setFeedback(error instanceof Error ? error.message : "Falha ao atualizar membro.");
    }
  });

  function submitMember(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!name.trim() || !email.trim() || !password.trim() || !roleCode.trim()) {
      setFeedback("Preencha nome, email e senha para convidar o membro.");
      return;
    }
    createMemberMutation.mutate({
      name: name.trim(),
      email: email.trim(),
      password: password.trim(),
      roleCode
    });
  }

  return (
    <>
      <PageHeader
        title="Equipe"
        description="Gerencie pessoas, papeis e acesso do workspace com trilha operacional real."
      />

      <Panel title="Adicionar membro" subtitle="Convide um novo operador para o workspace.">
        <form className="form-grid" onSubmit={submitMember}>
          <label>
            Nome
            <input value={name} onChange={(event) => setName(event.target.value)} placeholder="Nome do membro" />
          </label>
          <label>
            E-mail
            <input value={email} onChange={(event) => setEmail(event.target.value)} placeholder="pessoa@empresa.com" />
          </label>
          <label>
            Senha inicial
            <input
              type="password"
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              placeholder="Minimo de 6 caracteres"
            />
          </label>
          <label>
            Papel
            <select value={roleCode} onChange={(event) => setRoleCode(event.target.value)}>
              <option value="">Selecione</option>
              {roleOptions.map((role) => (
                <option key={role.code} value={role.code}>
                  {role.label}
                </option>
              ))}
            </select>
          </label>
          <div className="field-inline" style={{ gridColumn: "1 / -1" }}>
            <button
              className="button-primary"
              type="submit"
              disabled={createMemberMutation.isPending || roleOptions.length === 0 || !roleCode}
            >
              {createMemberMutation.isPending ? "Adicionando..." : "Adicionar membro"}
            </button>
          </div>
        </form>
        {feedback ? <p className="message-info">{feedback}</p> : null}
      </Panel>

      <Panel title="Membros ativos" subtitle="Permissoes e status atuais do workspace.">
        {membersQuery.isLoading ? (
          <p className="muted">Carregando equipe...</p>
        ) : membersQuery.data && membersQuery.data.length > 0 ? (
          <div className="item-list">
            {membersQuery.data.map((member) => (
              <div className="item-row" key={member.id}>
                <strong>{member.name}</strong>
                <p>{member.email}</p>
                <p className="muted">
                  {member.roleLabel} · criado em {member.createdAt}
                </p>
                <div className="field-inline" style={{ marginTop: "0.45rem", flexWrap: "wrap" }}>
                  <StatusBadge
                    label={member.active ? "Ativo" : "Inativo"}
                    variant={member.active ? "active" : "restricted"}
                  />
                  {member.currentUser ? <StatusBadge label="Voce" variant="attention" /> : null}
                  <button
                    type="button"
                    className="button-secondary"
                    onClick={() =>
                      updateMemberMutation.mutate({
                        memberId: member.id,
                        payload: {
                          roleCode: member.roleCode === "workspace_admin" ? "workspace_member" : "workspace_admin"
                        }
                      })
                    }
                    disabled={updateMemberMutation.isPending}
                  >
                    {member.roleCode === "workspace_admin" ? "Tornar membro" : "Tornar admin"}
                  </button>
                  <button
                    type="button"
                    className="button-secondary"
                    onClick={() =>
                      updateMemberMutation.mutate({
                        memberId: member.id,
                        payload: { active: !member.active }
                      })
                    }
                    disabled={updateMemberMutation.isPending}
                  >
                    {member.active ? "Desativar" : "Reativar"}
                  </button>
                </div>
              </div>
            ))}
          </div>
        ) : (
          <EmptyState
            title="Sem membros"
            description="Adicione o primeiro membro para operar o workspace em equipe."
          />
        )}
      </Panel>
    </>
  );
}
