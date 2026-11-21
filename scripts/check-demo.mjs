import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import vm from 'node:vm';

const html = readFileSync(new URL('../src/main/resources/static/demo.html', import.meta.url), 'utf8');
const script = html.match(/<script>([\s\S]*?)<\/script>/)?.[1];
assert.ok(script, 'Demo script exists');
new vm.Script(script);
assert.ok(html.includes('Interactive simulation.'), 'Demo discloses fictional state');
assert.ok(html.includes('nothing reaches a production API'), 'Demo discloses backend isolation');
assert.ok(!/\bfetch\s*\(/.test(script), 'Demo cannot fetch production API');
assert.ok(!/\b(localStorage|sessionStorage|XMLHttpRequest|WebSocket)\b/.test(script), 'Demo has no persistence or network clients');

const ids = ['customer', 'agent', 'inbox-title', 'inbox-subtitle', 'search', 'tickets', 'detail', 'total', 'open', 'resolved', 'reply', 'message', 'assign', 'resolve'];
const listeners = {};
const makeElement = id => ({
  id, value: '', innerHTML: '', textContent: '', dataset: {},
  classList: { toggle() {} },
  setAttribute() {},
  addEventListener(name, cb) { listeners[`${id}:${name}`] = cb; }
});
const elements = Object.fromEntries(ids.map(id => [id, makeElement(id)]));
const filters = ['ALL', 'OPEN', 'RESOLVED'].map(filter => ({
  dataset: { filter }, classList: { toggle() {} }, setAttribute() {},
  addEventListener(name, cb) { listeners[`filter:${filter}:${name}`] = cb; }
}));
const document = { getElementById: id => elements[id], querySelectorAll: () => filters };
vm.runInNewContext(script, { document }, { timeout: 1000 });
assert.equal(elements.total.textContent, 2);
assert.equal(elements.open.textContent, 1);
assert.match(elements.tickets.innerHTML, /Weekend overtime payment/);
assert.doesNotMatch(elements.tickets.innerHTML, /Subscription invoice correction/, 'Customer cannot see another sample customer');

listeners['agent:click']();
assert.equal(elements.total.textContent, 3);
assert.equal(elements['inbox-title'].textContent, 'Team inbox');
assert.match(elements.tickets.innerHTML, /Aarav Sharma/);

elements.search.value = '1041';
listeners['search:input']();
assert.match(elements.tickets.innerHTML, /Subscription invoice correction/);
assert.doesNotMatch(elements.tickets.innerHTML, /Weekend overtime payment/);

elements.search.value = '';
listeners['filter:RESOLVED:click']();
assert.match(elements.tickets.innerHTML, /Cannot access help-center article/);
assert.doesNotMatch(elements.tickets.innerHTML, /Subscription invoice correction/);

console.log('OnceTold interactive demo checks passed');
