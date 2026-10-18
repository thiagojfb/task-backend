package com.task.task_backend.mapper;

import com.task.task_backend.dto.PreferenciasDTO;
import com.task.task_backend.entity.PreferenciaUsuario;
import org.mapstruct.Mapper;

/** Conversão de {@link PreferenciaUsuario} para o DTO exposto pela API. */
@Mapper(componentModel = "spring")
public interface PreferenciaMapper {

    PreferenciasDTO paraResposta(PreferenciaUsuario preferencia);
}
