package com.epmapa.sigrc.domain.service;

import com.epmapa.sigrc.domain.dto.LoginRequest;
import com.epmapa.sigrc.domain.dto.LoginResponse;
import com.epmapa.sigrc.domain.dto.UsuarioDTO;
import com.epmapa.sigrc.domain.dto.UsuarioPermisoDTO;
import com.epmapa.sigrc.domain.entity.AsignacionPuesto;
import com.epmapa.sigrc.domain.entity.Usuario;
import com.epmapa.sigrc.domain.repository.AsignacionPuestoRepository;
import com.epmapa.sigrc.domain.repository.UsuarioPermisoRepository;
import com.epmapa.sigrc.domain.repository.UsuarioRepository;
import com.epmapa.sigrc.security.JwtTokenProvider;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class AuthService {

    private final AuthenticationManager authManager;
    private final JwtTokenProvider tokenProvider;
    private final UsuarioRepository usuarioRepository;
    private final UsuarioPermisoRepository usuarioPermisoRepository;
    private final AsignacionPuestoRepository asignacionPuestoRepository;
    private final AuditoriaEventos auditoriaEventos;

    public AuthService(AuthenticationManager authManager, JwtTokenProvider tokenProvider,
                       UsuarioRepository usuarioRepository,
                       UsuarioPermisoRepository usuarioPermisoRepository,
                       AsignacionPuestoRepository asignacionPuestoRepository,
                       AuditoriaEventos auditoriaEventos) {
        this.authManager = authManager;
        this.tokenProvider = tokenProvider;
        this.usuarioRepository = usuarioRepository;
        this.usuarioPermisoRepository = usuarioPermisoRepository;
        this.asignacionPuestoRepository = asignacionPuestoRepository;
        this.auditoriaEventos = auditoriaEventos;
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        var usuario = usuarioRepository.findByUsernameAndActivoTrue(request.username())
            .orElseThrow(() -> new BadCredentialsException("Credenciales inválidas"));

        if (usuario.getBloqueado()) {
            throw new BadCredentialsException("Usuario bloqueado. Contacte al administrador.");
        }

        try {
            authManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password()));
        } catch (BadCredentialsException e) {
            usuario.setIntentosFallidos(usuario.getIntentosFallidos() + 1);
            if (usuario.getIntentosFallidos() >= 5) {
                usuario.setBloqueado(true);
            }
            usuarioRepository.save(usuario);
            throw new BadCredentialsException("Credenciales inválidas");
        }

        usuario.setIntentosFallidos(0);
        usuario.setUltimoAcceso(LocalDateTime.now());
        usuarioRepository.save(usuario);

        String token = tokenProvider.generateToken(
            usuario.getUsername(),
            usuario.getRol().getCodigo(),
            usuario.getIdUsuario()
        );
        String refreshToken = tokenProvider.generateRefreshToken(usuario.getUsername());

        auditoriaEventos.registrar(usuario.getUsername(), usuario.getIdUsuario(),
            "LOGIN", "AUTENTICACION", "usuario", usuario.getIdUsuario(),
            null, null, "OK");

        var userDTO = toUsuarioDTO(usuario);

        return new LoginResponse(token, refreshToken, "Bearer",
            LocalDateTime.now().plusHours(24), userDTO);
    }

    public UsuarioDTO toUsuarioDTO(Usuario u) {
        var permisos = usuarioPermisoRepository.findByUsuarioIdUsuarioAndActivoTrue(u.getIdUsuario())
            .stream()
            .map(p -> new UsuarioPermisoDTO(p.getModulo(), p.getTipoAcceso()))
            .toList();

        AsignacionPuesto asignacionActual = null;
        if (u.getEmpleado() != null) {
            asignacionActual = asignacionPuestoRepository
                .findFirstByEmpleadoIdEmpleadoAndEsPrincipalTrueAndEstadoOrderByFechaInicioDesc(
                    u.getEmpleado().getIdEmpleado(), "ACTIVA")
                .orElse(null);
        }

        return new UsuarioDTO(
            u.getIdUsuario(), u.getUsername(), u.getEmail(),
            u.getNombres(), u.getApellidos(),
            u.getNombres() + " " + u.getApellidos(),
            u.getCargo(),
            u.getArea() != null ? u.getArea().getNombre() : null,
            u.getArea() != null ? u.getArea().getIdArea() : null,
            u.getRol().getCodigo(), u.getRol().getNombre(),
            u.getTelefono(), u.getActivo(),
            u.getDebeCambiarPassword(), u.getBloqueado(),
            permisos,
            u.getEmpleado() != null ? u.getEmpleado().getIdEmpleado() : null,
            u.getEmpleado() != null ? (u.getEmpleado().getNombres() + " " + u.getEmpleado().getApellidos()) : null,
            asignacionActual != null && asignacionActual.getPuesto() != null
                ? asignacionActual.getPuesto().getNombre() : null,
            asignacionActual != null && asignacionActual.getUnidadOrganizacional() != null
                ? asignacionActual.getUnidadOrganizacional().getNombre() : null
        );
    }

    @Transactional(readOnly = true)
    public UsuarioDTO obtenerMe(Integer idUsuario) {
        var usuario = usuarioRepository.findById(idUsuario)
            .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado: " + idUsuario));
        return toUsuarioDTO(usuario);
    }
}
