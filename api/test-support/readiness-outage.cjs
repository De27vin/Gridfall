// Real TCP outage/recovery test, restricted to the disposable community database.
const assert = require('node:assert/strict');
const net = require('node:net');
const testUrl = process.env.COMMUNITY_TEST_DATABASE_URL;
if (process.env.NODE_ENV !== 'test' || !/^postgresql:\/\/[^/]+@127\.0\.0\.1:55439\/gridfall_community_test$/.test(testUrl || '') || process.env.DATABASE_URL !== testUrl) {
  throw Error('Requires the explicit disposable loopback test database');
}
const sockets = new Set();
const proxy = net.createServer(incoming => {
  const upstream = net.connect(55439, '127.0.0.1');
  for (const socket of [incoming, upstream]) {
    sockets.add(socket);
    socket.on('close', () => sockets.delete(socket));
    socket.on('error', () => { incoming.destroy(); upstream.destroy(); });
  }
  incoming.pipe(upstream); upstream.pipe(incoming);
});
const listen = port => new Promise((resolve, reject) => {
  proxy.once('error', reject);
  proxy.listen(port, '127.0.0.1', () => { proxy.removeListener('error', reject); resolve(); });
});
const disconnect = () => new Promise(resolve => {
  for (const socket of sockets) socket.destroy();
  if (proxy.listening) proxy.close(resolve); else resolve();
});
(async () => {
  await listen(0);
  const port = proxy.address().port;
  const url = new URL(testUrl); url.port = String(port);
  url.searchParams.set('connect_timeout', '1'); url.searchParams.set('pool_timeout', '1');
  process.env.DATABASE_URL = url.toString();
  const {buildServer} = require('../dist/server');
  const app = buildServer();
  try {
    assert.equal((await app.inject({url: '/ready'})).statusCode, 200);
    await disconnect();
    assert.equal((await app.inject({url: '/ready'})).statusCode, 503);
    assert.equal((await app.inject({url: '/health'})).statusCode, 200);
    await listen(port);
    let recovered = false;
    const deadline = Date.now() + 10_000;
    while (Date.now() < deadline) {
      if ((await app.inject({url: '/ready'})).statusCode === 200) { recovered = true; break; }
      await new Promise(resolve => setTimeout(resolve, 100));
    }
    assert.equal(recovered, true);
    console.log('PASS: ready 200 -> database TCP outage 503; health 200; recovery 200 without API restart');
  } finally { await app.close(); await disconnect(); }
})().catch(async error => { console.error(error); await disconnect(); process.exitCode = 1; });
