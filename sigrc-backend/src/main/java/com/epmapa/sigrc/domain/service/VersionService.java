package com.epmapa.sigrc.domain.service;

import com.epmapa.sigrc.domain.dto.VersionDTO;
import com.epmapa.sigrc.domain.entity.Sistema;
import com.epmapa.sigrc.domain.entity.Version;
import com.epmapa.sigrc.domain.repository.*;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class VersionService {

    private final VersionRepository versionRepository;
    private final SistemaRepository sistemaRepository;
    private final CambioRepository cambioRepository;
    private final UsuarioRepository usuarioRepository;

    public VersionService(VersionRepository versionRepository, SistemaRepository sistemaRepository,
                          CambioRepository cambioRepository, UsuarioRepository usuarioRepository) {
        this.versionRepository = versionRepository;
        this.sistemaRepository = sistemaRepository;
        this.cambioRepository = cambioRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional(readOnly = true)
    public List<VersionDTO> listar() {
        return versionRepository.findAll().stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public VersionDTO obtenerPorId(Integer id) {
        return toDTO(versionRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Versión no encontrada: " + id)));
    }

    @Transactional(readOnly = true)
    public List<VersionDTO> listarPorSistema(Integer idSistema) {
        return versionRepository.findBySistemaIdSistemaOrderByCreadoEnDesc(idSistema)
            .stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional
    public VersionDTO crear(VersionDTO dto) {
        var sistema = sistemaRepository.findById(dto.idSistema())
            .orElseThrow(() -> new EntityNotFoundException("Sistema no encontrado: " + dto.idSistema()));
        var responsable = usuarioRepository.findById(dto.idResponsable())
            .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado: " + dto.idResponsable()));
        var version = Version.builder()
            .sistema(sistema)
            .version(dto.version())
            .tipo(dto.tipo())
            .descripcion(dto.descripcion())
            .notasLiberacion(dto.notasLiberacion())
            .estado(dto.estado() != null ? dto.estado() : "PENDIENTE")
            .ambiente(dto.ambiente() != null ? dto.ambiente() : "PRODUCCION")
            .fechaDespliegue(dto.fechaDespliegue())
            .responsable(responsable)
            .activo(true)
            .build();
        if (dto.idCambio() != null)
            version.setCambio(cambioRepository.findById(dto.idCambio()).orElse(null));
        return toDTO(versionRepository.save(version));
    }

    @Transactional
    public VersionDTO actualizar(Integer id, VersionDTO dto) {
        var version = versionRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Versión no encontrada: " + id));
        if (dto.tipo() != null) version.setTipo(dto.tipo());
        if (dto.descripcion() != null) version.setDescripcion(dto.descripcion());
        if (dto.notasLiberacion() != null) version.setNotasLiberacion(dto.notasLiberacion());
        if (dto.estado() != null) version.setEstado(dto.estado());
        if (dto.ambiente() != null) version.setAmbiente(dto.ambiente());
        if (dto.fechaDespliegue() != null) version.setFechaDespliegue(dto.fechaDespliegue());
        if (dto.idSistema() != null)
            version.setSistema(sistemaRepository.getReferenceById(dto.idSistema()));
        if (dto.idResponsable() != null)
            version.setResponsable(usuarioRepository.getReferenceById(dto.idResponsable()));
        if (dto.idCambio() != null)
            version.setCambio(cambioRepository.findById(dto.idCambio()).orElse(null));
        return toDTO(versionRepository.save(version));
    }

    @Transactional
    public void eliminar(Integer id) {
        var version = versionRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Versión no encontrada: " + id));
        version.setActivo(false);
        versionRepository.save(version);
    }

    private VersionDTO toDTO(Version v) {
        return new VersionDTO(
            v.getIdVersion(), v.getVersion(), v.getTipo(), v.getDescripcion(),
            v.getNotasLiberacion(), v.getEstado(), v.getAmbiente(),
            v.getFechaDespliegue(), v.getCreadoEn(), v.getActivo(),
            v.getSistema().getIdSistema(), v.getSistema().getNombre(),
            v.getCambio() != null ? v.getCambio().getIdCambio() : null,
            v.getCambio() != null ? v.getCambio().getCodigoCambio() : null,
            v.getResponsable().getIdUsuario(),
            v.getResponsable().getNombres() + " " + v.getResponsable().getApellidos()
        );
    }
}
