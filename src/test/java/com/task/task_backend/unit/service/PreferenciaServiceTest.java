package com.task.task_backend.unit.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.task.task_backend.dto.PreferenciasDTO;
import com.task.task_backend.entity.FiltroSituacao;
import com.task.task_backend.entity.PreferenciaUsuario;
import com.task.task_backend.mapper.PreferenciaMapper;
import com.task.task_backend.repository.PreferenciaUsuarioRepository;
import com.task.task_backend.seguranca.UsuarioAutenticado;
import com.task.task_backend.seguranca.UsuarioLogado;
import com.task.task_backend.service.PreferenciaService;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("PreferenciaService")
class PreferenciaServiceTest {

    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");
    private static final LocalDateTime AGORA = LocalDateTime.of(2026, 9, 20, 9, 0);

    @Mock
    private PreferenciaUsuarioRepository repositorio;

    @Mock
    private UsuarioAutenticado usuarioAutenticado;

    private PreferenciaService servico;

    @BeforeEach
    void configurar() {
        Clock relogio = Clock.fixed(AGORA.atZone(FUSO).toInstant(), FUSO);
        servico = new PreferenciaService(repositorio, Mappers.getMapper(PreferenciaMapper.class), usuarioAutenticado,
                relogio);
        when(usuarioAutenticado.obter()).thenReturn(new UsuarioLogado("ana-id", "Ana Silva"));
    }

    @Test
    @DisplayName("Deve devolver TODAS quando o usuário ainda não salvou preferência")
    void deveDevolverPadraoSemPreferencia() {
        when(repositorio.findById("ana-id")).thenReturn(Optional.empty());

        assertThat(servico.obter()).isEqualTo(new PreferenciasDTO(FiltroSituacao.TODAS));
        verify(repositorio, never()).save(any());
    }

    @Test
    @DisplayName("Deve devolver a preferência salva")
    void deveDevolverPreferenciaSalva() {
        when(repositorio.findById("ana-id")).thenReturn(Optional.of(
                PreferenciaUsuario.builder().usuarioId("ana-id").filtro(FiltroSituacao.ABERTAS).atualizadoEm(AGORA).build()));

        assertThat(servico.obter().filtro()).isEqualTo(FiltroSituacao.ABERTAS);
    }

    @Test
    @DisplayName("Deve criar a preferência quando não existir")
    void deveCriarPreferencia() {
        when(repositorio.findById("ana-id")).thenReturn(Optional.empty());
        when(repositorio.save(any(PreferenciaUsuario.class))).thenAnswer(chamada -> chamada.getArgument(0));

        PreferenciasDTO resposta = servico.salvar(new PreferenciasDTO(FiltroSituacao.CONCLUIDAS));

        ArgumentCaptor<PreferenciaUsuario> captor = ArgumentCaptor.forClass(PreferenciaUsuario.class);
        verify(repositorio).save(captor.capture());
        assertThat(captor.getValue().getUsuarioId()).isEqualTo("ana-id");
        assertThat(captor.getValue().getFiltro()).isEqualTo(FiltroSituacao.CONCLUIDAS);
        assertThat(captor.getValue().getAtualizadoEm()).isEqualTo(AGORA);
        assertThat(resposta.filtro()).isEqualTo(FiltroSituacao.CONCLUIDAS);
    }

    @Test
    @DisplayName("Deve atualizar a preferência existente")
    void deveAtualizarPreferenciaExistente() {
        PreferenciaUsuario existente = PreferenciaUsuario.builder()
                .usuarioId("ana-id").filtro(FiltroSituacao.ABERTAS).atualizadoEm(AGORA.minusDays(1)).build();
        when(repositorio.findById("ana-id")).thenReturn(Optional.of(existente));
        when(repositorio.save(any(PreferenciaUsuario.class))).thenAnswer(chamada -> chamada.getArgument(0));

        PreferenciasDTO resposta = servico.salvar(new PreferenciasDTO(FiltroSituacao.TODAS));

        assertThat(existente.getFiltro()).isEqualTo(FiltroSituacao.TODAS);
        assertThat(existente.getAtualizadoEm()).isEqualTo(AGORA);
        assertThat(resposta.filtro()).isEqualTo(FiltroSituacao.TODAS);
    }
}
