package com.task.task_backend.repository;

import com.task.task_backend.entity.PreferenciaUsuario;
import org.springframework.data.jpa.repository.JpaRepository;

/** Acesso às preferências por usuário (chave primária = identificador do usuário). */
public interface PreferenciaUsuarioRepository extends JpaRepository<PreferenciaUsuario, String> {
}
