package com.lume.application.port.output;

import com.lume.domain.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

/**
 * Port de saída para persistência de usuários.
 *
 * <p><b>Clean Architecture:</b> Define o contrato que a camada de infraestrutura
 * deve implementar. A camada de aplicação depende desta abstração, não da implementação.</p>
 *
 * <p><b>Princípio DIP (Dependency Inversion):</b> Módulos de alto nível (Application)
 * não dependem de módulos de baixo nível (Infrastructure); ambos dependem de abstrações.</p>
 */
public interface UserRepositoryPort {

    /**
     * Salva ou atualiza um usuário.
     * <p><b>Complexidade: O(log n)</b> para inserção/atualização com índice B-tree.</p>
     */
    User save(User user);

    /**
     * Busca um usuário por ID.
     * <p><b>Complexidade: O(log n)</b> - busca por chave primária (B-tree index).</p>
     */
    Optional<User> findById(Long id);

    /**
     * Busca um usuário por e-mail.
     * <p><b>Complexidade: O(log n)</b> - busca por índice no campo email.</p>
     */
    Optional<User> findByEmail(String email);

    /**
     * Verifica se existe um usuário com o e-mail informado.
     * <p><b>Complexidade: O(log n)</b> - verificação via índice.</p>
     */
    boolean existsByEmail(String email);

    /**
     * Lista todos os usuários de forma paginada.
     * <p><b>Complexidade: O(n/p + log n)</b> onde p é o tamanho da página.</p>
     */
    Page<User> findAll(Pageable pageable);
}
