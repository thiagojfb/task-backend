package com.task.task_backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Preferências de interface de um usuário; uma linha por usuário (chave = claim sub). */
@Entity
@Table(name = "preferencia_usuario")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class PreferenciaUsuario {

    @Id
    @EqualsAndHashCode.Include
    @Column(name = "usuario_id", nullable = false, length = 255)
    private String usuarioId;

    @Enumerated(EnumType.STRING)
    @Column(name = "filtro", nullable = false, length = 10)
    private FiltroSituacao filtro;

    @Column(name = "atualizado_em", nullable = false)
    private LocalDateTime atualizadoEm;
}
