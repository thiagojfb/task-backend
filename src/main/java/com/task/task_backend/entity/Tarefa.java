package com.task.task_backend.entity;

import com.task.task_backend.util.NormalizadorTexto;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Tarefa da lista pessoal de um usuário. Posições são contínuas (1..N) por usuário. */
@Entity
@Table(name = "tarefa")
@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Tarefa {

    @Id
    @EqualsAndHashCode.Include
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "usuario_id", nullable = false, length = 255)
    private String usuarioId;

    @Column(name = "descricao", nullable = false, length = 500)
    private String descricao;

    @Column(name = "descricao_normalizada", nullable = false, length = 500)
    private String descricaoNormalizada;

    @Enumerated(EnumType.STRING)
    @Column(name = "prioridade", nullable = false, length = 10)
    private Prioridade prioridade;

    @Enumerated(EnumType.STRING)
    @Column(name = "situacao", nullable = false, length = 10)
    private Situacao situacao;

    @Column(name = "data_abertura", nullable = false)
    private LocalDateTime dataAbertura;

    @Column(name = "data_prevista_conclusao")
    private LocalDate dataPrevistaConclusao;

    @Column(name = "data_conclusao_real")
    private LocalDateTime dataConclusaoReal;

    @Column(name = "valor", nullable = false, precision = 15, scale = 2)
    private BigDecimal valor;

    @Column(name = "posicao", nullable = false)
    private Integer posicao;

    @PrePersist
    public void aoPersistir() {
        if (situacao == null) {
            situacao = Situacao.ABERTA;
        }
        if (valor == null) {
            valor = BigDecimal.ZERO;
        }
        valor = valor.setScale(2, RoundingMode.HALF_UP);
        atualizarDescricaoNormalizada();
    }

    @PreUpdate
    public void aoAtualizar() {
        atualizarDescricaoNormalizada();
    }

    private void atualizarDescricaoNormalizada() {
        descricaoNormalizada = NormalizadorTexto.normalizar(descricao);
    }
}
