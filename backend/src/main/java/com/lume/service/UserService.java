package com.lume.service;

import com.lume.dto.UserRequestDTO;
import com.lume.dto.UserResponseDTO;
import com.lume.exception.BusinessException;
import com.lume.exception.ResourceNotFoundException;
import com.lume.model.User;
import com.lume.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public Page<UserResponseDTO> findAll(Pageable pageable) {
        return userRepository.findAll(pageable).map(UserResponseDTO::fromEntity);
    }

    @Transactional(readOnly = true)
    public UserResponseDTO findById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário", id));
        return UserResponseDTO.fromEntity(user);
    }

    @Transactional
    public UserResponseDTO create(UserRequestDTO dto) {
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new BusinessException("Já existe um usuário cadastrado com este e-mail");
        }

        User user = User.builder()
                .name(dto.getName())
                .email(dto.getEmail())
                .password(dto.getPassword()) // TODO: Implementar hash com BCrypt
                .build();

        User saved = userRepository.save(user);
        return UserResponseDTO.fromEntity(saved);
    }

    @Transactional
    public UserResponseDTO update(Long id, UserRequestDTO dto) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário", id));

        userRepository.findByEmail(dto.getEmail())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new BusinessException("Já existe um usuário cadastrado com este e-mail");
                });

        user.setName(dto.getName());
        user.setEmail(dto.getEmail());

        if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
            user.setPassword(dto.getPassword()); // TODO: Implementar hash com BCrypt
        }

        User saved = userRepository.save(user);
        return UserResponseDTO.fromEntity(saved);
    }

    @Transactional
    public void delete(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário", id));
        user.setActive(false);
        userRepository.save(user);
    }
}
