package com.epmapa.sigrc.domain.service;

import com.epmapa.sigrc.domain.entity.EmpleadoDocumento;
import com.epmapa.sigrc.domain.repository.EmpleadoRepository;
import com.epmapa.sigrc.domain.repository.UsuarioPermisoRepository;
import com.epmapa.sigrc.domain.repository.UsuarioRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;

/**
 * Repositorio documental del expediente (§29): archivos físicos asociados a
 * cada documento de empleado_documento, con autorización (§30) y auditoría (§31).
 */
@Service
public class EmpleadoDocumentoFileService {

    @Value("${app.upload.path:/data/sigrc/uploads}")
    private String uploadPath;

    private final EmpleadoRepository empleadoRepository;
    private final UsuarioRepository usuarioRepository;
    private final UsuarioPermisoRepository usuarioPermisoRepository;
    private final AuditoriaEventos auditoriaEventos;

    public EmpleadoDocumentoFileService(EmpleadoRepository empleadoRepository,
                                        UsuarioRepository usuarioRepository,
                                        UsuarioPermisoRepository usuarioPermisoRepository,
                                        AuditoriaEventos auditoriaEventos) {
        this.empleadoRepository = empleadoRepository;
        this.usuarioRepository = usuarioRepository;
        this.usuarioPermisoRepository = usuarioPermisoRepository;
        this.auditoriaEventos = auditoriaEventos;
    }

    @Transactional
    public EmpleadoDocumento subirArchivo(Integer idEmpleado, Integer idDocumento, MultipartFile file,
                                          Integer idUsuario) throws IOException {
        verificarAccesoEscritura(idUsuario);
        if (file == null || file.isEmpty())
            throw new IllegalArgumentException("Debe adjuntar un archivo");

        var empleado = empleadoRepository.findById(idEmpleado)
            .orElseThrow(() -> new EntityNotFoundException("Empleado no encontrado: " + idEmpleado));
        var documento = buscarDocumento(empleado, idDocumento);

        String dir = uploadPath + "/empleados/" + idEmpleado + "/";
        Files.createDirectories(Paths.get(dir));

        String ext = "";
        String nombreOriginal = file.getOriginalFilename();
        if (nombreOriginal != null && nombreOriginal.contains(".")) {
            ext = nombreOriginal.substring(nombreOriginal.lastIndexOf("."));
            if (ext.length() > 12) ext = "";
        }
        String nombreFisico = UUID.randomUUID().toString() + ext;
        Path rutaCompleta = Paths.get(dir, nombreFisico);
        Files.copy(file.getInputStream(), rutaCompleta);

        documento.setNombreArchivo(nombreOriginal != null ? nombreOriginal : "archivo");
        documento.setNombreFisico(nombreFisico);
        documento.setRutaArchivo(rutaCompleta.toString());
        documento.setMimeType(file.getContentType() != null ? file.getContentType() : "application/octet-stream");
        documento.setTamanoBytes(file.getSize());
        documento.setHashSha256(sha256(file.getBytes()));

        empleadoRepository.save(empleado);

        auditoriaEventos.registrar("SUBIR_DOCUMENTO_EXPEDIENTE", "REGISTRO", "empleado_documento",
            idDocumento, null, documento.getNombreArchivo(), "OK");
        return documento;
    }

    @Transactional(readOnly = true)
    public EmpleadoDocumento descargar(Integer idEmpleado, Integer idDocumento, Integer idUsuario,
                                       HttpServletRequest request) {
        var estado = evaluarAccesoLectura(idUsuario, empleadoRepository
            .findById(idEmpleado)
            .orElseThrow(() -> new EntityNotFoundException("Empleado no encontrado: " + idEmpleado)));

        var documento = estado.empleado().getDocumentos().stream()
            .filter(d -> d.getIdEmpleadoDocumento().equals(idDocumento))
            .findFirst()
            .orElseThrow(() -> new EntityNotFoundException("Documento del expediente no encontrado"));
        if (documento.getRutaArchivo() == null || documento.getRutaArchivo().isBlank())
            throw new IllegalArgumentException("El documento no tiene archivo asociado");
        if (estado.visionParcial() && esConfidencial(documento)) {
            auditoriaEventos.registrar("DESCARGAR_DOCUMENTO_CONFIDENCIAL", "DESCARGA",
                "empleado_documento", idDocumento, null, null, "DENEGADO",
                "Intento de descarga de documento confidencial sin permiso");
            throw new AccessDeniedException("Documento confidencial: no autorizado para descargarlo");
        }

        auditoriaEventos.registrar(
            esConfidencial(documento) ? "DESCARGAR_DOCUMENTO_CONFIDENCIAL" : "DESCARGAR_DOCUMENTO_EXPEDIENTE",
            "DESCARGA", "empleado_documento", idDocumento,
            null, documento.getNombreArchivo(), "OK");
        return documento;
    }

    // ─────────────────── Autorización ───────────────────

    record Acceso(com.epmapa.sigrc.domain.entity.Empleado empleado, boolean visionParcial) {}

    private Acceso evaluarAccesoLectura(Integer idUsuario,
                                        com.epmapa.sigrc.domain.entity.Empleado empleado) {
        var usuario = usuarioRepository.findById(idUsuario)
            .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado: " + idUsuario));
        boolean admin = usuario.getRol() != null && "ADMIN".equals(usuario.getRol().getCodigo());
        boolean esPropio = usuario.getEmpleado() != null
            && empleado.getIdEmpleado().equals(usuario.getEmpleado().getIdEmpleado());
        boolean permisoTH = usuarioPermisoRepository
            .findByUsuarioIdUsuarioAndModuloAndActivoTrue(idUsuario, "TALENTO_HUMANO").isPresent();
        if (!admin && !esPropio && !permisoTH) {
            throw new AccessDeniedException("No autorizado para acceder al expediente de este empleado");
        }
        return new Acceso(empleado, !(admin || permisoTH));
    }

    private void verificarAccesoEscritura(Integer idUsuario) {
        var usuario = usuarioRepository.findById(idUsuario)
            .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado: " + idUsuario));
        boolean admin = usuario.getRol() != null && "ADMIN".equals(usuario.getRol().getCodigo());
        boolean permisoEscritura = usuarioPermisoRepository
            .findByUsuarioIdUsuarioAndModuloAndActivoTrue(idUsuario, "TALENTO_HUMANO")
            .filter(p -> !"LECTURA".equals(p.getTipoAcceso()))
            .isPresent();
        if (!admin && !permisoEscritura) {
            throw new AccessDeniedException("No tiene permisos de escritura sobre el expediente");
        }
    }

    private com.epmapa.sigrc.domain.entity.EmpleadoDocumento buscarDocumento(
            com.epmapa.sigrc.domain.entity.Empleado empleado, Integer idDocumento) {
        return empleado.getDocumentos().stream()
            .filter(d -> d.getIdEmpleadoDocumento().equals(idDocumento))
            .findFirst()
            .orElseThrow(() -> new EntityNotFoundException("Documento del expediente no encontrado: " + idDocumento));
    }

    private boolean esConfidencial(EmpleadoDocumento d) {
        if (Boolean.TRUE.equals(d.getConfidencial())) return true;
        String n = d.getNivelAcceso();
        return "CONFIDENCIAL_RRHH".equalsIgnoreCase(n) || "RESTRINGIDO".equalsIgnoreCase(n);
    }

    private String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            return UUID.randomUUID().toString();
        }
    }
}