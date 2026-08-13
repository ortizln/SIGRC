# PLAN DE IMPLEMENTACIÓN — MÓDULO DE TALENTO HUMANO INTEGRADO AL SISTEMA DE DOCUMENTOS ELECTRÓNICOS

## 1. Propósito

Evolucionar el modelo actual de administración de usuarios (`usuario + rol + área + cargo`) hacia un **Módulo de Talento Humano y Estructura Organizacional**, tomando como referencia el *Manual Orgánico Funcional EPMAPA-T — Actualización 2023*.

El objetivo NO es convertir inmediatamente el sistema documental en un ERP completo de nómina. La primera evolución debe resolver correctamente:

- estructura organizacional;
- puestos institucionales;
- perfiles de puestos;
- expediente del servidor/trabajador;
- relación persona ↔ puesto ↔ unidad organizacional;
- responsables y jefaturas;
- movimientos de personal;
- acciones de personal;
- permisos de acceso al sistema;
- integración con documentos, trámites, firmas y derivaciones;
- historial y auditoría.

---

# 2. PRINCIPIO DE DISEÑO MÁS IMPORTANTE

## NO usar `usuario` como equivalente de `empleado`

Actualmente:

```text
Usuario
 ├── Rol
 ├── Área
 └── Cargo
```

Debe evolucionar a:

```text
PERSONA / EMPLEADO
        │
        ├── Expediente laboral
        │
        ├── Formación
        ├── Experiencia
        ├── Capacitaciones
        ├── Documentos
        └── Movimientos
                │
                ▼
       ASIGNACIÓN DE PUESTO
                │
       ┌────────┴────────┐
       ▼                 ▼
UNIDAD ORGANIZACIONAL   PUESTO
       │                 │
       │                 ├── Perfil
       │                 ├── Funciones
       │                 ├── Requisitos
       │                 ├── Productos
       │                 └── Grupo ocupacional
       │
       ▼
ESTRUCTURA JERÁRQUICA

PERSONA
   │
   └── USUARIO DEL SISTEMA (opcional)
             │
             ├── Roles
             └── Permisos
```

Una persona puede existir en Talento Humano sin tener acceso al sistema.

Un usuario del sistema siempre debe estar vinculado a una persona/empleado cuando corresponda.

---

# 3. CONCEPTOS QUE NO DEBEN CONFUNDIRSE

## 3.1 Unidad organizacional

Representa la estructura institucional.

Ejemplos:

- Gerencia General
- Asesoría Jurídica
- Secretaría General
- Dirección Administrativa
- Jefatura de Talento Humano
- Compras Públicas
- Servicios Generales y Logística
- Unidad de Tecnologías de la Información y Comunicaciones
- Dirección Financiera
- Dirección Comercial
- Dirección de Gestión Técnica

Debe soportar jerarquía mediante `unidad_padre_id`.

---

## 3.2 Puesto

Es la posición institucional definida por el Manual de Funciones.

Ejemplos:

- Gerente General
- Director Administrativo
- Jefe de Talento Humano
- Técnico de Talento Humano
- Analista de TIC
- Técnico de Desarrollo y Programación
- Contador
- Tesorero
- Director Comercial

El puesto NO es el rol de seguridad del sistema.

---

## 3.3 Rol del sistema

Define lo que una cuenta puede hacer en la aplicación.

Ejemplos:

- ADMINISTRADOR
- TALENTO_HUMANO
- SECRETARIA
- DIRECTOR
- JEFE
- FUNCIONARIO
- ARCHIVO
- CONSULTA

Una persona cuyo puesto sea `Técnico de Desarrollo y Programación` podría tener rol `ADMINISTRADOR`.

Por tanto:

```text
PUESTO != ROL
```

---

# 4. MODELO ORGANIZACIONAL

Crear catálogo:

## `nivel_organizacional`

Campos sugeridos:

```text
id
codigo
nombre
descripcion
orden
activo
```

Valores iniciales:

```text
DIRECTIVO
ASESOR
OPERATIVO
APOYO
```

---

## `unidad_organizacional`

```text
id
codigo
nombre
sigla
descripcion
tipo_unidad
nivel_organizacional_id
unidad_padre_id
responsable_asignacion_id
orden
activo
fecha_creacion
fecha_actualizacion
```

`tipo_unidad` puede manejar:

```text
DIRECTORIO
GERENCIA
DIRECCION
JEFATURA
UNIDAD
SECRETARIA
COORDINACION
PROCESO
SUBPROCESO
OTRO
```

### Reglas

- permitir estructura ilimitada padre/hijo;
- impedir ciclos jerárquicos;
- una unidad inactiva no puede recibir nuevas asignaciones;
- no eliminar físicamente unidades con historial;
- permitir reestructuración conservando vigencia histórica.

---

# 5. MÓDULO DE PUESTOS

Crear entidad:

## `puesto`

```text
id
codigo
nombre
unidad_organizacional_id
rol_funcional
eje
grupo_ocupacional
objetivo
nivel_instruccion
experiencia_meses
es_jefatura
es_responsable_unidad
numero_plazas
activo
vigente_desde
vigente_hasta
version
```

Ejemplo:

```text
Código: TH-JTH-001
Nombre: JEFE DE TALENTO HUMANO
Unidad: JEFATURA DE TALENTO HUMANO
Rol funcional: EJECUCIÓN DE PROCESOS
Eje: PROCESO
Grupo ocupacional: SP5
```

---

# 6. PERFIL DEL PUESTO

El manual no debe almacenarse como un simple campo `cargo`.

Crear ficha completa.

## `puesto_funcion`

```text
id
puesto_id
descripcion
tipo
orden
activo
```

Tipos:

```text
ESENCIAL
COMPLEMENTARIA
CONTROL
```

---

## `puesto_formacion`

```text
id
puesto_id
nivel_instruccion
titulo_area
detalle
obligatorio
```

---

## `puesto_experiencia`

```text
id
puesto_id
tiempo_meses
especificidad
obligatorio
```

---

## `puesto_capacitacion`

```text
id
puesto_id
nombre
descripcion
horas_requeridas
obligatorio
```

---

## `puesto_producto`

```text
id
puesto_id
descripcion
orden
activo
```

Representa los productos/resultados esperados del puesto.

---

## `puesto_interfaz`

```text
id
puesto_id
unidad_relacionada_id
descripcion
tipo_interfaz
```

Para representar las interfaces internas/externas indicadas en los perfiles institucionales.

---

# 7. CATÁLOGO DE PERSONAS / EMPLEADOS

Crear:

## `empleado`

```text
id
tipo_identificacion
identificacion
nombres
apellidos
fecha_nacimiento
sexo
estado_civil
correo_personal
correo_institucional
telefono
celular
direccion
foto_url
tipo_personal
estado_laboral
fecha_ingreso_institucion
fecha_salida_institucion
observaciones
activo
created_at
updated_at
```

Tipos de personal configurables:

```text
SERVIDOR_PUBLICO
TRABAJADOR
CONTRATO
NOMBRAMIENTO
OCASIONAL
CODIGO_TRABAJO
OTRO
```

Estados:

```text
ACTIVO
VACACIONES
LICENCIA
COMISION
SUSPENDIDO
DESVINCULADO
JUBILADO
```

No codificar estas opciones rígidamente si pueden variar; preferir catálogos.

---

# 8. EXPEDIENTE DIGITAL DE TALENTO HUMANO

Cada empleado debe disponer de una pantalla única:

```text
Expediente
├── Datos personales
├── Información institucional
├── Puesto actual
├── Historial de puestos
├── Formación académica
├── Experiencia laboral
├── Capacitaciones
├── Documentos
├── Acciones de personal
├── Vacaciones
├── Permisos y licencias
├── Movimientos
├── Evaluaciones
└── Auditoría
```

---

# 9. FORMACIÓN ACADÉMICA

## `empleado_formacion`

```text
id
empleado_id
nivel
titulo
institucion
pais
fecha_inicio
fecha_fin
registro_senescyt
documento_id
verificado
```

---

# 10. EXPERIENCIA LABORAL

## `empleado_experiencia`

```text
id
empleado_id
institucion
cargo
fecha_inicio
fecha_fin
descripcion
documento_id
```

---

# 11. CAPACITACIONES

## `empleado_capacitacion`

```text
id
empleado_id
nombre
institucion
fecha_inicio
fecha_fin
horas
tipo
certificado_documento_id
```

---

# 12. ASIGNACIÓN DE PUESTOS

Esta tabla reemplaza la relación directa:

```text
usuario.area_id
usuario.cargo_id
```

Crear:

## `asignacion_puesto`

```text
id
empleado_id
puesto_id
unidad_organizacional_id
tipo_asignacion
fecha_inicio
fecha_fin
es_principal
estado
accion_personal_id
observacion
created_at
```

Tipos:

```text
TITULAR
ENCARGO
SUBROGACION
TEMPORAL
COMISION
TRASLADO
```

### Reglas

Un empleado puede tener muchas asignaciones históricas.

Solo una debe ser principal/vigente, salvo excepciones explícitamente permitidas.

No sobrescribir la asignación anterior.

Al cambiar de puesto:

```text
cerrar asignación anterior
+
crear nueva asignación
```

Esto permitirá conocer:

> ¿Dónde trabajaba esta persona cuando firmó o recibió este documento?

---

# 13. MOVIMIENTOS DE PERSONAL

Crear:

## `movimiento_personal`

```text
id
empleado_id
tipo_movimiento
asignacion_origen_id
puesto_destino_id
unidad_destino_id
fecha_solicitud
fecha_desde
fecha_hasta
motivo
documento_respaldo_id
estado
creado_por
aprobado_por
created_at
```

Tipos sugeridos:

```text
INGRESO
NOMBRAMIENTO
CONTRATACION
TRASLADO
TRASPASO
CAMBIO_ADMINISTRATIVO
ENCARGO
SUBROGACION
COMISION_SERVICIOS
LICENCIA
VACACIONES
REINTEGRO
DESVINCULACION
JUBILACION
SUPRESION_PUESTO
OTRO
```

---

# 14. ACCIONES DE PERSONAL

El manual asigna a Talento Humano la elaboración de informes técnicos y acciones de personal.

Crear:

## `accion_personal`

```text
id
numero
empleado_id
tipo
fecha_emision
fecha_vigencia_desde
fecha_vigencia_hasta
motivo
situacion_actual
situacion_propuesta
documento_id
estado
elaborado_por
revisado_por
aprobado_por
created_at
```

Estados:

```text
BORRADOR
EN_REVISION
APROBADA
RECHAZADA
ANULADA
```

Idealmente la acción de personal debe generarse usando el mismo motor documental del sistema.

---

# 15. VACACIONES, PERMISOS Y LICENCIAS

## `solicitud_ausencia`

```text
id
empleado_id
tipo
fecha_desde
fecha_hasta
dias
horas
motivo
documento_respaldo_id
estado
jefe_aprobador_id
th_aprobador_id
created_at
```

Tipos:

```text
VACACION
PERMISO
LICENCIA
CALAMIDAD
ENFERMEDAD
MATERNIDAD
PATERNIDAD
COMISION
OTRO
```

Flujo sugerido:

```text
FUNCIONARIO
    ↓
JEFE INMEDIATO
    ↓
TALENTO HUMANO
    ↓
REGISTRO / ARCHIVO
```

---

# 16. RELACIÓN CON USUARIOS DEL SISTEMA

Modificar `usuario`.

## Modelo recomendado

```text
usuario
id
empleado_id
username
password_hash
email
estado
ultimo_acceso
requiere_cambio_password
```

Eliminar progresivamente:

```text
area_id
cargo_id
```

del usuario.

La información organizacional se obtiene:

```text
usuario
 → empleado
 → asignacion_puesto vigente
 → puesto
 → unidad_organizacional
```

---

# 17. ROLES Y PERMISOS

Mantener RBAC independiente de RRHH.

```text
usuario
   ↓
usuario_rol
   ↓
rol
   ↓
rol_permiso
   ↓
permiso
```

Ejemplo:

```text
TH_EMPLEADO_VER
TH_EMPLEADO_CREAR
TH_EMPLEADO_EDITAR
TH_EXPEDIENTE_VER
TH_EXPEDIENTE_ADMINISTRAR
TH_PUESTO_VER
TH_PUESTO_ADMINISTRAR
TH_MOVIMIENTO_CREAR
TH_MOVIMIENTO_APROBAR
TH_ACCION_PERSONAL_CREAR
TH_ACCION_PERSONAL_APROBAR
TH_VACACIONES_APROBAR
TH_REPORTES_VER
```

---

# 18. JEFATURA Y RESPONSABLES

No determinar al jefe por el rol `JEFE`.

Determinarlo por estructura organizacional.

Ejemplo:

```text
Dirección Administrativa
│
├── Jefatura Talento Humano
│   ├── Jefe Talento Humano
│   ├── Técnico Talento Humano
│   ├── Técnico SSO
│   ├── Trabajador Social
│   └── Médico Ocupacional
│
├── Compras Públicas
├── Servicios Generales y Logística
└── Unidad TIC
```

El sistema debe poder responder automáticamente:

```text
¿Quién es mi jefe inmediato?
¿Quién dirige esta unidad?
¿A qué Dirección pertenece?
¿Cuál es la cadena jerárquica?
```

---

# 19. ORGANIGRAMA DINÁMICO

Crear vista:

`/talento-humano/organigrama`

Características:

- árbol expandible;
- unidad → subunidades;
- responsable;
- puesto;
- funcionario asignado;
- número de plazas;
- plazas ocupadas;
- vacantes;
- navegación al perfil institucional.

No dibujar el organigrama manualmente en HTML.

Debe generarse desde `unidad_organizacional`.

---

# 20. MANUAL DE FUNCIONES DIGITAL

Crear:

`/talento-humano/manual-funciones`

Debe permitir seleccionar:

```text
Dirección
  → Unidad
     → Puesto
```

Ficha:

```text
Nombre del puesto
Código
Unidad
Nivel organizacional
Rol funcional
Eje
Grupo ocupacional
Objetivo
Funciones esenciales
Formación requerida
Experiencia requerida
Capacitación requerida
Interfaces
Productos esperados
Versión
Vigencia
```

Esto convierte el PDF en información administrable.

NO eliminar el PDF original: conservarlo como documento fuente/versionado.

---

# 21. CONTROL DE VERSIONES DEL MANUAL

Crear:

## `version_manual`

```text
id
nombre
version
fecha_aprobacion
fecha_vigencia
documento_id
estado
observaciones
```

Relacionar los puestos/perfiles con una versión.

Estados:

```text
BORRADOR
VIGENTE
DEROGADO
```

Nunca sobrescribir información histórica cuando se apruebe un nuevo manual.

---

# 22. INTEGRACIÓN CON GESTIÓN DOCUMENTAL

Esta es la principal ventaja de incorporar RRHH dentro de la aplicación existente.

Cada documento debe poder conocer:

```text
documento
 → usuario creador
 → empleado
 → asignación vigente en fecha del documento
 → puesto
 → unidad organizacional
```

Guardar además una **instantánea histórica** al firmar/emitir:

```text
firmante_nombre
firmante_puesto
firmante_unidad
firmante_asignacion_id
```

No reconstruir documentos históricos utilizando únicamente el cargo actual.

---

# 23. DERIVACIÓN AUTOMÁTICA DE DOCUMENTOS

Actualmente una derivación probablemente selecciona usuarios.

Mejorar para soportar:

```text
PERSONA
PUESTO
UNIDAD
RESPONSABLE_DE_UNIDAD
JEFE_INMEDIATO
```

Ejemplo:

```text
Enviar a:
[ JEFATURA DE TALENTO HUMANO ]

Sistema resuelve automáticamente:
Jefe de Talento Humano vigente.
```

Si cambia el funcionario, los nuevos documentos llegan al nuevo responsable sin reconfigurar el flujo.

---

# 24. BANDEJAS POR FUNCIÓN

Crear bandejas:

```text
Mis documentos
Documentos de mi puesto
Documentos de mi unidad
Documentos como responsable
Documentos delegados
Pendientes de aprobación
Pendientes de firma
```

---

# 25. DELEGACIONES

## `delegacion_funcion`

```text
id
asignacion_origen_id
asignacion_delegada_id
fecha_inicio
fecha_fin
tipo
alcance
documento_respaldo_id
estado
```

Esto permitirá cubrir vacaciones, encargos y ausencias sin cambiar manualmente todos los permisos.

---

# 26. MATRIZ PERSONA — PUESTO

Agregar herramienta para comparar requisitos del puesto contra expediente.

Ejemplo:

```text
JEFE DE TALENTO HUMANO

Nivel requerido        ✓
Formación requerida    ✓
Experiencia requerida  ✓
Capacitación           ⚠
```

No bloquear automáticamente nombramientos por esta matriz salvo que exista una regla institucional formal. Inicialmente debe funcionar como control/información.

---

# 27. DISTRIBUTIVO DE PERSONAL

El manual asigna a Talento Humano la elaboración y actualización del distributivo.

Crear reporte:

`/talento-humano/distributivo`

Columnas:

```text
Identificación
Funcionario
Unidad
Puesto
Grupo ocupacional
Tipo de relación
Fecha ingreso
Estado
```

Filtros:

```text
Dirección
Unidad
Puesto
Estado
Tipo personal
```

Exportación:

```text
PDF
Excel
```

---

# 28. DASHBOARD DE TALENTO HUMANO

Indicadores iniciales:

```text
Total empleados
Activos
Desvinculados
Por Dirección
Por Unidad
Por grupo ocupacional
Por tipo de personal
Puestos ocupados
Puestos vacantes
Personal en vacaciones
Personal en licencia
Capacitaciones registradas
Movimientos del mes
```

No incluir datos médicos sensibles en dashboards generales.

---

# 29. DOCUMENTOS DEL EXPEDIENTE

Usar el repositorio documental existente.

Clasificación sugerida:

```text
IDENTIFICACION
CONTRATO
NOMBRAMIENTO
ACCION_PERSONAL
TITULO
CERTIFICADO
CAPACITACION
EXPERIENCIA
EVALUACION
VACACION
PERMISO
LICENCIA
INFORME
DESVINCULACION
OTRO
```

Tabla puente:

```text
empleado_documento
id
empleado_id
documento_id
tipo
fecha_documento
descripcion
confidencial
```

---

# 30. SEGURIDAD DE INFORMACIÓN

El expediente de RRHH NO debe ser visible por cualquier usuario.

Niveles sugeridos:

```text
PUBLICO_INSTITUCIONAL
INTERNO
CONFIDENCIAL_RRHH
RESTRINGIDO
```

Ejemplo:

- nombre, puesto y unidad: institucional;
- formación: según permiso;
- contrato/acción de personal: RRHH;
- información médica: acceso especialmente restringido.

Aplicar autorización en backend, no solamente ocultamiento de botones Angular.

---

# 31. AUDITORÍA

Registrar obligatoriamente:

```text
LOGIN
CREAR_EMPLEADO
MODIFICAR_EMPLEADO
CREAR_PUESTO
MODIFICAR_PUESTO
ASIGNAR_PUESTO
FINALIZAR_ASIGNACION
CREAR_MOVIMIENTO
APROBAR_MOVIMIENTO
VER_DOCUMENTO_CONFIDENCIAL
DESCARGAR_DOCUMENTO_CONFIDENCIAL
CAMBIAR_ROL
CAMBIAR_PERMISO
DESVINCULAR_EMPLEADO
```

Campos:

```text
usuario
fecha_hora
ip
accion
entidad
entidad_id
valor_anterior
valor_nuevo
```

No almacenar contraseñas, tokens ni secretos en auditoría.

---

# 32. MODELO DE DATOS RESUMIDO

```text
nivel_organizacional
        │
        ▼
unidad_organizacional ───────┐
        │                    │
        ▼                    │
      puesto                 │
        │                    │
        ├── puesto_funcion   │
        ├── puesto_formacion │
        ├── puesto_experiencia
        ├── puesto_capacitacion
        ├── puesto_producto
        └── puesto_interfaz
        │
        ▼
asignacion_puesto
        ▲
        │
     empleado
        │
        ├── empleado_formacion
        ├── empleado_experiencia
        ├── empleado_capacitacion
        ├── empleado_documento
        ├── movimiento_personal
        ├── accion_personal
        └── solicitud_ausencia
        │
        ▼
      usuario
        │
        ▼
   usuario_rol
        │
        ▼
       rol
        │
        ▼
   rol_permiso
```

---

# 33. API REST PROPUESTA

```text
/api/talento-humano/unidades
/api/talento-humano/niveles-organizacionales
/api/talento-humano/puestos
/api/talento-humano/puestos/{id}/perfil
/api/talento-humano/empleados
/api/talento-humano/empleados/{id}
/api/talento-humano/empleados/{id}/expediente
/api/talento-humano/empleados/{id}/formacion
/api/talento-humano/empleados/{id}/experiencia
/api/talento-humano/empleados/{id}/capacitaciones
/api/talento-humano/empleados/{id}/documentos
/api/talento-humano/asignaciones
/api/talento-humano/movimientos
/api/talento-humano/acciones-personal
/api/talento-humano/ausencias
/api/talento-humano/organigrama
/api/talento-humano/distributivo
/api/talento-humano/dashboard
/api/talento-humano/manual-funciones
```

---

# 34. FRONTEND ANGULAR PROPUESTO

```text
/talento-humano
│
├── dashboard
├── empleados
│   ├── listado
│   ├── nuevo
│   └── :id
│       ├── datos
│       ├── puesto
│       ├── formacion
│       ├── experiencia
│       ├── capacitacion
│       ├── documentos
│       ├── movimientos
│       └── historial
│
├── estructura
│   ├── organigrama
│   └── unidades
│
├── puestos
│   ├── listado
│   └── :id/perfil
│
├── movimientos
├── acciones-personal
├── vacaciones-permisos
├── distributivo
├── manual-funciones
└── configuracion
```

---

# 35. MIGRACIÓN DESDE EL MODELO ACTUAL

## Fase 1 — Preparación

No eliminar todavía:

```text
usuario.area
usuario.cargo
```

Crear las nuevas tablas paralelamente.

---

## Fase 2 — Migrar áreas

Convertir cada área actual en `unidad_organizacional`.

Crear jerarquía según el Manual Orgánico Funcional.

---

## Fase 3 — Migrar cargos

Convertir cargos actuales en `puesto`.

Evitar duplicados.

Ejemplo incorrecto:

```text
Técnico
Técnico
Técnico
```

Debe distinguirse:

```text
Técnico de Talento Humano
Técnico de Desarrollo y Programación
Técnico de Presupuestos
Técnico Contable
```

---

## Fase 4 — Crear empleados

Por cada usuario actual:

```text
crear empleado
vincular usuario.empleado_id
```

---

## Fase 5 — Crear asignaciones

Transformar:

```text
usuario + área + cargo
```

en:

```text
empleado + asignacion_puesto + unidad + puesto
```

Usar como fecha inicial la mejor información disponible. Si se desconoce la fecha real, marcar el registro como migrado y no inventar una fecha histórica.

---

## Fase 6 — Cambiar autenticación

Después del login:

```text
Usuario
Empleado
Asignación vigente
Puesto
Unidad
Roles
Permisos
```

pueden devolverse en `/auth/me`.

---

## Fase 7 — Actualizar documentos

Cambiar las consultas que usan:

```text
usuario.area
usuario.cargo
```

por la asignación institucional vigente.

---

## Fase 8 — Retirar campos antiguos

Solo cuando ninguna funcionalidad dependa de ellos:

```text
usuario.area_id
usuario.cargo_id
```

podrán eliminarse.

---

# 36. ORDEN RECOMENDADO DE DESARROLLO

## Sprint 1 — Estructura organizacional

Implementar:

- niveles;
- unidades;
- jerarquía;
- organigrama.

## Sprint 2 — Puestos

Implementar:

- catálogo;
- perfiles;
- funciones;
- formación;
- experiencia;
- capacitación;
- productos.

## Sprint 3 — Empleados

Implementar:

- datos personales;
- expediente;
- documentos;
- formación;
- experiencia;
- capacitaciones.

## Sprint 4 — Asignaciones

Implementar:

- puesto actual;
- historial;
- responsables;
- jefatura automática.

## Sprint 5 — Usuarios

Implementar:

- `usuario.empleado_id`;
- RBAC;
- permisos;
- migración.

## Sprint 6 — Integración documental

Implementar:

- firma con puesto;
- unidad;
- destinatario institucional;
- jefe inmediato;
- bandejas por unidad;
- historial del firmante.

## Sprint 7 — Gestión de personal

Implementar:

- movimientos;
- acciones de personal;
- vacaciones;
- permisos;
- licencias.

## Sprint 8 — Reportes y control

Implementar:

- distributivo;
- dashboard;
- reportes;
- auditoría;
- matriz persona-puesto.

---

# 37. PRUEBAS OBLIGATORIAS

## Estructura

- crear unidad raíz;
- crear subunidad;
- impedir ciclos;
- cambiar unidad padre;
- inactivar unidad.

## Puestos

- crear perfil completo;
- agregar funciones;
- modificar versión;
- consultar requisitos.

## Empleados

- crear empleado sin usuario;
- crear usuario desde empleado;
- impedir identificación duplicada;
- desvincular sin borrar historial.

## Asignaciones

- asignar puesto;
- trasladar funcionario;
- encargar puesto temporal;
- finalizar encargo;
- conservar historial.

## Documentos

Caso crítico:

1. Juan es Director Administrativo.
2. Juan firma documento A.
3. Juan cambia de puesto.
4. María pasa a ser Directora Administrativa.
5. Consultar documento A.

Resultado esperado:

```text
Documento A continúa mostrando:
Juan — Director Administrativo
```

y NO el puesto actual de Juan ni el nombre de María.

## Seguridad

- funcionario no accede a expedientes ajenos;
- jefe ve únicamente información autorizada;
- RRHH accede según permisos;
- descarga confidencial queda auditada;
- modificar frontend no permite saltar autorización backend.

---

# 38. CRITERIOS DE ACEPTACIÓN

El módulo se considerará correctamente implementado cuando:

- [x] usuario y empleado sean entidades independientes;
- [x] rol y puesto sean conceptos independientes;
- [x] exista estructura jerárquica administrable;
- [x] el organigrama se genere desde base de datos;
- [x] cada puesto tenga perfil institucional;
- [x] cada empleado tenga expediente;
- [x] exista historial de asignaciones;
- [x] se identifique automáticamente al jefe inmediato;
- [x] documentos históricos conserven puesto/unidad del momento de emisión;
- [x] los documentos puedan dirigirse a unidades o puestos;
- [x] existan movimientos y acciones de personal;
- [x] exista distributivo;
- [x] los expedientes tengan control de acceso;
- [x] toda operación sensible tenga auditoría;
- [x] el Manual de Funciones pueda administrarse/versionarse.

---

# 39. INSTRUCCIÓN PARA EL AGENTE DE DESARROLLO

Antes de modificar código:

1. Analizar completamente el backend y frontend actuales.
2. Identificar las entidades `Usuario`, `Rol`, `Area`, `Cargo`, `Documento`, `Derivacion`, `Firma`, `Auditoria` y equivalentes.
3. Identificar todas las FK y consultas que dependan de `area` y `cargo`.
4. Generar un inventario de impacto.
5. Proponer migraciones de base de datos sin pérdida de información.
6. No eliminar columnas antiguas en la primera fase.
7. Implementar cada módulo con migraciones incrementales.
8. Crear pruebas unitarias y de integración.
9. Verificar autorización en backend.
10. Documentar endpoints y cambios.
11. Mantener compatibilidad con los documentos electrónicos existentes.
12. No inventar reglas laborales que no estén definidas en los requisitos o normativa institucional.

---

# 40. PROMPT MAESTRO PARA IMPLEMENTACIÓN

```text
Actúa como arquitecto de software senior especializado en sistemas empresariales,
gestión documental, RRHH, Spring Boot, Angular y PostgreSQL.

Necesito evolucionar el módulo actual de usuarios de mi sistema de documentos
electrónicos. Actualmente el usuario posee directamente rol, área y cargo.

Implementa la arquitectura definida en el archivo:
PLAN_IMPLEMENTACION_RRHH_DOCUMENTOS_ELECTRONICOS.md

OBJETIVO PRINCIPAL:

Separar claramente:

PERSONA/EMPLEADO
USUARIO
ROL DE SEGURIDAD
UNIDAD ORGANIZACIONAL
PUESTO
ASIGNACIÓN DE PUESTO

y construir un módulo de Talento Humano integrado con gestión documental.

REGLAS:

1. No eliminar funcionalidad existente.
2. No eliminar inicialmente area_id ni cargo_id del usuario.
3. Analizar dependencias antes de modificar entidades.
4. Crear migraciones de BD reversibles cuando sea posible.
5. Mantener historial de asignaciones.
6. Nunca sobrescribir datos históricos de puestos.
7. Los documentos emitidos deben conservar el puesto y unidad del firmante al momento
   de la emisión.
8. Aplicar permisos en backend.
9. Registrar auditoría.
10. Implementar por fases.
11. Antes de cada fase, indicar archivos/tablas/endpoints afectados.
12. Después de cada fase ejecutar pruebas y corregir errores antes de continuar.

COMIENZA ÚNICAMENTE CON:

FASE 0 — ANÁLISIS DEL SISTEMA ACTUAL

Entrega:

- arquitectura actual;
- entidades afectadas;
- tablas afectadas;
- endpoints afectados;
- componentes Angular afectados;
- riesgos;
- dependencias;
- plan de migración;
- propuesta del modelo ER;
- orden exacto de implementación.

NO escribas todavía las migraciones ni modifiques código hasta terminar este análisis.
```

---

# 41. RESULTADO ESPERADO

La evolución final debe transformar:

```text
USUARIO
 ├── rol
 ├── área
 └── cargo
```

en una arquitectura institucional:

```text
ORGANIZACIÓN
    │
    ├── Estructura organizacional
    │       └── Unidades
    │
    ├── Puestos institucionales
    │       └── Manual de funciones
    │
    ├── Talento Humano
    │       ├── Empleados
    │       ├── Expedientes
    │       ├── Asignaciones
    │       ├── Movimientos
    │       └── Acciones de personal
    │
    ├── Seguridad
    │       ├── Usuarios
    │       ├── Roles
    │       └── Permisos
    │
    └── Gestión Documental
            ├── Documentos
            ├── Firmas
            ├── Derivaciones
            ├── Bandejas
            └── Auditoría
```

---

# ESTADO DE IMPLEMENTACIÓN (actualizado)

| Sección | Estado |
|---------|--------|
| §1-§19 Modelo organizacional, puestos, empleados, asignaciones, movimientos, ausencias, roles/permisos, jefatura, organigrama | ✅ Implementado (Sprints 1-7) — §19 organigrama interactivo (árbol expandible/colapsable, responsable y puesto, plazas/ocupadas/vacantes, navegación al perfil de la unidad) |
| §20-21 Manual de funciones digital y control de versiones | ✅ Implementado (`version_manual`, `ManualFuncionesService/Controller`) |
| §22-23 Integración documental y derivación automática | ✅ Implementado (CorrespondenciaService) |
| §24 Bandejas por función | ✅ Implementado (`/bandeja-puesto`, `/pendientes`) |
| §25 Delegaciones | ✅ Implementado (`delegacion_funcion`, resolver en derivación) |
| §26 Matriz persona-puesto | ✅ Implementado (`/matriz-persona-puesto/{idEmpleado}`) |
| §27 Distributivo de personal | ✅ Implementado (JSON, CSV en frontend, export Excel/PDF `/distributivo/exportar`) |
| §28 Dashboard de Talento Humano | ✅ Implementado (`/dashboard`) |
| §29 Documentos del expediente | ✅ Implementado (`empleado_documento` con `confidencial`, `nivel_acceso` y repositorio de archivos: subir/descargar con hash SHA-256) |
| §30 Seguridad de información | ✅ Implementado (autorización en backend para expediente; auto-consulta con documentos confidenciales ocultos; 403 para no autorizados) |
| §31 Auditoría | ✅ Implementado (`AuditoriaEventos`; eventos obligatorios registrados, incluido `DESCARGAR_DOCUMENTO_CONFIDENCIAL`) |
| §32-§34 Documentación (modelo, API, frontend) | ✅ `docs/API.md` actualizado |
| §35 Migración usuarios → empleados | ✅ Implementado como `MigracionTHService` + `POST /talento-humano/migracion/usuarios` (Fases 1-5: vincula empleado, crea asignación desde `area+cargo`, reutiliza unidades/puestos por nombre; idempotente, auditable y con modo `dryRun`). Pendiente: ejecución contra la BD productiva con respaldo previo |
| §37 Pruebas obligatorias | ✅ Implementado — 48 pruebas unitarias (JUnit 5 + Mockito) en `sigrc-backend/src/test`: estructura/ciclos/organigrama, puestos/perfil, empleados (duplicados, desvinculación), asignaciones (traslado conserva historial, jefatura automática), autorización de expediente §30, matriz persona-puesto, organigrama con plazas/vacantes y migración §35 (idempotencia, dry-run sin persistir, reutilización de unidad/puesto) |

Pendientes fuera de alcance: ninguna funcional del plan. (`documentoId` de `empleado_documento` sigue siendo una referencia entera al repositorio documental general; el repositorio del expediente usa archivos físicos bajo `app.upload.path`.)


Esta arquitectura permitirá que el sistema documental refleje la estructura institucional real y pueda evolucionar posteriormente hacia módulos adicionales de Talento Humano sin volver a rediseñar el núcleo de usuarios.
