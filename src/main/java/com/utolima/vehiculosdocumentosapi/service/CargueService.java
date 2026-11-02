package com.utolima.vehiculosdocumentosapi.service;

import org.springframework.web.multipart.MultipartFile;

import com.utolima.vehiculosdocumentosapi.dto.CargueResultadoDTO;

public interface CargueService {

    CargueResultadoDTO procesarArchivo(MultipartFile archivo);
}
  