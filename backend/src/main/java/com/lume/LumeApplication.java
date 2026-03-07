package com.lume;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Ponto de entrada da aplicação Lume.
 *
 * <p>A aplicação segue os princípios de Clean Architecture, organizando o código
 * em quatro camadas principais: Domain, Application, Infrastructure e Presentation.</p>
 *
 * <p>A regra de dependência flui de fora para dentro:
 * Presentation → Application → Domain ← Infrastructure</p>
 */
@SpringBootApplication
public class LumeApplication {

    public static void main(String[] args) {
        SpringApplication.run(LumeApplication.class, args);
    }
}
