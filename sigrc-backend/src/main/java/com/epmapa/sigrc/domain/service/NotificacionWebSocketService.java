package com.epmapa.sigrc.domain.service;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

@Service
public class NotificacionWebSocketService {

    private final SimpMessagingTemplate messagingTemplate;

    public NotificacionWebSocketService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void notificar(String tipo, String titulo, String mensaje, Integer idEntidad, String creadoPor) {
        var data = Map.of(
            "tipo", tipo,
            "titulo", titulo,
            "mensaje", mensaje,
            "idEntidad", idEntidad,
            "creadoPor", creadoPor,
            "fecha", LocalDateTime.now().toString()
        );
        messagingTemplate.convertAndSend("/topic/notificaciones", data);
    }

    public void notificarAsignacion(Integer idUsuario, String tipo, String titulo, String mensaje, Integer idEntidad) {
        var data = Map.of(
            "tipo", tipo,
            "titulo", titulo,
            "mensaje", mensaje,
            "idEntidad", idEntidad,
            "fecha", LocalDateTime.now().toString()
        );
        messagingTemplate.convertAndSend("/topic/asignaciones/" + idUsuario, data);
    }
}
