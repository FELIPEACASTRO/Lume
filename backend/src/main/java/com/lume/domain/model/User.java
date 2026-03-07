package com.lume.domain.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Entidade de domínio que representa um Usuário no sistema Lume.
 *
 * <p>Esta classe pertence à camada de domínio (núcleo) e não possui
 * dependências de frameworks externos, garantindo isolamento e testabilidade.</p>
 *
 * <p><b>Princípios aplicados:</b></p>
 * <ul>
 *   <li>SRP: responsável apenas por representar o estado e regras do usuário</li>
 *   <li>Encapsulamento: estado interno protegido com validações nos setters</li>
 *   <li>Imutabilidade parcial: campos de identidade são imutáveis após criação</li>
 * </ul>
 */
public class User {

    private Long id;
    private String name;
    private String email;
    private String password;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private User() {
        // Construtor privado para forçar uso do Builder (Factory Pattern)
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public boolean isActive() {
        return active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    /**
     * Atualiza o nome do usuário com validação.
     *
     * @param name novo nome (não pode ser nulo ou vazio)
     * @throws IllegalArgumentException se o nome for inválido
     */
    public void updateName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("O nome não pode ser vazio");
        }
        this.name = name.trim();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Atualiza o e-mail do usuário com validação.
     *
     * @param email novo e-mail (não pode ser nulo ou vazio)
     * @throws IllegalArgumentException se o e-mail for inválido
     */
    public void updateEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("O e-mail não pode ser vazio");
        }
        this.email = email.trim().toLowerCase();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Atualiza a senha do usuário (já codificada).
     *
     * @param encodedPassword senha codificada
     */
    public void updatePassword(String encodedPassword) {
        if (encodedPassword == null || encodedPassword.isBlank()) {
            throw new IllegalArgumentException("A senha não pode ser vazia");
        }
        this.password = encodedPassword;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Desativa o usuário (soft delete).
     */
    public void deactivate() {
        this.active = false;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Reativa o usuário.
     */
    public void activate() {
        this.active = true;
        this.updatedAt = LocalDateTime.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(id, user.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "User{id=" + id + ", name='" + name + "', email='" + email + "', active=" + active + "}";
    }

    // ─── Builder (Factory Pattern) ──────────────────────────────────────

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder para construção de instâncias de User.
     * Implementa o padrão Factory/Builder para criação controlada de objetos.
     */
    public static class Builder {
        private final User user;

        private Builder() {
            this.user = new User();
            this.user.active = true;
            this.user.createdAt = LocalDateTime.now();
            this.user.updatedAt = LocalDateTime.now();
        }

        public Builder id(Long id) {
            this.user.id = id;
            return this;
        }

        public Builder name(String name) {
            this.user.name = name != null ? name.trim() : null;
            return this;
        }

        public Builder email(String email) {
            this.user.email = email != null ? email.trim().toLowerCase() : null;
            return this;
        }

        public Builder password(String password) {
            this.user.password = password;
            return this;
        }

        public Builder active(boolean active) {
            this.user.active = active;
            return this;
        }

        public Builder createdAt(LocalDateTime createdAt) {
            this.user.createdAt = createdAt;
            return this;
        }

        public Builder updatedAt(LocalDateTime updatedAt) {
            this.user.updatedAt = updatedAt;
            return this;
        }

        public User build() {
            Objects.requireNonNull(user.name, "O nome é obrigatório");
            Objects.requireNonNull(user.email, "O e-mail é obrigatório");
            Objects.requireNonNull(user.password, "A senha é obrigatória");
            return this.user;
        }
    }
}
