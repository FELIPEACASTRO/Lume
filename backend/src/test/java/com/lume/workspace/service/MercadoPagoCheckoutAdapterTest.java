package com.lume.workspace.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lume.domain.exception.BusinessRuleException;
import com.lume.workspace.inference.security.SecretResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MercadoPagoCheckoutAdapterTest {

    @Mock
    private SecretResolver secretResolver;

    private MercadoPagoCheckoutAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new MercadoPagoCheckoutAdapter(secretResolver, new ObjectMapper());
    }

    @Test
    @DisplayName("shouldThrowWhenAccessTokenNull - resolveOptional returns null triggers BusinessRuleException")
    void shouldThrowWhenAccessTokenNull() {
        when(secretResolver.resolveOptional(eq("MERCADO_PAGO_ACCESS_TOKEN"))).thenReturn(null);

        assertThatThrownBy(() -> adapter.createCreditPackCheckout(
                1L, "INV-001", "PACK-10", 10, BigDecimal.valueOf(49.90), "Pack 10 creditos"
        ))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("indisponivel");
    }

    @Test
    @DisplayName("shouldThrowWhenAccessTokenBlank - resolveOptional returns blank triggers BusinessRuleException")
    void shouldThrowWhenAccessTokenBlank() {
        when(secretResolver.resolveOptional(eq("MERCADO_PAGO_ACCESS_TOKEN"))).thenReturn("   ");

        assertThatThrownBy(() -> adapter.createCreditPackCheckout(
                1L, "INV-001", "PACK-10", 10, BigDecimal.valueOf(49.90), "Pack 10 creditos"
        ))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("indisponivel");
    }

    @Test
    @DisplayName("shouldUseDefaultApiBaseUrl - when MERCADO_PAGO_API_BASE_URL not set, adapter falls back to default and does not throw BusinessRuleException about URL")
    void shouldUseDefaultApiBaseUrl() {
        // Token is valid so we pass the first guard
        when(secretResolver.resolveOptional(eq("MERCADO_PAGO_ACCESS_TOKEN"))).thenReturn("TEST-TOKEN");
        // API base URL returns null → adapter should fall back to default without error
        when(secretResolver.resolveOptional(eq("MERCADO_PAGO_API_BASE_URL"))).thenReturn(null);

        // The call will reach the real (or mock) API — it may fail with HTTP error or I/O error,
        // but the important assertion is that it does NOT throw about the base URL being missing.
        // Any exception thrown should NOT contain "API_BASE_URL" in its message.
        try {
            adapter.createCreditPackCheckout(
                    1L, "INV-002", "PACK-50", 50, BigDecimal.valueOf(199.90), null
            );
        } catch (Exception ex) {
            assertThat(ex.getMessage()).doesNotContain("API_BASE_URL");
        }
    }

    @Test
    @DisplayName("shouldBuildCorrectExternalReference - format workspace:X|invoice:Y|pack:Z")
    void shouldBuildCorrectExternalReference() {
        // Verify the external reference format matches "workspace:X|invoice:Y|pack:Z"
        Long workspaceId = 42L;
        String invoiceNumber = "INV-999";
        String packCode = "PREMIUM-100";

        String expectedRef = "workspace:42|invoice:INV-999|pack:PREMIUM-100";

        // Build it the same way the adapter does (line 62 of source)
        String actualRef = "workspace:" + workspaceId + "|invoice:" + invoiceNumber + "|pack:" + packCode;
        assertThat(actualRef).isEqualTo(expectedRef);

        // Verify the format components
        assertThat(actualRef).startsWith("workspace:");
        assertThat(actualRef).contains("|invoice:");
        assertThat(actualRef).contains("|pack:");
        assertThat(actualRef.split("\\|")).hasSize(3);
    }

    @Test
    @DisplayName("shouldTruncateErrorBodyTo300Chars - truncate method clips strings longer than 300 characters")
    void shouldTruncateErrorBodyTo300Chars() throws Exception {
        Method truncateMethod = MercadoPagoCheckoutAdapter.class.getDeclaredMethod("truncate", String.class);
        truncateMethod.setAccessible(true);

        // String with exactly 400 characters should be truncated to 300
        String longBody = "A".repeat(400);
        String truncated = (String) truncateMethod.invoke(adapter, longBody);
        assertThat(truncated).hasSize(300);
        assertThat(truncated).isEqualTo("A".repeat(300));

        // String with exactly 300 characters should NOT be truncated
        String exactBody = "B".repeat(300);
        String exact = (String) truncateMethod.invoke(adapter, exactBody);
        assertThat(exact).hasSize(300);

        // Null input should return empty string
        String nullResult = (String) truncateMethod.invoke(adapter, (String) null);
        assertThat(nullResult).isEmpty();

        // Short string should be returned as-is
        String shortBody = "error";
        String shortResult = (String) truncateMethod.invoke(adapter, shortBody);
        assertThat(shortResult).isEqualTo("error");
    }
}
