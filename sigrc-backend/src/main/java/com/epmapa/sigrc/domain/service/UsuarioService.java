package com.epmapa.sigrc.domain.service;

import com.epmapa.sigrc.domain.dto.UsuarioCrearRequest;
import com.epmapa.sigrc.domain.dto.UsuarioDTO;
import com.epmapa.sigrc.domain.dto.UsuarioPermisoDTO;
import com.epmapa.sigrc.domain.entity.Usuario;
import com.epmapa.sigrc.domain.entity.UsuarioPermiso;
import com.epmapa.sigrc.domain.repository.AreaRepository;
import com.epmapa.sigrc.domain.repository.RolRepository;
import com.epmapa.sigrc.domain.repository.UsuarioPermisoRepository;
import com.epmapa.sigrc.domain.repository.UsuarioRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final AreaRepository areaRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthService authService;
    private final UsuarioPermisoRepository usuarioPermisoRepository;

    public UsuarioService(UsuarioRepository usuarioRepository, RolRepository rolRepository,
                           AreaRepository areaRepository, PasswordEncoder passwordEncoder,
                           AuthService authService,
                           UsuarioPermisoRepository usuarioPermisoRepository) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.areaRepository = areaRepository;
        this.passwordEncoder = passwordEncoder;
        this.authService = authService;
        this.usuarioPermisoRepository = usuarioPermisoRepository;
    }

    @Transactional(readOnly = true)
    public List<UsuarioDTO> listarActivos() {
        return usuarioRepository.findAll().stream()
            .filter(Usuario::getActivo)
            .map(authService::toUsuarioDTO)
            .toList();
    }

    @Transactional(readOnly = true)
    public UsuarioDTO obtenerPorId(Integer id) {
        var usuario = usuarioRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado: " + id));
        return authService.toUsuarioDTO(usuario);
    }

    @Transactional
    public UsuarioDTO crear(UsuarioCrearRequest request) {
        if (usuarioRepository.existsByUsername(request.username()))
            throw new IllegalArgumentException("El username ya existe: " + request.username());
        if (usuarioRepository.existsByEmail(request.email()))
            throw new IllegalArgumentException("El email ya existe: " + request.email());

        var rol = rolRepository.findByCodigo(request.rolCodigo())
            .orElseThrow(() -> new EntityNotFoundException("Rol no encontrado: " + request.rolCodigo()));

        var usuario = Usuario.builder()
            .username(request.username())
            .email(request.email())
            .passwordHash(passwordEncoder.encode(request.password()))
            .nombres(request.nombres())
            .apellidos(request.apellidos())
            .cargo(request.cargo())
            .telefono(request.telefono())
            .rol(rol)
            .activo(true)
            .debeCambiarPassword(true)
            .bloqueado(false)
            .intentosFallidos(0)
            .build();

        if (request.idArea() != null)
            usuario.setArea(areaRepository.getReferenceById(request.idArea()));

        return authService.toUsuarioDTO(usuarioRepository.save(usuario));
    }

    @Transactional
    public UsuarioDTO actualizar(Integer id, UsuarioCrearRequest request) {
        var usuario = usuarioRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado: " + id));

        if (request.nombres() != null) usuario.setNombres(request.nombres());
        if (request.apellidos() != null) usuario.setApellidos(request.apellidos());
        if (request.email() != null) usuario.setEmail(request.email());
        if (request.cargo() != null) usuario.setCargo(request.cargo());
        if (request.telefono() != null) usuario.setTelefono(request.telefono());
        if (request.password() != null && !request.password().isBlank())
            usuario.setPasswordHash(passwordEncoder.encode(request.password()));
        if (request.rolCodigo() != null) {
            var rol = rolRepository.findByCodigo(request.rolCodigo())
                .orElseThrow(() -> new EntityNotFoundException("Rol no encontrado: " + request.rolCodigo()));
            usuario.setRol(rol);
        }

        return authService.toUsuarioDTO(usuarioRepository.save(usuario));
    }

    @Transactional
    public void desactivar(Integer id) {
        var usuario = usuarioRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado: " + id));
        usuario.setActivo(false);
        usuarioRepository.save(usuario);
    }

    @Transactional(readOnly = true)
    public List<UsuarioPermisoDTO> obtenerPermisos(Integer idUsuario) {
        return usuarioPermisoRepository.findByUsuarioIdUsuarioAndActivoTrue(idUsuario)
            .stream()
            .map(p -> new UsuarioPermisoDTO(p.getModulo(), p.getTipoAcceso()))
            .toList();
    }

    @Transactional
    public List<UsuarioPermisoDTO> guardarPermisos(Integer idUsuario, List<UsuarioPermisoDTO> permisos) {
        var usuario = usuarioRepository.findById(idUsuario)
            .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado: " + idUsuario));
        usuarioPermisoRepository.deleteByUsuarioIdUsuario(idUsuario);
        for (var p : permisos) {
            usuarioPermisoRepository.save(UsuarioPermiso.builder()
                .usuario(usuario)
                .modulo(p.modulo())
                .tipoAcceso(p.tipoAcceso())
                .build());
        }
        return obtenerPermisos(idUsuario);
    }
}
