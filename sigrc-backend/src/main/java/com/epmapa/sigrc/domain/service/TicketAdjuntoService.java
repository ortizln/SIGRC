package com.epmapa.sigrc.domain.service;

import com.epmapa.sigrc.domain.entity.Ticket;
import com.epmapa.sigrc.domain.entity.TicketAdjunto;
import com.epmapa.sigrc.domain.entity.Usuario;
import com.epmapa.sigrc.domain.repository.TicketAdjuntoRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Service
public class TicketAdjuntoService {

    private final TicketAdjuntoRepository adjuntoRepository;

    @Value("${app.upload.path:./uploads}")
    private String uploadPath;

    public TicketAdjuntoService(TicketAdjuntoRepository adjuntoRepository) {
        this.adjuntoRepository = adjuntoRepository;
    }

    @Transactional(readOnly = true)
    public List<TicketAdjunto> listarPorTicket(Integer idTicket) {
        return adjuntoRepository.findByTicketIdTicket(idTicket);
    }

    @Transactional
    public TicketAdjunto subir(Integer idTicket, Integer idUsuario, MultipartFile file) {
        try {
            var uploadDir = Paths.get(uploadPath, "tickets", idTicket.toString());
            Files.createDirectories(uploadDir);

            var nombreOriginal = file.getOriginalFilename();
            var extension = "";
            if (nombreOriginal != null && nombreOriginal.contains(".")) {
                extension = nombreOriginal.substring(nombreOriginal.lastIndexOf("."));
            }
            var nombreArchivo = UUID.randomUUID().toString() + extension;
            var rutaCompleta = uploadDir.resolve(nombreArchivo);
            Files.copy(file.getInputStream(), rutaCompleta);

            var adjunto = TicketAdjunto.builder()
                    .ticket(Ticket.builder().idTicket(idTicket).build())
                    .usuario(Usuario.builder().idUsuario(idUsuario).build())
                    .nombreOriginal(nombreOriginal != null ? nombreOriginal : "archivo")
                    .nombreArchivo(nombreArchivo)
                    .rutaArchivo(rutaCompleta.toString())
                    .tipoMime(file.getContentType() != null ? file.getContentType() : "application/octet-stream")
                    .tamanoBytes(file.getSize())
                    .hashSha256(calcularHash(file))
                    .creadoEn(LocalDateTime.now())
                    .build();

            return adjuntoRepository.save(adjunto);
        } catch (IOException e) {
            throw new RuntimeException("Error al subir archivo", e);
        }
    }

    public Resource descargar(Integer idAdjunto) {
        var adjunto = adjuntoRepository.findById(idAdjunto)
                .orElseThrow(() -> new EntityNotFoundException("Archivo no encontrado: " + idAdjunto));
        try {
            var ruta = Paths.get(adjunto.getRutaArchivo());
            var resource = new UrlResource(ruta.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            }
            throw new RuntimeException("No se pudo leer el archivo: " + adjunto.getNombreOriginal());
        } catch (MalformedURLException e) {
            throw new RuntimeException("Error al leer archivo", e);
        }
    }

    @Transactional(readOnly = true)
    public TicketAdjunto obtenerInfo(Integer idAdjunto) {
        return adjuntoRepository.findById(idAdjunto)
                .orElseThrow(() -> new EntityNotFoundException("Archivo no encontrado: " + idAdjunto));
    }

    @Transactional
    public void eliminar(Integer idAdjunto) {
        var adjunto = adjuntoRepository.findById(idAdjunto)
                .orElseThrow(() -> new EntityNotFoundException("Archivo no encontrado: " + idAdjunto));
        try {
            Files.deleteIfExists(Paths.get(adjunto.getRutaArchivo()));
        } catch (IOException ignored) {
        }
        adjuntoRepository.delete(adjunto);
    }

    private String calcularHash(MultipartFile file) throws IOException {
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            digest.update(file.getBytes());
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 no disponible", e);
        }
    }
}
