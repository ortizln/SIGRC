package com.epmapa.sigrc.domain.service;

import com.epmapa.sigrc.domain.dto.NivelOrganizacionalDTO;
import com.epmapa.sigrc.domain.dto.NodoOrganigramaDTO;
import com.epmapa.sigrc.domain.dto.PuestoOcupacionDTO;
import com.epmapa.sigrc.domain.dto.UnidadOrganizacionalDTO;
import com.epmapa.sigrc.domain.dto.UnidadOrganizacionalRequest;
import com.epmapa.sigrc.domain.entity.AsignacionPuesto;
import com.epmapa.sigrc.domain.entity.NivelOrganizacional;
import com.epmapa.sigrc.domain.entity.Puesto;
import com.epmapa.sigrc.domain.entity.UnidadOrganizacional;
import com.epmapa.sigrc.domain.repository.AsignacionPuestoRepository;
import com.epmapa.sigrc.domain.repository.NivelOrganizacionalRepository;
import com.epmapa.sigrc.domain.repository.PuestoRepository;
import com.epmapa.sigrc.domain.repository.UnidadOrganizacionalRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class EstructuraOrganizacionalService {

    private static final String ACTIVA = "ACTIVA";

    private final NivelOrganizacionalRepository nivelRepository;
    private final UnidadOrganizacionalRepository unidadRepository;
    private final AsignacionPuestoRepository asignacionRepository;
    private final PuestoRepository puestoRepository;

    public EstructuraOrganizacionalService(NivelOrganizacionalRepository nivelRepository,
                                           UnidadOrganizacionalRepository unidadRepository,
                                           AsignacionPuestoRepository asignacionRepository,
                                           PuestoRepository puestoRepository) {
        this.nivelRepository = nivelRepository;
        this.unidadRepository = unidadRepository;
        this.asignacionRepository = asignacionRepository;
        this.puestoRepository = puestoRepository;
    }

    // ---------- Niveles organizacionales ----------

    @Transactional(readOnly = true)
    public List<NivelOrganizacionalDTO> listarNiveles() {
        return nivelRepository.findByActivoTrueOrderByOrdenAsc().stream()
            .map(EstructuraOrganizacionalService::toNivelDTO)
            .toList();
    }

    @Transactional
    public NivelOrganizacionalDTO crearNivel(String codigo, String nombre, String descripcion, Integer orden) {
        if (codigo == null || codigo.isBlank()) throw new IllegalArgumentException("El código es obligatorio");
        if (nivelRepository.existsByCodigo(codigo.trim()))
            throw new IllegalArgumentException("El código del nivel ya existe: " + codigo);
        var nivel = NivelOrganizacional.builder()
            .codigo(codigo.trim().toUpperCase())
            .nombre(nombre)
            .descripcion(descripcion)
            .orden(orden)
            .activo(true)
            .build();
        return toNivelDTO(nivelRepository.save(nivel));
    }

    @Transactional
    public NivelOrganizacionalDTO actualizarNivel(Integer id, String codigo, String nombre, String descripcion, Integer orden) {
        var nivel = nivelRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Nivel organizacional no encontrado: " + id));
        if (codigo != null && !codigo.isBlank() && !codigo.trim().equalsIgnoreCase(nivel.getCodigo())
            && nivelRepository.existsByCodigo(codigo.trim()))
            throw new IllegalArgumentException("El código del nivel ya existe: " + codigo);
        if (codigo != null && !codigo.isBlank()) nivel.setCodigo(codigo.trim().toUpperCase());
        if (nombre != null) nivel.setNombre(nombre);
        if (descripcion != null) nivel.setDescripcion(descripcion);
        if (orden != null) nivel.setOrden(orden);
        return toNivelDTO(nivelRepository.save(nivel));
    }

    @Transactional
    public void desactivarNivel(Integer id) {
        var nivel = nivelRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Nivel organizacional no encontrado: " + id));
        nivel.setActivo(false);
        nivelRepository.save(nivel);
    }

    // ---------- Unidades organizacionales ----------

    @Transactional(readOnly = true)
    public List<UnidadOrganizacionalDTO> listarUnidades() {
        return unidadRepository.findByActivoTrueOrderByOrdenAsc().stream()
            .map(EstructuraOrganizacionalService::toUnidadDTO)
            .toList();
    }

    @Transactional(readOnly = true)
    public UnidadOrganizacionalDTO obtenerUnidad(Integer id) {
        return unidadRepository.findById(id)
            .map(EstructuraOrganizacionalService::toUnidadDTO)
            .orElseThrow(() -> new EntityNotFoundException("Unidad no encontrada: " + id));
    }

    @Transactional
    public UnidadOrganizacionalDTO asignarResponsable(Integer idUnidad, Integer responsableAsignacionId) {
        var unidad = unidadRepository.findById(idUnidad)
            .orElseThrow(() -> new EntityNotFoundException("Unidad no encontrada: " + idUnidad));
        if (responsableAsignacionId == null) {
            unidad.setResponsableAsignacionId(null);
        } else {
            var asignacion = asignacionRepository.findById(responsableAsignacionId)
                .orElseThrow(() -> new EntityNotFoundException("Asignación no encontrada: " + responsableAsignacionId));
            if (asignacion.getUnidadOrganizacional() == null
                || !asignacion.getUnidadOrganizacional().getIdUnidad().equals(idUnidad))
                throw new IllegalArgumentException("La asignación no pertenece a esta unidad");
            unidad.setResponsableAsignacionId(responsableAsignacionId);
        }
        return toUnidadDTO(unidadRepository.save(unidad));
    }

    @Transactional
    public UnidadOrganizacionalDTO crearUnidad(UnidadOrganizacionalRequest req) {
        if (req.codigo() == null || req.codigo().isBlank())
            throw new IllegalArgumentException("El código es obligatorio");
        if (unidadRepository.existsByCodigo(req.codigo().trim()))
            throw new IllegalArgumentException("El código de unidad ya existe: " + req.codigo());

        var unidad = UnidadOrganizacional.builder()
            .codigo(req.codigo().trim().toUpperCase())
            .nombre(req.nombre())
            .sigla(req.sigla())
            .descripcion(req.descripcion())
            .tipoUnidad(req.tipoUnidad())
            .orden(req.orden())
            .activo(true)
            .build();

        if (req.idNivel() != null)
            unidad.setNivelOrganizacional(nivelRepository.getReferenceById(req.idNivel()));

        if (req.idUnidadPadre() != null) {
            var padre = unidadRepository.findById(req.idUnidadPadre())
                .orElseThrow(() -> new EntityNotFoundException("Unidad padre no encontrada: " + req.idUnidadPadre()));
            if (!Boolean.TRUE.equals(padre.getActivo()))
                throw new IllegalArgumentException("La unidad padre está inactiva y no puede recibir unidades hijas");
            unidad.setUnidadPadre(padre);
        }

        return toUnidadDTO(unidadRepository.save(unidad));
    }

    @Transactional
    public UnidadOrganizacionalDTO actualizarUnidad(Integer id, UnidadOrganizacionalRequest req) {
        var unidad = unidadRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Unidad no encontrada: " + id));

        if (req.codigo() != null && !req.codigo().isBlank()
            && !req.codigo().trim().equalsIgnoreCase(unidad.getCodigo())
            && unidadRepository.existsByCodigo(req.codigo().trim()))
            throw new IllegalArgumentException("El código de unidad ya existe: " + req.codigo());

        if (req.codigo() != null && !req.codigo().isBlank()) unidad.setCodigo(req.codigo().trim().toUpperCase());
        if (req.nombre() != null) unidad.setNombre(req.nombre());
        if (req.sigla() != null) unidad.setSigla(req.sigla());
        if (req.descripcion() != null) unidad.setDescripcion(req.descripcion());
        if (req.tipoUnidad() != null) unidad.setTipoUnidad(req.tipoUnidad());
        if (req.orden() != null) unidad.setOrden(req.orden());

        if (req.idNivel() != null)
            unidad.setNivelOrganizacional(nivelRepository.getReferenceById(req.idNivel()));

        if (req.idUnidadPadre() != null && !req.idUnidadPadre().equals(unidad.getIdUnidad())) {
            validarSinCiclo(unidad.getIdUnidad(), req.idUnidadPadre());
            var padre = unidadRepository.findById(req.idUnidadPadre())
                .orElseThrow(() -> new EntityNotFoundException("Unidad padre no encontrada: " + req.idUnidadPadre()));
            if (!Boolean.TRUE.equals(padre.getActivo()))
                throw new IllegalArgumentException("La unidad padre está inactiva y no puede recibir unidades hijas");
            unidad.setUnidadPadre(padre);
        }

        return toUnidadDTO(unidadRepository.save(unidad));
    }

    @Transactional
    public void desactivarUnidad(Integer id) {
        var unidad = unidadRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Unidad no encontrada: " + id));
        if (unidadRepository.countByUnidadPadreIdUnidad(id) > 0)
            throw new IllegalArgumentException("No se puede desactivar una unidad que tiene unidades hijas");
        unidad.setActivo(false);
        unidadRepository.save(unidad);
    }

    private void validarSinCiclo(Integer idUnidad, Integer idNuevoPadre) {
        Integer actual = idNuevoPadre;
        var visitados = new ArrayList<Integer>();
        while (actual != null) {
            if (actual.equals(idUnidad))
                throw new IllegalArgumentException("No se puede asignar esta unidad como padre porque generaría un ciclo jerárquico");
            visitados.add(actual);
            actual = unidadRepository.findById(actual)
                .map(u -> u.getUnidadPadre() != null ? u.getUnidadPadre().getIdUnidad() : null)
                .orElse(null);
        }
    }

    // ---------- Organigrama ----------

    @Transactional(readOnly = true)
    public List<NodoOrganigramaDTO> organigrama() {
        var unidades = unidadRepository.findAllByOrderByOrdenAsc();
        var puestos = puestoRepository.findByActivoTrueOrderByNombre();
        var asignaciones = asignacionRepository
            .findByEstadoAndEsPrincipalTrueAndPuestoActivoTrueOrderByFechaInicioDesc(ACTIVA);
        var raices = unidades.stream()
            .filter(u -> u.getUnidadPadre() == null)
            .toList();
        return raices.stream()
            .map(u -> construirNodo(u, unidades, puestos, asignaciones))
            .toList();
    }

    private NodoOrganigramaDTO construirNodo(UnidadOrganizacional unidad, List<UnidadOrganizacional> todas,
                                             List<Puesto> puestos, List<AsignacionPuesto> asignaciones) {
        var hijos = todas.stream()
            .filter(h -> h.getUnidadPadre() != null
                && h.getUnidadPadre().getIdUnidad().equals(unidad.getIdUnidad()))
            .map(h -> construirNodo(h, todas, puestos, asignaciones))
            .toList();

        var asignacionesUnidad = asignaciones.stream()
            .filter(a -> a.getUnidadOrganizacional() != null
                && a.getUnidadOrganizacional().getIdUnidad().equals(unidad.getIdUnidad()))
            .toList();

        int plazas = puestos.stream()
            .filter(p -> p.getUnidadOrganizacional() != null
                && p.getUnidadOrganizacional().getIdUnidad().equals(unidad.getIdUnidad()))
            .mapToInt(p -> p.getNumeroPlazas() != null ? p.getNumeroPlazas() : 0)
            .sum();
        int plazasOcupadas = asignacionesUnidad.size();
        int vacantes = Math.max(0, plazas - plazasOcupadas);

        List<PuestoOcupacionDTO> puestosUnidad = puestos.stream()
            .filter(p -> p.getUnidadOrganizacional() != null
                && p.getUnidadOrganizacional().getIdUnidad().equals(unidad.getIdUnidad())
                && p.getActivo() != null && p.getActivo())
            .map(p -> {
                int numPlazas = p.getNumeroPlazas() != null ? p.getNumeroPlazas() : 0;
                int ocupados = (int) asignacionesUnidad.stream()
                    .filter(a -> a.getPuesto() != null && a.getPuesto().getIdPuesto().equals(p.getIdPuesto()))
                    .count();
                return new PuestoOcupacionDTO(
                    p.getIdPuesto(),
                    p.getCodigo(),
                    p.getNombre(),
                    p.getEsJefatura(),
                    p.getEsResponsableUnidad(),
                    numPlazas,
                    ocupados,
                    Math.max(0, numPlazas - ocupados));
            })
            .toList();

        var responsable = resolverResponsable(unidad, asignacionesUnidad);

        return new NodoOrganigramaDTO(
            unidad.getIdUnidad(),
            unidad.getCodigo(),
            unidad.getNombre(),
            unidad.getSigla(),
            unidad.getTipoUnidad(),
            unidad.getNivelOrganizacional() != null ? unidad.getNivelOrganizacional().getNombre() : null,
            unidad.getOrden(),
            unidad.getActivo(),
            responsable != null && responsable.getEmpleado() != null
                ? responsable.getEmpleado().getNombres() + " " + responsable.getEmpleado().getApellidos() : null,
            responsable != null && responsable.getPuesto() != null ? responsable.getPuesto().getNombre() : null,
            plazas,
            plazasOcupadas,
            vacantes,
            puestosUnidad,
            hijos
        );
    }

    private AsignacionPuesto resolverResponsable(UnidadOrganizacional unidad, List<AsignacionPuesto> asignacionesUnidad) {
        if (unidad.getResponsableAsignacionId() != null) {
            var resp = asignacionesUnidad.stream()
                .filter(a -> a.getIdAsignacion().equals(unidad.getResponsableAsignacionId()))
                .findFirst().orElse(null);
            if (resp != null) return resp;
        }
        return asignacionesUnidad.stream()
            .filter(a -> a.getPuesto() != null && Boolean.TRUE.equals(a.getPuesto().getEsResponsableUnidad()))
            .findFirst()
            .orElseGet(() -> asignacionesUnidad.stream()
                .filter(a -> a.getPuesto() != null && Boolean.TRUE.equals(a.getPuesto().getEsJefatura()))
                .findFirst().orElse(null));
    }

    // ---------- Mappers ----------

    private static NivelOrganizacionalDTO toNivelDTO(NivelOrganizacional n) {
        return new NivelOrganizacionalDTO(n.getIdNivel(), n.getCodigo(), n.getNombre(),
            n.getDescripcion(), n.getOrden(), n.getActivo());
    }

    private static UnidadOrganizacionalDTO toUnidadDTO(UnidadOrganizacional u) {
        return new UnidadOrganizacionalDTO(
            u.getIdUnidad(),
            u.getCodigo(),
            u.getNombre(),
            u.getSigla(),
            u.getDescripcion(),
            u.getTipoUnidad(),
            u.getNivelOrganizacional() != null ? u.getNivelOrganizacional().getIdNivel() : null,
            u.getNivelOrganizacional() != null ? u.getNivelOrganizacional().getNombre() : null,
            u.getUnidadPadre() != null ? u.getUnidadPadre().getIdUnidad() : null,
            u.getUnidadPadre() != null ? u.getUnidadPadre().getNombre() : null,
            u.getResponsableAsignacionId(),
            u.getOrden(),
            u.getActivo()
        );
    }
}
