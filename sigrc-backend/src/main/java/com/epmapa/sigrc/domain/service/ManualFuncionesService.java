package com.epmapa.sigrc.domain.service;

import com.epmapa.sigrc.domain.dto.ManualFuncionesDTO;
import com.epmapa.sigrc.domain.dto.VersionManualDTO;
import com.epmapa.sigrc.domain.dto.VersionManualRequest;
import com.epmapa.sigrc.domain.entity.Puesto;
import com.epmapa.sigrc.domain.entity.UnidadOrganizacional;
import com.epmapa.sigrc.domain.entity.VersionManual;
import com.epmapa.sigrc.domain.repository.PuestoRepository;
import com.epmapa.sigrc.domain.repository.UnidadOrganizacionalRepository;
import com.epmapa.sigrc.domain.repository.VersionManualRepository;
import jakarta.persistence.EntityNotFoundException;
import org.hibernate.Hibernate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class ManualFuncionesService {

    public static final String ESTADO_BORRADOR = "BORRADOR";
    public static final String ESTADO_VIGENTE = "VIGENTE";
    public static final String ESTADO_DEROGADO = "DEROGADO";

    private final VersionManualRepository versionRepository;
    private final UnidadOrganizacionalRepository unidadRepository;
    private final PuestoRepository puestoRepository;

    public ManualFuncionesService(VersionManualRepository versionRepository,
                                  UnidadOrganizacionalRepository unidadRepository,
                                  PuestoRepository puestoRepository) {
        this.versionRepository = versionRepository;
        this.unidadRepository = unidadRepository;
        this.puestoRepository = puestoRepository;
    }

    // ---------- Versiones del manual ----------

    @Transactional(readOnly = true)
    public List<VersionManualDTO> listarVersiones() {
        return versionRepository.findAllByOrderByCreadoEnDesc().stream()
            .map(ManualFuncionesService::toDTO)
            .toList();
    }

    @Transactional
    public VersionManualDTO crearVersion(VersionManualRequest req) {
        if (req.nombre() == null || req.nombre().isBlank())
            throw new IllegalArgumentException("El nombre del manual es obligatorio");
        if (req.version() == null || req.version().isBlank())
            throw new IllegalArgumentException("El número de versión es obligatorio");

        var version = VersionManual.builder()
            .nombre(req.nombre().trim())
            .version(req.version().trim())
            .fechaAprobacion(req.fechaAprobacion())
            .fechaVigencia(req.fechaVigencia())
            .documentoId(req.documentoId())
            .observaciones(req.observaciones())
            .estado(ESTADO_BORRADOR)
            .build();
        return toDTO(versionRepository.save(version));
    }

    @Transactional
    public VersionManualDTO actualizarVersion(Integer id, VersionManualRequest req) {
        var version = obtener(id);
        if (req.nombre() != null && !req.nombre().isBlank()) version.setNombre(req.nombre().trim());
        if (req.version() != null && !req.version().isBlank()) version.setVersion(req.version().trim());
        if (req.fechaAprobacion() != null) version.setFechaAprobacion(req.fechaAprobacion());
        if (req.fechaVigencia() != null) version.setFechaVigencia(req.fechaVigencia());
        if (req.documentoId() != null) version.setDocumentoId(req.documentoId());
        if (req.observaciones() != null) version.setObservaciones(req.observaciones());
        return toDTO(versionRepository.save(version));
    }

    @Transactional
    public VersionManualDTO aprobarVersion(Integer id) {
        var version = obtener(id);
        List<VersionManual> vigentes = versionRepository.findByEstadoOrderByCreadoEnDesc(ESTADO_VIGENTE);
        for (var vigente : vigentes) {
            if (!vigente.getIdVersionManual().equals(id)) {
                vigente.setEstado(ESTADO_DEROGADO);
                versionRepository.save(vigente);
            }
        }
        if (version.getFechaAprobacion() == null) version.setFechaAprobacion(LocalDate.now());
        if (version.getFechaVigencia() == null) version.setFechaVigencia(LocalDate.now());
        version.setEstado(ESTADO_VIGENTE);
        return toDTO(versionRepository.save(version));
    }

    @Transactional
    public VersionManualDTO derogarVersion(Integer id) {
        var version = obtener(id);
        version.setEstado(ESTADO_DEROGADO);
        return toDTO(versionRepository.save(version));
    }

    private VersionManual obtener(Integer id) {
        return versionRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Versión del manual no encontrada: " + id));
    }

    // ---------- Estructura del manual ----------

    @Transactional(readOnly = true)
    public ManualFuncionesDTO estructuraManual() {
        var versionVigente = versionRepository.findFirstByEstadoOrderByCreadoEnDesc(ESTADO_VIGENTE);

        var unidades = unidadRepository.findByActivoTrueOrderByOrdenAsc();
        var puestos = puestoRepository.findByActivoTrueOrderByNombre();
        puestos.forEach(ManualFuncionesService::inicializarPerfilPuesto);

        var direcciones = unidades.stream()
            .filter(u -> u.getUnidadPadre() == null)
            .map(u -> construirDireccion(u, unidades, puestos))
            .toList();

        return new ManualFuncionesDTO(
            versionVigente.map(ManualFuncionesService::toDTO).orElse(null),
            direcciones
        );
    }

    private ManualFuncionesDTO.DireccionManualDTO construirDireccion(UnidadOrganizacional direccion,
                                                                     List<UnidadOrganizacional> unidades,
                                                                     List<Puesto> puestos) {
        var puestosDirectos = puestos.stream()
            .filter(p -> p.getUnidadOrganizacional() != null
                && p.getUnidadOrganizacional().getIdUnidad().equals(direccion.getIdUnidad()))
            .map(ManualFuncionesService::toPuestoManualDTO)
            .toList();

        var hijas = unidades.stream()
            .filter(u -> u.getUnidadPadre() != null
                && u.getUnidadPadre().getIdUnidad().equals(direccion.getIdUnidad()))
            .map(u -> new ManualFuncionesDTO.UnidadManualDTO(
                u.getIdUnidad(),
                u.getNombre(),
                u.getSigla(),
                u.getTipoUnidad(),
                puestos.stream()
                    .filter(p -> p.getUnidadOrganizacional() != null
                        && p.getUnidadOrganizacional().getIdUnidad().equals(u.getIdUnidad()))
                    .map(ManualFuncionesService::toPuestoManualDTO)
                    .toList()))
            .toList();

        return new ManualFuncionesDTO.DireccionManualDTO(
            direccion.getIdUnidad(),
            direccion.getNombre(),
            direccion.getSigla(),
            direccion.getTipoUnidad(),
            direccion.getNivelOrganizacional() != null ? direccion.getNivelOrganizacional().getNombre() : null,
            hijas,
            puestosDirectos
        );
    }

    private static void inicializarPerfilPuesto(Puesto p) {
        Hibernate.initialize(p.getFunciones());
        Hibernate.initialize(p.getFormaciones());
        Hibernate.initialize(p.getExperiencias());
        Hibernate.initialize(p.getCapacitaciones());
        Hibernate.initialize(p.getProductos());
        Hibernate.initialize(p.getInterfaces());
    }

    private static ManualFuncionesDTO.PuestoManualDTO toPuestoManualDTO(Puesto p) {
        return new ManualFuncionesDTO.PuestoManualDTO(
            p.getIdPuesto(),
            p.getCodigo(),
            p.getNombre(),
            p.getRolFuncional(),
            p.getEje(),
            p.getGrupoOcupacional(),
            p.getObjetivo(),
            p.getNivelInstruccion(),
            p.getExperienciaMeses(),
            p.getEsJefatura(),
            p.getEsResponsableUnidad(),
            p.getNumeroPlazas(),
            p.getVigenteDesde(),
            p.getVigenteHasta(),
            p.getVersion(),
            p.getFunciones(),
            p.getFormaciones(),
            p.getExperiencias(),
            p.getCapacitaciones(),
            p.getProductos(),
            p.getInterfaces()
        );
    }

    private static VersionManualDTO toDTO(VersionManual v) {
        return new VersionManualDTO(
            v.getIdVersionManual(),
            v.getNombre(),
            v.getVersion(),
            v.getFechaAprobacion(),
            v.getFechaVigencia(),
            v.getDocumentoId(),
            v.getEstado(),
            v.getObservaciones()
        );
    }
}