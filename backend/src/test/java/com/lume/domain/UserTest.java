package com.lume.domain;

import com.lume.domain.model.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes unitários para a entidade de domínio User.
 * Valida regras de negócio, construção via Builder e comportamento dos métodos.
 */
@DisplayName("User - Entidade de Domínio")
class UserTest {

    @Nested
    @DisplayName("Builder (Factory Pattern)")
    class BuilderTests {

        @Test
        @DisplayName("Deve criar um usuário válido com todos os campos")
        void shouldCreateValidUser() {
            User user = User.builder()
                    .name("João Silva")
                    .email("joao@email.com")
                    .password("encoded123")
                    .build();

            assertNotNull(user);
            assertEquals("João Silva", user.getName());
            assertEquals("joao@email.com", user.getEmail());
            assertEquals("encoded123", user.getPassword());
            assertTrue(user.isActive());
            assertNotNull(user.getCreatedAt());
            assertNotNull(user.getUpdatedAt());
        }

        @Test
        @DisplayName("Deve normalizar e-mail para minúsculas")
        void shouldNormalizeEmail() {
            User user = User.builder()
                    .name("Test")
                    .email("JOAO@EMAIL.COM")
                    .password("pass123")
                    .build();

            assertEquals("joao@email.com", user.getEmail());
        }

        @Test
        @DisplayName("Deve lançar exceção quando nome é nulo")
        void shouldThrowWhenNameIsNull() {
            assertThrows(NullPointerException.class, () ->
                    User.builder()
                            .email("test@email.com")
                            .password("pass123")
                            .build()
            );
        }

        @Test
        @DisplayName("Deve lançar exceção quando e-mail é nulo")
        void shouldThrowWhenEmailIsNull() {
            assertThrows(NullPointerException.class, () ->
                    User.builder()
                            .name("Test")
                            .password("pass123")
                            .build()
            );
        }

        @Test
        @DisplayName("Deve lançar exceção quando senha é nula")
        void shouldThrowWhenPasswordIsNull() {
            assertThrows(NullPointerException.class, () ->
                    User.builder()
                            .name("Test")
                            .email("test@email.com")
                            .build()
            );
        }
    }

    @Nested
    @DisplayName("Métodos de atualização")
    class UpdateTests {

        private User createValidUser() {
            return User.builder()
                    .id(1L)
                    .name("João Silva")
                    .email("joao@email.com")
                    .password("encoded123")
                    .build();
        }

        @Test
        @DisplayName("Deve atualizar o nome com sucesso")
        void shouldUpdateName() {
            User user = createValidUser();
            user.updateName("Maria Santos");
            assertEquals("Maria Santos", user.getName());
        }

        @Test
        @DisplayName("Deve lançar exceção ao atualizar nome com valor vazio")
        void shouldThrowWhenUpdatingWithEmptyName() {
            User user = createValidUser();
            assertThrows(IllegalArgumentException.class, () -> user.updateName(""));
        }

        @Test
        @DisplayName("Deve lançar exceção ao atualizar nome com valor nulo")
        void shouldThrowWhenUpdatingWithNullName() {
            User user = createValidUser();
            assertThrows(IllegalArgumentException.class, () -> user.updateName(null));
        }

        @Test
        @DisplayName("Deve atualizar o e-mail com sucesso")
        void shouldUpdateEmail() {
            User user = createValidUser();
            user.updateEmail("novo@email.com");
            assertEquals("novo@email.com", user.getEmail());
        }

        @Test
        @DisplayName("Deve normalizar e-mail na atualização")
        void shouldNormalizeEmailOnUpdate() {
            User user = createValidUser();
            user.updateEmail("NOVO@EMAIL.COM");
            assertEquals("novo@email.com", user.getEmail());
        }

        @Test
        @DisplayName("Deve lançar exceção ao atualizar e-mail com valor vazio")
        void shouldThrowWhenUpdatingWithEmptyEmail() {
            User user = createValidUser();
            assertThrows(IllegalArgumentException.class, () -> user.updateEmail(""));
        }

        @Test
        @DisplayName("Deve atualizar a senha com sucesso")
        void shouldUpdatePassword() {
            User user = createValidUser();
            user.updatePassword("newEncoded456");
            assertEquals("newEncoded456", user.getPassword());
        }

        @Test
        @DisplayName("Deve lançar exceção ao atualizar senha com valor vazio")
        void shouldThrowWhenUpdatingWithEmptyPassword() {
            User user = createValidUser();
            assertThrows(IllegalArgumentException.class, () -> user.updatePassword(""));
        }
    }

    @Nested
    @DisplayName("Ativação e desativação")
    class ActivationTests {

        @Test
        @DisplayName("Deve desativar o usuário")
        void shouldDeactivateUser() {
            User user = User.builder()
                    .name("Test").email("test@email.com").password("pass123").build();
            assertTrue(user.isActive());

            user.deactivate();
            assertFalse(user.isActive());
        }

        @Test
        @DisplayName("Deve reativar o usuário")
        void shouldActivateUser() {
            User user = User.builder()
                    .name("Test").email("test@email.com").password("pass123").active(false).build();
            assertFalse(user.isActive());

            user.activate();
            assertTrue(user.isActive());
        }
    }

    @Nested
    @DisplayName("Equals e HashCode")
    class EqualsHashCodeTests {

        @Test
        @DisplayName("Deve ser igual quando IDs são iguais")
        void shouldBeEqualWhenSameId() {
            User user1 = User.builder().id(1L).name("A").email("a@a.com").password("p").build();
            User user2 = User.builder().id(1L).name("B").email("b@b.com").password("p").build();
            assertEquals(user1, user2);
        }

        @Test
        @DisplayName("Deve ser diferente quando IDs são diferentes")
        void shouldNotBeEqualWhenDifferentId() {
            User user1 = User.builder().id(1L).name("A").email("a@a.com").password("p").build();
            User user2 = User.builder().id(2L).name("A").email("a@a.com").password("p").build();
            assertNotEquals(user1, user2);
        }
    }
}
