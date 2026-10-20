package com.epmapa.sigrc.domain.service;

import com.epmapa.sigrc.domain.dto.VinculoAsistenteDTO;
import com.epmapa.sigrc.domain.dto.VinculoAsistenteRequest;
import com.epmapa.sigrc.domain.entity.AsignacionPuesto;
import com.epmapa.sigrc.domain.entity.CorrespondenciaVinculoAsistente;
import com.epmapa.sigrc.domain.entity.Puesto;
import com.epmapa.sigrc.domain.entity.Usuario;
import com.epmapa.sigrc.domain.repository.AsignacionPuestoRepository;
import com.epmapa.sigrc.domain.repository.CorrespondenciaVinculoRepository;
import com.epmapa.sigrc.domain.repository.PuestoRepository;
import com.epmapa.sigrc.domain.repository.UsuarioRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * BCC — Administración de vínculos asistente→titular.
 * Solo ADMIN puede crear/modificar/desactivar.
 */
@Service
public class VinculoAsistenteService {

    private final CorrespondenciaVinculoRepository vinculoRepository;
    private final PuestoRepository puestoRepository;
    private final UsuarioRepository usuarioRepository;
    private final AsignacionPuestoRepository asignacionRepository;
    private final AuditoriaEventos auditoriaEventos;

    public VinculoAsistenteService(CorrespondenciaVinculoRepository vinculoRepository,
                                   PuestoRepository puestoRepository,
                                   UsuarioRepository usuarioRepository,
                                   AsignacionPuestoRepository asignacionRepository,
                                   AuditoriaEventos auditoriaEventos) {
        this.vinculoRepository = vinculoRepository;
        this.puestoRepository = puestoRepository;
        this.usuarioRepository = usuarioRepository;
        this.asignacionRepository = asignacionRepository;
        this.auditoriaEventos = auditoriaEventos;
    }

    @Transactional(readOnly = true)
    public List<VinculoAsistenteDTO> listarTodos() {
        return vinculoRepository.findByActivoTrueOrderByCreadoEnDesc().stream()
                .map(this::toDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<VinculoAsistenteDTO> listarVigentesPorAsistente(Integer idPuestoAsistente) {
        LocalDate hoy = LocalDate.now();
        return vinculoRepository.findVigentesComoAsistente(idPuestoAsistente, hoy).stream()
                .map(this::toDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public CorrespondenciaVinculoAsistente buscarVigente(Integer idAsistente, Integer idTitular) {
        LocalDate hoy = LocalDate.now();
        return vinculoRepository.findVigenteEntre(idAsistente, idTitular, hoy)
                .orElseThrow(() -> new EntityNotFoundException(
                    "No existe vínculo vigente entre el puesto " + idAsistente + " y el puesto " + idTitular));
    }

    @Transactional(readOnly = true)
    public boolean esVigente(Integer idAsistente, Integer idTitular) {
        LocalDate hoy = LocalDate.now();
        return vinculoRepository.findVigenteEntre(idAsistente, idTitular, hoy).isPresent();
    }

    /**
     * Resuelve el puesto vigente del usuario autenticado.
     * Devuelve null si no tiene asignación activa.
     */
    @Transactional(readOnly = true)
    public Integer resolverPuestoVigente(Integer idUsuario) {
        var usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado: " + idUsuario));
        if (usuario.getEmpleado() == null) return null;
        return asignacionRepository
                .findFirstByEmpleadoIdEmpleadoAndEsPrincipalTrueAndEstadoOrderByFechaInicioDesc(
                        usuario.getEmpleado().getIdEmpleado(), "ACTIVA")
                .map(AsignacionPuesto::getPuesto)
                .map(Puesto::getIdPuesto)
                .orElse(null);
    }

    @Transactional
    public VinculoAsistenteDTO crear(VinculoAsistenteRequest req, Integer idUsuarioAdmin) {
        if (req.puestoAsistenteId() == null || req.puestoTitularId() == null)
            throw new IllegalArgumentException("Los puestos asistente y titular son obligatorios");
        if (req.puestoAsistenteId().equals(req.puestoTitularId()))
            throw new IllegalArgumentException("El puesto asistente y el titular no pueden ser el mismo");

        var asistente = puestoRepository.findById(req.puestoAsistenteId())
                .orElseThrow(() -> new EntityNotFoundException("Puesto asistente no encontrado"));
        var titular = puestoRepository.findById(req.puestoTitularId())
                .orElseThrow(() -> new EntityNotFoundException("Puesto titular no encontrado"));

        if (vinculoRepository.existsByPuestoAsistente_IdPuestoAndPuestoTitular_IdPuestoAndActivoTrue(
                req.puestoAsistenteId(), req.puestoTitularId()))
            throw new IllegalArgumentException("Ya existe un vínculo activo entre estos puestos");

        LocalDate hoy = LocalDate.now();
        var admin = usuarioRepository.findById(idUsuarioAdmin)
                .orElseThrow(() -> new EntityNotFoundException("Usuario admin no encontrado"));

        var entity = CorrespondenciaVinculoAsistente.builder()
                .puestoAsistente(asistente)
                .puestoTitular(titular)
                .nombreAsistente(asistente.getNombre())
                .nombreTitular(titular.getNombre())
                .puedeRecepcionar(req.puedeRecepcionar() != null ? req.puedeRecepcionar() : true)
                .puedeOrganizar(req.puedeOrganizar() != null ? req.puedeOrganizar() : true)
                .puedeSeguimiento(req.puedeSeguimiento() != null ? req.puedeSeguimiento() : true)
                .vigenteDesde(req.vigenteDesde() != null ? req.vigenteDesde() : hoy)
                .vigenteHasta(req.vigenteHasta())
                .activo(true)
                .autorizadoPor(admin)
                .build();

        var guardado = vinculoRepository.save(entity);
        auditoriaEventos.registrar("CREAR_VINCULO_ASISTENTE", "REGISTRO",
                "correspondencia_vinculo_asistente", guardado.getIdVinculo(),
                null, resumenVinculo(guardado), "OK");
        return toDTO(guardado);
    }

    @Transactional
    public VinculoAsistenteDTO actualizar(Integer id, VinculoAsistenteRequest req) {
        var entity = vinculoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Vínculo no encontrado"));

        if (req.puedeRecepcionar() != null) entity.setPuedeRecepcionar(req.puedeRecepcionar());
        if (req.puedeOrganizar() != null) entity.setPuedeOrganizar(req.puedeOrganizar());
        if (req.puedeSeguimiento() != null) entity.setPuedeSeguimiento(req.puedeSeguimiento());
        if (req.vigenteDesde() != null) entity.setVigenteDesde(req.vigenteDesde());
        if (req.vigenteHasta() != null) entity.setVigenteHasta(req.vigenteHasta());

        var actualizado = vinculoRepository.save(entity);
        auditoriaEventos.registrar("ACTUALIZAR_VINCULO_ASISTENTE", "MODIFICACION",
                "correspondencia_vinculo_asistente", id,
                null, resumenVinculo(actualizado), "OK");
        return toDTO(actualizado);
    }

    @Transactional
    public void desactivar(Integer id) {
        var entity = vinculoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Vínculo no encontrado"));
        entity.setActivo(false);
        vinculoRepository.save(entity);
        auditoriaEventos.registrar("DESACTIVAR_VINCULO_ASISTENTE", "DESACTIVACION",
                "correspondencia_vinculo_asistente", id,
                resumenVinculo(entity), null, "OK");
    }

    private java.util.Map<String, Object> resumenVinculo(CorrespondenciaVinculoAsistente v) {
        var m = new java.util.LinkedHashMap<String, Object>();
        m.put("puestoAsistente", v.getPuestoAsistente() != null ? v.getPuestoAsistente().getNombre() : null);
        m.put("puestoTitular", v.getPuestoTitular() != null ? v.getPuestoTitular().getNombre() : null);
        m.put("activo", v.getActivo());
        return m;
    }

    private VinculoAsistenteDTO toDTO(CorrespondenciaVinculoAsistente v) {
        return new VinculoAsistenteDTO(
                v.getIdVinculo(),
                v.getPuestoAsistente() != null ? v.getPuestoAsistente().getIdPuesto() : null,
                v.getNombreAsistente(),
                v.getNombreAsistente(),
                v.getPuestoTitular() != null ? v.getPuestoTitular().getIdPuesto() : null,
                v.getNombreTitular(),
                v.getNombreTitular(),
                v.getPuedeRecepcionar(),
                v.getPuedeOrganizar(),
                v.getPuedeSeguimiento(),
                v.getVigenteDesde() != null ? v.getVigenteDesde().toString() : null,
                v.getVigenteHasta() != null ? v.getVigenteHasta().toString() : null,
                v.getActivo(),
                v.vigente(LocalDate.now()),
                v.getAutorizadoPor() != null ? v.getAutorizadoPor().getIdUsuario() : null,
                v.getAutorizadoPor() != null
                    ? v.getAutorizadoPor().getNombres() + " " + v.getAutorizadoPor().getApellidos()
                    : null
        );
    }
}
