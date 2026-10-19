package com.task.task_backend.repository;

import com.task.task_backend.entity.RegistroHistorico;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Acesso ao histórico de alterações de tarefas. */
public interface RegistroHistoricoRepository extends JpaRepository<RegistroHistorico, UUID> {

    List<RegistroHistorico> findByTarefaIdOrderByDataHoraDesc(UUID tarefaId);
}
