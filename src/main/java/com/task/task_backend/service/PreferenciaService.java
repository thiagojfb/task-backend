package com.task.task_backend.service;

import com.task.task_backend.dto.PreferenciasDTO;
import com.task.task_backend.entity.FiltroSituacao;
import com.task.task_backend.entity.PreferenciaUsuario;
import com.task.task_backend.mapper.PreferenciaMapper;
import com.task.task_backend.repository.PreferenciaUsuarioRepository;
import com.task.task_backend.seguranca.UsuarioAutenticado;
import java.time.Clock;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Preferências de interface persistidas por usuário — valem em qualquer navegador/dispositivo. */
@Slf4j
@Service
@RequiredArgsConstructor
public class PreferenciaService {

    private static final PreferenciasDTO PADRAO = new PreferenciasDTO(FiltroSituacao.TODAS);

    private final PreferenciaUsuarioRepository repositorio;
    private final PreferenciaMapper mapper;
    private final UsuarioAutenticado usuarioAutenticado;
    private final Clock relogio;

    @Transactional(readOnly = true)
    public PreferenciasDTO obter() {
        return repositorio.findById(usuarioAutenticado.obter().id())
                .map(mapper::paraResposta)
                .orElse(PADRAO);
    }

    /** Cria ou atualiza a preferência do usuário autenticado. */
    @Transactional
    public PreferenciasDTO salvar(PreferenciasDTO preferencias) {
        String usuarioId = usuarioAutenticado.obter().id();
        PreferenciaUsuario preferencia = repositorio.findById(usuarioId)
                .orElseGet(() -> PreferenciaUsuario.builder().usuarioId(usuarioId).build());
        preferencia.setFiltro(preferencias.filtro());
        preferencia.setAtualizadoEm(LocalDateTime.now(relogio));
        PreferenciaUsuario salva = repositorio.save(preferencia);
        log.info("Preferência de filtro do usuário {} atualizada para {}", usuarioId, salva.getFiltro());
        return mapper.paraResposta(salva);
    }
}
