package com.epmapa.sigrc.domain.service;

import com.epmapa.sigrc.domain.dto.PermisoDTO;
import com.epmapa.sigrc.domain.entity.Permiso;
import com.epmapa.sigrc.domain.repository.PermisoRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class PermisoService {

    private final PermisoRepository permisoRepository;

    public PermisoService(PermisoRepository permisoRepository) {
        this.permisoRepository = permisoRepository;
    }

    public List<PermisoDTO> listar() {
        return permisoRepository.findAll().stream().map(this::toDTO).collect(Collectors.toList());
    }

    public PermisoDTO obtenerPorId(Integer id) {
        return toDTO(permisoRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Permiso no encontrado: " + id)));
    }

    @Transactional
    public PermisoDTO crear(PermisoDTO dto) {
        var permiso = Permiso.builder()
            .codigo(dto.codigo())
            .nombre(dto.nombre())
            .modulo(dto.modulo())
            .tipoAcceso(dto.tipoAcceso())
            .descripcion(dto.descripcion())
            .activo(dto.activo() != null ? dto.activo() : true)
            .build();
        return toDTO(permisoRepository.save(permiso));
    }

    @Transactional
    public PermisoDTO actualizar(Integer id, PermisoDTO dto) {
        var permiso = permisoRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Permiso no encontrado: " + id));
        if (dto.codigo() != null) permiso.setCodigo(dto.codigo());
        if (dto.nombre() != null) permiso.setNombre(dto.nombre());
        if (dto.modulo() != null) permiso.setModulo(dto.modulo());
        if (dto.tipoAcceso() != null) permiso.setTipoAcceso(dto.tipoAcceso());
        if (dto.descripcion() != null) permiso.setDescripcion(dto.descripcion());
        if (dto.activo() != null) permiso.setActivo(dto.activo());
        return toDTO(permisoRepository.save(permiso));
    }

    @Transactional
    public void eliminar(Integer id) {
        var permiso = permisoRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Permiso no encontrado: " + id));
        permiso.setActivo(false);
        permisoRepository.save(permiso);
    }

    private PermisoDTO toDTO(Permiso p) {
        return new PermisoDTO(
            p.getIdPermiso(), p.getCodigo(), p.getNombre(), p.getModulo(),
            p.getTipoAcceso(), p.getDescripcion(), p.getActivo(), p.getCreadoEn()
        );
    }
}
