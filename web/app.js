/* ============================================================
   Sistema de Gestion para una Clinica - logica del frontend web
   Interfaz SPA que consume la API y reutiliza toda la logica del
   sistema (controlador -> servicio -> DAO).
   ============================================================ */
'use strict';

// ---------- Estado de sesion ----------
const SO = {
  token: null,
  usuario: null,
  permisos: null,
  pagina: 'dashboard',
  periodo: null
};

const $ = id => document.getElementById(id);
const esc = s => String(s == null ? '' : s).replace(/[&<>"']/g,
  c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
const fe = iso => iso ? String(iso).split('-').reverse().join('/') : '--';
const hr = iso => iso ? String(iso).slice(0, 5) : '--';
const moneda = v => 'S/ ' + Number(v || 0).toFixed(2);
const hoy = () => new Date().toISOString().slice(0, 10);
const manana = () => { const d = new Date(); d.setDate(d.getDate() + 1); return d.toISOString().slice(0, 10); };
const mesInicio = () => { const d = new Date(); return d.getFullYear() + '-' + String(d.getMonth() + 1).padStart(2, '0') + '-01'; };

const PALETA = ['#14b8a6', '#2563eb', '#f59e0b', '#ef4444', '#8b5cf6', '#10b981', '#d97706', '#64748b'];
const COLOR_ESTADO = { PROGRAMADA: '#f59e0b', CONFIRMADA: '#2563eb', CANCELADA: '#ef4444', ATENDIDA: '#16a34a' };
const NOMBRE_ESTADO = { PROGRAMADA: 'Programada', CONFIRMADA: 'Confirmada', CANCELADA: 'Cancelada', ATENDIDA: 'Atendida' };

function insignia(estado) {
  const el = estado => `<span class="insignia insignia-${claseBadge(estado)}">${esc(estado)}</span>`;
  return el(estado);
}
function claseBadge(e) {
  if (['ACTIVO', 'CONTABILIZADO', 'CONFIRMADA', 'ATENDIDA', 'PAGADO'].includes(e)) return 'verde';
  if (['INACTIVO', 'ANULADO', 'CANCELADA'].includes(e)) return 'roja';
  if (['PROGRAMADA', 'PENDIENTE'].includes(e)) return 'ambar';
  return 'azul';
}

// ---------- API ----------
async function api(ruta, opciones = {}) {
  const cab = {};
  if (SO.token) cab['Authorization'] = 'Bearer ' + SO.token;
  let body;
  if (opciones.body !== undefined) {
    cab['Content-Type'] = 'application/json';
    body = JSON.stringify(opciones.body);
  }
  let resp;
  try {
    resp = await fetch(ruta, { method: opciones.metodo || 'GET', headers: cab, body });
  } catch (e) {
    // fetch solo lanza cuando no hay servidor escuchando. Se indica como
    // arrancarlo, porque el mensaje generico no ayuda a ninguna parte.
    throw new Error('No se pudo conectar con el servidor. Ejecute run.bat y '
      + 'abras http://localhost:8080 (el servidor escucha en el puerto 8080).');
  }
  let datos = {};
  try { datos = await resp.json(); } catch (e) { /* sin cuerpo */ }
  if (resp.status === 401) { cerrarSesion(); throw new Error(datos.error || 'Sesion expirada.'); }
  if (!resp.ok) throw new Error(datos.error || 'Error del servidor (' + resp.status + ').');
  return datos;
}

// ---------- Toasts ----------
let AViso = 0;
function toast(mensaje, tipo = 'exito') {
  const id = 't' + (++AViso);
  const el = document.createElement('div');
  el.className = 'toast ' + tipo;
  el.id = id;
  const icono = tipo === 'error' ? '⛔' : tipo === 'info' ? 'ℹ️' : '✅';
  el.innerHTML = `<span>${icono}</span><span>${esc(mensaje)}</span>`;
  $('toasts').appendChild(el);
  setTimeout(() => { const n = $(id); if (n) n.remove(); }, 3800);
}

// ---------- Modal ----------
let modalAceptarFn = null;
function abrirModal(titulo, cuerpoHtml, textoAceptar, alAceptar) {
  $('modalTitulo').textContent = titulo;
  $('modalCuerpo').innerHTML = cuerpoHtml;
  const btn = $('modalAceptar');
  btn.textContent = textoAceptar || 'Aceptar';
  modalAceptarFn = alAceptar;
  $('modalFondo').hidden = false;
  const primero = $('modalCuerpo').querySelector('input, select, textarea');
  if (primero) setTimeout(() => primero.focus(), 60);
}
function cerrarModal() { $('modalFondo').hidden = true; modalAceptarFn = null; }
function modalConfirmar(titulo, mensajeHtml, alConfirmar, textoBoton) {
  abrirModal(titulo, `<div class="mensaje-form">${mensajeHtml}</div>`,
    textoBoton || 'Confirmar', alConfirmar);
}
$('modalAceptar').addEventListener('click', () => {
  if (modalAceptarFn) modalAceptarFn();
});
$('modalCancelar').addEventListener('click', cerrarModal);
$('modalCerrar').addEventListener('click', cerrarModal);
$('modalFondo').addEventListener('click', e => { if (e.target === $('modalFondo')) cerrarModal(); });

// ---------- Util: formulario a objeto ----------
function valorForm(idForm) {
  const datos = {};
  const form = $(idForm);
  new FormData(form).forEach((v, k) => { datos[k] = v; });
  return datos;
}
function desdeCantidad(html, id) {
  const tmp = document.createElement('div');
  tmp.innerHTML = html;
  const el = tmp.querySelector('#' + id);
  return { elemento: el, valor: () => (el ? el.value.trim() : '') };
}

// ---------- Graficos canvas ----------
// Los graficos se registran para poder redibujarse cuando cambia el tamano
// de la ventana (responsive real, no solo escalado por CSS).
const GRAFICOS = new Map();

function limpiarGraficos() {
  GRAFICOS.forEach((_fn, cv) => { if (!cv.isConnected) GRAFICOS.delete(cv); });
}
function registrarGrafico(cv, fn) {
  GRAFICOS.set(cv, fn);
  fn();
}
function prepararCanvas(cv, alto) {
  const ancho = cv.parentElement.clientWidth - 2;
  const cssAlto = alto || 220;
  const dpr = window.devicePixelRatio || 1;
  const w = Math.max(80, ancho);
  cv.style.width = w + 'px';
  cv.style.height = cssAlto + 'px';
  cv.width = Math.max(2, Math.round(w * dpr));
  cv.height = Math.max(2, Math.round(cssAlto * dpr));
  const ctx = cv.getContext('2d');
  ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
  ctx.clearRect(0, 0, w, cssAlto);
  return ctx;
}
function dibujarDonut(cv, items, etiquetaCentro) {
  registrarGrafico(cv, () => {
    const ctx = prepararCanvas(cv, 226);
    const total = items.reduce((s, i) => s + i.valor, 0);
    const cx = cv.clientWidth / 2, cy = cv.clientHeight / 2;
    const r = Math.min(cx, cy) - 10;
    if (total <= 0) {
      ctx.strokeStyle = 'rgba(125,175,240,.2)'; ctx.lineWidth = 26;
      ctx.beginPath(); ctx.arc(cx, cy, r - 13, 0, Math.PI * 2); ctx.stroke();
      ctx.fillStyle = '#6e84a6'; ctx.font = '13px Segoe UI'; ctx.textAlign = 'center';
      ctx.fillText('Sin datos', cx, cy);
      return;
    }
    let ang = -Math.PI / 2;
    items.forEach(item => {
      const porcion = (item.valor / total) * Math.PI * 2;
      ctx.fillStyle = item.color;
      ctx.beginPath();
      ctx.moveTo(cx, cy);
      ctx.arc(cx, cy, r, ang, ang + porcion);
      ctx.closePath();
      ctx.fill();
      ctx.strokeStyle = 'rgba(9,17,31,.85)'; ctx.lineWidth = 2;
      ctx.stroke();
      ang += porcion;
    });
    ctx.shadowColor = 'rgba(34,211,238,.35)'; ctx.shadowBlur = 22;
    ctx.fillStyle = '#0c1728';
    ctx.beginPath(); ctx.arc(cx, cy, r - 27, 0, Math.PI * 2); ctx.fill();
    ctx.shadowBlur = 0;
    ctx.fillStyle = '#eaf1ff'; ctx.font = 'bold 22px Segoe UI'; ctx.textAlign = 'center';
    ctx.fillText(String(total), cx, cy - 2);
    ctx.font = '11px Segoe UI'; ctx.fillStyle = '#8ea3c4';
    ctx.fillText(etiquetaCentro || 'total', cx, cy + 17);
  });
}
function dibujarBarras(cv, etiquetas, valores, colores, unidades) {
  registrarGrafico(cv, () => {
    const ctx = prepararCanvas(cv, 236);
    const w = cv.clientWidth, h = cv.clientHeight;
    const margen = 36;
    const max = Math.max(1, ...valores);
    const n = etiquetas.length;
    const anchoBarra = Math.min(58, (w - margen - 10) / n * 0.58);
    const base = h - 26;
    ctx.strokeStyle = 'rgba(125,175,240,.12)'; ctx.lineWidth = 1;
    for (let g = 0; g <= 3; g++) {
      const y = 12 + (base - 12) * g / 3;
      ctx.beginPath(); ctx.moveTo(margen - 6, y); ctx.lineTo(w - 6, y); ctx.stroke();
    }
    ctx.textAlign = 'center';
    for (let i = 0; i < n; i++) {
      const altoBarra = Math.max(4, (valores[i] / max) * (base - 26));
      const x = margen + (w - margen - 10) / n * (i + 0.5);
      const y = base - altoBarra;
      const g = ctx.createLinearGradient(0, y, 0, base);
      const color = colores[i] || '#22d3ee';
      g.addColorStop(0, color);
      g.addColorStop(1, color + '33');
      ctx.fillStyle = g;
      ctx.beginPath();
      ctx.roundRect(x - anchoBarra / 2, y, anchoBarra, altoBarra, [7, 7, 2, 2]);
      ctx.fill();
      ctx.fillStyle = '#eaf1ff'; ctx.font = 'bold 12px Segoe UI';
      ctx.fillText((unidades || '') + Number(valores[i]).toLocaleString('es-PE',
        { maximumFractionDigits: 0 }), x, y - 8);
      ctx.fillStyle = '#8ea3c4'; ctx.font = '11px Segoe UI';
      ctx.fillText(etiquetas[i], x, base + 16);
    }
  });
}
// Grafico de area/linea premium: rejilla, relleno degradado, brillo y puntos.
function dibujarArea(cv, etiquetas, valores, color) {
  registrarGrafico(cv, () => {
    const ctx = prepararCanvas(cv, 236);
    const w = cv.clientWidth, h = cv.clientHeight;
    const izq = 30, der = 12, arriba = 16, base = h - 30;
    const max = Math.max(1, ...valores);
    const n = valores.length;
    const paso = (w - izq - der) / Math.max(1, n - 1);
    const py = v => base - (v / max) * (base - arriba);

    ctx.strokeStyle = 'rgba(125,175,240,.1)'; ctx.lineWidth = 1;
    for (let g = 0; g <= 4; g++) {
      const y = arriba + (base - arriba) * g / 4;
      ctx.beginPath(); ctx.moveTo(izq, y); ctx.lineTo(w - der, y); ctx.stroke();
    }
    const area = ctx.createLinearGradient(0, arriba, 0, base);
    area.addColorStop(0, color + '66');
    area.addColorStop(1, color + '00');
    ctx.beginPath();
    ctx.moveTo(izq, base);
    valores.forEach((v, i) => ctx.lineTo(izq + paso * i, py(v)));
    ctx.lineTo(izq + paso * (n - 1), base);
    ctx.closePath();
    ctx.fillStyle = area; ctx.fill();

    ctx.beginPath();
    valores.forEach((v, i) => { const x = izq + paso * i; i ? ctx.lineTo(x, py(v)) : ctx.moveTo(x, py(v)); });
    ctx.strokeStyle = color; ctx.lineWidth = 2.6; ctx.lineJoin = 'round';
    ctx.shadowColor = color; ctx.shadowBlur = 16;
    ctx.stroke();
    ctx.shadowBlur = 0;

    ctx.textAlign = 'center';
    valores.forEach((v, i) => {
      const x = izq + paso * i;
      ctx.fillStyle = '#0c1728';
      ctx.beginPath(); ctx.arc(x, py(v), 5, 0, Math.PI * 2); ctx.fill();
      ctx.strokeStyle = color; ctx.lineWidth = 2.4; ctx.stroke();
      ctx.fillStyle = '#eaf1ff'; ctx.font = 'bold 11.5px Segoe UI';
      ctx.fillText(String(v), x, py(v) - 12);
      ctx.fillStyle = '#8ea3c4'; ctx.font = '11px Segoe UI';
      ctx.fillText(etiquetas[i], x, base + 18);
    });
  });
}
// Minigrafico decorativo de las tarjetas KPI.
function dibujarSparkline(cv, valores, color) {
  registrarGrafico(cv, () => {
    const ctx = prepararCanvas(cv, 42);
    const w = cv.clientWidth, h = cv.clientHeight;
    const max = Math.max(1, ...valores);
    const min = Math.min(...valores);
    const rango = Math.max(1, max - min);
    const paso = w / Math.max(1, valores.length - 1);
    const py = v => h - 6 - ((v - min) / rango) * (h - 14);
    const g = ctx.createLinearGradient(0, 0, 0, h);
    g.addColorStop(0, color + '55');
    g.addColorStop(1, color + '00');
    ctx.beginPath();
    ctx.moveTo(0, h);
    valores.forEach((v, i) => ctx.lineTo(paso * i, py(v)));
    ctx.lineTo(w, h);
    ctx.closePath();
    ctx.fillStyle = g; ctx.fill();
    ctx.beginPath();
    valores.forEach((v, i) => { const x = paso * i; i ? ctx.lineTo(x, py(v)) : ctx.moveTo(x, py(v)); });
    ctx.strokeStyle = color; ctx.lineWidth = 2; ctx.lineJoin = 'round'; ctx.stroke();
  });
}
// ---------- Utilidades de fecha y texto (soporte de las vistas) ----------
const MESES = ['Enero', 'Febrero', 'Marzo', 'Abril', 'Mayo', 'Junio',
  'Julio', 'Agosto', 'Septiembre', 'Octubre', 'Noviembre', 'Diciembre'];
const DIAS_SEMANA = ['Lu', 'Ma', 'Mi', 'Ju', 'Vi', 'Sa', 'Do'];
// Fecha en formato YYYY-MM-DD usando hora local (las fechas del backend son
// LocalDate, por lo que no se debe usar toISOString: correria de dia).
const isoLocal = d => d.getFullYear() + '-' + String(d.getMonth() + 1).padStart(2, '0')
  + '-' + String(d.getDate()).padStart(2, '0');
const sinAcentos = s => String(s == null ? '' : s).normalize('NFD')
  .replace(/[\u0300-\u036f]/g, '').toLowerCase();
const sufijoDia = d => String(d.getDate()).padStart(2, '0');
const etiquetaDia = d => {
  const nombres = ['dom', 'lun', 'mar', 'mie', 'jue', 'vie', 'sab'];
  return nombres[d.getDay()] + ' ' + sufijoDia(d);
};
function ultimosDias(n) {
  const salida = [];
  const d = new Date();
  d.setHours(12, 0, 0, 0);
  for (let i = n - 1; i >= 0; i--) {
    const x = new Date(d.getTime());
    x.setDate(d.getDate() - i);
    salida.push(x);
  }
  return salida;
}

// ---------- Sesion / navegacion ----------
function construirNav() {
  if (!SO.permisos) return;
  const permisos = SO.permisos;
  const entrada = (id, emoji, etiqueta) => {
    if (!ventanaVisible(id)) return '';
    return `<button class="nav-item" data-pagina="${id}"><span class="emoji">${emoji}</span>`
      + `<span class="nav-txt">${esc(etiqueta)}</span>`
      + '<span class="nav-flecha" aria-hidden="true"></span></button>';
  };
  const ventanaVisible = id => {
    switch (id) {
      case 'medicos': return !!permisos.gestionarMedicos;
      case 'pagos': return !!permisos.gestionarPagos;
      case 'gastos': return !!permisos.gestionarGastos;
      case 'presupuesto': return !!permisos.gestionarPresupuesto;
      case 'usuarios': return !!permisos.gestionarUsuarios;
      default: return true;
    }
  };
  const nav = $('sidebarNav');
  nav.innerHTML =
    '<div class="nav-grupo">Principal</div>' +
    entrada('dashboard', '🏠', 'Dashboard') +
    entrada('pacientes', '👥', 'Pacientes') +
    entrada('medicos', '👨‍⚕️', 'Medicos') +
    entrada('citas', '📅', 'Citas') +
    entrada('atenciones', '🩺', 'Atenciones') +
    '<div class="nav-grupo">Finanzas</div>' +
    entrada('pagos', '💳', 'Pagos') +
    entrada('gastos', '💰', 'Gastos') +
    entrada('reportes', '📊', 'Reportes') +
    entrada('presupuesto', '💵', 'Presupuesto') +
    entrada('estadisticas', '📈', 'Estadisticas') +
    '<div class="nav-grupo">Sistema</div>' +
    entrada('usuarios', '👥', 'Usuarios') +
    entrada('clave', '🔐', 'Cambiar contrasenia') +
    '<button class="nav-item salir" id="navCerrar"><span class="emoji">🚪</span>'
    + '<span class="nav-txt">Cerrar sesion</span><span class="nav-flecha" aria-hidden="true"></span></button>';

  nav.querySelectorAll('.nav-item').forEach(btn => {
    const pag = btn.dataset.pagina;
    btn.addEventListener('click', () => {
      if (pag) navegar(pag);
      else cerrarSesion();
    });
  });
  marcarActivo();
}
function marcarActivo() {
  document.querySelectorAll('.nav-item').forEach(b => b.classList.remove('activo'));
  const act = document.querySelector(`.nav-item[data-pagina="${SO.pagina}"]`);
  if (act) act.classList.add('activo');
}
function navegar(pagina) {
  SO.pagina = pagina;
  const titulos = {
    dashboard: 'Dashboard', pacientes: 'Modulo de Pacientes', medicos: 'Modulo de Medicos',
    citas: 'Modulo de Citas', atenciones: 'Modulo de Atenciones', pagos: 'Modulo de Pagos',
    gastos: 'Modulo de Gastos', reportes: 'Reportes y resumen economico',
    presupuesto: 'Presupuesto del periodo', estadisticas: 'Estadisticas',
    usuarios: 'Gestion de usuarios', clave: 'Cambiar mi contrasena'
  };
  $('tituloPagina').textContent = titulos[pagina] || 'Dashboard';
  marcarActivo();
  $('sidebar').classList.remove('abierta');
  $('backdrop').hidden = true;
  cerrarPaneles();
  renderPagina();
}
async function renderPagina() {
  const contenido = $('contenido');
  limpiarGraficos();
  contenido.innerHTML = '<div class="cargando"><div class="spinner"></div><span>Cargando...</span></div>';
  try {
    switch (SO.pagina) {
      case 'dashboard': await mostrarDashboard(); break;
      case 'pacientes': await mostrarPacientes(); break;
      case 'medicos': await mostrarMedicos(); break;
      case 'citas': await mostrarCitas(); break;
      case 'atenciones': await mostrarAtenciones(); break;
      case 'pagos': await mostrarPagos(); break;
      case 'gastos': await mostrarGastos(); break;
      case 'reportes': await mostrarReportes(); break;
      case 'presupuesto': await mostrarPresupuesto(); break;
      case 'estadisticas': await mostrarEstadisticas(); break;
      case 'usuarios': await mostrarUsuarios(); break;
      case 'clave': mostrarClave(); break;
      default: contenido.innerHTML = '<div class="cargando">Pagina no encontrada.</div>';
    }
  } catch (e) {
    contenido.innerHTML = `<div class="tarjeta"><div class="mensaje-form" style="color:var(--rojo)">${esc(e.message)}</div></div>`;
  }
  // Tras cada render se releen los avisos: si se registro una cita, pago,
  // paciente o atencion nueva, su id no esta en el mapa de leidas y la
  // campana vuelve a mostrar el contador sin romper nada de lo existente.
  refrescarContador(true);
}

// ---------- DASHBOARD ----------
// La atencion no trae especialidad, asi que la distribucion por especialidad se
// deduce del medico (las citas si la traen). No se toca el backend: solo se
// cruza informacion que ya devolvian /api/citas y /api/atenciones.
const CATEGORIAS_ESP = [
  { etiqueta: 'Medicina General', claves: ['medicina general', 'medico general', 'general'], color: '#22d3ee' },
  { etiqueta: 'Pediatria', claves: ['pediatr'], color: '#3b82f6' },
  { etiqueta: 'Cardiologia', claves: ['cardiolog'], color: '#f472b6' },
  { etiqueta: 'Ginecologia', claves: ['ginecolog', 'obstetr'], color: '#a78bfa' },
  { etiqueta: 'Otras', claves: [], color: '#64748b' }
];
const CATEGORIA_OTRAS = CATEGORIAS_ESP[CATEGORIAS_ESP.length - 1];
const CAL = { anio: new Date().getFullYear(), mes: new Date().getMonth(), sel: isoLocal(new Date()) };
let CAL_CITAS = [];

function buscarCategoria(texto) {
  const t = sinAcentos(texto);
  for (const c of CATEGORIAS_ESP) {
    for (const k of c.claves) {
      if (t.indexOf(k) >= 0) return c;
    }
  }
  return CATEGORIA_OTRAS;
}
function porcentajeDelta(actual, anterior) {
  if (!anterior) return actual > 0 ? 100 : 0;
  return Math.round((actual - anterior) / anterior * 100);
}
function textoDelta(porcentaje) {
  return (porcentaje > 0 ? '+' : '') + porcentaje + '% vs. semana anterior';
}

async function mostrarDashboard() {
  const d = await api('/api/dashboard');
  // Endpoints que ya existian: solo se amplian los datos que se muestran.
  const [citasRes, atencionesRes] = await Promise.all([
    api('/api/citas').catch(() => null),
    api('/api/atenciones').catch(() => null)
  ]);
  const citas = (citasRes && citasRes.datos) || [];
  const atenciones = (atencionesRes && atencionesRes.datos) || [];
  const resumen = d.resumen || {};
  const proximas = d.proximas || [];
  const actividad = d.actividad || [];

  // La alerta de presupuesto entra tambien como notificacion. Si cambia, se
  // invalida la cache para que la campana refleje el estado real.
  if (NOTIF_EXCEDE !== !!resumen.excedePresupuesto) {
    NOTIF_EXCEDE = !!resumen.excedePresupuesto;
    NOTIF_DATOS = null;
  }

  const hoyIso = isoLocal(new Date());
  const ayer = new Date();
  ayer.setDate(ayer.getDate() - 1);
  const citasHoy = citas.filter(c => c.fecha === hoyIso).length;
  const citasAyer = citas.filter(c => c.fecha === isoLocal(ayer)).length;

  const dias = ultimosDias(7);
  const atPorDia = dias.map(x => atenciones.filter(a => a.fecha === isoLocal(x)).length);
  const citasPorDia = dias.map(x => citas.filter(c => c.fecha === isoLocal(x)).length);
  const semanaPrevia = ultimosDias(14).slice(0, 7)
    .map(x => atenciones.filter(a => a.fecha === isoLocal(x)).length);
  const totalSemana = atPorDia.reduce((s, v) => s + v, 0);
  const totalPrevia = semanaPrevia.reduce((s, v) => s + v, 0);
  const deltaAtenciones = porcentajeDelta(totalSemana, totalPrevia);

  // Especialidad de cada medico, deducida de las citas ya cargadas.
  const espDeMedico = {};
  citas.forEach(c => { if (c.medico && c.especialidad) espDeMedico[c.medico] = c.especialidad; });
  const conteoEsp = {};
  CATEGORIAS_ESP.forEach(c => { conteoEsp[c.etiqueta] = 0; });
  atenciones.forEach(a => {
    const cat = buscarCategoria(espDeMedico[a.medico] || '');
    conteoEsp[cat.etiqueta]++;
  });
  const itemsEsp = CATEGORIAS_ESP
    .map(c => ({ etiqueta: c.etiqueta, valor: conteoEsp[c.etiqueta], color: c.color }));
  const totalEsp = itemsEsp.reduce((s, i) => s + i.valor, 0);

  const nombre = (SO.usuario && SO.usuario.nombre) || '';
  const primero = nombre.split(' ')[0] || '';

  let html = '';
  if (resumen.excedePresupuesto) {
    html += '<div class="alerta alerta-roja">⚠️ ALERTA: Los gastos superan el presupuesto establecido.</div>';
  }

  // ---- Banner principal ----
  html += `<div class="hero">
    <div class="hero-bg"></div>
    <div class="hero-cuerpo">
      <div class="hero-texto">
        <span class="hero-eyebrow">Sistema de Gestion</span>
        <h2>¡Bienvenido${primero ? ', ' + esc(primero) : ''}! <em>Clinica Salud</em></h2>
        <p class="hero-sub">Tu salud, nuestra prioridad</p>
        <div class="hero-marca">
          <div class="marca-ico">
            <svg viewBox="0 0 48 48" width="26" height="26" aria-hidden="true">
              <path d="M24 42s-15-9-15-19.2A9.1 9.1 0 0 1 24 16.4 9.1 9.1 0 0 1 39 22.8C39 33 24 42 24 42Z" fill="url(#hgd)"/>
              <path d="M11.5 24h5l2.4-4.8 3.4 8.6 2.9-5.6 1.9 1.8h9.4" fill="none" stroke="#04121f" stroke-width="2.3" stroke-linecap="round" stroke-linejoin="round"/>
              <defs><linearGradient id="hgd" x1="0" y1="0" x2="1" y2="1">
                <stop offset="0" stop-color="#67e8f9"/><stop offset=".5" stop-color="#3b82f6"/><stop offset="1" stop-color="#0891b2"/>
              </linearGradient></defs>
            </svg>
          </div>
          <div><b>Clinica Salud</b><span>Sistema de Gestion</span></div>
        </div>
      </div>
      <div class="hero-stats">
        <div class="hero-stat"><b>${esc(d.totalPacientes)}</b><span>Pacientes</span></div>
        <div class="hero-stat"><b>${esc(citasHoy)}</b><span>Citas hoy</span></div>
        <div class="hero-stat"><b>${esc(d.totalMedicos)}</b><span>Medicos</span></div>
        <div class="hero-stat"><b>${esc(d.totalAtenciones)}</b><span>Atenciones</span></div>
      </div>
    </div>
  </div>`;

  // ---- Tarjetas KPI ----
  html += '<div class="grid-cards stagger">' +
    cardEstad('👥', 'Pacientes', d.totalPacientes, 'icono-teal',
      { pie: 'Registrados en el sistema' }) +
    cardEstad('📅', 'Citas hoy', citasHoy, 'icono-azul', {
      delta: (citasAyer ? (citasHoy - citasAyer >= 0 ? '+' : '') + porcentajeDelta(citasHoy, citasAyer) + '% vs. ayer' : 'sin citas ayer'),
      deltaTipo: citasAyer ? (citasHoy >= citasAyer ? 'sube' : 'baja') : 'neutro',
      spark: citasPorDia, sparkId: 'sparkCitas', sparkColor: '#3b82f6'
    }) +
    cardEstad('👨‍⚕️', 'Medicos', d.totalMedicos, 'icono-morado',
      { pie: 'Con especialidad asignada' }) +
    cardEstad('💰', 'Ingresos', moneda(resumen.ingresos), 'icono-verde',
      { pie: 'Acumulado del periodo' }) +
    '</div>';

  html += '<div class="grid-cards">' +
    cardEstad('🩺', 'Atenciones', d.totalAtenciones, 'icono-ambar', {
      delta: textoDelta(deltaAtenciones),
      deltaTipo: deltaAtenciones >= 0 ? 'sube' : 'baja',
      spark: atPorDia, sparkId: 'sparkAten', sparkColor: '#22d3ee'
    }) +
    cardEstad('💳', 'Pagos registrados', d.pagosRegistrados, 'icono-azul',
      { pie: 'Historico completo' }) +
    cardEstad('⚖️', 'Saldo del periodo', moneda(resumen.saldo),
      resumen.saldo < 0 ? 'icono-rojo' : 'icono-teal', { pie: 'Ingresos menos gastos' }) +
    cardEstad('💵', 'Presupuesto', moneda(resumen.presupuesto), 'icono-morado',
      { pie: 'Asignado al periodo' }) +
    '</div>';

  // ---- Graficos ----
  html += '<div class="grid-2">';
  html += `<div class="tarjeta"><h3><span class="h3-ico">📈</span>Atenciones de los ultimos 7 dias`
    + `<span class="h3-nota">${esc(totalSemana)} atenciones</span></h3>`
    + '<div class="chart-caja"><canvas id="cvDias"></canvas></div></div>';
  html += `<div class="tarjeta"><h3><span class="h3-ico">🥧</span>Distribucion por especialidad`
    + `<span class="h3-nota">${esc(totalEsp)} atenciones</span></h3><div class="chart-caja">`
    + '<canvas id="cvEsp"></canvas><div class="leyenda">' +
    itemsEsp.map(i => `<span class="item"><span class="punto" style="background:${i.color};color:${i.color}">`
      + `</span>${esc(i.etiqueta)} <b>${esc(i.valor)}</b></span>`).join('') +
    '</div></div></div>';
  html += '</div>';

  // ---- Proximas citas + calendario ----
  const filasCitas = proximas.length
    ? proximas.map(c => '<tr><td>' + hr(c.hora) + '</td><td>' + esc(c.paciente) + '</td><td>' +
        esc(c.medico) + '</td><td>' + esc(c.especialidad) + '</td><td>' + insignia(c.estado) + '</td></tr>').join('')
    : '<tr><td colspan="5" class="tabla-vacia">No hay citas programadas o confirmadas.</td></tr>';
  html += '<div class="grid-2">';
  html += `<div class="tarjeta"><h3><span class="h3-ico">📅</span>Proximas citas`
    + `<span class="h3-nota">${esc(proximas.length)} agendadas</span></h3>`
    + '<div class="tabla-envoltorio"><table class="tabla">'
    + '<thead><tr><th>Hora</th><th>Paciente</th><th>Medico</th><th>Especialidad</th><th>Estado</th></tr></thead>'
    + `<tbody>${filasCitas}</tbody></table></div></div>`;
  html += `<div class="tarjeta"><h3><span class="h3-ico">📆</span>Calendario`
    + '<span class="h3-nota" id="calResumen">-</span></h3><div class="cal">'
    + '<div class="cal-cabeza"><div class="cal-mes" id="calMes"></div><div class="cal-nav">'
    + '<button class="cal-btn" onclick="moverMes(-1)" aria-label="Mes anterior">‹</button>'
    + '<button class="cal-btn cal-hoy" onclick="irHoy()">Hoy</button>'
    + '<button class="cal-btn" onclick="moverMes(1)" aria-label="Mes siguiente">›</button>'
    + '</div></div><div class="cal-rejilla" id="calRejilla"></div>'
    + '<div class="cal-lista" id="calLista"></div></div></div>';
  html += '</div>';

  // ---- Resumen economico + actividad ----
  const etiquetas = ['Ingresos', 'Gastos', 'Presupuesto'];
  const valores = [resumen.ingresos, resumen.gastos, resumen.presupuesto];
  const colores = ['#34d399', '#f87171', '#3b82f6'];
  const filasActividad = actividad.length
    ? actividad.map(a => '<tr><td>' + fe(a.fecha) + '</td><td>' + esc(a.paciente) + '</td><td>' +
        esc(a.diagnostico) + '</td></tr>').join('')
    : '<tr><td colspan="3" class="tabla-vacia">Aun no hay atenciones.</td></tr>';
  html += '<div class="grid-2">';
  html += `<div class="tarjeta"><h3><span class="h3-ico">💵</span>Resumen economico`
    + `<span class="h3-nota">${esc(d.proximas ? '' : '')}Periodo actual</span></h3>`
    + '<div class="lista-total">' +
    filaTotal('Ingresos (pagos)', moneda(resumen.ingresos)) +
    filaTotal('Gastos activos', moneda(resumen.gastos)) +
    filaTotal('Saldo del periodo', moneda(resumen.saldo)) +
    filaTotal('Presupuesto', moneda(resumen.presupuesto)) +
    `<div class="fila destacado"><b>Saldo del presupuesto</b><b>${moneda(resumen.saldoPresupuestal)}</b></div>` +
    '</div><div class="chart-caja" style="margin-top:16px"><canvas id="cvBarras"></canvas></div></div>';
  html += `<div class="tarjeta"><h3><span class="h3-ico">🩺</span>Actividad reciente`
    + `<span class="h3-nota">Ultimas atenciones</span></h3><div class="tabla-envoltorio"><table class="tabla">`
    + '<thead><tr><th>Fecha</th><th>Paciente</th><th>Diagnostico</th></tr></thead>'
    + `<tbody>${filasActividad}</tbody></table></div></div>`;
  html += '</div>';

  $('contenido').innerHTML = html;

  // ---- Pintado de graficos ----
  CAL_CITAS = citas;
  NOTIF_DATOS = d;
  actualizarContadorNotif();
  dibujarArea($('cvDias'), dias.map(etiquetaDia), atPorDia, '#22d3ee');
  dibujarDonut($('cvEsp'), itemsEsp, 'atenciones');
  dibujarBarras($('cvBarras'), etiquetas, valores, colores, 'S/ ');
  if ($('sparkCitas')) dibujarSparkline($('sparkCitas'), citasPorDia, '#3b82f6');
  if ($('sparkAten')) dibujarSparkline($('sparkAten'), atPorDia, '#22d3ee');
  pintarCalendario();
}

// ---------- Calendario mensual ----------
function pintarCalendario() {
  const rejilla = $('calRejilla');
  if (!rejilla) return;
  $('calMes').textContent = MESES[CAL.mes] + ' ' + CAL.anio;

  const porDia = {};
  CAL_CITAS.forEach(c => {
    if (!c.fecha) return;
    (porDia[c.fecha] = porDia[c.fecha] || []).push(c);
  });

  // getDay() devuelve 0 para domingo; la semana del calendario empieza en lunes.
  const desplazamiento = (new Date(CAL.anio, CAL.mes, 1, 12).getDay() + 6) % 7;
  const diasMes = new Date(CAL.anio, CAL.mes + 1, 0, 12).getDate();
  const hoyIso = isoLocal(new Date());
  let totalMes = 0;
  let html = DIAS_SEMANA.map(d => '<div class="cal-dow">' + d + '</div>').join('');

  for (let i = 0; i < desplazamiento; i++) {
    html += '<div class="cal-dia fuera">' + (1 - desplazamiento + i) + '</div>';
  }
  for (let dia = 1; dia <= diasMes; dia++) {
    const fecha = CAL.anio + '-' + String(CAL.mes + 1).padStart(2, '0') + '-' + String(dia).padStart(2, '0');
    const delDia = porDia[fecha] || [];
    totalMes += delDia.length;
    const puntos = delDia.slice(0, 3).map(c => {
      const color = COLOR_ESTADO[c.estado] || '#64748b';
      return '<span class="cal-dot" style="background:' + color + ';color:' + color + '"></span>';
    }).join('');
    const clases = ['cal-dia'];
    if (fecha === hoyIso) clases.push('hoy');
    if (fecha === CAL.sel) clases.push('sel');
    html += '<button class="' + clases.join(' ') + '" onclick="seleccionarDia(\'' + fecha + '\')"'
      + ' title="' + esc(fecha.split('-').reverse().join('/') + ' - ' + delDia.length + ' cita(s)') + '">'
      + dia + (puntos ? '<span class="cal-dots">' + puntos + '</span>' : '') + '</button>';
  }
  const resto = (7 - (desplazamiento + diasMes) % 7) % 7;
  for (let i = 1; i <= resto; i++) {
    html += '<div class="cal-dia fuera">' + i + '</div>';
  }
  rejilla.innerHTML = html;
  if ($('calResumen')) $('calResumen').textContent = totalMes + ' cita(s) en el mes';
  pintarCitasDelDia();
}
function pintarCitasDelDia() {
  const lista = $('calLista');
  if (!lista) return;
  const delDia = CAL_CITAS.filter(c => c.fecha === CAL.sel)
    .sort((a, b) => String(a.hora || '').localeCompare(String(b.hora || '')));
  if (!delDia.length) {
    lista.innerHTML = '<div class="notif-vacio">Sin citas para el '
      + esc(CAL.sel.split('-').reverse().join('/')) + '</div>';
    return;
  }
  lista.innerHTML = delDia.map(c => '<div class="cal-item"><time>' + hr(c.hora) + '</time>'
    + '<div style="flex:1;min-width:0"><b>' + esc(c.paciente) + '</b><small>'
    + esc(c.especialidad || 'Sin especialidad') + ' · ' + esc(c.medico || '') + '</small></div>'
    + insignia(c.estado) + '</div>').join('');
}
function moverMes(delta) {
  const d = new Date(CAL.anio, CAL.mes + delta, 1, 12);
  CAL.anio = d.getFullYear();
  CAL.mes = d.getMonth();
  pintarCalendario();
}
function irHoy() {
  const d = new Date();
  CAL.anio = d.getFullYear();
  CAL.mes = d.getMonth();
  CAL.sel = isoLocal(d);
  pintarCalendario();
}
function seleccionarDia(fecha) {
  CAL.sel = fecha;
  pintarCalendario();
}

function cardEstad(emoji, etiqueta, valor, clase, op) {
  const o = op || {};
  let extra = '';
  if (o.delta) {
    const tipo = o.deltaTipo || 'sube';
    const flecha = tipo === 'baja' ? '▼' : tipo === 'neutro' ? '●' : '▲';
    extra += '<span class="card-delta ' + tipo + '">' + flecha + ' ' + esc(o.delta) + '</span>';
  }
  if (o.pie) extra += '<span class="card-pie">' + esc(o.pie) + '</span>';
  const spark = o.sparkId ? '<canvas class="card-spark" id="' + o.sparkId + '"></canvas>' : '';
  return `<div class="card-estad"><div class="icono-estad ${clase}">${emoji}</div>`
    + '<div class="datos"><div class="valor">' + esc(valor == null ? '-' : valor) + '</div>'
    + '<div class="etiqueta">' + esc(etiqueta) + '</div>' + extra + '</div>' + spark + '</div>';
}
function filaTotal(nombre, valor) {
  return `<div class="fila"><span>${esc(nombre)}</span><b>${esc(valor)}</b></div>`;
}

// ---------- PACIENTES ----------
let listaPacientes = [];
async function mostrarPacientes() {
  listaPacientes = [];
  const d = await api('/api/pacientes');
  listaPacientes = d.datos || [];
  const p = SO.permisos;
  let html = encabezadoModulo('Pacientes',
    'Registro y administracion de pacientes (RF-01)',
    p.registrarPaciente ? '<button class="btn btn-primario" onclick="nuevoPaciente()">➕ Nuevo paciente</button>' : '');
  html += `<div class="toolbar">
    <div class="buscador"><svg viewBox="0 0 24 24" width="17" height="17"><circle cx="11" cy="11" r="6" fill="none" stroke="currentColor" stroke-width="1.8"/><path d="M16 16l4 4" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/></svg>
      <input id="buscarPacientes" placeholder="Buscar por nombre o DNI..."></div>
  </div>`;
  html += tablaPacientes(p);
  $('contenido').innerHTML = html;
  $('buscarPacientes').addEventListener('input', e => filtrarPacientes(e.target.value));
}
function filtrarPacientes(texto) {
  const q = (texto || '').toLowerCase();
  const filas = q
    ? listaPacientes.filter(x => (x.nombre || '').toLowerCase().includes(q) || (x.dni || '').includes(q))
    : listaPacientes;
  $('cuerpoPacientes').innerHTML = filasPacientes(filas);
}
function encabezadoModulo(titulo, desc, botonesHtml) {
  return `<div class="encabezado-modulo"><div><h2>${esc(titulo)}</h2><p>${esc(desc)}</p></div>
    <div class="acciones-modulo">${botonesHtml}</div></div>`;
}
function tablaPacientes(p) {
  return `<div class="tarjeta"><div class="tabla-envoltorio"><table class="tabla">
    <thead><tr><th>ID</th><th>Nombre</th><th>DNI</th><th>Telefono</th><th>Correo</th><th>Estado</th><th>Acciones</th></tr></thead>
    <tbody id="cuerpoPacientes">${filasPacientes(listaPacientes, p)}</tbody></table></div></div>`;
}
function filasPacientes(lista, p) {
  if (!lista.length) return '<tr><td colspan="7" class="tabla-vacia">No hay pacientes registrados.</td></tr>';
  return lista.map(x => `<tr>
    <td>${x.id}</td><td>${esc(x.nombre)}</td><td>${esc(x.dni)}</td><td>${esc(x.telefono)}</td><td>${esc(x.correo)}</td>
    <td>${insignia(x.estado)}</td>
    <td><div class="acciones">
      <button class="btn btn-neutro btn-sm" onclick="verPaciente(${x.id})">Ver</button>
      ${p && p.actualizarPaciente ? '<button class="btn btn-azul btn-sm" onclick="editarPaciente(' + x.id + ')">Editar</button>' : ''}
      ${p && p.actualizarPaciente ? '<button class="btn btn-secundario btn-sm" onclick="estadoPaciente(' + x.id + ')">Estado</button>' : ''}
    </div></td></tr>`).join('');
}
function nuevoPaciente() {
  abrirModal('Nuevo paciente', `
    <form id="formPaciente" class="formulario">
      <div class="form-campo ancho-completo"><label>Nombre y apellidos</label><input name="nombre" required placeholder="Nombre del paciente"></div>
      <div class="form-campo"><label>DNI (8 digitos)</label><input name="dni" required maxlength="8" inputmode="numeric" placeholder="00000000"></div>
      <div class="form-campo"><label>Telefono (opcional)</label><input name="telefono" placeholder="Telefono"></div>
      <div class="form-campo"><label>Correo (opcional)</label><input name="correo" type="email" placeholder="correo@ejemplo.com"></div>
      <div class="form-campo ancho-completo"><label>Direccion (opcional)</label><input name="direccion" placeholder="Direccion"></div>
    </form>`, 'Guardar', async () => {
    try {
      const datos = valorForm('formPaciente');
      if (!datos.nombre || !datos.dni) throw new Error('Nombre y DNI son obligatorios.');
      await api('/api/pacientes', { metodo: 'POST', body: { nombre: datos.nombre, dni: datos.dni, telefono: datos.telefono, correo: datos.correo, direccion: datos.direccion } });
      toast('Paciente registrado.'); cerrarModal(); renderPagina();
    } catch (e) { toast(e.message, 'error'); }
  });
}
function verPaciente(id) {
  const x = listaPacientes.find(v => v.id === id) || { id };
  abrirModal('Paciente #' + id, `<div class="detalle">
    ${detFila('Nombre', x.nombre)}${detFila('DNI', x.dni)}${detFila('Telefono', x.telefono)}
    ${detFila('Correo', x.correo)}${detFila('Direccion', x.direccion)}
    ${detFila('Fecha de registro', fe(x.fechaRegistro))}${detFila('Estado', x.estado)}</div>`, 'Cerrar', cerrarModal);
}
function editarPaciente(id) {
  const x = listaPacientes.find(v => v.id === id);
  abrirModal('Editar paciente - ' + x.nombre, `
    <form id="formPaciente" class="formulario">
      <div class="form-campo ancho-completo"><label>Nombre</label><input name="nombre" required value="${esc(x.nombre)}"></div>
      <div class="form-campo"><label>Telefono</label><input name="telefono" value="${esc(x.telefono)}"></div>
      <div class="form-campo"><label>Correo</label><input name="correo" type="email" value="${esc(x.correo)}"></div>
      <div class="form-campo ancho-completo"><label>Direccion</label><input name="direccion" value="${esc(x.direccion)}"></div>
    </form>`, 'Guardar', async () => {
    try {
      const datos = valorForm('formPaciente');
      await api('/api/pacientes/' + id, { metodo: 'PUT', body: datos });
      toast('Paciente actualizado.'); cerrarModal(); renderPagina();
    } catch (e) { toast(e.message, 'error'); }
  });
}
function estadoPaciente(id) {
  const x = listaPacientes.find(v => v.id === id);
  const nuevo = x.estado === 'ACTIVO' ? 'INACTIVO' : 'ACTIVO';
  modalConfirmar('Cambiar estado',
    `Cambiar el estado de <b>${esc(x.nombre)}</b> a <b>${nuevo}</b>?`, async () => {
      try { await api('/api/pacientes/' + id + '/estado', { metodo: 'PUT', body: { estado: nuevo } }); toast('Estado actualizado.'); cerrarModal(); renderPagina(); }
      catch (e) { toast(e.message, 'error'); }
    });
}

// ---------- MEDICOS ----------
let listaMedicos = [];
async function mostrarMedicos() {
  if (!SO.permisos.gestionarMedicos) { cerrarSesion(); return; }
  const d = await api('/api/medicos');
  listaMedicos = d.datos || [];
  const p = SO.permisos;
  let html = encabezadoModulo('Medicos', 'Registro y administracion de medicos (RF-02)',
    '<button class="btn btn-primario" onclick="nuevoMedico()">➕ Nuevo medico</button>');
  html += `<div class="toolbar">
    <div class="buscador"><svg viewBox="0 0 24 24" width="17" height="17"><circle cx="11" cy="11" r="6" fill="none" stroke="currentColor" stroke-width="1.8"/><path d="M16 16l4 4" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/></svg>
      <input id="buscarMedicos" placeholder="Buscar por nombre, DNI o especialidad..."></div></div>`;
  html += `<div class="tarjeta"><div class="tabla-envoltorio"><table class="tabla">
    <thead><tr><th>ID</th><th>Nombre</th><th>DNI</th><th>Especialidad</th><th>Telefono</th><th>Estado</th><th>Acciones</th></tr></thead>
    <tbody id="cuerpoMedicos">${filasMedicos(listaMedicos)}</tbody></table></div></div>`;
  $('contenido').innerHTML = html;
  $('buscarMedicos').addEventListener('input', e => {
    const q = (e.target.value || '').toLowerCase();
    $('cuerpoMedicos').innerHTML = filasMedicos(listaMedicos.filter(x =>
      (x.nombre || '').toLowerCase().includes(q) || (x.dni || '').includes(q) ||
      (x.especialidad || '').toLowerCase().includes(q)));
  });
}
function filasMedicos(lista) {
  if (!lista.length) return '<tr><td colspan="7" class="tabla-vacia">No hay medicos registrados.</td></tr>';
  return lista.map(x => `<tr>
    <td>${x.id}</td><td>${esc(x.nombre)}</td><td>${esc(x.dni)}</td><td>${esc(x.especialidad)}</td>
    <td>${esc(x.telefono)}</td><td>${insignia(x.estado)}</td>
    <td><div class="acciones">
      <button class="btn btn-neutro btn-sm" onclick="verMedico(${x.id})">Ver</button>
      <button class="btn btn-azul btn-sm" onclick="editarMedico(${x.id})">Editar</button>
      <button class="btn btn-secundario btn-sm" onclick="estadoMedico(${x.id})">Estado</button>
    </div></td></tr>`).join('');
}
function nuevoMedico() {
  abrirModal('Nuevo medico', `
    <form id="formMedico" class="formulario">
      <div class="form-campo ancho-completo"><label>Nombre y apellidos</label><input name="nombre" required></div>
      <div class="form-campo"><label>DNI (8 digitos)</label><input name="dni" required maxlength="8" inputmode="numeric" placeholder="00000000"></div>
      <div class="form-campo"><label>Especialidad</label><input name="especialidad" required></div>
      <div class="form-campo"><label>Telefono (opcional)</label><input name="telefono"></div>
    </form>`, 'Guardar', async () => {
    try {
      const datos = valorForm('formMedico');
      await api('/api/medicos', { metodo: 'POST', body: datos });
      toast('Medico registrado.'); cerrarModal(); renderPagina();
    } catch (e) { toast(e.message, 'error'); }
  });
}
function verMedico(id) {
  const x = listaMedicos.find(v => v.id === id) || {};
  abrirModal('Medico #' + id, `<div class="detalle">
    ${detFila('Nombre', x.nombre)}${detFila('DNI', x.dni)}${detFila('Especialidad', x.especialidad)}
    ${detFila('Telefono', x.telefono)}${detFila('Fecha de registro', fe(x.fechaRegistro))}${detFila('Estado', x.estado)}</div>`,
    'Cerrar', cerrarModal);
}
function editarMedico(id) {
  const x = listaMedicos.find(v => v.id === id);
  abrirModal('Editar medico - ' + x.nombre, `
    <form id="formMedico" class="formulario">
      <div class="form-campo ancho-completo"><label>Nombre</label><input name="nombre" required value="${esc(x.nombre)}"></div>
      <div class="form-campo"><label>Especialidad</label><input name="especialidad" required value="${esc(x.especialidad)}"></div>
      <div class="form-campo"><label>Telefono</label><input name="telefono" value="${esc(x.telefono)}"></div>
    </form>`, 'Guardar', async () => {
    try { await api('/api/medicos/' + id, { metodo: 'PUT', body: valorForm('formMedico') }); toast('Medico actualizado.'); cerrarModal(); renderPagina(); }
    catch (e) { toast(e.message, 'error'); }
  });
}
function estadoMedico(id) {
  const x = listaMedicos.find(v => v.id === id);
  const nuevo = x.estado === 'ACTIVO' ? 'INACTIVO' : 'ACTIVO';
  modalConfirmar('Cambiar estado', `Cambiar el estado de <b>${esc(x.nombre)}</b> a <b>${nuevo}</b>?`, async () => {
    try { await api('/api/medicos/' + id + '/estado', { metodo: 'PUT', body: { estado: nuevo } }); toast('Estado actualizado.'); cerrarModal(); renderPagina(); }
    catch (e) { toast(e.message, 'error'); }
  });
}

// ---------- CITAS ----------
let listaCitas = [];
let listaActivosPacientes = [];
let listaActivosMedicos = [];
async function mostrarCitas() {
  const d = await api('/api/citas');
  listaCitas = d.datos || [];
  const p = SO.permisos;
  let html = encabezadoModulo('Citas', 'Agenda de citas y atenciones (RF-03)',
    p.programarCita ? '<button class="btn btn-primario" onclick="nuevaCita()">➕ Nueva cita</button>' : '');
  html += `<div class="toolbar">
    <div class="buscador"><svg viewBox="0 0 24 24" width="17" height="17"><circle cx="11" cy="11" r="6" fill="none" stroke="currentColor" stroke-width="1.8"/><path d="M16 16l4 4" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/></svg>
      <input id="buscarCitas" placeholder="Buscar por paciente o medico..."></div>
    <select id="filtroEstadoCita">
      <option value="">Todos los estados</option>
      <option value="PROGRAMADA">Programada</option><option value="CONFIRMADA">Confirmada</option>
      <option value="CANCELADA">Cancelada</option><option value="ATENDIDA">Atendida</option>
    </select>
  </div>`;
  html += `<div class="tarjeta"><div class="tabla-envoltorio"><table class="tabla">
    <thead><tr><th>ID</th><th>Fecha</th><th>Hora</th><th>Paciente</th><th>Medico</th><th>Especialidad</th><th>Estado</th><th>Observacion</th><th>Acciones</th></tr></thead>
    <tbody id="cuerpoCitas">${filasCitas(listaCitas, p)}</tbody></table></div></div>`;
  $('contenido').innerHTML = html;
  const filtrar = () => {
    const q = ($('buscarCitas').value || '').toLowerCase();
    const est = $('filtroEstadoCita').value;
    $('cuerpoCitas').innerHTML = filasCitas(listaCitas.filter(x =>
      (!est || x.estado === est) &&
      ((x.paciente || '').toLowerCase().includes(q) || (x.medico || '').toLowerCase().includes(q))), p);
  };
  $('buscarCitas').addEventListener('input', filtrar);
  $('filtroEstadoCita').addEventListener('change', filtrar);
}
function filasCitas(lista, p) {
  if (!lista.length) return '<tr><td colspan="9" class="tabla-vacia">No hay citas registradas.</td></tr>';
  return lista.map(x => `<tr>
    <td>${x.id}</td><td>${fe(x.fecha)}</td><td>${hr(x.hora)}</td>
    <td>${esc(x.paciente)}</td><td>${esc(x.medico)}</td><td>${esc(x.especialidad)}</td>
    <td>${insignia(x.estado)}</td><td>${esc(x.observacion)}</td>
    <td><div class="acciones">
      <button class="btn btn-neutro btn-sm" onclick="verCita(${x.id})">Ver</button>
      ${p && p.modificarCita ? '<button class="btn btn-azul btn-sm" onclick="modificarCita(' + x.id + ')">Modif.</button>' : ''}
      <button class="btn btn-verde btn-sm" onclick="confirmarCita(${x.id})">Confirmar</button>
      <button class="btn btn-rojo btn-sm" onclick="cancelarCita(${x.id})">Cancelar</button>
    </div></td></tr>`).join('');
}
async function opcionesDe(valorBuscado) {
  return Promise.all([
    api('/api/pacientes').then(d => listaActivosPacientes = (d.datos || []).filter(v => v.estado === 'ACTIVO')),
    api('/api/medicos').then(d => listaActivosMedicos = (d.datos || []).filter(v => v.estado === 'ACTIVO'))
  ]);
}
function opcionesHtml() {
  const pac = listaActivosPacientes.map(v => `<option value="${v.id}">id ${v.id} - ${esc(v.nombre)}</option>`).join('');
  const med = listaActivosMedicos.map(v => `<option value="${v.id}">id ${v.id} - ${esc(v.nombre)} (${esc(v.especialidad)})</option>`).join('');
  return `<select name="idPaciente" required><option value="">-- Seleccionar paciente --</option>${pac}</select>
    <select name="idMedico" required><option value="">-- Seleccionar medico --</option>${med}</select>`;
}
async function nuevaCita() {
  try { await opcionesDe(); } catch (e) { toast(e.message, 'error'); return; }
  if (!listaActivosPacientes.length) { toast('No hay pacientes activos disponibles.', 'error'); return; }
  if (!listaActivosMedicos.length) { toast('No hay medicos activos disponibles.', 'error'); return; }
  abrirModal('Nueva cita', `
    <form id="formCita" class="formulario">
      <div class="form-campo"><label>Paciente</label>${opcionesHtml().split('</select><select').join('</select></div><div class="form-campo"><label>Medico</label><select')}</div>
      <div class="form-campo"><label>Fecha (dd/mm/aaaa)</label><input name="fecha" type="date" value="${manana()}" required></div>
      <div class="form-campo"><label>Hora (HH:mm)</label><input name="hora" type="time" value="08:00" required></div>
      <div class="form-campo ancho-completo"><label>Observacion (opcional)</label><input name="observacion"></div>
    </form>`, 'Programar cita', async () => {
    try {
      const datos = valorForm('formCita');
      if (!datos.idPaciente || !datos.idMedico) throw new Error('Seleccione paciente y medico.');
      await api('/api/citas', { metodo: 'POST', body: { idPaciente: Number(datos.idPaciente), idMedico: Number(datos.idMedico), fecha: datos.fecha, hora: datos.hora, observacion: datos.observacion } });
      toast('Cita programada.'); cerrarModal(); renderPagina();
    } catch (e) { toast(e.message, 'error'); }
  });
}
function verCita(id) {
  const x = listaCitas.find(v => v.id === id) || {};
  abrirModal('Cita #' + id, `<div class="detalle">
    ${detFila('Paciente', x.paciente)}${detFila('Medico', x.medico)}${detFila('Especialidad', x.especialidad)}
    ${detFila('Fecha', fe(x.fecha))}${detFila('Hora', hr(x.hora))}${detFila('Estado', x.estado)}
    ${detFila('Observacion', x.observacion)}</div>`, 'Cerrar', cerrarModal);
}
function modificarCita(id) {
  const x = listaCitas.find(v => v.id === id) || {};
  abrirModal('Modificar cita #' + id, `
    <form id="formCita" class="formulario">
      <div class="form-campo"><label>Fecha (dd/mm/aaaa)</label><input name="fecha" type="date" value="${x.fecha}" required></div>
      <div class="form-campo"><label>Hora (HH:mm)</label><input name="hora" type="time" value="${x.hora}" required></div>
      <div class="form-campo"><label>Estado</label>
        <select name="estado"><option value="PROGRAMADA" ${x.estado === 'PROGRAMADA' ? 'selected' : ''}>PROGRAMADA</option>
        <option value="CONFIRMADA" ${x.estado === 'CONFIRMADA' ? 'selected' : ''}>CONFIRMADA</option></select></div>
      <div class="form-campo ancho-completo"><label>Observacion</label><input name="observacion" value="${esc(x.observacion)}"></div>
    </form>`, 'Guardar', async () => {
    try {
      const datos = valorForm('formCita');
      await api('/api/citas/' + id, { metodo: 'PUT', body: { fecha: datos.fecha, hora: datos.hora, estado: datos.estado, observacion: datos.observacion } });
      toast('Cita modificada.'); cerrarModal(); renderPagina();
    } catch (e) { toast(e.message, 'error'); }
  });
}
function confirmarCita(id) {
  const x = listaCitas.find(v => v.id === id) || {};
  modalConfirmar('Confirmar cita', `Confirmar la cita <b>#${id}</b> de <b>${esc(x.paciente)}</b>?`, async () => {
    try { await api('/api/citas/' + id + '/confirmar', { metodo: 'PUT' }); toast('Cita confirmada.'); cerrarModal(); renderPagina(); }
    catch (e) { toast(e.message, 'error'); }
  }, 'Confirmar');
}
function cancelarCita(id) {
  const x = listaCitas.find(v => v.id === id) || {};
  modalConfirmar('Cancelar cita',
    `Cancelar la cita <b>#${id}</b> de <b>${esc(x.paciente)}</b>? Su horario quedara disponible.`, async () => {
      try { await api('/api/citas/' + id + '/cancelar', { metodo: 'PUT' }); toast('Cita cancelada.'); cerrarModal(); renderPagina(); }
      catch (e) { toast(e.message, 'error'); }
    }, 'Cancelar cita');
}

// ---------- ATENCIONES ----------
let listaAtenciones = [];
let listaCitasPendientes = [];
async function mostrarAtenciones() {
  const d = await api('/api/atenciones');
  listaAtenciones = d.datos || [];
  const p = SO.permisos;
  let html = encabezadoModulo('Atenciones', 'Registro y seguimiento de atenciones medicas (RF-04, RF-09)',
    (p.registrarAtencion ? '<button class="btn btn-primario" onclick="registrarAtencion()">➕ Registrar atencion</button>' : '') +
    '<button class="btn btn-neutro" onclick="historialPaciente()">📋 Historial por paciente</button>');
  html += `<div class="tarjeta"><div class="tabla-envoltorio"><table class="tabla">
    <thead><tr><th>ID</th><th>Fecha</th><th>Paciente</th><th>Medico</th><th>Diagnostico</th><th>Acciones</th></tr></thead>
    <tbody id="cuerpoAtenciones">${filasAtenciones(listaAtenciones)}</tbody></table></div></div>`;
  $('contenido').innerHTML = html;
}
function filasAtenciones(lista) {
  if (!lista.length) return '<tr><td colspan="6" class="tabla-vacia">No hay atenciones registradas.</td></tr>';
  return lista.map(x => `<tr>
    <td>${x.id}</td><td>${fe(x.fecha)}</td><td>${esc(x.paciente)}</td><td>${esc(x.medico)}</td><td>${esc(x.diagnostico)}</td>
    <td><button class="btn btn-neutro btn-sm" onclick="verAtencion(${x.id})">Ver</button></td></tr>`).join('');
}
async function registrarAtencion() {
  try {
    const d = await api('/api/citas');
    listaCitasPendientes = (d.datos || []).filter(v => v.estado === 'PROGRAMADA' || v.estado === 'CONFIRMADA');
  } catch (e) { toast(e.message, 'error'); return; }
  if (!listaCitasPendientes.length) { toast('No hay citas programadas o confirmadas para atender.', 'error'); return; }
  const citasHtml = listaCitasPendientes.map(v =>
    `<option value="${v.id}">Cita ${v.id} - ${esc(v.paciente)} - ${esc(v.medico)} - ${fe(v.fecha)} ${hr(v.hora)} [${v.estado}]</option>`).join('');
  abrirModal('Registrar atencion', `
    <form id="formAtencion" class="formulario">
      <div class="form-campo ancho-completo"><label>Cita a atender</label><select name="idCita" required>${citasHtml}</select></div>
      <div class="form-campo ancho-completo"><label>Diagnostico</label><textarea name="diagnostico" rows="2" required></textarea></div>
      <div class="form-campo ancho-completo"><label>Observaciones (opcional)</label><textarea name="observaciones" rows="2"></textarea></div>
      <div class="form-campo"><label>Fecha de atencion (dd/mm/aaaa)</label><input name="fecha" type="date" value="${hoy()}" required></div>
    </form>`, 'Registrar', async () => {
    try {
      const datos = valorForm('formAtencion');
      await api('/api/atenciones', { metodo: 'POST', body: { idCita: Number(datos.idCita), diagnostico: datos.diagnostico, observaciones: datos.observaciones, fecha: datos.fecha } });
      toast('Atencion registrada.'); cerrarModal(); renderPagina();
    } catch (e) { toast(e.message, 'error'); }
  });
}
function verAtencion(id) {
  const x = listaAtenciones.find(v => v.id === id) || {};
  abrirModal('Atencion #' + id, `<div class="detalle">
    ${detFila('Fecha', fe(x.fecha))}${detFila('Paciente', x.paciente)}${detFila('Medico', x.medico)}
    ${detFila('Diagnostico', x.diagnostico)}</div>`, 'Cerrar', cerrarModal);
}
async function historialPaciente() {
  let listaActivos;
  try { listaActivos = (await api('/api/pacientes')).datos.filter(v => v.estado === 'ACTIVO'); } catch (e) { toast(e.message, 'error'); return; }
  if (!listaActivos.length) { toast('No hay pacientes activos.', 'error'); return; }
  const html = `<form id="formHistorial"><div class="form-campo">
    <label>Paciente</label><select name="idPaciente">${listaActivos.map(v => `<option value="${v.id}">id ${v.id} - ${esc(v.nombre)}</option>`).join('')}</select></div></form>`;
  abrirModal('Historial de atenciones por paciente', html, 'Ver historial', async () => {
    try {
      const id = Number(valorForm('formHistorial').idPaciente);
      const filas = (await api('/api/atenciones/paciente/' + id)).datos || [];
      const nombre = listaActivos.find(v => v.id === id);
      const cuerpo = filas.length
        ? filas.map(a => `<div class="detalle"><div class="fila"><b>Fecha</b><span>${fe(a.fecha)}</span></div>
            <div class="fila"><b>Medico</b><span>${esc(a.medico)}</span></div>
            <div class="fila"><b>Diagnostico</b><span>${esc(a.diagnostico)}</span></div></div><br>`).join('')
        : '<div class="mensaje-form">El paciente no tiene atenciones registradas.</div>';
      abrirModal('Historial de: ' + (nombre ? nombre.nombre : 'Paciente ' + id), cuerpo, 'Cerrar', cerrarModal);
    } catch (e) { toast(e.message, 'error'); }
  });
}

// ---------- PAGOS ----------
let listaPagos = [];
async function mostrarPagos() {
  if (!SO.permisos.gestionarPagos) { cerrarSesion(); return; }
  const d = await api('/api/pagos');
  listaPagos = d.datos || [];
  let html = encabezadoModulo('Pagos', 'Registro de pagos recibidos (RF-06)',
    '<button class="btn btn-primario" onclick="nuevoPago()">➕ Nuevo pago</button>' +
    '<button class="btn btn-neutro" onclick="resumenEconomico()">💵 Resumen economico</button>');
  html += `<div class="tarjeta"><div class="tabla-envoltorio"><table class="tabla">
    <thead><tr><th>ID</th><th>Monto</th><th>Fecha</th><th>Metodo</th><th>Id atencion</th></tr></thead>
    <tbody id="cuerpoPagos">${filasPagos(listaPagos)}</tbody></table></div></div>`;
  $('contenido').innerHTML = html;
}
function filasPagos(lista) {
  if (!lista.length) return '<tr><td colspan="5" class="tabla-vacia">No hay pagos registrados.</td></tr>';
  return lista.map(x => `<tr><td>${x.id}</td><td><b>${moneda(x.monto)}</b></td><td>${fe(x.fecha)}</td>
    <td>${esc(x.metodo)}</td><td>${x.idAtencion != null ? x.idAtencion : '-'}</td></tr>`).join('');
}
async function nuevoPago() {
  let idsAtencion = [];
  try { idsAtencion = (await api('/api/atenciones')).datos || []; } catch (e) { toast(e.message, 'error'); return; }
  const atHtml = idsAtencion.map(v => `<option value="${v.id}">Atencion ${v.id} - ${esc(v.paciente)} (${fe(v.fecha)})</option>`).join('');
  abrirModal('Nuevo pago', `
    <form id="formPago" class="formulario">
      <div class="form-campo"><label>Monto (mayor que cero)</label><input name="monto" type="number" step="0.01" min="0.01" required></div>
      <div class="form-campo"><label>Fecha (dd/mm/aaaa)</label><input name="fecha" type="date" value="${hoy()}" required></div>
      <div class="form-campo"><label>Metodo de pago</label>
        <select name="metodo"><option>EFECTIVO</option><option>TARJETA</option><option>TRANSFERENCIA</option><option>OTRO</option></select></div>
      <div class="form-campo"><label>Id de atencion (0 = ninguno)</label>
        <select name="idAtencion"><option value="">0 - Ninguno</option>${atHtml}</select></div>
    </form>`, 'Registrar pago', async () => {
    try {
      const datos = valorForm('formPago');
      await api('/api/pagos', { metodo: 'POST', body: { monto: Number(datos.monto), fecha: datos.fecha, metodo: datos.metodo, idAtencion: datos.idAtencion ? Number(datos.idAtencion) : null } });
      toast('Pago registrado.'); cerrarModal(); renderPagina();
    } catch (e) { toast(e.message, 'error'); }
  });
}
async function resumenEconomico() {
  try {
    const r = await api('/api/reportes/resumen');
    let html = '<div class="lista-total">' +
      filaTotal('Ingresos (pagos)', moneda(r.ingresos)) +
      filaTotal('Gastos activos', moneda(r.gastos)) +
      filaTotal('Saldo del periodo', moneda(r.saldo)) +
      filaTotal('Presupuesto', moneda(r.presupuesto)) +
      `<div class="fila destacado"><b>Saldo del presupuesto</b><b>${moneda(r.saldoPresupuestal)}</b></div></div>`;
    if (r.excedePresupuesto) html = '<div class="alerta alerta-roja">ALERTA: Los gastos superan el presupuesto establecido.</div>' + html;
    abrirModal('Resumen economico', html, 'Cerrar', cerrarModal);
  } catch (e) { toast(e.message, 'error'); }
}

// ---------- GASTOS ----------
let listaGastos = [];
async function mostrarGastos() {
  if (!SO.permisos.gestionarGastos) { cerrarSesion(); return; }
  const d = await api('/api/gastos');
  listaGastos = d.datos || [];
  let html = '';
  try {
    const res = await api('/api/reportes/resumen');
    if (res.excedePresupuesto) html += '<div class="alerta alerta-roja">⚠️ ALERTA: Los gastos superan el presupuesto establecido.</div>';
  } catch (e) { /* el modulo sigue */ }
  html += encabezadoModulo('Gastos', 'Registro de gastos y control de presupuesto (RF-07)',
    '<button class="btn btn-primario" onclick="nuevoGasto()">➕ Nuevo gasto</button>' +
    (SO.permisos.gestionarPresupuesto ? '<button class="btn btn-azul" onclick="navegar(\'presupuesto\')">💵 Presupuesto</button>' : ''));
  html += `<div class="tarjeta"><div class="tabla-envoltorio"><table class="tabla">
    <thead><tr><th>ID</th><th>Descripcion</th><th>Monto</th><th>Fecha</th><th>Categoria</th><th>Estado</th><th>Acciones</th></tr></thead>
    <tbody id="cuerpoGastos">${filasGastos(listaGastos)}</tbody></table></div></div>`;
  $('contenido').innerHTML = html;
}
function filasGastos(lista) {
  if (!lista.length) return '<tr><td colspan="7" class="tabla-vacia">No hay gastos registrados.</td></tr>';
  return lista.map(x => `<tr><td>${x.id}</td><td>${esc(x.descripcion)}</td><td><b>${moneda(x.monto)}</b></td>
    <td>${fe(x.fecha)}</td><td>${esc(x.categoria)}</td><td>${insignia(x.estado)}</td>
    <td>${x.estado === 'CONTABILIZADO' ? '<button class="btn btn-rojo btn-sm" onclick="anularGasto(' + x.id + ')">Anular</button>' : '-'}</td></tr>`).join('');
}
function nuevoGasto() {
  abrirModal('Nuevo gasto', `
    <form id="formGasto" class="formulario">
      <div class="form-campo ancho-completo"><label>Descripcion</label><input name="descripcion" required></div>
      <div class="form-campo"><label>Monto (mayor que cero)</label><input name="monto" type="number" step="0.01" min="0.01" required></div>
      <div class="form-campo"><label>Fecha (dd/mm/aaaa)</label><input name="fecha" type="date" value="${hoy()}" required></div>
      <div class="form-campo"><label>Categoria</label><select name="categoria"><option>INSUMOS</option><option>SERVICIOS</option><option>MANTENIMIENTO</option></select></div>
    </form>`, 'Registrar gasto', async () => {
    try {
      const datos = valorForm('formGasto');
      await api('/api/gastos', { metodo: 'POST', body: datos });
      toast('Gasto registrado.'); cerrarModal(); renderPagina();
    } catch (e) { toast(e.message, 'error'); }
  });
}
function anularGasto(id) {
  modalConfirmar('Anular gasto', `Anular el gasto <b>#${id}</b>? Ya no se incluira en los totales activos.`, async () => {
    try { await api('/api/gastos/' + id + '/anular', { metodo: 'PUT' }); toast('Gasto anulado.'); cerrarModal(); renderPagina(); }
    catch (e) { toast(e.message, 'error'); }
  }, 'Anular gasto');
}

// ---------- REPORTES ----------
async function mostrarReportes() {
  const res = await api('/api/reportes/resumen');
  if (!SO.periodo) { SO.periodo = { inicio: mesInicio(), fin: hoy() }; }
  let html = '';
  html += `<div class="grid-cards">
    <div class="card-estad"><div class="icono-estad icono-verde">💰</div><div><div class="valor">${moneda(res.ingresos)}</div><div class="etiqueta">Ingresos (pagos)</div></div></div>
    <div class="card-estad"><div class="icono-estad icono-rojo">📤</div><div><div class="valor">${moneda(res.gastos)}</div><div class="etiqueta">Gastos activos</div></div></div>
    <div class="card-estad"><div class="icono-estad icono-azul">⚖️</div><div><div class="valor">${moneda(res.saldo)}</div><div class="etiqueta">Saldo del periodo</div></div></div>
    <div class="card-estad"><div class="icono-estad icono-ambar">💵</div><div><div class="valor">${moneda(res.presupuesto)}</div><div class="etiqueta">Presupuesto</div></div></div>
  </div>`;
  if (res.excedePresupuesto) html += '<div class="alerta alerta-roja">⚠️ ALERTA: Los gastos superan el presupuesto establecido.</div>';

  html += `<div class="tarjeta"><h3>📆 Periodo de los reportes</h3>
    <div class="toolbar">
      <div class="form-campo"><label>Desde</label><input id="perInicio" type="date" value="${SO.periodo.inicio}"></div>
      <div class="form-campo"><label>Hasta</label><input id="perFin" type="date" value="${SO.periodo.fin}"></div>
      <button class="btn btn-primario" style="margin-top:18px" onclick="aplicarPeriodo()">Fijar periodo</button>
    </div></div>`;

  html += `<div class="tarjeta"><h3>📑 Reportes disponibles</h3>
    <div class="toolbar">
      <button class="btn btn-azul" onclick="generarReporte('ingresos-gastos')">📊 Ingresos y gastos</button>
      <button class="btn btn-azul" onclick="generarReporte('citas')">📅 Reporte de citas</button>
      <button class="btn btn-azul" onclick="generarReporte('atenciones')">🩺 Reporte de atenciones</button>
    </div>
    <div id="resultadoReporte"></div></div>`;

  html += `<div class="tarjeta"><h3>⬇️ Exportar (PDF / Excel)</h3><div class="toolbar" id="zonaExportar">${botonesExportar()}</div></div>`;
  $('contenido').innerHTML = html;
  if (SO.periodoReporte) generarReporte(SO.periodoReporte);
}
function botonesExportar() {
  const p = SO.periodo || { inicio: mesInicio(), fin: hoy() };
  const links = r => `<button class="btn btn-neutro btn-sm" onclick="descargar('${r}', 'PDF')">PDF</button>
    <button class="btn btn-neutro btn-sm" onclick="descargar('${r}', 'EXCEL')">Excel</button>`;
  return `<b>Resumen:</b> ${links('resumen')} <b>Ingresos/gastos:</b> ${links('ingresos-gastos')}
    <b>Citas:</b> ${links('citas')} <b>Atencion:</b> ${links('atenciones')} <b>Dashboard:</b> ${links('dashboard')}`;
}
function aplicarPeriodo() {
  const inicio = $('perInicio').value, fin = $('perFin').value;
  if (!inicio || !fin) { toast('Indique el inicio y el fin del periodo.', 'error'); return; }
  if (inicio > fin) { toast('La fecha de inicio debe ser anterior o igual a la de fin.', 'error'); return; }
  SO.periodo = { inicio, fin };
  $('resultadoReporte').innerHTML = '';
  toast('Periodo fijado: ' + fe(inicio) + ' a ' + fe(fin), 'info');
}
async function generarReporte(tipo) {
  if (!SO.periodo) { toast('Fije primero el periodo de fechas.', 'error'); return; }
  const zona = $('resultadoReporte');
  zona.innerHTML = '<div class="cargando"><div class="spinner"></div><span>Generando...</span></div>';
  try {
    const r = await api('/api/reportes/' + tipo, { metodo: 'POST', body: SO.periodo });
    SO.periodoReporte = tipo;
    let html = '<h3>Reporte de ' + (tipo === 'citas' ? 'citas' : tipo === 'atenciones' ? 'atenciones' : 'ingresos y gastos') +
      ' (' + fe(r.inicio) + ' - ' + fe(r.fin) + ')</h3>';
    if (tipo === 'citas') {
      html += '<p style="color:var(--suave);margin:8px 0"><b>Total de citas:</b> ' + r.totalCitas + '</p>';
      if (r.citasPorEstado) html += '<div class="leyenda" style="margin-bottom:10px">' +
        Object.keys(r.citasPorEstado).map(k => `<span class="item"><span class="punto" style="background:${COLOR_ESTADO[k] || '#14b8a6'}"></span>${NOMBRE_ESTADO[k] || k}: <b>${r.citasPorEstado[k]}</b></span>`).join('') + '</div>';
      html += tablaReporte([ 'ID', 'Fecha', 'Hora', 'Paciente', 'Medico', 'Estado' ],
        (r.detalleCitas || []).map(c => [c.id, fe(c.fecha), hr(c.hora), c.paciente, c.medico, `<span class="insignia insignia-${claseBadge(c.estado)}">${c.estado}</span>`]));
    } else if (tipo === 'atenciones') {
      html += '<p style="color:var(--suave);margin:8px 0"><b>Total de atenciones:</b> ' + r.totalAtenciones + '</p>';
      html += tablaReporte([ 'ID', 'Fecha', 'Paciente', 'Medico', 'Diagnostico' ],
        (r.detalleAtenciones || []).map(a => [a.id, fe(a.fecha), a.paciente, a.medico, a.diagnostico]));
    } else {
      html += tablaReporte([ 'ID', 'Monto', 'Fecha', 'Metodo', 'Id atencion' ],
        (r.detallePagos || []).map(p => [p.id, moneda(p.monto), fe(p.fecha), p.metodo, p.idAtencion != null ? p.idAtencion : '-']));
      html += '<br><h3>Detalle de gastos activos</h3>';
      html += tablaReporte([ 'ID', 'Descripcion', 'Monto', 'Fecha' ],
        (r.detalleGastos || []).map(g => [g.id, g.descripcion, moneda(g.monto), fe(g.fecha)]));
      if (r.gastosPorCategoria) {
        html += '<br><h3>Desglose por categoria</h3>' + tablaReporte([ 'Categoria', 'Monto' ],
          Object.keys(r.gastosPorCategoria).map(k => [k, moneda(r.gastosPorCategoria[k])]));
      }
      html += '<br><div class="lista-total">' + filaTotal('Total ingresos', moneda(r.totalIngresos)) +
        filaTotal('Total gastos (activos)', moneda(r.totalGastos)) + filaTotal('Saldo del periodo', moneda(r.saldo)) +
        filaTotal('Saldo del presupuesto', moneda(r.saldoPresupuestal)) + '</div>';
    }
    zona.innerHTML = html;
  } catch (e) { zona.innerHTML = `<div class="mensaje-form" style="color:var(--rojo)">${esc(e.message)}</div>`; }
}
function tablaReporte(columnas, filas) {
  const cab = columnas.map(c => `<th>${esc(c)}</th>`).join('');
  const cuerpo = filas.length ? filas.map(f => '<tr>' + f.map(c => `<td>${c == null ? '' : c}</td>`).join('') + '</tr>').join('')
    : '<tr><td colspan="' + columnas.length + '" class="tabla-vacia">Sin registros en el periodo.</td></tr>';
  return `<div class="tabla-envoltorio"><table class="tabla"><thead><tr>${cab}</tr></thead><tbody>${cuerpo}</tbody></table></div>`;
}
async function descargar(tipo, formato) {
  try {
    let url = `/api/exportar/${tipo}?formato=${formato}`;
    if (tipo !== 'resumen' && tipo !== 'dashboard') {
      if (!SO.periodo) throw new Error('Fije primero el periodo.');
      url += `&inicio=${SO.periodo.inicio}&fin=${SO.periodo.fin}`;
    }
    const resp = await fetch(url, { headers: { 'Authorization': 'Bearer ' + SO.token } });
    if (resp.status === 401) { cerrarSesion(); throw new Error('Sesion expirada.'); }
    if (!resp.ok) {
      const datos = await resp.json().catch(() => ({ error: 'No se pudo exportar.' }));
      throw new Error(datos.error || 'No se pudo exportar.');
    }
    const blob = await resp.blob();
    const link = document.createElement('a');
    link.href = URL.createObjectURL(blob);
    link.download = tipo + '_' + (formato.toLowerCase() === 'pdf' ? 'reporte.pdf' : 'reporte.xlsx');
    document.body.appendChild(link);
    link.click();
    link.remove();
    setTimeout(() => URL.revokeObjectURL(link.href), 4000);
  } catch (e) { toast(e.message, 'error'); }
}

// ---------- PRESUPUESTO ----------
async function mostrarPresupuesto() {
  if (!SO.permisos.gestionarPresupuesto) { cerrarSesion(); return; }
  const pre = await api('/api/presupuesto');
  const res = await api('/api/reportes/resumen');
  let html = '';
  if (res.excedePresupuesto) html += '<div class="alerta alerta-roja">⚠️ ALERTA: Los gastos superan el presupuesto establecido.</div>';
  html += `<div class="grid-cards">
    <div class="card-estad"><div class="icono-estad icono-ambar">💵</div><div><div class="valor">${moneda(res.presupuesto)}</div><div class="etiqueta">Presupuesto actual</div></div></div>
    <div class="card-estad"><div class="icono-estad icono-verde">💰</div><div><div class="valor">${moneda(res.ingresos)}</div><div class="etiqueta">Ingresos (pagos)</div></div></div>
    <div class="card-estad"><div class="icono-estad icono-rojo">📤</div><div><div class="valor">${moneda(res.gastos)}</div><div class="etiqueta">Gastos activos</div></div></div>
    <div class="card-estad"><div class="icono-estad icono-azul">⚖️</div><div><div class="valor">${moneda(res.saldoPresupuestal)}</div><div class="etiqueta">Saldo del presupuesto</div></div></div>
  </div>`;
  html += `<div class="tarjeta"><h3>Actualizar presupuesto del periodo</h3>
    <form id="formPresupuesto" class="formulario">
      <div class="form-campo ancho-completo"><label>Nuevo presupuesto (mayor que cero; 0 = no cambiar)</label>
        <input name="presupuesto" type="number" step="0.01" min="0" required></div>
    </form>
    <div class="toolbar" style="margin-top:12px"><button class="btn btn-primario" onclick="guardarPresupuesto()">Guardar presupuesto</button></div></div>`;
  $('contenido').innerHTML = html;
}
async function guardarPresupuesto() {
  try {
    const pres = Number(valorForm('formPresupuesto').presupuesto);
    if (!(pres > 0)) { toast('El presupuesto debe ser mayor que cero.', 'error'); return; }
    const r = await api('/api/presupuesto', { metodo: 'PUT', body: { presupuesto: pres } });
    toast(r.mensaje); renderPagina();
  } catch (e) { toast(e.message, 'error'); }
}

// ---------- ESTADISTICAS ----------
async function mostrarEstadisticas() {
  const d = await api('/api/dashboard');
  const p = await api('/api/reportes/ingresos-gastos', { metodo: 'POST', body: { inicio: mesInicio(), fin: hoy() } });
  let html = '';
  if (d.resumen.excedePresupuesto) html += '<div class="alerta alerta-roja">⚠️ ALERTA: Los gastos superan el presupuesto.</div>';
  html += `<div class="grid-cards">
    ${cardEstad('📅', 'Citas registradas', d.totalCitas, 'icono-azul')}
    ${cardEstad('🩺', 'Atenciones realizadas', d.totalAtenciones, 'icono-verde')}
    ${cardEstad('⚖️', 'Saldo del periodo', moneda(d.resumen.saldo), 'icono-teal')}
    ${cardEstad('💵', 'Presupuesto', moneda(d.resumen.presupuesto), 'icono-ambar')}
  </div>`;
  html += '<div class="grid-2">';
  const itemsDonut = Object.keys(d.citasPorEstado || {}).map(k => ({
    etiqueta: k, valor: d.citasPorEstado[k], color: COLOR_ESTADO[k] || '#14b8a6'
  }));
  html += `<div class="tarjeta"><h3>Citas por estado</h3><div class="chart-caja"><canvas id="cvDonut"></canvas>
    <div class="leyenda" id="leyendaDonut"></div></div></div>`;
  const catLabels = Object.keys(p.gastosPorCategoria || {});
  const catVals = catLabels.map(k => p.gastosPorCategoria[k]);
  html += `<div class="tarjeta"><h3>Gastos por categoria (mes actual)</h3><div class="chart-caja"><canvas id="cvCat"></canvas>
    <div class="leyenda" id="leyendaCat"></div></div></div>`;
  html += '</div>';
  $('contenido').innerHTML = html;

  dibujarDonut($('cvDonut'), itemsDonut.length ? itemsDonut : [{ etiqueta: 'Sin datos', valor: 1, color: '#e2e8f0' }], 'citas');
  $('leyendaDonut').innerHTML = itemsDonut.map((i, n) =>
    `<span class="item"><span class="punto" style="background:${i.color}"></span>${esc(NOMBRE_ESTADO[i.etiqueta] || i.etiqueta)}: ${i.valor}</span>`).join('');
  dibujarBarras($('cvCat'), catLabels.length ? catLabels : ['Sin datos'], catVals.length ? catVals : [0], PALETA, 'S/ ');
  $('leyendaCat').innerHTML = catLabels.map((k, n) =>
    `<span class="item"><span class="punto" style="background:${PALETA[n % PALETA.length]}"></span>${esc(k)}</span>`).join('');
}

// ---------- USUARIOS ----------
let listaUsuarios = [];
async function mostrarUsuarios() {
  if (!SO.permisos.gestionarUsuarios) { cerrarSesion(); return; }
  const d = await api('/api/usuarios');
  listaUsuarios = d.datos || [];
  let html = encabezadoModulo('Usuarios', 'Gestion de cuentas, roles y permisos del sistema',
    '<button class="btn btn-primario" onclick="nuevoUsuario()">➕ Nuevo usuario</button>');

  html += '<div class="grid-cards">' + resumenUsuarios() + '</div>';

  html += `<div class="toolbar">
    <div class="buscador"><svg viewBox="0 0 24 24" width="17" height="17"><circle cx="11" cy="11" r="6" fill="none" stroke="currentColor" stroke-width="1.8"/><path d="M16 16l4 4" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/></svg>
      <input id="buscarUsuarios" placeholder="Buscar por nombre, usuario o rol..."></div>
    <select id="filtroRolUsuario" aria-label="Filtrar por rol">
      <option value="">Todos los roles</option>
      <option value="ADMINISTRADOR">Administrador</option>
      <option value="RECEPCIONISTA">Recepcionista</option>
      <option value="MEDICO">Medico</option>
    </select>
  </div>`;

  html += `<div class="tarjeta"><div class="tabla-envoltorio"><table class="tabla">
    <thead><tr><th>N°</th><th>Nombre</th><th>Usuario</th><th>Rol</th><th>Medico vinculado</th><th>Estado</th><th>Acciones</th></tr></thead>
    <tbody id="cuerpoUsuarios">${filasUsuarios(listaUsuarios)}</tbody></table></div></div>`;
  $('contenido').innerHTML = html;
  $('buscarUsuarios').addEventListener('input', filtrarUsuarios);
  $('filtroRolUsuario').addEventListener('change', filtrarUsuarios);
}
function resumenUsuarios() {
  const total = listaUsuarios.length;
  const activos = listaUsuarios.filter(u => u.activo).length;
  const admins = listaUsuarios.filter(u => u.rol === 'ADMINISTRADOR').length;
  const recep = listaUsuarios.filter(u => u.rol === 'RECEPCIONISTA').length;
  const medicos = listaUsuarios.filter(u => u.rol === 'MEDICO').length;
  return cardEstad('👥', 'Usuarios', total, 'icono-azul', { pie: 'Cuentas registradas' })
    + cardEstad('✅', 'Activos', activos, 'icono-verde', { pie: 'Con acceso al sistema' })
    + cardEstad('🛡️', 'Administradores', admins, 'icono-morado', { pie: 'Control total del sistema' })
    + cardEstad('🩺', 'Recepcionistas', recep, 'icono-ambar', { pie: 'Atencion y agenda' })
    + cardEstad('👨‍⚕️', 'Medicos', medicos, 'icono-teal', { pie: 'Con medico vinculado' });
}
function filtrarUsuarios() {
  const campo = $('buscarUsuarios');
  const selector = $('filtroRolUsuario');
  const q = ((campo && campo.value) || '').toLowerCase().trim();
  const rol = (selector && selector.value) || '';
  const filas = listaUsuarios.filter(x => (!rol || x.rol === rol)
    && (!q || (x.nombre || '').toLowerCase().includes(q)
      || (x.usuario || '').toLowerCase().includes(q)
      || (x.rol || '').toLowerCase().includes(q)));
  const cuerpo = $('cuerpoUsuarios');
  if (cuerpo) cuerpo.innerHTML = filasUsuarios(filas);
}
const ETIQUETA_ROL = { ADMINISTRADOR: 'Administrador', RECEPCIONISTA: 'Recepcionista', MEDICO: 'Medico' };
const CLASE_ROL = { ADMINISTRADOR: 'azul', RECEPCIONISTA: 'ambar', MEDICO: 'verde' };
function insigniaRol(rol) {
  return `<span class="insignia insignia-${CLASE_ROL[rol] || 'gris'}">${esc(ETIQUETA_ROL[rol] || rol)}</span>`;
}
function filasUsuarios(lista) {
  if (!lista.length) return '<tr><td colspan="7" class="tabla-vacia">No hay usuarios que coincidan con la busqueda.</td></tr>';
  return lista.map(x => `<tr>
    <td>${x.id}</td>
    <td><div class="usuario-fila"><div class="avatar avatar-md">${esc(letraDe({ nombre: x.nombre }))}</div>
      <strong>${esc(x.nombre)}</strong></div></td>
    <td>${esc(x.usuario)}</td>
    <td>${insigniaRol(x.rol)}</td>
    <td>${x.medicoVinculado ? esc(x.medicoVinculado) : '-'}</td>
    <td>${x.activo ? insignia('ACTIVO') : insignia('INACTIVO')}</td>
    <td><div class="acciones">
      <button class="btn btn-azul btn-sm" onclick="editarUsuario(${x.id})">Editar</button>
      <button class="btn btn-secundario btn-sm" onclick="claveUsuario(${x.id})">Clave</button>
      <button class="btn ${x.activo ? 'btn-rojo' : 'btn-verde'} btn-sm" onclick="estadoUsuario(${x.id})">${x.activo ? 'Eliminar' : 'Activar'}</button>
    </div></td></tr>`).join('');
}
let medicosActivos = [];
async function opcionesMedicos() {
  try { medicosActivos = (await api('/api/medicos')).datos.filter(v => v.estado === 'ACTIVO'); } catch (e) { toast(e.message, 'error'); }
}
function selectMedico(seleccion) {
  const opts = medicosActivos.map(v => `<option value="${v.id}" ${seleccion && v.id === seleccion ? 'selected' : ''}>id ${v.id} - ${esc(v.nombre)} (${esc(v.especialidad)})</option>`).join('');
  return { html: `<div class="form-campo ancho-completo" id="cajaMedico"><label>Medico vinculado</label><select name="idMedico">${opts}</select></div>`,
    actualizar: () => { /* html estatico */ } };
}
function nuevoUsuario() {
  opcionesMedicos().then(() => {
    abrirModal('Nuevo usuario', `
      <form id="formUsuario" class="formulario">
        <div class="form-campo"><label>Nombre de usuario</label><input name="usuario" required placeholder="usuario"></div>
        <div class="form-campo"><label>Nombre completo</label><input name="nombre" required></div>
        <div class="form-campo"><label>Clave (minimo 6)</label><input name="clave" type="password" required minlength="6"></div>
        <div class="form-campo"><label>Confirme la clave</label><input name="confirma" type="password" required></div>
        <div class="form-campo"><label>Rol</label><select name="rol" id="rolSelect"><option>ADMINISTRADOR</option><option>RECEPCIONISTA</option><option>MEDICO</option></select></div>
        ${selectMedico().html}
      </form>`, 'Registrar', async () => {
      try {
        const datos = valorForm('formUsuario');
        if (datos.clave !== datos.confirma) throw new Error('Las claves no coinciden.');
        if (datos.rol === 'MEDICO' && !datos.idMedico) throw new Error('Para el rol MEDICO debe vincular un medico.');
        await api('/api/usuarios', { metodo: 'POST', body: { usuario: datos.usuario, nombre: datos.nombre, clave: datos.clave, rol: datos.rol, idMedico: datos.rol === 'MEDICO' ? Number(datos.idMedico) : null } });
        toast('Usuario registrado.'); cerrarModal(); renderPagina();
      } catch (e) { toast(e.message, 'error'); }
    });
    $('rolSelect').addEventListener('change', e => {
      $('cajaMedico').style.display = e.target.value === 'MEDICO' ? '' : 'none';
    });
    $('cajaMedico').style.display = 'none';
  });
}
function editarUsuario(id) {
  const x = listaUsuarios.find(v => v.id === id);
  opcionesMedicos().then(() => {
    abrirModal('Editar usuario - ' + x.usuario, `
      <form id="formUsuario" class="formulario">
        <div class="form-campo ancho-completo"><label>Nombre completo</label><input name="nombre" required value="${esc(x.nombre)}"></div>
        <div class="form-campo"><label>Rol</label><select name="rol" id="rolSelect">
          ${['ADMINISTRADOR', 'RECEPCIONISTA', 'MEDICO'].map(r => `<option ${x.rol === r ? 'selected' : ''}>${r}</option>`).join('')}
        </select></div>
        <div class="form-campo"><label>Estado</label><select name="activo">
          <option value="true" ${x.activo ? 'selected' : ''}>1 - Activo</option>
          <option value="false" ${!x.activo ? 'selected' : ''}>2 - Inactivo</option>
        </select></div>
        ${selectMedico(x.idMedico).html}
      </form>`, 'Guardar', async () => {
      try {
        const datos = valorForm('formUsuario');
        await api('/api/usuarios/' + id, { metodo: 'PUT', body: { nombre: datos.nombre, rol: datos.rol, activo: datos.activo === 'true', idMedico: datos.rol === 'MEDICO' ? Number(datos.idMedico) || null : null } });
        toast('Usuario actualizado.'); cerrarModal(); renderPagina();
      } catch (e) { toast(e.message, 'error'); }
    });
    const sinc = () => { $('cajaMedico').style.display = $('rolSelect').value === 'MEDICO' ? '' : 'none'; };
    $('rolSelect').addEventListener('change', sinc);
    sinc();
  });
}
function claveUsuario(id) {
  const x = listaUsuarios.find(v => v.id === id);
  abrirModal('Cambiar clave de ' + x.usuario, `
    <form id="formClave" class="formulario">
      <div class="form-campo"><label>Nueva clave (minimo 6)</label><input name="clave" type="password" required minlength="6"></div>
      <div class="form-campo"><label>Confirme la nueva clave</label><input name="confirma" type="password" required></div>
    </form>`, 'Cambiar clave', async () => {
    try {
      const datos = valorForm('formClave');
      if (datos.clave !== datos.confirma) throw new Error('Las claves no coinciden.');
      await api('/api/usuarios/' + id + '/clave', { metodo: 'PUT', body: { clave: datos.clave } });
      toast('Clave actualizada.'); cerrarModal();
    } catch (e) { toast(e.message, 'error'); }
  });
}
function estadoUsuario(id) {
  const x = listaUsuarios.find(v => v.id === id);
  const nuevo = !x.activo;
  modalConfirmar(x.activo ? 'Eliminar acceso' : 'Activar cuenta',
    x.activo
      ? `Se eliminara el acceso de <b>${esc(x.usuario)}</b> (${esc(x.nombre)}). `
        + 'La cuenta se conserva con su historico, pero no podra iniciar sesion. '
        + 'Puede reactivarla en cualquier momento.'
      : `Reactivar el acceso de <b>${esc(x.usuario)}</b> (${esc(x.nombre)})?`, async () => {
      try { await api('/api/usuarios/' + id + '/estado', { metodo: 'PUT', body: { activo: nuevo } }); toast(x.activo ? 'Acceso eliminado.' : 'Cuenta activada.'); cerrarModal(); renderPagina(); }
      catch (e) { toast(e.message, 'error'); }
    }, x.activo ? 'Eliminar acceso' : 'Activar');
}

// ---------- CAMBIAR MI CONTRASENA ----------
function mostrarClave() {
  $('contenido').innerHTML = encabezadoModulo('Cambiar contrasena',
    'Actualice la contrasena de su cuenta de acceso.', '') +
    '<div class="grid-2">' +
    `<div class="tarjeta"><h3><span class="h3-ico">🔐</span>Datos de la nueva contrasena</h3>
      <form id="formMiClave" class="formulario">
        <div class="form-campo ancho-completo"><label>Contrasena actual</label>
          <input name="claveActual" type="password" required autocomplete="current-password" placeholder="Contrasena con la que ingreso"></div>
        <div class="form-campo"><label>Nueva contrasena (minimo 6)</label>
          <input name="claveNueva" type="password" required minlength="6" autocomplete="new-password" placeholder="Nueva contrasena"></div>
        <div class="form-campo"><label>Confirmar nueva contrasena</label>
          <input name="confirma" type="password" required autocomplete="new-password" placeholder="Repita la nueva contrasena"></div>
        <div class="form-campo ancho-completo" id="cajaClaveAviso"></div>
      </form>
      <div class="toolbar" style="margin-top:14px">
        <button class="btn btn-primario" onclick="guardarMiClave()">🔑 Cambiar contrasena</button>
      </div></div>` +
    `<div class="tarjeta"><h3><span class="h3-ico">🛡️</span>Recomendaciones de seguridad</h3>
      <div class="detalle">
        <div class="fila"><b>Minimo</b><span>6 caracteres</span></div>
        <div class="fila"><b>Combinacion</b><span>Use letras, numeros y simbolos</span></div>
        <div class="fila"><b>Unica</b><span>No reutilice la contrasena de otro sistema</span></div>
        <div class="fila"><b>Validacion</b><span>La contrasena actual se verifica en el servidor</span></div>
        <div class="fila"><b>Sesion</b><span>La sesion actual sigue vigente al cambiarla</span></div>
      </div></div>` +
    '</div>';

  const form = $('formMiClave');
  const aviso = $('cajaClaveAviso');
  const revisar = () => {
    const nueva = form.claveNueva.value;
    const confirmacion = form.confirma.value;
    if (!nueva && !confirmacion) { aviso.innerHTML = ''; return; }
    aviso.innerHTML = nueva === confirmacion
      ? '<span class="card-delta sube">✓ Las contrasenas coinciden</span>'
      : '<span class="card-delta baja">✕ Las contrasenas no coinciden</span>';
  };
  form.claveNueva.addEventListener('input', revisar);
  form.confirma.addEventListener('input', revisar);
}
async function guardarMiClave() {
  try {
    const datos = valorForm('formMiClave');
    if (datos.claveNueva !== datos.confirma) throw new Error('Las claves no coinciden.');
    await api('/api/cambiar-clave', { metodo: 'POST', body: { claveActual: datos.claveActual, claveNueva: datos.claveNueva } });
    $('formMiClave').reset();
    toast('Contrasena actualizada correctamente.');
  } catch (e) { toast(e.message, 'error'); }
}

// ---------- Util: fila de detalle ----------
function detFila(nombre, valor) {
  return `<div class="fila"><b>${esc(nombre)}</b><span>${valor == null || valor === '' ? '-' : esc(valor)}</span></div>`;
}

// ---------- Cerrar sesion ----------
function cerrarSesion() {
  if (SO.token) { fetch('/api/logout', { method: 'POST', headers: { 'Authorization': 'Bearer ' + SO.token } }).catch(() => {}); }
  sessionStorage.clear();
  SO.token = null; SO.usuario = null; SO.permisos = null;
  // El estado de lectura se conserva (es por usuario y vive en localStorage),
  // pero la lista en memoria y el contador se limpian al cambiar de usuario.
  NOTIF_DATOS = null; NOTIF_EXCEDE = false;
  clearTimeout(temporizadorLectura);
  cerrarPaneles();
  const badge = $('notifContador');
  if (badge) { badge.classList.remove('visible'); badge.textContent = '0'; }
  $('appView').hidden = true;
  $('loginView').style.display = '';
  $('loginClave').value = '';
  $('loginError').textContent = '';
  $('loginUsuario').focus();
}

// ---------- Login ----------
async function entrarApp() {
  $('appView').hidden = false;
  $('loginView').style.display = 'none';
  const u = SO.usuario || {};
  $('usuarioNombre').textContent = u.nombre;
  $('usuarioRol').textContent = u.rolNombre;
  $('perfilNombre').textContent = u.nombre;
  $('perfilRol').textContent = u.rolNombre;
  $('sidebarUsuario').innerHTML = `
    <div class="usuario-fila">
      <div class="avatar foto-avatar" id="avatarChip">${esc(letraDe(u))}</div>
      <div class="usuario-datos">
        <strong>${esc(u.nombre)}</strong>
        <span>${esc(u.rolNombre)}</span>
        <span class="usuario-rol">${esc(u.rolNombre)}</span>
      </div>
    </div>
    <div class="usuario-acciones">
      <button class="btn-cerrar" onclick="$('fotoArchivo').click()">📷 Cambiar foto</button>
      <button class="btn-cerrar peligro" onclick="cerrarSesion()">🚪 Cerrar sesion</button>
    </div>`;
  pintarFotos();
  construirNav();
  navegar('dashboard');
  // La campana arranca con el conteo real de avisos no leidos de este usuario.
  refrescarContador(true);
}

// ---------- Foto de perfil ----------
// El endpoint /api/foto/<id> exige sesion, y un <img src> NO puede enviar el
// header Authorization (por eso antes caia en 401 y se veia la inicial).
// Por eso la imagen se pide con fetch + token y se pinta como object URL.
const SIN_FOTO = 'SIN_FOTO';
const SIN_SESION = 'SIN_SESION';

async function pedirFoto(id) {
  try {
    const resp = await fetch('/api/foto/' + encodeURIComponent(id), {
      headers: { 'Authorization': 'Bearer ' + SO.token },
      cache: 'no-store'
    });
    if (resp.status === 401 || resp.status === 403) {
      return SIN_SESION;
    }
    if (resp.status === 404) {
      return SIN_FOTO;
    }
    if (!resp.ok) {
      return SIN_FOTO;
    }
    const blob = await resp.blob();
    if (!blob || !blob.size) {
      return SIN_FOTO;
    }
    return URL.createObjectURL(blob);
  } catch (e) {
    return SIN_FOTO;
  }
}

function pintarInicial(el, letra) {
  const previa = el.querySelector('img');
  if (previa) {
    previa.remove();
  }
  if (el.dataset.fotoUrl) {
    URL.revokeObjectURL(el.dataset.fotoUrl);
    el.dataset.fotoUrl = '';
  }
  el.textContent = letra;
}

function pintarImagen(el, url) {
  const previa = el.querySelector('img');
  if (previa) {
    previa.remove();
  }
  el.textContent = '';
  const img = document.createElement('img');
  img.alt = '';
  img.className = 'avatar-img';
  img.src = url;
  img.onerror = () => pintarInicial(el, letraDe(SO.usuario));
  el.appendChild(img);
  el.dataset.fotoUrl = url;
}

function letraDe(u) {
  const nombre = (u && u.nombre ? String(u.nombre) : 'U').trim();
  return nombre.charAt(0).toUpperCase();
}

async function pintarFotos() {
  const usuario = SO.usuario || {};
  const id = usuario.idUsuario || usuario.id;
  const letra = letraDe(usuario);
  const elementos = [$('avatarLetra'), $('avatarChip'), $('perfilAvatar')].filter(Boolean);
  for (const el of elementos) {
    // Al hacer clic se abre la foto en grande; la opcion de cambiarla sigue
    // disponible en el menu de perfil y dentro del propio modal.
    el.classList.add('avatar-foto-abrible');
    el.setAttribute('role', 'button');
    el.setAttribute('tabindex', '0');
    el.setAttribute('title', 'Ver foto de perfil');
    el.onclick = verFotoModal;
    el.onkeydown = e => {
      if (e.key === 'Enter' || e.key === ' ') { e.preventDefault(); verFotoModal(); }
    };
    pintarInicial(el, letra);
    // El backend informa "foto": null cuando el usuario no tiene imagen.
    if (usuario.foto === null) {
      continue;
    }
    if (!id) {
      continue;
    }
    const url = await pedirFoto(id);
    if (url === SIN_SESION) {
      pintarInicial(el, letra);
      continue;
    }
    if (url === SIN_FOTO) {
      pintarInicial(el, letra);
      continue;
    }
    pintarImagen(el, url);
  }
}

// ---------- Modal de foto de perfil (amplia la vista, no cambia nada) ----------
// Se reutiliza EXACTAMENTE la foto ya cargada del usuario: primero se toma la
// URL que pintaron los avatares (mismo object URL) y, solo si aun no hay,
// se pide al mismo endpoint GET /api/foto/<id> que ya usa el sistema. No se
// reemplaza el archivo ni se crea otro usuario.
function urlFotoActual() {
  const avatar = $('avatarLetra') || $('perfilAvatar') || $('avatarChip');
  return (avatar && avatar.dataset && avatar.dataset.fotoUrl) || '';
}
async function verFotoModal() {
  const fondo = $('fotoModal');
  if (!fondo) return;
  const u = SO.usuario || {};
  $('fotoModalNombre').textContent = u.nombre || 'Usuario';
  $('fotoModalRol').textContent = u.rolNombre || '';
  const img = $('fotoGrande');
  const inicial = $('fotoInicial');
  let url = urlFotoActual();
  if (!url) {
    const id = u.idUsuario || u.id;
    if (id) url = await pedirFoto(id);
  }
  if (url && url !== SIN_FOTO && url !== SIN_SESION) {
    // Se asigna la misma foto; si falla la carga se cae a la inicial.
    img.hidden = false;
    inicial.hidden = true;
    img.src = url;
    img.onerror = () => {
      img.hidden = true;
      inicial.hidden = false;
      $('fotoInicialLetra').textContent = letraDe(u);
    };
  } else {
    img.hidden = true;
    inicial.hidden = false;
    $('fotoInicialLetra').textContent = letraDe(u);
  }
  fondo.hidden = false;
  fondo.classList.remove('saliendo');
  document.addEventListener('keydown', teclaFotoModal);
}
function cerrarFotoModal() {
  const fondo = $('fotoModal');
  if (!fondo || fondo.hidden) return;
  document.removeEventListener('keydown', teclaFotoModal);
  fondo.classList.add('saliendo');
  // Se espera la animacion de salida antes de esconderlo.
  setTimeout(() => {
    fondo.hidden = true;
    fondo.classList.remove('saliendo');
  }, 190);
}
function teclaFotoModal(e) {
  if (e.key === 'Escape') cerrarFotoModal();
}
// Clic fuera de la fotografia (sobre el fondo oscuro) cierra el modal.
$('fotoModal').addEventListener('click', e => {
  if (!e.target.closest('.foto-modal')) cerrarFotoModal();
});
$('fotoArchivo').addEventListener('change', async e => {
  const archivo = e.target.files && e.target.files[0];
  e.target.value = '';
  if (!archivo) return;
  // El backend valida tipo (JPEG/PNG/WEBP) y tamano (<= 1 MB); se replica aqui
  // para avisar antes de enviar y no recibir un error del servidor.
  if (!/^image\/(png|jpe?g|webp)$/i.test(archivo.type)) {
    toast('Formato no admitido. Usa una imagen JPG, PNG o WEBP.', 'error');
    return;
  }
  if (archivo.size > 1024 * 1024) {
    toast('La imagen supera el limite de 1 MB. Elija una imagen mas pequena.', 'error');
    return;
  }
  const lector = new FileReader();
  lector.onload = async () => {
    try {
      await api('/api/foto', { metodo: 'PUT', body: { imagen: lector.result } });
    } catch (err) {
      toast(err.message || 'No se pudo guardar la foto.', 'error');
      return;
    }
    // La foto ya esta guardada: se refleja ya mismo, sin depender de /api/me.
    SO.usuario = Object.assign({}, SO.usuario, {
      foto: '/api/foto/' + (SO.usuario.idUsuario || SO.usuario.id)
    });
    pintarFotos();
    // Si el modal esta abierto, se actualiza al instante con la foto nueva
    // (mismo origen y mismo usuario, solo cambia el archivo guardado).
    if (!$('fotoModal').hidden) verFotoModal();
    toast('Foto de perfil actualizada.');
  };
  lector.onerror = () => toast('No se pudo leer la imagen seleccionada.', 'error');
  lector.readAsDataURL(archivo);
});

$('loginForm').addEventListener('submit', async e => {
  e.preventDefault();
  const boton = $('loginBoton');
  boton.disabled = true;
  boton.textContent = 'Verificando...';
  $('loginError').textContent = '';
  try {
    const r = await api('/api/login', { metodo: 'POST', body: { usuario: $('loginUsuario').value.trim(), clave: $('loginClave').value } });
    SO.token = r.token;
    SO.usuario = r.usuario;
    const me = await api('/api/me');
    SO.permisos = me.permisos;
    sessionStorage.setItem('clinicaToken', SO.token);
    sessionStorage.setItem('clinicaSesion', JSON.stringify({ usuario: SO.usuario, permisos: SO.permisos }));
    entrarApp();
  } catch (err) {
    $('loginError').textContent = err.message;
  } finally {
    boton.disabled = false;
    boton.textContent = 'Iniciar sesion';
  }
});

// ---------- Menu movil ----------
$('btnMenu').addEventListener('click', () => {
  $('sidebar').classList.toggle('abierta');
  $('backdrop').hidden = !$('sidebar').classList.contains('abierta');
  $('btnMenu').setAttribute('aria-expanded', String($('sidebar').classList.contains('abierta')));
});
$('backdrop').addEventListener('click', () => {
  $('sidebar').classList.remove('abierta');
  $('backdrop').hidden = true;
  $('btnMenu').setAttribute('aria-expanded', 'false');
});

// ---------- Header: buscador global, notificaciones y menu de perfil ----------
// SISTEMA DE NOTIFICACIONES
// La base de datos no tiene tabla de notificaciones (ver crear_bd.sql: atencion,
// cita, configuracion, gasto, medico, paciente, pago, usuario) y la tabla
// configuracion es DECIMAL y solo guarda el presupuesto, asi que no permite
// guardar el estado de lectura por usuario. Por eso:
//   1) los avisos se generan con datos REALES de /api/citas, /api/atenciones,
//      /api/pagos y /api/pacientes (endpoints que ya existian);
//   2) cada aviso tiene un id estable (ej. "cita:12"), por lo que un registro
//      nuevo produce un id nuevo -> vuelve a estar NO LEIDO, mientras que los
//      avisos antiguos siguen marcados como LEIDOS;
//   3) el estado leido se persiste en localStorage, separado por usuario.
const MAX_NOTIFICACIONES = 30;
const TTL_NOTIF = 45000;
let NOTIF_DATOS = null;          // { lista, ts }
let NOTIF_EXCEDE = false;        // alerta de presupuesto (viene de /api/dashboard)
let temporizadorLectura = null;

const claveLeidas = () => 'clinica_notif_leidas_'
  + ((SO.usuario && (SO.usuario.idUsuario || SO.usuario.id)) || '0');

function leerLeidas() {
  try {
    const v = JSON.parse(localStorage.getItem(claveLeidas()));
    return v && typeof v === 'object' ? v : {};
  } catch (e) { return {}; }
}
function guardarLeidas(mapa) {
  try { localStorage.setItem(claveLeidas(), JSON.stringify(mapa)); } catch (e) { /* modo privado */ }
}
function momentoDe(iso) {
  if (!iso) return null;
  const d = new Date(String(iso).trim().replace(' ', 'T'));
  return isNaN(d.getTime()) ? null : d;
}
function haceCuanto(m) {
  if (!m) return '';
  const min = Math.floor((Date.now() - m.getTime()) / 60000);
  if (min < 1) return 'Hace instantes';
  if (min < 60) return 'Hace ' + min + (min === 1 ? ' minuto' : ' minutos');
  const hs = Math.floor(min / 60);
  if (hs < 24) return 'Hace ' + hs + (hs === 1 ? ' hora' : ' horas');
  const ds = Math.floor(hs / 24);
  return 'Hace ' + ds + (ds === 1 ? ' dia' : ' dias');
}
function construirNotificaciones(d) {
  const lista = [];
  (d.citas || []).forEach(c => {
    const f = (c.fecha || '') + (c.hora ? 'T' + String(c.hora).slice(0, 5) : '');
    lista.push({
      id: 'cita:' + c.id, ico: '📅', ir: 'citas',
      titulo: c.estado === 'CANCELADA' ? 'Cita cancelada' : 'Cita registrada',
      detalle: c.paciente + ' - ' + (c.especialidad || 'Consulta')
        + (c.medico ? ' con ' + c.medico : '') + (c.hora ? ' a las ' + String(c.hora).slice(0, 5) : ''),
      momento: momentoDe(f)
    });
  });
  (d.pagos || []).forEach(p => {
    lista.push({
      id: 'pago:' + p.id, ico: '💳', ir: 'pagos',
      titulo: 'Pago recibido',
      detalle: 'Se registro un pago de ' + moneda(p.monto) + ' (' + (p.metodo || '') + ')',
      momento: momentoDe(p.fecha)
    });
  });
  (d.pacientes || []).forEach(x => {
    lista.push({
      id: 'paciente:' + x.id, ico: '👤', ir: 'pacientes',
      titulo: 'Nuevo paciente',
      detalle: 'Se registro a ' + x.nombre + (x.dni ? ' (DNI ' + x.dni + ')' : ''),
      momento: momentoDe(x.fechaRegistro)
    });
  });
  (d.atenciones || []).forEach(a => {
    lista.push({
      id: 'atencion:' + a.id, ico: '🩺', ir: 'atenciones',
      titulo: 'Atencion registrada',
      detalle: a.paciente + ' - ' + (a.diagnostico || 'sin diagnostico')
        + (a.medico ? ' (' + a.medico + ')' : ''),
      momento: momentoDe(a.fecha)
    });
  });
  if (d.excedePresupuesto) {
    lista.push({
      id: 'presupuesto:excedido', ico: '⚠️', ir: 'presupuesto',
      titulo: 'Presupuesto excedido',
      detalle: 'Los gastos superan el presupuesto establecido',
      momento: null
    });
  }
  // Mas recientes primero; los avisos sin fecha al final.
  lista.sort((a, b) => {
    if (a.momento && b.momento) return b.momento - a.momento;
    if (a.momento) return -1;
    if (b.momento) return 1;
    return a.id.localeCompare(b.id);
  });
  return lista.slice(0, MAX_NOTIFICACIONES);
}
async function obtenerNotificaciones(forzar) {
  if (!forzar && NOTIF_DATOS && Date.now() - NOTIF_DATOS.ts < TTL_NOTIF) return NOTIF_DATOS.lista;
  const p = SO.permisos || {};
  const [citas, atenciones, pagos, pacientes] = await Promise.all([
    api('/api/citas').catch(() => null),
    api('/api/atenciones').catch(() => null),
    p.gestionarPagos ? api('/api/pagos').catch(() => null) : Promise.resolve(null),
    api('/api/pacientes').catch(() => null)
  ]);
  const lista = construirNotificaciones({
    citas: (citas && citas.datos) || [],
    atenciones: (atenciones && atenciones.datos) || [],
    pagos: (pagos && pagos.datos) || [],
    pacientes: (pacientes && pacientes.datos) || [],
    excedePresupuesto: NOTIF_EXCEDE
  });
  NOTIF_DATOS = { lista, ts: Date.now() };
  return lista;
}
function contarNoLeidas(lista) {
  const leidas = leerLeidas();
  return (lista || []).filter(n => !leidas[n.id]).length;
}
function actualizarContadorNotif() {
  const badge = $('notifContador');
  if (!badge) return;
  const n = NOTIF_DATOS ? contarNoLeidas(NOTIF_DATOS.lista) : 0;
  badge.textContent = n > 99 ? '99+' : String(n);
  badge.classList.toggle('visible', n > 0);
}
function pintarNotificaciones() {
  const lista = $('notifLista');
  if (!lista) return;
  const datos = (NOTIF_DATOS && NOTIF_DATOS.lista) || [];
  const leidas = leerLeidas();
  const resumen = $('notifResumen');
  if (resumen) {
    const nuevas = contarNoLeidas(datos);
    resumen.textContent = nuevas > 0
      ? nuevas + (nuevas === 1 ? ' nueva' : ' nuevas')
      : datos.length + (datos.length === 1 ? ' aviso' : ' avisos');
  }
  const boton = $('notifMarcar');
  if (boton) boton.disabled = datos.length === 0 || datos.every(n => leidas[n.id]);
  lista.innerHTML = datos.length
    ? datos.map((n, i) => {
      const leida = !!leidas[n.id];
      return `<div class="notif-item ${leida ? 'leida' : 'nueva'}" style="animation-delay:${Math.min(i * 35, 300)}ms"`
        + ` onclick="irNotificacion('${n.ir}')">`
        + `<div class="notif-ico">${n.ico}</div>`
        + '<div class="notif-txt"><b>' + esc(n.titulo) + '</b><span>' + esc(n.detalle) + '</span>'
        + (n.momento ? '<small>' + esc(haceCuanto(n.momento)) + '</small>' : '') + '</div>'
        + '<span class="notif-punto" aria-hidden="true"></span></div>';
    }).join('')
    : '<div class="notif-vacio">No hay notificaciones por ahora.</div>';
}
function aplicarLectura(silencioso) {
  const datos = (NOTIF_DATOS && NOTIF_DATOS.lista) || [];
  if (contarNoLeidas(datos) === 0) return false;
  const mapa = leerLeidas();
  const marca = Date.now();
  datos.forEach(n => { mapa[n.id] = marca; });
  guardarLeidas(mapa);
  pintarNotificaciones();
  actualizarContadorNotif();
  if (!silencioso) toast('Notificaciones marcadas como leidas.', 'info');
  return true;
}
function marcarTodasLeidas() { aplicarLectura(false); }
function irNotificacion(destino) {
  cerrarPaneles();
  if (destino) navegar(destino);
}
// Al entrar al sistema el contador ya muestra los avisos no leidos.
async function refrescarContador(forzar) {
  if ($('appView').hidden) return;
  try {
    await obtenerNotificaciones(!!forzar);
    actualizarContadorNotif();
  } catch (e) { /* silencioso: la campana no debe romper la pagina */ }
}

function cerrarPaneles() {
  const panel = $('notifPanel');
  if (panel) panel.hidden = true;
  const menu = $('perfilMenu');
  if (menu) menu.hidden = true;
  const chip = $('perfilBtn');
  if (chip) { chip.classList.remove('abierto'); chip.setAttribute('aria-expanded', 'false'); }
  const campana = $('btnNotif');
  if (campana) campana.setAttribute('aria-expanded', 'false');
}
function alternarPanel(panel, boton) {
  const abrir = panel.hidden;
  cerrarPaneles();
  clearTimeout(temporizadorLectura);
  if (abrir) {
    panel.hidden = false;
    boton.setAttribute('aria-expanded', 'true');
    boton.classList.add('abierto');
  }
}
$('perfilBtn').addEventListener('click', e => alternarPanel($('perfilMenu'), e.currentTarget));
$('btnNotif').addEventListener('click', e => {
  alternarPanel($('notifPanel'), e.currentTarget);
  if ($('notifPanel').hidden) return;
  // 1) pinta al instante con el estado actual para que la apertura sea fluida
  pintarNotificaciones();
  // 2) refresca los datos por si hay avisos nuevos
  obtenerNotificaciones(true)
    .then(() => { pintarNotificaciones(); actualizarContadorNotif(); })
    .catch(() => { /* silencioso */ });
  // 3) al abrir el panel, lo no leido pasa a leido (y queda persistido)
  temporizadorLectura = setTimeout(() => {
    if ($('notifPanel').hidden) return;
    aplicarLectura(true);
  }, 400);
});
document.addEventListener('click', e => {
  if (!e.target.closest('.perfil') && !e.target.closest('.notif')) cerrarPaneles();
});
document.addEventListener('keydown', e => {
  if (e.key === 'Escape') cerrarPaneles();
  const escribiendo = /^(INPUT|TEXTAREA|SELECT)$/.test(document.activeElement.tagName);
  if (e.key === '/' && !escribiendo && $('appView').hidden === false) {
    e.preventDefault();
    $('buscarGlobal').focus();
  }
});
// El buscador global reutiliza el filtro existente del modulo Pacientes.
$('buscarGlobal').addEventListener('keydown', e => {
  if (e.key !== 'Enter') return;
  const q = e.currentTarget.value.trim();
  if (!q) return;
  e.preventDefault();
  navegar('pacientes');
  let intentos = 0;
  const aplicar = () => {
    const input = $('buscarPacientes');
    if (input) {
      input.value = q;
      input.dispatchEvent(new Event('input'));
      toast('Buscando "' + q + '" en pacientes.', 'info');
      return;
    }
    if (++intentos < 25) setTimeout(aplicar, 60);
  };
  setTimeout(aplicar, 60);
});

// ---------- Graficos responsive ----------
let temporizadorResize = null;
window.addEventListener('resize', () => {
  clearTimeout(temporizadorResize);
  temporizadorResize = setTimeout(() => {
    limpiarGraficos();
    GRAFICOS.forEach(fn => {
      try { fn(); } catch (e) { /* el canvas puede estar oculto */ }
    });
  }, 180);
});

// ---------- Arranque: reanudar sesion ----------
(async function iniciar() {
  const guardada = sessionStorage.getItem('clinicaSesion');
  const token = sessionStorage.getItem('clinicaToken');
  if (token && guardada) {
    try {
      const datos = JSON.parse(guardada);
      SO.token = token;
      const me = await api('/api/me');
      SO.usuario = me.usuario || datos.usuario;
      SO.permisos = me.permisos || datos.permisos;
      entrarApp();
      return;
    } catch (e) { sessionStorage.clear(); }
  }
  $('loginView').style.display = '';
  $('loginUsuario').focus();
})();