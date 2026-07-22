package com.epmapa.sigrc.domain.service;

import com.epmapa.sigrc.domain.dto.RolDTO;
import com.epmapa.sigrc.domain.entity.Permiso;
import com.epmapa.sigrc.domain.entity.Rol;
import com.epmapa.sigrc.domain.entity.RolesPermiso;
import com.epmapa.sigrc.domain.repository.PermisoRepository;
import com.epmapa.sigrc.domain.repository.RolRepository;
import com.epmapa.sigrc.domain.repository.RolesPermisoRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class RolService {

    private final RolRepository rolRepository;
    private final PermisoRepository permisoRepository;
    private final RolesPermisoRepository rolesPermisoRepository;

    public RolService(RolRepository rolRepository, PermisoRepository permisoRepository,
                      RolesPermisoRepository rolesPermisoRepository) {
        this.rolRepository = rolRepository;
        this.permisoRepository = permisoRepository;
        this.rolesPermisoRepository = rolesPermisoRepository;
    }

    @Transactional(readOnly = true)
    public List<RolDTO> listar() {
        return rolRepository.findAll().stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public RolDTO obtenerPorId(Integer id) {
        return toDTO(rolRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Rol no encontrado: " + id)));
    }

    @Transactional
    public RolDTO crear(RolDTO dto) {
        var rol = Rol.builder()
            .codigo(dto.codigo())
            .nombre(dto.nombre())
            .descripcion(dto.descripcion())
            .activo(dto.activo() != null ? dto.activo() : true)
            .build();
        rol = rolRepository.save(rol);
        asignarPermisos(rol, dto.permisoIds());
        return toDTO(rolRepository.findById(rol.getIdRol()).orElse(rol));
    }

    @Transactional
    public RolDTO actualizar(Integer id, RolDTO dto) {
        var rol = rolRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Rol no encontrado: " + id));
        if (dto.codigo() != null) rol.setCodigo(dto.codigo());
        if (dto.nombre() != null) rol.setNombre(dto.nombre());
        if (dto.descripcion() != null) rol.setDescripcion(dto.descripcion());
        if (dto.activo() != null) rol.setActivo(dto.activo());
        rolRepository.save(rol);
        if (dto.permisoIds() != null) {
            rolesPermisoRepository.deleteByRolIdRol(id);
            asignarPermisos(rol, dto.permisoIds());
        }
        return toDTO(rolRepository.findById(id).orElse(rol));
    }

    @Transactional
    public void eliminar(Integer id) {
        var rol = rolRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Rol no encontrado: " + id));
        rol.setActivo(false);
        rolRepository.save(rol);
    }

    private void asignarPermisos(Rol rol, List<Integer> permisoIds) {
        if (permisoIds == null) return;
        for (Integer idPermiso : permisoIds) {
            var permiso = permisoRepository.findById(idPermiso)
                .orElseThrow(() -> new EntityNotFoundException("Permiso no encontrado: " + idPermiso));
            rolesPermisoRepository.save(RolesPermiso.builder().rol(rol).permiso(permiso).build());
        }
    }

    private RolDTO toDTO(Rol r) {
        var permisos = rolesPermisoRepository.findByRolIdRol(r.getIdRol());
        return new RolDTO(
            r.getIdRol(), r.getCodigo(), r.getNombre(), r.getDescripcion(),
            r.getActivo(), r.getCreadoEn(),
            permisos.stream().map(rp -> rp.getPermiso().getIdPermiso()).collect(Collectors.toList())
        );
    }
}
