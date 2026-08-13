package com.epmapa.sigrc.domain.service;

import com.epmapa.sigrc.security.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Registro de eventos de auditoría (§31) con datos del contexto de seguridad
 * (usuario autenticado vía JWT) y de la petición HTTP actual, sin tener que
 * propagarlos manualmente desde los controladores.
 */
@Component
public class AuditoriaEventos {

    private final AuditoriaService auditoriaService;

    public AuditoriaEventos(AuditoriaService auditoriaService) {
        this.auditoriaService = auditoriaService;
    }

    public void registrar(String accion, String tipoOperacion, String tabla, Integer idRegistro,
                          Object antes, Object despues, String resultado) {
        registrar(accion, tipoOperacion, tabla, idRegistro, antes, despues, resultado, null);
    }

    public void registrar(String accion, String tipoOperacion, String tabla, Integer idRegistro,
                          Object antes, Object despues, String resultado, String detalle) {
        String username = null;
        Integer idUsuario = null;
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
            username = principal.username();
            idUsuario = principal.idUsuario();
        }

        HttpServletRequest request = null;
        var attrs = RequestContextHolder.getRequestAttributes();
        if (attrs instanceof ServletRequestAttributes servletAttributes) {
            request = servletAttributes.getRequest();
        }

        auditoriaService.registrarEvento(username, idUsuario, accion, tipoOperacion, tabla, idRegistro,
            antes, despues, request, resultado, detalle);
    }

    /**
     * Registra un evento con un actor explícito (útil en flujos como LOGIN,
     * donde todavía no hay contexto de seguridad).
     */
    public void registrar(String username, Integer idUsuario, String accion, String tipoOperacion,
                          String tabla, Integer idRegistro, Object antes, Object despues,
                          String resultado) {
        auditoriaService.registrarEvento(username, idUsuario, accion, tipoOperacion, tabla, idRegistro,
            antes, despues, null, resultado, null);
    }
}
