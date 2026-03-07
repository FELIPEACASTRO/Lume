package com.lume.infrastructure.persistence.repository;

import com.lume.application.port.output.UserRepositoryPort;
import com.lume.domain.model.User;
import com.lume.infrastructure.persistence.mapper.UserPersistenceMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Adapter que implementa o port de repositório, conectando o domínio ao JPA.
 *
 * <p><b>Design Pattern: Adapter</b> - Adapta a interface do Spring Data JPA
 * para a interface definida na camada de aplicação (UserRepositoryPort).</p>
 *
 * <p><b>Clean Architecture:</b> Esta classe é o ponto de conexão entre a camada
 * de aplicação e a infraestrutura de persistência. A camada de aplicação depende
 * apenas da abstração (UserRepositoryPort), não desta implementação.</p>
 *
 * <p><b>Princípio DIP:</b> A inversão de dependência é alcançada: módulos de alto
 * nível (Application) dependem da abstração (Port), e este módulo de baixo nível
 * (Infrastructure) implementa a abstração.</p>
 */
@Component
public class UserRepositoryAdapter implements UserRepositoryPort {

    private final JpaUserRepository jpaRepository;

    public UserRepositoryAdapter(JpaUserRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    /**
     * {@inheritDoc}
     * <p><b>Complexidade: O(log n)</b> - inserção/atualização com índice B-tree.</p>
     */
    @Override
    public User save(User user) {
        var entity = UserPersistenceMapper.toJpaEntity(user);
        var saved = jpaRepository.save(entity);
        return UserPersistenceMapper.toDomain(saved);
    }

    /**
     * {@inheritDoc}
     * <p><b>Complexidade: O(log n)</b> - busca por chave primária.</p>
     */
    @Override
    public Optional<User> findById(Long id) {
        return jpaRepository.findById(id)
                .map(UserPersistenceMapper::toDomain);
    }

    /**
     * {@inheritDoc}
     * <p><b>Complexidade: O(log n)</b> - busca por índice no campo email.</p>
     */
    @Override
    public Optional<User> findByEmail(String email) {
        return jpaRepository.findByEmail(email)
                .map(UserPersistenceMapper::toDomain);
    }

    /**
     * {@inheritDoc}
     * <p><b>Complexidade: O(log n)</b> - verificação via índice.</p>
     */
    @Override
    public boolean existsByEmail(String email) {
        return jpaRepository.existsByEmail(email);
    }

    /**
     * {@inheritDoc}
     * <p><b>Complexidade: O(p + log n)</b> onde p é o tamanho da página.</p>
     */
    @Override
    public Page<User> findAll(Pageable pageable) {
        return jpaRepository.findAll(pageable)
                .map(UserPersistenceMapper::toDomain);
    }
}
