package com.lume.domain;

import com.lume.domain.exception.BusinessRuleException;
import com.lume.domain.validation.EmailValidationStrategy;
import com.lume.domain.validation.NameValidationStrategy;
import com.lume.domain.validation.PasswordValidationStrategy;
import com.lume.domain.validation.ValidationStrategy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes unitários para as estratégias de validação (Strategy Pattern).
 */
@DisplayName("Estratégias de Validação (Strategy Pattern)")
class ValidationStrategyTest {

    @Nested
    @DisplayName("EmailValidationStrategy")
    class EmailValidationTests {

        private final ValidationStrategy<String> validator = new EmailValidationStrategy();

        @Test
        @DisplayName("Deve aceitar e-mail válido")
        void shouldAcceptValidEmail() {
            assertDoesNotThrow(() -> validator.validate("user@example.com"));
        }

        @Test
        @DisplayName("Deve aceitar e-mail com subdomínio")
        void shouldAcceptEmailWithSubdomain() {
            assertDoesNotThrow(() -> validator.validate("user@mail.example.com"));
        }

        @Test
        @DisplayName("Deve rejeitar e-mail nulo")
        void shouldRejectNullEmail() {
            assertThrows(BusinessRuleException.class, () -> validator.validate(null));
        }

        @Test
        @DisplayName("Deve rejeitar e-mail vazio")
        void shouldRejectEmptyEmail() {
            assertThrows(BusinessRuleException.class, () -> validator.validate(""));
        }

        @Test
        @DisplayName("Deve rejeitar e-mail sem @")
        void shouldRejectEmailWithoutAt() {
            assertThrows(BusinessRuleException.class, () -> validator.validate("userexample.com"));
        }

        @Test
        @DisplayName("Deve rejeitar e-mail sem domínio")
        void shouldRejectEmailWithoutDomain() {
            assertThrows(BusinessRuleException.class, () -> validator.validate("user@"));
        }
    }

    @Nested
    @DisplayName("NameValidationStrategy")
    class NameValidationTests {

        private final ValidationStrategy<String> validator = new NameValidationStrategy();

        @Test
        @DisplayName("Deve aceitar nome válido")
        void shouldAcceptValidName() {
            assertDoesNotThrow(() -> validator.validate("João Silva"));
        }

        @Test
        @DisplayName("Deve rejeitar nome nulo")
        void shouldRejectNullName() {
            assertThrows(BusinessRuleException.class, () -> validator.validate(null));
        }

        @Test
        @DisplayName("Deve rejeitar nome vazio")
        void shouldRejectEmptyName() {
            assertThrows(BusinessRuleException.class, () -> validator.validate(""));
        }

        @Test
        @DisplayName("Deve rejeitar nome com menos de 2 caracteres")
        void shouldRejectShortName() {
            assertThrows(BusinessRuleException.class, () -> validator.validate("A"));
        }

        @Test
        @DisplayName("Deve rejeitar nome com mais de 150 caracteres")
        void shouldRejectLongName() {
            String longName = "A".repeat(151);
            assertThrows(BusinessRuleException.class, () -> validator.validate(longName));
        }
    }

    @Nested
    @DisplayName("PasswordValidationStrategy")
    class PasswordValidationTests {

        private final ValidationStrategy<String> validator = new PasswordValidationStrategy();

        @Test
        @DisplayName("Deve aceitar senha válida")
        void shouldAcceptValidPassword() {
            assertDoesNotThrow(() -> validator.validate("senha123"));
        }

        @Test
        @DisplayName("Deve rejeitar senha nula")
        void shouldRejectNullPassword() {
            assertThrows(BusinessRuleException.class, () -> validator.validate(null));
        }

        @Test
        @DisplayName("Deve rejeitar senha com menos de 6 caracteres")
        void shouldRejectShortPassword() {
            assertThrows(BusinessRuleException.class, () -> validator.validate("12345"));
        }

        @Test
        @DisplayName("Deve rejeitar senha com mais de 100 caracteres")
        void shouldRejectLongPassword() {
            String longPassword = "A".repeat(101);
            assertThrows(BusinessRuleException.class, () -> validator.validate(longPassword));
        }
    }
}
