package com.lume.domain.service;

/**
 * Interface de serviço de domínio para codificação de senhas.
 *
 * <p><b>Princípio aplicado - DIP (Dependency Inversion Principle):</b>
 * A camada de domínio define a interface; a camada de infraestrutura
 * fornece a implementação concreta (ex: BCrypt).</p>
 *
 * <p><b>Complexidade:</b> O(1) para encode e matches (operações de hash).</p>
 */
public interface PasswordEncoder {

    /**
     * Codifica uma senha em texto plano.
     *
     * @param rawPassword senha em texto plano
     * @return senha codificada
     */
    String encode(String rawPassword);

    /**
     * Verifica se uma senha em texto plano corresponde à senha codificada.
     *
     * @param rawPassword     senha em texto plano
     * @param encodedPassword senha codificada
     * @return true se corresponder
     */
    boolean matches(String rawPassword, String encodedPassword);
}
