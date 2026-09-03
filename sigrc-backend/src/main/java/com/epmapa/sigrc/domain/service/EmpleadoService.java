package com.epmapa.sigrc.domain.service;

import com.epmapa.sigrc.domain.dto.EmpleadoDTO;
import com.epmapa.sigrc.domain.dto.EmpleadoRequest;
import com.epmapa.sigrc.domain.entity.Empleado;
import com.epmapa.sigrc.domain.entity.EmpleadoCapacitacion;
import com.epmapa.sigrc.domain.entity.EmpleadoDocumento;
import com.epmapa.sigrc.domain.entity.EmpleadoExperiencia;
import com.epmapa.sigrc.domain.entity.EmpleadoFormacion;
import com.epmapa.sigrc.domain.entity.Usuario;
import com.epmapa.sigrc.domain.repository.AreaRepository;
import com.epmapa.sigrc.domain.repository.EmpleadoRepository;
import com.epmapa.sigrc.domain.repository.RolRepository;
import com.epmapa.sigrc.domain.repository.UsuarioPermisoRepository;
import com.epmapa.sigrc.domain.repository.UsuarioRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.hibernate.Hibernate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class EmpleadoService {

    private final EmpleadoRepository empleadoRepository;
    private final UsuarioRepository usuarioRepository;
    private final UsuarioPermisoRepository usuarioPermisoRepository;
    private final RolRepository rolRepository;
    private final AreaRepository areaRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditoriaService auditoriaService;
    private final AuditoriaEventos auditoriaEventos;

    public EmpleadoService(EmpleadoRepository empleadoRepository,
                           UsuarioRepository usuarioRepository,
                           UsuarioPermisoRepository usuarioPermisoRepository,
                           RolRepository rolRepository,
                           AreaRepository areaRepository,
                           PasswordEncoder passwordEncoder,
                           AuditoriaService auditoriaService,
                           AuditoriaEventos auditoriaEventos) {
        this.empleadoRepository = empleadoRepository;
        this.usuarioRepository = usuarioRepository;
        this.usuarioPermisoRepository = usuarioPermisoRepository;
        this.rolRepository = rolRepository;
        this.areaRepository = areaRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditoriaService = auditoriaService;
        this.auditoriaEventos = auditoriaEventos;
    }

    @Transactional(readOnly = true)
    public List<EmpleadoDTO> listar() {
        var empleados = empleadoRepository.findByActivoTrueOrderByApellidosAsc();
        return empleados.stream()
            .map(e -> {
                var usuario = usuarioRepository.findByEmpleadoIdEmpleadoAndActivoTrue(e.getIdEmpleado()).orElse(null);
                return toDTO(e, usuario != null ? usuario.getIdUsuario() : null);
            })
            .toList();
    }

    @Transactional(readOnly = true)
    public Empleado obtenerConExpediente(Integer id) {
        var empleado = empleadoRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Empleado no encontrado: " + id));
        return (Empleado) Hibernate.unproxy(empleado);
    }

    @Transactional(readOnly = true)
    public Empleado obtenerConExpedienteAutorizado(Integer id, Integer idUsuario, String username,
                                                   HttpServletRequest request) {
        var empleado = obtenerConExpediente(id);
        var usuario = usuarioRepository.findById(idUsuario)
            .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado: " + idUsuario));

        boolean admin = usuario.getRol() != null && "ADMIN".equals(usuario.getRol().getCodigo());
        boolean esPropio = usuario.getEmpleado() != null
            && empleado.getIdEmpleado().equals(usuario.getEmpleado().getIdEmpleado());
        boolean permisoTH = usuarioPermisoRepository
            .findByUsuarioIdUsuarioAndModuloAndActivoTrue(idUsuario, "TALENTO_HUMANO").isPresent();

        if (!admin && !esPropio && !permisoTH) {
            auditoriaService.registrar(username, idUsuario, "CONSULTAR_EXPEDIENTE", "CONSULTA",
                "empleado", id, null, null, request, "DENEGADO");
            throw new AccessDeniedException("No autorizado para consultar el expediente de este empleado");
        }

        boolean accesoCompleto = admin || permisoTH;
        if (!accesoCompleto && empleado.getDocumentos() != null) {
            empleado.getDocumentos().removeIf(this::esConfidencial);
        }

        boolean hayConfidencial = empleado.getDocumentos() != null
            && empleado.getDocumentos().stream().anyMatch(this::esConfidencial);
        if (accesoCompleto && hayConfidencial) {
            auditoriaEventos.registrar("VER_DOCUMENTO_CONFIDENCIAL", "CONSULTA",
                "empleado_documento", id, null, null, "OK",
                "Expediente con documentos confidenciales consultado");
        }

        auditoriaService.registrar(username, idUsuario, "CONSULTAR_EXPEDIENTE", "CONSULTA",
            "empleado", id, null, null, request,
            accesoCompleto ? "OK" : "PARCIAL");

        Hibernate.initialize(empleado.getFormaciones());
        Hibernate.initialize(empleado.getExperiencias());
        Hibernate.initialize(empleado.getCapacitaciones());
        Hibernate.initialize(empleado.getDocumentos());
        return empleado;
    }

    private boolean esConfidencial(EmpleadoDocumento d) {
        if (Boolean.TRUE.equals(d.getConfidencial())) return true;
        String n = d.getNivelAcceso();
        return "CONFIDENCIAL_RRHH".equalsIgnoreCase(n) || "RESTRINGIDO".equalsIgnoreCase(n);
    }

    @Transactional(readOnly = true)
    public Empleado obtenerMiExpediente(Integer idUsuario, String username, HttpServletRequest request) {
        var usuario = usuarioRepository.findById(idUsuario)
            .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado: " + idUsuario));
        if (usuario.getEmpleado() == null) {
            throw new IllegalStateException("Su usuario no está vinculado a un empleado");
        }
        return obtenerConExpedienteAutorizado(usuario.getEmpleado().getIdEmpleado(), idUsuario, username, request);
    }

    @Transactional
    public EmpleadoDTO crear(EmpleadoRequest req) {
        if (req.identificacion() == null || req.identificacion().isBlank())
            throw new IllegalArgumentException("La identificación es obligatoria");
        if (empleadoRepository.existsByIdentificacion(req.identificacion().trim()))
            throw new IllegalArgumentException("La identificación ya está registrada: " + req.identificacion());

        var empleado = Empleado.builder()
            .tipoIdentificacion(req.tipoIdentificacion())
            .identificacion(req.identificacion().trim())
            .nombres(req.nombres())
            .apellidos(req.apellidos())
            .fechaNacimiento(req.fechaNacimiento())
            .sexo(req.sexo())
            .estadoCivil(req.estadoCivil())
            .correoPersonal(req.correoPersonal())
            .correoInstitucional(req.correoInstitucional())
            .telefono(req.telefono())
            .celular(req.celular())
            .direccion(req.direccion())
            .fotoUrl(req.fotoUrl())
            .tipoPersonal(req.tipoPersonal())
            .estadoLaboral(req.estadoLaboral() != null ? req.estadoLaboral() : "ACTIVO")
            .fechaIngresoInstitucion(req.fechaIngresoInstitucion())
            .fechaSalidaInstitucion(req.fechaSalidaInstitucion())
            .observaciones(req.observaciones())
            .activo(true)
            .build();

        aplicarExpediente(empleado, req);
        var guardado = empleadoRepository.save(empleado);

        Integer idUsuarioCreado = null;

        if (Boolean.TRUE.equals(req.crearUsuario())) {
            idUsuarioCreado = crearUsuarioParaEmpleado(guardado, req);
        }

        var dto = toDTO(guardado, idUsuarioCreado);
        auditoriaEventos.registrar("CREAR_EMPLEADO", "REGISTRO", "empleado",
            dto.idEmpleado(), null, resumenExpediente(req), "OK");
        return dto;
    }

    private Integer crearUsuarioParaEmpleado(Empleado empleado, EmpleadoRequest req) {
        String username = req.usuarioUsername() != null ? req.usuarioUsername().trim() : null;
        String email = req.usuarioEmail() != null ? req.usuarioEmail().trim() : null;
        String password = req.usuarioPassword();
        String rolCodigo = req.usuarioRolCodigo();

        if (username == null || username.isBlank())
            throw new IllegalArgumentException("El username es obligatorio para crear el usuario");
        if (email == null || email.isBlank())
            throw new IllegalArgumentException("El email es obligatorio para crear el usuario");
        if (password == null || password.isBlank())
            throw new IllegalArgumentException("La contraseña es obligatoria para crear el usuario");
        if (rolCodigo == null || rolCodigo.isBlank())
            throw new IllegalArgumentException("El rol es obligatorio para crear el usuario");

        if (usuarioRepository.existsByUsername(username))
            throw new IllegalArgumentException("El username ya existe: " + username);
        if (usuarioRepository.existsByEmail(email))
            throw new IllegalArgumentException("El email ya existe: " + email);

        var rol = rolRepository.findByCodigo(rolCodigo)
            .orElseThrow(() -> new EntityNotFoundException("Rol no encontrado: " + rolCodigo));

        var usuario = Usuario.builder()
            .username(username)
            .email(email)
            .passwordHash(passwordEncoder.encode(password))
            .nombres(empleado.getNombres())
            .apellidos(empleado.getApellidos())
            .cargo(empleado.getTipoPersonal())
            .telefono(empleado.getTelefono())
            .rol(rol)
            .empleado(empleado)
            .activo(true)
            .debeCambiarPassword(true)
            .bloqueado(false)
            .intentosFallidos(0)
            .build();

        if (req.usuarioIdArea() != null)
            usuario.setArea(areaRepository.getReferenceById(req.usuarioIdArea()));

        var guardado = usuarioRepository.save(usuario);
        return guardado.getIdUsuario();
    }

    @Transactional
    public EmpleadoDTO actualizar(Integer id, EmpleadoRequest req) {
        var empleado = empleadoRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Empleado no encontrado: " + id));
        Map<String, Object> antes = resumenEmpleado(empleado);

        if (req.identificacion() != null && !req.identificacion().isBlank()
            && !req.identificacion().trim().equalsIgnoreCase(empleado.getIdentificacion())
            && empleadoRepository.existsByIdentificacion(req.identificacion().trim()))
            throw new IllegalArgumentException("La identificación ya está registrada: " + req.identificacion());

        if (req.tipoIdentificacion() != null) empleado.setTipoIdentificacion(req.tipoIdentificacion());
        if (req.identificacion() != null && !req.identificacion().isBlank())
            empleado.setIdentificacion(req.identificacion().trim());
        if (req.nombres() != null) empleado.setNombres(req.nombres());
        if (req.apellidos() != null) empleado.setApellidos(req.apellidos());
        if (req.fechaNacimiento() != null) empleado.setFechaNacimiento(req.fechaNacimiento());
        if (req.sexo() != null) empleado.setSexo(req.sexo());
        if (req.estadoCivil() != null) empleado.setEstadoCivil(req.estadoCivil());
        if (req.correoPersonal() != null) empleado.setCorreoPersonal(req.correoPersonal());
        if (req.correoInstitucional() != null) empleado.setCorreoInstitucional(req.correoInstitucional());
        if (req.telefono() != null) empleado.setTelefono(req.telefono());
        if (req.celular() != null) empleado.setCelular(req.celular());
        if (req.direccion() != null) empleado.setDireccion(req.direccion());
        if (req.fotoUrl() != null) empleado.setFotoUrl(req.fotoUrl());
        if (req.tipoPersonal() != null) empleado.setTipoPersonal(req.tipoPersonal());
        if (req.estadoLaboral() != null) empleado.setEstadoLaboral(req.estadoLaboral());
        if (req.fechaIngresoInstitucion() != null) empleado.setFechaIngresoInstitucion(req.fechaIngresoInstitucion());
        if (req.fechaSalidaInstitucion() != null) empleado.setFechaSalidaInstitucion(req.fechaSalidaInstitucion());
        if (req.observaciones() != null) empleado.setObservaciones(req.observaciones());

        aplicarExpediente(empleado, req);

        var actualizado = empleadoRepository.save(empleado);
        var usuario = usuarioRepository.findByEmpleadoIdEmpleadoAndActivoTrue(id).orElse(null);
        var dto = toDTO(actualizado, usuario != null ? usuario.getIdUsuario() : null);
        auditoriaEventos.registrar("MODIFICAR_EMPLEADO", "MODIFICACION", "empleado", id,
            antes, resumenExpediente(req), "OK");
        return dto;
    }

    @Transactional
    public void desactivar(Integer id) {
        var empleado = empleadoRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Empleado no encontrado: " + id));
        empleado.setActivo(false);
        empleadoRepository.save(empleado);
        auditoriaEventos.registrar("DESVINCULAR_EMPLEADO", "DESACTIVACION", "empleado", id,
            resumenEmpleado(empleado), null, "OK");
    }

    private Map<String, Object> resumenEmpleado(Empleado e) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("identificacion", e.getIdentificacion());
        m.put("nombres", e.getNombres());
        m.put("apellidos", e.getApellidos());
        m.put("estadoLaboral", e.getEstadoLaboral());
        m.put("activo", e.getActivo());
        return m;
    }

    private Map<String, Object> resumenExpediente(EmpleadoRequest req) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("identificacion", req.identificacion());
        m.put("nombres", req.nombres());
        m.put("apellidos", req.apellidos());
        m.put("tipoPersonal", req.tipoPersonal());
        m.put("estadoLaboral", req.estadoLaboral());
        m.put("formaciones", req.formaciones() != null ? req.formaciones().size() : 0);
        m.put("experiencias", req.experiencias() != null ? req.experiencias().size() : 0);
        m.put("capacitaciones", req.capacitaciones() != null ? req.capacitaciones().size() : 0);
        m.put("documentos", req.documentos() != null ? req.documentos().size() : 0);
        m.put("crearUsuario", Boolean.TRUE.equals(req.crearUsuario()));
        return m;
    }

    private void aplicarExpediente(Empleado empleado, EmpleadoRequest req) {
        empleado.getFormaciones().clear();
        empleado.getExperiencias().clear();
        empleado.getCapacitaciones().clear();
        empleado.getDocumentos().clear();

        if (req.formaciones() != null) {
            for (var f : req.formaciones()) {
                empleado.getFormaciones().add(EmpleadoFormacion.builder()
                    .empleado(empleado)
                    .nivel(f.getNivel())
                    .titulo(f.getTitulo())
                    .institucion(f.getInstitucion())
                    .pais(f.getPais())
                    .fechaInicio(f.getFechaInicio())
                    .fechaFin(f.getFechaFin())
                    .registroSenescyt(f.getRegistroSenescyt())
                    .documentoId(f.getDocumentoId())
                    .verificado(f.getVerificado() != null ? f.getVerificado() : false)
                    .build());
            }
        }
        if (req.experiencias() != null) {
            for (var e : req.experiencias()) {
                empleado.getExperiencias().add(EmpleadoExperiencia.builder()
                    .empleado(empleado)
                    .institucion(e.getInstitucion())
                    .cargo(e.getCargo())
                    .fechaInicio(e.getFechaInicio())
                    .fechaFin(e.getFechaFin())
                    .descripcion(e.getDescripcion())
                    .documentoId(e.getDocumentoId())
                    .build());
            }
        }
        if (req.capacitaciones() != null) {
            for (var c : req.capacitaciones()) {
                empleado.getCapacitaciones().add(EmpleadoCapacitacion.builder()
                    .empleado(empleado)
                    .nombre(c.getNombre())
                    .institucion(c.getInstitucion())
                    .fechaInicio(c.getFechaInicio())
                    .fechaFin(c.getFechaFin())
                    .horas(c.getHoras())
                    .tipo(c.getTipo())
                    .certificadoDocumentoId(c.getCertificadoDocumentoId())
                    .build());
            }
        }
        if (req.documentos() != null) {
            for (var d : req.documentos()) {
                empleado.getDocumentos().add(EmpleadoDocumento.builder()
                    .empleado(empleado)
                    .documentoId(d.getDocumentoId())
                    .tipo(d.getTipo())
                    .fechaDocumento(d.getFechaDocumento())
                    .descripcion(d.getDescripcion())
                    .confidencial(d.getConfidencial() != null ? d.getConfidencial() : false)
                    .nivelAcceso(d.getNivelAcceso() != null ? d.getNivelAcceso()
                        : (Boolean.TRUE.equals(d.getConfidencial()) ? "CONFIDENCIAL_RRHH" : "PUBLICO_INSTITUCIONAL"))
                    .nombreArchivo(d.getNombreArchivo())
                    .nombreFisico(d.getNombreFisico())
                    .rutaArchivo(d.getRutaArchivo())
                    .mimeType(d.getMimeType())
                    .tamanoBytes(d.getTamanoBytes())
                    .hashSha256(d.getHashSha256())
                    .build());
            }
        }
    }

    private static EmpleadoDTO toDTO(Empleado e, Integer idUsuario) {
        return new EmpleadoDTO(
            e.getIdEmpleado(),
            e.getTipoIdentificacion(),
            e.getIdentificacion(),
            e.getNombres(),
            e.getApellidos(),
            (e.getNombres() != null ? e.getNombres() : "") + " " + (e.getApellidos() != null ? e.getApellidos() : ""),
            e.getFechaNacimiento(),
            e.getSexo(),
            e.getEstadoCivil(),
            e.getCorreoPersonal(),
            e.getCorreoInstitucional(),
            e.getTelefono(),
            e.getCelular(),
            e.getDireccion(),
            e.getTipoPersonal(),
            e.getEstadoLaboral(),
            e.getFechaIngresoInstitucion(),
            e.getFechaSalidaInstitucion(),
            e.getActivo(),
            idUsuario
        );
    }
}
