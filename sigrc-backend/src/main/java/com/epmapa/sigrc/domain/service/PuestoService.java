package com.epmapa.sigrc.domain.service;

import com.epmapa.sigrc.domain.dto.PuestoDTO;
import com.epmapa.sigrc.domain.dto.PuestoRequest;
import com.epmapa.sigrc.domain.entity.Puesto;
import com.epmapa.sigrc.domain.entity.PuestoCapacitacion;
import com.epmapa.sigrc.domain.entity.PuestoExperiencia;
import com.epmapa.sigrc.domain.entity.PuestoFormacion;
import com.epmapa.sigrc.domain.entity.PuestoFuncion;
import com.epmapa.sigrc.domain.entity.PuestoInterfaz;
import com.epmapa.sigrc.domain.entity.PuestoProducto;
import com.epmapa.sigrc.domain.repository.PuestoRepository;
import com.epmapa.sigrc.domain.repository.UnidadOrganizacionalRepository;
import com.epmapa.sigrc.domain.repository.VersionManualRepository;
import jakarta.persistence.EntityNotFoundException;
import org.hibernate.Hibernate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class PuestoService {

    private final PuestoRepository puestoRepository;
    private final UnidadOrganizacionalRepository unidadRepository;
    private final VersionManualRepository versionRepository;
    private final AuditoriaEventos auditoriaEventos;

    public PuestoService(PuestoRepository puestoRepository,
                         UnidadOrganizacionalRepository unidadRepository,
                         VersionManualRepository versionRepository,
                         AuditoriaEventos auditoriaEventos) {
        this.puestoRepository = puestoRepository;
        this.unidadRepository = unidadRepository;
        this.versionRepository = versionRepository;
        this.auditoriaEventos = auditoriaEventos;
    }

    @Transactional(readOnly = true)
    public List<PuestoDTO> listar() {
        return puestoRepository.findByActivoTrueOrderByNombre().stream()
            .map(PuestoService::toDTO)
            .toList();
    }

    @Transactional(readOnly = true)
    public Puesto obtenerConPerfil(Integer id) {
        var puesto = puestoRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Puesto no encontrado: " + id));
        puesto = (Puesto) Hibernate.unproxy(puesto);
        Hibernate.initialize(puesto.getFunciones());
        Hibernate.initialize(puesto.getFormaciones());
        Hibernate.initialize(puesto.getExperiencias());
        Hibernate.initialize(puesto.getCapacitaciones());
        Hibernate.initialize(puesto.getProductos());
        Hibernate.initialize(puesto.getInterfaces());
        return puesto;
    }

    @Transactional
    public PuestoDTO crear(PuestoRequest req) {
        if (req.codigo() == null || req.codigo().isBlank())
            throw new IllegalArgumentException("El código es obligatorio");
        if (puestoRepository.existsByCodigo(req.codigo().trim()))
            throw new IllegalArgumentException("El código del puesto ya existe: " + req.codigo());

        var puesto = Puesto.builder()
            .codigo(req.codigo().trim().toUpperCase())
            .nombre(req.nombre())
            .rolFuncional(req.rolFuncional())
            .eje(req.eje())
            .grupoOcupacional(req.grupoOcupacional())
            .objetivo(req.objetivo())
            .nivelInstruccion(req.nivelInstruccion())
            .experienciaMeses(req.experienciaMeses())
            .esJefatura(req.esJefatura() != null && req.esJefatura())
            .esResponsableUnidad(req.esResponsableUnidad() != null && req.esResponsableUnidad())
            .numeroPlazas(req.numeroPlazas())
            .activo(true)
            .vigenteDesde(req.vigenteDesde())
            .vigenteHasta(req.vigenteHasta())
            .version(req.version() != null ? req.version() : 1)
            .build();

        if (req.idUnidad() != null)
            puesto.setUnidadOrganizacional(unidadRepository.getReferenceById(req.idUnidad()));

        if (req.idVersionManual() != null)
            puesto.setVersionManual(versionRepository.getReferenceById(req.idVersionManual()));

        aplicarPerfil(puesto, req);

        var creado = toDTO(puestoRepository.save(puesto));
        auditoriaEventos.registrar("CREAR_PUESTO", "REGISTRO", "puesto", creado.idPuesto(),
            null, resumenPuesto(req), "OK");
        return creado;
    }

    @Transactional
    public PuestoDTO actualizar(Integer id, PuestoRequest req) {
        var puesto = puestoRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Puesto no encontrado: " + id));
        var antes = resumenPuesto(puestoToRequest(puesto));

        if (req.codigo() != null && !req.codigo().isBlank()
            && !req.codigo().trim().equalsIgnoreCase(puesto.getCodigo())
            && puestoRepository.existsByCodigo(req.codigo().trim()))
            throw new IllegalArgumentException("El código del puesto ya existe: " + req.codigo());

        if (req.codigo() != null && !req.codigo().isBlank()) puesto.setCodigo(req.codigo().trim().toUpperCase());
        if (req.nombre() != null) puesto.setNombre(req.nombre());
        if (req.rolFuncional() != null) puesto.setRolFuncional(req.rolFuncional());
        if (req.eje() != null) puesto.setEje(req.eje());
        if (req.grupoOcupacional() != null) puesto.setGrupoOcupacional(req.grupoOcupacional());
        if (req.objetivo() != null) puesto.setObjetivo(req.objetivo());
        if (req.nivelInstruccion() != null) puesto.setNivelInstruccion(req.nivelInstruccion());
        if (req.experienciaMeses() != null) puesto.setExperienciaMeses(req.experienciaMeses());
        if (req.esJefatura() != null) puesto.setEsJefatura(req.esJefatura());
        if (req.esResponsableUnidad() != null) puesto.setEsResponsableUnidad(req.esResponsableUnidad());
        if (req.numeroPlazas() != null) puesto.setNumeroPlazas(req.numeroPlazas());
        if (req.vigenteDesde() != null) puesto.setVigenteDesde(req.vigenteDesde());
        if (req.vigenteHasta() != null) puesto.setVigenteHasta(req.vigenteHasta());
        if (req.version() != null) puesto.setVersion(req.version());

        if (req.idUnidad() != null)
            puesto.setUnidadOrganizacional(unidadRepository.getReferenceById(req.idUnidad()));

        if (req.idVersionManual() != null)
            puesto.setVersionManual(versionRepository.getReferenceById(req.idVersionManual()));

        aplicarPerfil(puesto, req);

        var actualizado = toDTO(puestoRepository.save(puesto));
        auditoriaEventos.registrar("MODIFICAR_PUESTO", "MODIFICACION", "puesto", id,
            antes, resumenPuesto(req), "OK");
        return actualizado;
    }

    private Map<String, Object> resumenPuesto(PuestoRequest req) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("codigo", req.codigo());
        m.put("nombre", req.nombre());
        m.put("grupoOcupacional", req.grupoOcupacional());
        m.put("esJefatura", req.esJefatura());
        m.put("numeroPlazas", req.numeroPlazas());
        return m;
    }

    private PuestoRequest puestoToRequest(Puesto p) {
        return new PuestoRequest(
            p.getCodigo(), p.getNombre(),
            p.getUnidadOrganizacional() != null ? p.getUnidadOrganizacional().getIdUnidad() : null,
            p.getRolFuncional(), p.getEje(), p.getGrupoOcupacional(), p.getObjetivo(),
            p.getNivelInstruccion(), p.getExperienciaMeses(), p.getEsJefatura(),
            p.getEsResponsableUnidad(), p.getNumeroPlazas(), p.getVigenteDesde(), p.getVigenteHasta(),
            p.getVersion(),
            p.getVersionManual() != null ? p.getVersionManual().getIdVersionManual() : null,
            List.of(), List.of(), List.of(), List.of(), List.of(), List.of()
        );
    }

    @Transactional
    public void desactivar(Integer id) {
        var puesto = puestoRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Puesto no encontrado: " + id));
        puesto.setActivo(false);
        puestoRepository.save(puesto);
    }

    private void aplicarPerfil(Puesto puesto, PuestoRequest req) {
        puesto.getFunciones().clear();
        puesto.getFormaciones().clear();
        puesto.getExperiencias().clear();
        puesto.getCapacitaciones().clear();
        puesto.getProductos().clear();
        puesto.getInterfaces().clear();

        if (req.funciones() != null) {
            for (var f : req.funciones()) {
                var fn = PuestoFuncion.builder()
                    .puesto(puesto)
                    .descripcion(f.getDescripcion())
                    .tipo(f.getTipo())
                    .orden(f.getOrden())
                    .activo(true)
                    .build();
                puesto.getFunciones().add(fn);
            }
        }
        if (req.formaciones() != null) {
            for (var f : req.formaciones()) {
                puesto.getFormaciones().add(PuestoFormacion.builder()
                    .puesto(puesto)
                    .nivelInstruccion(f.getNivelInstruccion())
                    .tituloArea(f.getTituloArea())
                    .detalle(f.getDetalle())
                    .obligatorio(f.getObligatorio() != null ? f.getObligatorio() : true)
                    .build());
            }
        }
        if (req.experiencias() != null) {
            for (var e : req.experiencias()) {
                puesto.getExperiencias().add(PuestoExperiencia.builder()
                    .puesto(puesto)
                    .tiempoMeses(e.getTiempoMeses())
                    .especificidad(e.getEspecificidad())
                    .obligatorio(e.getObligatorio() != null ? e.getObligatorio() : true)
                    .build());
            }
        }
        if (req.capacitaciones() != null) {
            for (var c : req.capacitaciones()) {
                puesto.getCapacitaciones().add(PuestoCapacitacion.builder()
                    .puesto(puesto)
                    .nombre(c.getNombre())
                    .descripcion(c.getDescripcion())
                    .horasRequeridas(c.getHorasRequeridas())
                    .obligatorio(c.getObligatorio() != null ? c.getObligatorio() : true)
                    .build());
            }
        }
        if (req.productos() != null) {
            for (var p : req.productos()) {
                puesto.getProductos().add(PuestoProducto.builder()
                    .puesto(puesto)
                    .descripcion(p.getDescripcion())
                    .orden(p.getOrden())
                    .activo(true)
                    .build());
            }
        }
        if (req.interfaces() != null) {
            for (var i : req.interfaces()) {
                puesto.getInterfaces().add(PuestoInterfaz.builder()
                    .puesto(puesto)
                    .unidadRelacionadaId(i.getUnidadRelacionadaId())
                    .descripcion(i.getDescripcion())
                    .tipoInterfaz(i.getTipoInterfaz())
                    .build());
            }
        }
    }

    private static PuestoDTO toDTO(Puesto p) {
        return new PuestoDTO(
            p.getIdPuesto(),
            p.getCodigo(),
            p.getNombre(),
            p.getUnidadOrganizacional() != null ? p.getUnidadOrganizacional().getIdUnidad() : null,
            p.getUnidadOrganizacional() != null ? p.getUnidadOrganizacional().getNombre() : null,
            p.getRolFuncional(),
            p.getEje(),
            p.getGrupoOcupacional(),
            p.getNivelInstruccion(),
            p.getExperienciaMeses(),
            p.getEsJefatura(),
            p.getEsResponsableUnidad(),
            p.getNumeroPlazas(),
            p.getActivo(),
            p.getVigenteDesde(),
            p.getVigenteHasta(),
            p.getVersion(),
            p.getVersionManual() != null ? p.getVersionManual().getIdVersionManual() : null
        );
    }
}
