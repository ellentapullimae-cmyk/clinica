# Sistema de Gestion para una Clinica

Sistema desarrollado en **Java** aplicando **Programacion Orientada a Objetos (POO)**, basado en el documento de analisis `Sistema_Gestion_Clinica_POO_Profesional_Estefany_con_Informe.docx` (IESTP "PAIJAN", 2026).

El sistema centraliza la informacion de **pacientes, medicos, citas, atenciones, pagos y gastos**, calcula el **saldo de ingresos y gastos**, genera **reportes** para el control administrativo e incluye **acceso restringido por usuario con roles** y un **dashboard de estadisticas**.

## Acceso al sistema (login)

Al iniciar el programa se muestra una **ventana grafica de inicio de sesion** con la imagen del proyecto (`assets/images/clinica.png`) como panel lateral y el formulario de acceso a la derecha; se adapta al tamano de la pantalla (en ventanas estrechas el panel lateral se oculta y el formulario queda centrado). Se pide **usuario y clave** (la clave se almacena cifrada con SHA-256 mas una sal, nunca en texto plano). Tras 3 intentos fallidos el acceso queda denegado. Si no hay entorno grafico disponible, el sistema usa automaticamente el login de consola.

Usuarios por defecto (creados por `crear_bd.sql`):

| Usuario      | Clave     | Rol            | Alcance |
|--------------|-----------|----------------|---------|
| `admin`      | `admin123` | ADMINISTRADOR  | Todos los modulos |
| `recepcionista` | `recep123` | RECEPCIONISTA | Pacientes, citas, pagos, reportes y dashboard |
| `medico`     | `medico123` | MEDICO        | Citas y atenciones de su especialidad (solo las suyas), reportes y dashboard |

Cada rol ve unicamente los modulos que puede utilizar. El modulo **Gestion de usuarios** (solo administrador) permite registrar usuarios, editar datos y rol, cambiar clave y activar/desactivar cuentas. Desde el menu se puede **cambiar de usuario** sin cerrar el programa (**Cerrar sesion**, opcion 11) o **cambiar la contrasena propia** (opcion 12, pide la clave actual), y la opcion **Salir** cierra el programa.

## Modulos implementados

| Modulo   | Requerimiento | Funcionalidades principales |
|----------|---------------|-----------------------------|
| Pacientes | RF-01 | Registrar, listar, buscar por DNI, actualizar, activar/inactivar |
| Medicos   | RF-02 | Registrar, listar, buscar, actualizar, activar/inactivar |
| Citas     | RF-03 | Programar, listar, listar por periodo, modificar, confirmar, cancelar |
| Atenciones| RF-04 / RF-09 | Registrar atencion ligada a una cita, historial por paciente |
| Pagos     | RF-05 | Registrar y listar pagos (efectivo, tarjeta, transferencia) |
| Gastos    | RF-06 | Registrar, listar y anular gastos por categoria |
| Reportes  | RF-07 / RF-08 | Resumen economico, reporte de ingresos/gastos, citas y atenciones, presupuesto |
| Exportacion | RF-07 | Reportes a **PDF (OpenPDF)** y **Excel (XLSX)** |
| Dashboard | RF-10 | Estadisticas de pacientes, medicos, citas, atenciones, ingresos y gastos |
| Usuarios  | Login y acceso | Administrar usuarios, claves y roles; control de acceso por rol |

## Reglas de negocio implementadas

- Un paciente puede tener varias citas; una cita pertenece a un solo paciente.
- Toda cita debe registrar medico, fecha y hora.
- **Prevencion de citas duplicadas**: no se asignan dos citas al mismo medico en la misma fecha y hora (una cita cancelada libera el horario).
- Una atencion debe estar relacionada con una cita (1 cita = 1 atencion). Al registrar la atencion la cita pasa a ATENDIDA.
- Todo pago debe registrar un monto mayor que cero.
- Todo gasto debe registrar descripcion, categoria, fecha y monto; el monto debe ser mayor que cero.
- Los gastos anulados no se incluyen en el total de gastos activos.
- El saldo se calcula como **ingresos menos gastos**; tambien se calcula el **saldo del presupuesto** (presupuesto - gastos).
- El sistema genera una **alerta** cuando los gastos superan el presupuesto establecido.
- DNI unico y de 8 digitos; validacion de telefono y correo.
- Atencion ligada a una cita asignada al medico (1 cita = 1 atencion, transaccional).
- Reglas de acceso: solo los roles autorizados ejecutan cada accion; un medico solo opera sobre sus propias citas; el ultimo usuario activo ADMINISTRADOR no puede desactivarse ni degradarse, y un administrador no puede desactivarse a si mismo.

## Arquitectura por capas

```
src/clinica/
├── config/        ConfiguracionBD.java   (configuracion centralizada)
├── modelo/        Entidades POO y enums
├── datos/         DAO y ConexionBD (JDBC)
├── servicio/      Reglas de negocio y validaciones
├── controlador/   Controladores por modulo
└── presentacion/  Menu.java, Interfaz.java, Sesion.java, LoginVentana.java (consola + login grafico)

assets/images/  imagen del proyecto usada en la ventana de acceso (clinica.png)
```

Flujo: `presentacion -> controlador -> servicio -> datos (DAO) -> MySQL`.

## Requisitos

- **Java JDK 17** o superior (con `javac` y `java` en el PATH).
- **MySQL Server 8** o **MariaDB** en `localhost:3306`.
- Dependencias incluidas en `lib/`: `mysql-connector-j-8.3.0.jar` (JDBC) y `openpdf-1.3.30.jar` (reportes PDF).

## Instalacion y ejecucion

### 1. Crear la base de datos

Ejecute el script `crear_bd.sql` en su cliente de MySQL (phpMyAdmin, HeidiSQL, Workbench o linea de comandos):

```sql
SOURCE crear_bd.sql;
```

El script crea la base de datos `clinica` con todas las tablas, sus relaciones y **datos de prueba** (3 pacientes, 3 medicos, 7 citas, 3 atenciones, 3 pagos, 5 gastos y 3 usuarios).

Si ya tenia una base de datos existente de una version anterior, ejecute `actualizar_bd.sql`: agrega la tabla `usuario` y sus datos por defecto **sin borrar** la informacion existente.

### 2. Configurar la conexion

Edite el archivo **`db.properties`** (raiz del proyecto) con las credenciales de su MySQL:

```properties
db.url=jdbc:mysql://localhost:3306/clinica?useSSL=false&serverTimezone=UTC&characterEncoding=utf8
db.usuario=root
db.clave=tu_clave
```

Todos los valores se leen desde este unico archivo (configuracion centralizada); la clase `ConexionBD.java` no contiene credenciales.

### 3. Ejecutar el sistema

**Con el archivo por lotes (recomendado):**

```
run.bat
```

El `run.bat` compila el proyecto (codificacion UTF-8) y ejecuta el menu.

**Con un IDE** (NetBeans, IntelliJ IDEA, Eclipse, VS Code):
1. Abra la carpeta del proyecto.
2. Agregue todos los jars de `lib/` (conector MySQL y OpenPDF) al classpath.
3. Ejecute la clase `clinica.Main`.

## Uso

El menu principal ofrece los modulos segun el rol del usuario. Ejemplos de flujo:

- **Iniciar sesion** -> en la ventana grafica de acceso ingrese usuario y clave (o el login de consola si no hay entorno grafico); el menu se adapta al rol. `admin/admin123`, `recepcionista/recep123`, `medico/medico123`.
- **Programar una cita** -> Modulo de Citas (3) -> Programar cita -> elija paciente y medico activos, fecha, hora y observacion. Si el medico ya tiene una cita en esa fecha y hora, se muestra un error.
- **Registrar una atencion** -> Modulo de Atenciones (4) -> Registrar atencion -> elija una cita programada/confirmada (un medico solo ve las suyas) e ingrese diagnostico. La cita pasa a ATENDIDA.
- **Ver saldo** -> Reportes (7) -> Resumen economico. Con los datos de prueba: ingresos S/ 470.00, gastos S/ 1,180.00, saldo S/ -710.00 y saldo del presupuesto S/ 820.00.
- **Exportar reportes** -> Reportes (7) -> Exportar reportes -> elija el reporte y el formato (PDF, Excel o ambos). Los archivos se guardan en la carpeta `reportes/` del directorio del proyecto (o en `reportes/` de la carpeta personal si no hay permisos de escritura).
- **Cerrar sesion** -> opcion 11 del menu principal; el sistema vuelve al login e inicia la sesion de otro usuario sin cerrar el programa.
- **Cambiar mi contrasena** -> opcion 12 del menu principal; pide la clave actual, la nueva (minimo 6 caracteres) y su confirmacion. Disponible para todos los roles.
- **Dashboard** -> Dashboard (9) para ver las estadisticas generales y exportarlas.
- **Administrar usuarios** -> Gestion de usuarios (10, solo administrador) -> registrar, editar, cambiar clave o activar/desactivar cuentas.

## Notas

- Volver a ejecutar `crear_bd.sql` reinicia la base de datos (la deja con los datos de prueba).
- `actualizar_bd.sql` es seguro y reusable: solo agrega la tabla `usuario` y sus datos por defecto si no existen; no borra ningun registro.
- Los gastos de ejemplo corresponden a los registros G-001 a G-005 del documento (total S/ 1,180.00).
- Si no aparece codificacion correcta de simbolos en consola, ejecute `chcp 65001` antes de iniciar el programa.