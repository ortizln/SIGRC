package com.epmapa.sigrc.domain.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "empleado_documento", schema = "sigrc")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EmpleadoDocumento {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idEmpleadoDocumento;
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empleado_id", nullable = false)
    private Empleado empleado;
    @Column(name = "documento_id")
    private Integer documentoId;
    @Column(length = 50)
    private String tipo;
    @Column(name = "fecha_documento")
    private LocalDate fechaDocumento;
    @Column(length = 500)
    private String descripcion;
    @Builder.Default
    private Boolean confidencial = false;
    @Column(name = "nivel_acceso", length = 30)
    private String nivelAcceso;
    @Column(name = "nombre_archivo", length = 255)
    private String nombreArchivo;
    @Column(name = "nombre_fisico", length = 255)
    private String nombreFisico;
    @Column(name = "ruta_archivo", length = 500)
    private String rutaArchivo;
    @Column(name = "mime_type", length = 100)
    private String mimeType;
    @Column(name = "tamano_bytes")
    private Long tamanoBytes;
    @Column(name = "hash_sha256")
    private String hashSha256;
}
