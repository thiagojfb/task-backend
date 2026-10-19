package com.task.task_backend.repository;

import com.task.task_backend.entity.Situacao;
import com.task.task_backend.entity.Tarefa;
import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Acesso às tarefas; toda consulta é restrita ao usuário proprietário. */
public interface TarefaRepository extends JpaRepository<Tarefa, UUID> {

    List<Tarefa> findByUsuarioIdOrderByPosicaoAsc(String usuarioId);

    List<Tarefa> findByUsuarioIdAndSituacaoOrderByPosicaoAsc(String usuarioId, Situacao situacao);

    Optional<Tarefa> findByIdAndUsuarioId(UUID id, String usuarioId);

    int countByUsuarioId(String usuarioId);

    /** Busca por trecho da descrição normalizada (o termo já deve estar normalizado). */
    @Query("SELECT t FROM Tarefa t WHERE t.usuarioId = :usuarioId "
            + "AND t.descricaoNormalizada LIKE CONCAT('%', :termo, '%') ORDER BY t.posicao ASC")
    List<Tarefa> buscar(@Param("usuarioId") String usuarioId, @Param("termo") String termo);

    /** Busca por trecho da descrição normalizada restrita a uma situação. */
    @Query("SELECT t FROM Tarefa t WHERE t.usuarioId = :usuarioId AND t.situacao = :situacao "
            + "AND t.descricaoNormalizada LIKE CONCAT('%', :termo, '%') ORDER BY t.posicao ASC")
    List<Tarefa> buscarPorSituacao(@Param("usuarioId") String usuarioId, @Param("situacao") Situacao situacao,
            @Param("termo") String termo);

    /** Soma dos valores das tarefas abertas cujo prazo está no intervalo (inclusive). */
    @Query("SELECT COALESCE(SUM(t.valor), 0) FROM Tarefa t WHERE t.usuarioId = :usuarioId "
            + "AND t.situacao = com.task.task_backend.entity.Situacao.ABERTA "
            + "AND t.dataPrevistaConclusao BETWEEN :inicio AND :fim")
    BigDecimal somarValorAbertasNoPeriodo(@Param("usuarioId") String usuarioId, @Param("inicio") LocalDate inicio,
            @Param("fim") LocalDate fim);

    /** Carrega a tarefa com bloqueio pessimista para serializar reordenações do mesmo usuário. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM Tarefa t WHERE t.id = :id AND t.usuarioId = :usuarioId")
    Optional<Tarefa> buscarParaMover(@Param("id") UUID id, @Param("usuarioId") String usuarioId);

    /** Soma 1 às posições no intervalo [inicio, fim] do usuário (abre espaço acima). */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Tarefa t SET t.posicao = t.posicao + 1 "
            + "WHERE t.usuarioId = :usuarioId AND t.posicao BETWEEN :inicio AND :fim")
    int incrementarPosicoes(@Param("usuarioId") String usuarioId, @Param("inicio") int inicio, @Param("fim") int fim);

    /** Subtrai 1 das posições no intervalo [inicio, fim] do usuário (fecha espaço abaixo). */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Tarefa t SET t.posicao = t.posicao - 1 "
            + "WHERE t.usuarioId = :usuarioId AND t.posicao BETWEEN :inicio AND :fim")
    int decrementarPosicoes(@Param("usuarioId") String usuarioId, @Param("inicio") int inicio, @Param("fim") int fim);

    /** Subtrai 1 das posições posteriores à informada (fecha a lacuna deixada por uma exclusão). */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Tarefa t SET t.posicao = t.posicao - 1 WHERE t.usuarioId = :usuarioId AND t.posicao > :posicao")
    int decrementarPosicoesApos(@Param("usuarioId") String usuarioId, @Param("posicao") int posicao);
}
