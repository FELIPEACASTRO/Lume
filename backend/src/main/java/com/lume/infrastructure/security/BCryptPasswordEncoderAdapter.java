package com.lume.infrastructure.security;

import com.lume.domain.service.PasswordEncoder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Adapter que implementa o PasswordEncoder do domínio usando BCrypt.
 *
 * <p><b>Design Pattern: Adapter</b> - Adapta o BCryptPasswordEncoder do Spring Security
 * para a interface definida na camada de domínio.</p>
 *
 * <p><b>Princípio DIP:</b> O domínio define a interface; esta classe na camada de
 * infraestrutura fornece a implementação concreta.</p>
 *
 * <p><b>Complexidade: O(1)</b> para encode e matches (custo fixo do algoritmo BCrypt).</p>
 */
public class BCryptPasswordEncoderAdapter implements PasswordEncoder {

    private final BCryptPasswordEncoder bcrypt;

    public BCryptPasswordEncoderAdapter() {
        this.bcrypt = new BCryptPasswordEncoder(12);
    }

    @Override
    public String encode(String rawPassword) {
        return bcrypt.encode(rawPassword);
    }

    @Override
    public boolean matches(String rawPassword, String encodedPassword) {
        return bcrypt.matches(rawPassword, encodedPassword);
    }
}
