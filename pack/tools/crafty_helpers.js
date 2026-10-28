// Helpers for driving the Crafty 4 API from a logged-in browser tab of the panel.
// Load: open https://<crafty>/static/assets/images/favicon.png (a page that never reloads), then
//   eval(await fetch('http://127.0.0.1:8765/_crafty_helpers.js').then(r => r.text()))
// with pack/tools/cors_serve.py serving deploy/server_upload (this file is copied there as _crafty_helpers.js).
window.SID = '9d44536e-1116-41d9-98b3-5f16563eafa7';
window.LOCAL = 'http://127.0.0.1:8765/';
window.hex = buf => [...new Uint8Array(buf)].map(b => b.toString(16).padStart(2, '0')).join('');
window.xsrf = () => { const x = document.cookie.split('; ').find(c => c.startsWith('_xsrf=')); return x ? decodeURIComponent(x.split('=')[1]) : ''; };
window.cApi = async (method, url, body) => {
  const r = await fetch(url, { method, headers: { 'X-XSRFToken': xsrf(), 'Content-Type': 'application/json' }, body: body ? JSON.stringify(body) : undefined });
  const t = await r.text();
  try { return { status: r.status, json: JSON.parse(t) }; } catch (e) { return { status: r.status, text: t.slice(0, 500) }; }
};
window.stdin = async c => (await fetch('/api/v2/servers/' + SID + '/stdin', { method: 'POST', headers: { 'X-XSRFToken': xsrf() }, body: c })).status;
window.logs = async () => { const l = await cApi('GET', '/api/v2/servers/' + SID + '/logs?file=true&raw=true'); return (l.json && l.json.data) || []; };
window.stats = async () => (await cApi('GET', '/api/v2/servers/' + SID + '/stats')).json.data;
window.readFile = async rel => { const r = await cApi('POST', '/api/v2/servers/' + SID + '/files', { path: '/crafty/servers/' + SID + '/' + rel }); return r.json && r.json.data && r.json.data.content; };
window.mkdir = async (parent, name) => (await fetch('/api/v2/servers/' + SID + '/files/create/', { method: 'PUT', headers: { 'X-XSRFToken': xsrf(), 'Content-Type': 'application/json' }, body: JSON.stringify({ parent, name, directory: true }) })).status;
window.uploadBlob = async (sid, blob, name, location) => {
  const chunkSize = 10 * 1024 * 1024, total = Math.max(1, Math.ceil(blob.size / chunkSize)), fileId = crypto.randomUUID();
  const url = '/api/v2/servers/' + sid + '/files/upload/';
  const base = { 'X-XSRFToken': xsrf(), chunked: 'true', fileSize: String(blob.size), type: 'server_upload', totalChunks: String(total), fileName: name, location, fileId };
  let r = await fetch(url, { method: 'POST', headers: base, body: null }); let j = await r.json();
  if (j.status !== 'ok') throw new Error('init ' + JSON.stringify(j));
  const fid = (j.data && j.data['file-id']) || fileId; let last;
  for (let i = 0; i < total; i++) {
    const start = i * chunkSize, end = Math.min(start + chunkSize, blob.size), chunk = blob.slice(start, end);
    const h = hex(await crypto.subtle.digest('SHA-256', await chunk.arrayBuffer()));
    r = await fetch(url, { method: 'POST', body: chunk, headers: { ...base, fileId: fid, 'Content-Range': 'bytes ' + start + '-' + (end - 1) + '/' + blob.size, chunkHash: h, chunkId: String(i) } });
    last = await r.json();
    if (last.status !== 'completed' && last.status !== 'partial') throw new Error('chunk ' + i + ' ' + JSON.stringify(last));
  }
  return last.status;
};
// Upload manifest entries {p, h, s} served by cors_serve.py; progress in window.UP. Missing folders must exist (mkdir).
window.uploadLocal = async files => {
  window.UP = { done: 0, total: files.length, bytes: 0, errs: [], running: true };
  for (const f of files) {
    try {
      const b = await fetch(LOCAL + encodeURI(f.p)).then(r => { if (!r.ok) throw new Error('local ' + r.status); return r.blob(); });
      if (hex(await crypto.subtle.digest('SHA-1', await b.arrayBuffer())) !== f.h) throw new Error('sha1');
      const i = f.p.lastIndexOf('/');
      await uploadBlob(SID, b, f.p.slice(i + 1), '/crafty/servers/' + SID + (i < 0 ? '' : '/' + f.p.slice(0, i)));
      UP.bytes += f.s;
    } catch (e) { UP.errs.push(f.p + ': ' + e.message); }
    UP.done++;
  }
  UP.running = false;
};
'crafty helpers loaded';
