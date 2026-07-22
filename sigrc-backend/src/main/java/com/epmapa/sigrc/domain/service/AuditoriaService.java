package com.epmapa.sigrc.domain.service;

import com.epmapa.sigrc.domain.dto.AuditoriaDTO;
import com.epmapa.sigrc.domain.dto.PaginacionDTO;
import com.epmapa.sigrc.domain.entity.Auditoria;
import com.epmapa.sigrc.domain.entity.Usuario;
import com.epmapa.sigrc.domain.repository.AuditoriaRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AuditoriaService {

    private final AuditoriaRepository auditoriaRepository;
    private final ObjectMapper objectMapper;

    public AuditoriaService(AuditoriaRepository auditoriaRepository, ObjectMapper objectMapper) {
        this.auditoriaRepository = auditoriaRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrar(String username, Integer idUsuario, String accion, String tipoOperacion,
                           String tablaAfectada, Integer idRegistro, Object datosAnteriores,
                           Object datosNuevos, HttpServletRequest request, String resultado) {
        try {
            var auditoria = Auditoria.builder()
                .username(username)
                .usuario(idUsuario != null ? Usuario.builder().idUsuario(idUsuario).build() : null)
                .accion(accion)
                .tipoOperacion(tipoOperacion)
                .tablaAfectada(tablaAfectada)
                .idRegistro(idRegistro)
                .datosAnteriores(datosAnteriores != null ? objectMapper.writeValueAsString(datosAnteriores) : null)
                .datosNuevos(datosNuevos != null ? objectMapper.writeValueAsString(datosNuevos) : null)
                .direccionIp(obtenerIp(request))
                .userAgent(request != null ? request.getHeader("User-Agent") : null)
                .resultado(resultado)
                .creadoEn(LocalDateTime.now())
                .build();
            auditoriaRepository.save(auditoria);
        } catch (JsonProcessingException e) {
        }
    }

    @Transactional(readOnly = true)
    public PaginacionDTO<AuditoriaDTO> listar(int pagina, int tamanio, String username,
                                               String tabla, String tipoOperacion,
                                               LocalDateTime desde, LocalDateTime hasta) {
        var pageable = PageRequest.of(pagina, tamanio, Sort.by(Sort.Direction.DESC, "creadoEn"));
        var page = auditoriaRepository.findByCreadoEnBetween(desde, hasta, pageable);
        List<AuditoriaDTO> contenido = page.getContent().stream()
            .map(this::toDTO).collect(Collectors.toList());
        return new PaginacionDTO<>(contenido, page.getNumber(), page.getSize(),
            page.getTotalElements(), page.getTotalPages(),
            page.isFirst(), page.isLast());
    }

    private AuditoriaDTO toDTO(Auditoria a) {
        return new AuditoriaDTO(
            a.getIdAuditoria(), a.getUsername(), a.getAccion(),
            a.getTipoOperacion(), a.getTablaAfectada(), a.getIdRegistro(),
            a.getDatosAnteriores(), a.getDatosNuevos(),
            a.getDireccionIp(), a.getUserAgent(), a.getResultado(),
            a.getDetalle(), a.getCreadoEn()
        );
    }

    private String obtenerIp(HttpServletRequest request) {
        if (request == null) return "0.0.0.0";
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty()) ip = request.getRemoteAddr();
        return ip;
    }
}
