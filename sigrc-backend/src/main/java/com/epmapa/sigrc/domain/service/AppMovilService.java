package com.epmapa.sigrc.domain.service;

import com.epmapa.sigrc.domain.dto.AppMovilDTO;
import com.epmapa.sigrc.domain.entity.AppMovil;
import com.epmapa.sigrc.domain.repository.AppMovilRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AppMovilService {

    private final AppMovilRepository repository;

    @Value("${app.upload.path:/data/sigrc/uploads}")
    private String uploadPath;

    public AppMovilService(AppMovilRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<AppMovilDTO> listar() {
        return repository.findByActivoTrueOrderByCreadoEnDesc().stream()
                .map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional
    public AppMovilDTO subir(String version, String descripcion, MultipartFile archivo) {
        try {
            var uploadDir = Paths.get(uploadPath, "app-movil");
            Files.createDirectories(uploadDir);

            String nombreOriginal = archivo.getOriginalFilename();
            String extension = "";
            if (nombreOriginal != null && nombreOriginal.contains(".")) {
                extension = nombreOriginal.substring(nombreOriginal.lastIndexOf("."));
            }
            String nombreArchivo = "app-" + version.replace(".", "-") + extension;
            Path rutaDestino = uploadDir.resolve(nombreArchivo);

            archivo.transferTo(rutaDestino.toFile());

            String checksum = calcularChecksum(rutaDestino);

            var entity = AppMovil.builder()
                    .version(version)
                    .nombreArchivo(nombreOriginal)
                    .rutaArchivo(rutaDestino.toString())
                    .tamanioBytes(archivo.getSize())
                    .descripcion(descripcion)
                    .checksum(checksum)
                    .activo(true)
                    .creadoEn(LocalDateTime.now())
                    .build();

            entity = repository.save(entity);
            return toDTO(entity);
        } catch (IOException e) {
            throw new RuntimeException("Error al subir el archivo APK", e);
        }
    }

    @Transactional(readOnly = true)
    public AppMovilDTO obtenerPorId(Integer id) {
        return toDTO(repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("APK no encontrado: " + id)));
    }

    @Transactional(readOnly = true)
    public AppMovil obtenerUltimo() {
        var lista = repository.findByActivoTrueOrderByCreadoEnDesc();
        return lista.isEmpty() ? null : lista.get(0);
    }

    @Transactional
    public void eliminar(Integer id) {
        var entity = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("APK no encontrado: " + id));
        entity.setActivo(false);
        repository.save(entity);
    }

    private String calcularChecksum(Path ruta) {
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = Files.readAllBytes(ruta);
            byte[] hash = digest.digest(bytes);
            var hex = new StringBuilder();
            for (byte b : hash) hex.append(String.format("%02x", b));
            return hex.toString();
        } catch (NoSuchAlgorithmException | IOException e) {
            return null;
        }
    }

    private AppMovilDTO toDTO(AppMovil a) {
        return new AppMovilDTO(
                a.getIdAppMovil(), a.getVersion(), a.getNombreArchivo(),
                a.getRutaArchivo(), a.getTamanioBytes(), a.getDescripcion(),
                a.getChecksum(), a.getActivo(), a.getCreadoEn()
        );
    }
}
