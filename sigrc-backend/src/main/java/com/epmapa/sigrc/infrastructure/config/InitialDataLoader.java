package com.epmapa.sigrc.infrastructure.config;

import com.epmapa.sigrc.domain.entity.Rol;
import com.epmapa.sigrc.domain.entity.Usuario;
import com.epmapa.sigrc.domain.repository.RolRepository;
import com.epmapa.sigrc.domain.repository.UsuarioRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class InitialDataLoader implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;

    public InitialDataLoader(UsuarioRepository usuarioRepository, RolRepository rolRepository,
                             PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (!usuarioRepository.existsByUsername("admin")) {
            var adminRol = rolRepository.findByCodigo("ADMIN")
                .orElseThrow(() -> new RuntimeException("Rol ADMIN no encontrado"));

            var admin = Usuario.builder()
                .username("admin")
                .email("admin@epmapa.gob.ec")
                .passwordHash(passwordEncoder.encode("admin"))
                .nombres("Administrador")
                .apellidos("SIGRC")
                .cargo("Administrador del Sistema")
                .rol(adminRol)
                .activo(true)
                .debeCambiarPassword(true)
                .bloqueado(false)
                .intentosFallidos(0)
                .build();

            usuarioRepository.save(admin);
            System.out.println("[SIGRC] Usuario admin creado. Password: admin");
        }
    }
}
