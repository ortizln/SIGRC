package com.epmapa.sigrc.domain.repository;

import com.epmapa.sigrc.domain.entity.Usuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {
    Optional<Usuario> findByUsernameAndActivoTrue(String username);
    Optional<Usuario> findByEmailAndActivoTrue(String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    List<Usuario> findByActivoTrueAndEmpleadoIsNull();
    List<Usuario> findAllByActivoTrue();
    Optional<Usuario> findByEmpleadoIdEmpleadoAndActivoTrue(Integer idEmpleado);

    @Query("SELECT u FROM Usuario u WHERE u.activo = true AND u.empleado.id IN " +
           "(SELECT ap.empleado.id FROM AsignacionPuesto ap WHERE ap.unidadOrganizacional.id = :idUnidad " +
           " AND ap.estado = 'ACTIVA' AND ap.esPrincipal = true)")
    List<Usuario> findByUnidadVigente(@Param("idUnidad") Integer idUnidad);

    @Query("SELECT u FROM Usuario u WHERE u.activo = true AND u.empleado.id IN " +
           "(SELECT ap.empleado.id FROM AsignacionPuesto ap WHERE ap.puesto.id = :idPuesto " +
           " AND ap.estado = 'ACTIVA' AND ap.esPrincipal = true)")
    List<Usuario> findByPuestoVigente(@Param("idPuesto") Integer idPuesto);

    @Query(value = "SELECT * FROM sigrc.usuarios u WHERE (:texto IS NULL OR LOWER(u.username) LIKE LOWER(CONCAT('%',:texto,'%')) OR LOWER(u.nombres) LIKE LOWER(CONCAT('%',:texto,'%')) OR LOWER(u.apellidos) LIKE LOWER(CONCAT('%',:texto,'%')) OR LOWER(u.email) LIKE LOWER(CONCAT('%',:texto,'%'))) ORDER BY u.apellidos ASC, u.nombres ASC",
            countQuery = "SELECT COUNT(*) FROM sigrc.usuarios u WHERE (:texto IS NULL OR LOWER(u.username) LIKE LOWER(CONCAT('%',:texto,'%')) OR LOWER(u.nombres) LIKE LOWER(CONCAT('%',:texto,'%')) OR LOWER(u.apellidos) LIKE LOWER(CONCAT('%',:texto,'%')) OR LOWER(u.email) LIKE LOWER(CONCAT('%',:texto,'%')))",
            nativeQuery = true)
    Page<Usuario> buscar(@Param("texto") String texto, Pageable pageable);
}
