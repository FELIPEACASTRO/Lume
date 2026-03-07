package com.lume.domain.validation;

/**
 * Interface do padrão Strategy para validações de domínio.
 *
 * <p><b>Design Pattern: Strategy</b> - Permite definir uma família de algoritmos
 * de validação, encapsulá-los e torná-los intercambiáveis.</p>
 *
 * <p><b>Princípios SOLID aplicados:</b></p>
 * <ul>
 *   <li>SRP: cada implementação valida uma única regra</li>
 *   <li>OCP: novas validações são adicionadas sem modificar código existente</li>
 *   <li>ISP: interface enxuta com método único</li>
 *   <li>DIP: dependência na abstração, não na implementação</li>
 * </ul>
 *
 * @param <T> tipo do objeto a ser validado
 */
public interface ValidationStrategy<T> {

    /**
     * Valida o objeto fornecido.
     *
     * @param target objeto a ser validado
     * @throws com.lume.domain.exception.BusinessRuleException se a validação falhar
     */
    void validate(T target);
}
