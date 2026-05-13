/* Dashboard logic — fetch /api/dashboard and render stats, charts, table, strengths. */

const DONUT = ["#6C63FF", "#22c55e", "#f59e0b", "#ff6b6b", "#3b82f6"];
const $ = id => document.getElementById(id);
const esc = s => String(s ?? "").replace(/[&<>"']/g, c => ({ "&":"&amp;","<":"&lt;",">":"&gt;",'"':"&quot;","'":"&#39;" }[c]));
const fmt = (iso, opt) => { const d = new Date(iso); return isNaN(d) ? (iso||"—") : d.toLocaleDateString("en-US", opt); };
const show = state => ["loadingState:loading","errorState:error","dashboardData:ready"]
  .forEach(p => { const [id,s] = p.split(":"); $(id).classList.toggle("hidden", state !== s); });

document.addEventListener("DOMContentLoaded", load);

function load() {
  show("loading");
  fetch("/api/dashboard")
    .then(r => r.ok ? r.json() : Promise.reject("HTTP " + r.status))
    .then(d => { render(d); show("ready"); })
    .catch(err => { console.error("[dashboard]", err); $("errorMsg").textContent = "Couldn't reach the server. " + err; show("error"); });
}

function render(d) {
  $("statTotal").textContent = d.totalInterviews ?? 0;
  $("statAvg").textContent   = (d.averageScore   ?? 0) + "%";
  $("statBest").textContent  = (d.bestScore      ?? 0) + "%";
  $("statTime").textContent  =  d.totalPracticeTime || "0m";
  lineChart(d.scoreTrend       || []);
  donut    (d.weakAreas        || []);
  table    (d.recentInterviews || []);
  strengths(d.strengths        || []);
}

function lineChart(trend) {
  const wrap = $("lineChart");
  if (!trend.length) return wrap.innerHTML = '<p style="color:#9CA3AF;font-size:12px">No data yet.</p>';

  const W=700, H=200, pX=50, pY=40, iw=W-pX*2, ih=H-pY*2;
  const step = trend.length > 1 ? iw / (trend.length - 1) : 0;
  const pts = trend.map((p, i) => ({
    x: pX + step*i,
    y: pY + (1 - (p.score||0)/100)*ih,
    label: fmt(p.date, {weekday:"short"}),
    score: p.score
  }));

  const poly = pts.map(p => `${p.x},${p.y}`).join(" ");
  const area = `M ${pts[0].x},${pts[0].y} ${pts.slice(1).map(p=>`L ${p.x},${p.y}`).join(" ")} L ${pts.at(-1).x},${H-20} L ${pts[0].x},${H-20} Z`;
  const dots = pts.map(p => `<circle cx="${p.x}" cy="${p.y}" r="4.5" fill="white" stroke="#6C63FF" stroke-width="2.5"><title>${p.label}: ${p.score}%</title></circle>`).join("");
  const lbls = pts.map(p => `<text x="${p.x}" y="${H-5}" fill="#9CA3AF" font-size="11" text-anchor="middle">${p.label}</text>`).join("");

  wrap.innerHTML = `
    <svg viewBox="0 0 ${W} ${H}" preserveAspectRatio="none" aria-hidden="true">
      <defs><linearGradient id="areaGrad" x1="0" y1="0" x2="0" y2="1">
        <stop offset="0%"  stop-color="#6C63FF" stop-opacity="0.6"/>
        <stop offset="100%" stop-color="#6C63FF" stop-opacity="0"/>
      </linearGradient></defs>
      <g stroke="#F1F2F6">
        <line x1="0" y1="${pY}"      x2="${W}" y2="${pY}"/>
        <line x1="0" y1="${pY+ih/2}" x2="${W}" y2="${pY+ih/2}"/>
        <line x1="0" y1="${pY+ih}"   x2="${W}" y2="${pY+ih}"/>
      </g>
      <path d="${area}" fill="url(#areaGrad)" opacity="0.35"/>
      <polyline points="${poly}" fill="none" stroke="#6C63FF" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"/>
      <g>${dots}</g><g font-family="Poppins">${lbls}</g>
    </svg>`;
}

function donut(areas) {
  const svg = $("donutChart"), legend = $("donutLegend");
  if (!areas.length) { svg.innerHTML = ""; legend.innerHTML = '<li style="color:#9CA3AF;font-size:12px">No data</li>'; return; }

  const total = areas.reduce((s, a) => s + (a.value||0), 0) || 1;
  let off = 25; // start at 12 o'clock

  svg.innerHTML = `<circle cx="21" cy="21" r="15.915" fill="none" stroke="#F1F2F6" stroke-width="6"/>` +
    areas.map((a, i) => {
      const pct = (a.value||0) / total * 100;
      const seg = `<circle cx="21" cy="21" r="15.915" fill="none" stroke="${DONUT[i%DONUT.length]}" stroke-width="6" stroke-dasharray="${pct.toFixed(2)} ${(100-pct).toFixed(2)}" stroke-dashoffset="${off.toFixed(2)}" transform="rotate(-90 21 21)"/>`;
      off -= pct;
      return seg;
    }).join("");

  legend.innerHTML = areas.map((a, i) =>
    `<li><span class="swatch" style="background:${DONUT[i%DONUT.length]}"></span>${esc(a.name)}<span class="pct">${Math.round((a.value||0)/total*100)}%</span></li>`).join("");
}

function table(rows) {
  const tb = $("recentTbody");
  if (!rows.length) return tb.innerHTML = `<tr><td colspan="5" style="text-align:center;color:#9CA3AF;padding:18px">No interviews yet. Click <b>Start Interview</b> above!</td></tr>`;

  const badge = s => s === "Completed" ? "green" : s === "Needs Review" ? "amber" : "purple";
  tb.innerHTML = rows.map(r => `
    <tr>
      <td>${esc(r.role)}</td>
      <td>${esc(r.level)}</td>
      <td>${fmt(r.date, {month:"short",day:"2-digit"})}</td>
      <td><b>${r.score}%</b></td>
      <td><span class="badge ${badge(r.status)}">${esc(r.status)}</span></td>
    </tr>`).join("");
}

function strengths(items) {
  const list = $("strengthList");
  if (!items.length) return list.innerHTML = '<p style="color:#9CA3AF;font-size:12px">No strengths data yet.</p>';

  list.innerHTML = items.map(s => `
    <div class="strength-row">
      <div class="top"><span>${esc(s.name)}</span><b>${s.value}%</b></div>
      <div class="strength-track"><div class="strength-fill" data-w="${s.value}"></div></div>
    </div>`).join("");

  requestAnimationFrame(() => list.querySelectorAll(".strength-fill")
    .forEach((el, i) => setTimeout(() => el.style.width = el.dataset.w + "%", 120 + i*100)));
}

