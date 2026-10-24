const $ = (id) => document.getElementById(id);
const pendingStorageKey = 'transact.pendingTransfer';
const descriptions = {
  overview: 'A clear view of your virtual payment activity.',
  accounts: 'Manage balances and inspect account statements.',
  transfers: 'Move virtual funds safely between accounts.',
  history: 'Search and inspect your transaction history.'
};
let activeSection = 'overview';
let accounts = [];
let pending = loadPending();
let transferBusy = false;
let historyPage = 0;
let historyFilters = {};
let statementAccountId = null;
let statementPage = 0;
let detailsReturnFocus = null;

function loadPending() {
  try {
    const saved = JSON.parse(sessionStorage.getItem(pendingStorageKey));
    return saved?.key && saved?.payload ? saved : null;
  } catch { return null; }
}
function savePending(value) {
  pending = value;
  try {
    if (value) sessionStorage.setItem(pendingStorageKey, JSON.stringify(value));
    else sessionStorage.removeItem(pendingStorageKey);
  } catch { /* Keep the attempt in memory until this page closes. */ }
  updateTransferControls();
}
function money(value) {
  const raw = String(value ?? '0');
  const match = raw.match(/^(\d+)(?:\.(\d+))?$/);
  if (!match) return raw;
  return match[1].replace(/\B(?=(\d{3})+(?!\d))/g, ',') + '.' + (match[2] || '').padEnd(2, '0').slice(0, 2);
}
function cents(value) {
  const match = String(value).match(/^(\d+)(?:\.(\d{1,2}))?$/);
  return match ? BigInt(match[1]) * 100n + BigInt((match[2] || '').padEnd(2, '0')) : null;
}
function when(value) {
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? '—' : date.toLocaleString();
}
function state(id, message, error = false) {
  const node = $(id);
  node.hidden = !message;
  node.textContent = message;
  node.className = error ? 'state error' : 'state';
}
function feedback(id, message, kind = '') {
  const node = $(id);
  node.textContent = message;
  node.className = 'feedback ' + kind;
}
async function api(path, options = {}) {
  let response;
  try { response = await fetch(path, options); }
  catch { throw { uncertain: true, message: 'Could not reach the server. Check the connection and try again.' }; }
  let data;
  try { data = await response.json(); }
  catch { data = null; }
  if (!response.ok) throw {
    uncertain: response.status >= 500,
    message: response.status >= 500 ? 'The server could not confirm the request.' : (data?.error || 'The request could not be completed.')
  };
  if (data === null) throw { uncertain: true, message: 'The server response could not be confirmed.' };
  return data;
}
function accountName(id) {
  const account = accounts.find((item) => item.id === id);
  return account ? account.displayName : 'Account #' + id;
}
function cell(row, value, className = '') {
  const td = document.createElement('td');
  td.textContent = value;
  td.className = className;
  row.append(td);
  return td;
}
function pill(row, value, className) {
  const td = cell(row, '');
  const span = document.createElement('span');
  span.className = className;
  span.textContent = value;
  td.append(span);
}
function selectSection(name) {
  if (!descriptions[name]) return;
  activeSection = name;
  for (const section of Object.keys(descriptions)) {
    $('section-' + section).hidden = section !== name;
    const button = document.querySelector('[data-section="' + section + '"]');
    button.classList.toggle('active', section === name);
    if (section === name) button.setAttribute('aria-current', 'page');
    else button.removeAttribute('aria-current');
  }
  $('page-title').textContent = name[0].toUpperCase() + name.slice(1);
  $('page-description').textContent = descriptions[name];
  if (name === 'overview') loadOverview();
  if (name === 'accounts') loadAccounts();
  if (name === 'transfers') loadAccounts();
  if (name === 'history') loadAccounts().finally(loadHistory);
}
function populateSelect(id, selected, placeholder = 'Select account') {
  const select = $(id);
  select.replaceChildren();
  const empty = document.createElement('option');
  empty.value = '';
  empty.textContent = placeholder;
  select.append(empty);
  for (const account of accounts) {
    const option = document.createElement('option');
    option.value = String(account.id);
    option.textContent = account.displayName + ' · #' + account.id;
    select.append(option);
  }
  select.value = selected || '';
}
function renderAccounts() {
  const list = $('accounts-list');
  list.replaceChildren();
  $('account-count').textContent = accounts.length + (accounts.length === 1 ? ' account' : ' accounts');
  state('accounts-state', accounts.length ? '' : 'No accounts yet. Create one to get started.');
  for (const account of accounts) {
    const row = document.createElement('div');
    row.className = 'account-item';
    const identity = document.createElement('div');
    const name = document.createElement('div');
    name.className = 'account-name';
    name.textContent = account.displayName;
    const id = document.createElement('div');
    id.className = 'account-id';
    id.textContent = 'Account #' + account.id;
    identity.append(name, id);
    const actions = document.createElement('div');
    actions.className = 'account-actions';
    const balance = document.createElement('div');
    balance.className = 'account-balance';
    balance.textContent = money(account.balance);
    const unit = document.createElement('small');
    unit.textContent = 'virtual units';
    balance.append(unit);
    const button = document.createElement('button');
    button.className = 'text-button';
    button.type = 'button';
    button.textContent = 'Statement →';
    button.setAttribute('aria-label', 'View statement for ' + account.displayName);
    button.addEventListener('click', () => openStatement(account.id));
    actions.append(balance, button);
    row.append(identity, actions);
    list.append(row);
  }
}
function updateBalanceHint() {
  const account = accounts.find((item) => String(item.id) === $('from-account').value);
  $('available-balance').textContent = account
    ? 'Available: ' + money(account.balance) + ' virtual units'
    : 'Select a sender to see its balance.';
}
function updateTransferControls() {
  const locked = Boolean(pending);
  for (const id of ['from-account', 'to-account', 'transfer-amount']) $(id).disabled = locked || transferBusy;
  $('transfer-submit').disabled = locked || transferBusy || accounts.length < 2;
  $('retry-transfer').disabled = transferBusy;
  $('discard-transfer').disabled = transferBusy;
  $('pending-attempt').hidden = !locked;
  if (locked) {
    $('from-account').value = String(pending.payload.fromAccountId);
    $('to-account').value = String(pending.payload.toAccountId);
    $('transfer-amount').value = pending.payload.amount;
    updateBalanceHint();
  }
}
async function loadAccounts() {
  $('refresh-accounts').disabled = true;
  state('accounts-state', 'Loading accounts…');
  try {
    const result = await api('/api/accounts');
    if (!Array.isArray(result)) throw new Error('Invalid accounts response');
    const selections = ['from-account', 'to-account', 'filter-from', 'filter-to'].map((id) => $(id).value);
    accounts = result;
    renderAccounts();
    ['from-account', 'to-account', 'filter-from', 'filter-to'].forEach((id, index) =>
      populateSelect(id, selections[index], id.startsWith('filter') ? 'Any account' : 'Select account'));
    updateBalanceHint();
    updateTransferControls();
  } catch {
    accounts = [];
    renderAccounts();
    ['from-account', 'to-account', 'filter-from', 'filter-to'].forEach((id) =>
      populateSelect(id, '', id.startsWith('filter') ? 'Any account' : 'Select account'));
    updateBalanceHint();
    updateTransferControls();
    state('accounts-state', 'Accounts could not be loaded. Try refreshing.', true);
  } finally { $('refresh-accounts').disabled = false; }
}
async function loadSummary() {
  state('overview-state', 'Loading overview…');
  $('metric-grid').hidden = true;
  try {
    const result = await api('/api/analytics/summary');
    $('metric-accounts').textContent = String(result.accountCount);
    $('metric-transfers').textContent = String(result.completedTransferCount);
    $('metric-volume').textContent = money(result.totalTransferredAmount);
    $('metric-balances').textContent = money(result.totalAccountBalances);
    $('metric-grid').hidden = false;
    state('overview-state', '');
  } catch { state('overview-state', 'Overview totals could not be loaded. Try refreshing.', true); }
}
async function loadActivity() {
  state('activity-state', 'Loading activity…');
  $('activity-chart').hidden = true;
  try {
    const days = await api('/api/analytics/daily-volume');
    if (!Array.isArray(days)) throw new Error('Invalid activity response');
    const chart = $('activity-chart');
    chart.replaceChildren();
    const amounts = days.map((day) => cents(day.totalAmount) ?? 0n);
    const max = amounts.reduce((highest, amount) => amount > highest ? amount : highest, 0n);
    const labels = [];
    days.forEach((day, index) => {
      const column = document.createElement('div'); column.className = 'chart-day';
      const value = document.createElement('div'); value.className = 'chart-value';
      value.textContent = money(day.totalAmount);
      const track = document.createElement('div'); track.className = 'chart-track';
      const bar = document.createElement('div'); bar.className = 'chart-bar';
      bar.style.height = (max === 0n ? 0 : Number(amounts[index] * 100n / max)) + '%';
      track.append(bar);
      const label = document.createElement('div'); label.className = 'chart-label';
      const date = new Date(day.date + 'T00:00:00Z');
      label.textContent = Number.isNaN(date.getTime()) ? day.date : date.toLocaleDateString(undefined, {month:'short',day:'numeric',timeZone:'UTC'});
      column.append(value, track, label);
      chart.append(column);
      labels.push(day.date + ': ' + money(day.totalAmount) + ' virtual units');
    });
    chart.setAttribute('aria-label', 'Daily transfer volume in UTC. ' + labels.join('; '));
    chart.hidden = days.length === 0;
    state('activity-state', days.length ? '' : 'No activity data is available.');
  } catch { state('activity-state', 'Daily activity could not be loaded.', true); }
}
async function loadRecent() {
  state('recent-state', 'Loading transfers…');
  $('recent-table').hidden = true;
  try {
    const result = await api('/api/transfers?paged=true&page=0&size=5');
    if (!Array.isArray(result.content)) throw new Error('Invalid transfer response');
    const body = $('recent-body'); body.replaceChildren();
    for (const transfer of result.content) {
      const row = document.createElement('tr');
      cell(row, '#' + transfer.id);
      cell(row, accountName(transfer.fromAccountId) + ' → ' + accountName(transfer.toAccountId));
      cell(row, money(transfer.amount), 'amount');
      cell(row, when(transfer.createdAt));
      body.append(row);
    }
    $('recent-table').hidden = result.content.length === 0;
    state('recent-state', result.content.length ? '' : 'No transfers yet.');
  } catch { state('recent-state', 'Recent transfers could not be loaded.', true); }
}
async function loadOverview() {
  $('refresh-overview').disabled = true;
  await Promise.all([loadAccounts().finally(loadRecent), loadSummary(), loadActivity()]);
  $('refresh-overview').disabled = false;
}
async function openStatement(id) {
  statementAccountId = id;
  statementPage = 0;
  $('accounts-main').hidden = true;
  $('statement-panel').hidden = false;
  await loadStatement();
}
async function loadStatement() {
  if (statementAccountId === null) return;
  const id = statementAccountId;
  state('statement-state', 'Loading statement…');
  $('statement-table').hidden = true;
  $('statement-pagination').hidden = true;
  try {
    const result = await api('/api/accounts/' + encodeURIComponent(id) + '/statement?page=' + statementPage + '&size=10');
    if (statementAccountId !== id) return;
    const page = result.entries;
    if (!page || !Array.isArray(page.content)) throw new Error('Invalid statement response');
    $('statement-heading').textContent = result.displayName + ' · #' + result.accountId;
    $('statement-balance').textContent = 'Current balance: ' + money(result.currentBalance) + ' virtual units';
    const body = $('statement-body'); body.replaceChildren();
    for (const entry of page.content) {
      const row = document.createElement('tr');
      cell(row, when(entry.createdAt));
      pill(row, entry.direction, 'direction-pill ' + (entry.direction === 'CREDIT' ? 'credit' : 'debit'));
      cell(row, accountName(entry.counterpartyAccountId));
      cell(row, (entry.direction === 'CREDIT' ? '+' : '−') + money(entry.amount), 'amount');
      cell(row, '#' + entry.transferId);
      body.append(row);
    }
    $('statement-table').hidden = page.content.length === 0;
    state('statement-state', page.content.length ? '' : 'No statement entries yet.');
    $('statement-page-info').textContent = 'Page ' + (page.page + 1) + ' of ' + Math.max(page.totalPages, 1) + ' · ' + page.totalElements + ' entries';
    $('statement-previous').disabled = page.page <= 0;
    $('statement-next').disabled = page.page + 1 >= page.totalPages;
    $('statement-pagination').hidden = page.totalElements === 0;
  } catch { state('statement-state', 'Statement could not be loaded. Try again.', true); }
}
function readFilters() {
  const pairs = [
    ['transactionId', 'filter-id'], ['fromAccountId', 'filter-from'], ['toAccountId', 'filter-to'],
    ['startDate', 'filter-start'], ['endDate', 'filter-end']
  ];
  return Object.fromEntries(pairs.map(([key,id]) => [key, $(id).value.trim()]).filter(([,value]) => value));
}
async function loadHistory() {
  $('refresh-history').disabled = true;
  state('history-state', 'Loading transfers…');
  $('history-table').hidden = true;
  $('history-pagination').hidden = true;
  $('history-count').textContent = '';
  const params = new URLSearchParams({paged:'true',page:String(historyPage),size:'10',...historyFilters});
  try {
    const result = await api('/api/transfers?' + params);
    if (!Array.isArray(result.content)) throw new Error('Invalid history response');
    const body = $('history-body'); body.replaceChildren();
    for (const transfer of result.content) {
      const row = document.createElement('tr');
      cell(row, '#' + transfer.id);
      cell(row, accountName(transfer.fromAccountId));
      cell(row, accountName(transfer.toAccountId));
      cell(row, money(transfer.amount), 'amount');
      pill(row, transfer.status, 'status-pill');
      cell(row, when(transfer.createdAt));
      const action = cell(row, '');
      const button = document.createElement('button');
      button.className = 'text-button'; button.type = 'button'; button.textContent = 'View details';
      button.setAttribute('aria-label', 'View details for transaction ' + transfer.id);
      button.addEventListener('click', () => openDetails(transfer.id, button));
      action.append(button); body.append(row);
    }
    $('history-table').hidden = result.content.length === 0;
    state('history-state', result.content.length ? '' : 'No transfers match these filters.');
    $('history-count').textContent = result.totalElements + (result.totalElements === 1 ? ' result' : ' results');
    $('history-page-info').textContent = 'Page ' + (result.page + 1) + ' of ' + Math.max(result.totalPages, 1) + ' · ' + result.totalElements + ' results';
    $('history-previous').disabled = result.page <= 0;
    $('history-next').disabled = result.page + 1 >= result.totalPages;
    $('history-pagination').hidden = result.totalElements === 0;
  } catch { state('history-state', 'Transfer history could not be loaded. Try refreshing.', true); }
  finally { $('refresh-history').disabled = false; }
}
async function openDetails(id, trigger) {
  detailsReturnFocus = trigger;
  const dialog = $('details-dialog');
  $('details-heading').textContent = 'Transaction #' + id;
  $('details-list').hidden = true;
  state('details-state', 'Loading details…');
  dialog.showModal();
  $('close-details').focus();
  try {
    const result = await api('/api/transfers/' + encodeURIComponent(id));
    const rows = [
      ['Transaction ID', '#' + result.id], ['Sender account', accountName(result.fromAccountId)],
      ['Receiver account', accountName(result.toAccountId)], ['Amount', money(result.amount) + ' virtual units'],
      ['Status', result.status], ['Timestamp', when(result.createdAt)]
    ];
    if (result.idempotencyKey != null) rows.push(['Idempotency key', result.idempotencyKey]);
    const list = $('details-list'); list.replaceChildren();
    for (const [term, value] of rows) {
      const dt = document.createElement('dt'); dt.textContent = term;
      const dd = document.createElement('dd'); dd.textContent = value;
      list.append(dt, dd);
    }
    list.hidden = false;
    state('details-state', '');
  } catch { state('details-state', 'Transaction details could not be loaded.', true); }
}
function newKey() {
  if (crypto.randomUUID) return crypto.randomUUID();
  const bytes = crypto.getRandomValues(new Uint8Array(16));
  return Array.from(bytes, (byte) => byte.toString(16).padStart(2, '0')).join('');
}
async function sendTransfer() {
  if (!pending || transferBusy) return;
  transferBusy = true; updateTransferControls();
  feedback('transfer-feedback', 'Submitting transfer…');
  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), 15000);
  try {
    const result = await api('/api/transfers', {
      method:'POST',headers:{'Content-Type':'application/json','Idempotency-Key':pending.key},
      body:JSON.stringify(pending.payload),signal:controller.signal
    });
    if (!result?.id || result.status !== 'COMPLETED') throw { uncertain:true,message:'The server response could not be confirmed.' };
    savePending(null);
    $('transfer-form').reset();
    feedback('transfer-feedback', 'Transfer #' + result.id + ' completed.', 'success');
    await Promise.all([loadAccounts(), loadSummary(), loadActivity(), loadRecent(), activeSection === 'history' ? loadHistory() : Promise.resolve()]);
  } catch (error) {
    if (error.uncertain) feedback('transfer-feedback', error.message + ' Retry with the same key to confirm the outcome.', 'error');
    else {
      savePending(null);
      feedback('transfer-feedback', error.message || 'Transfer rejected.', 'error');
      await loadAccounts();
    }
  } finally {
    clearTimeout(timeout);
    transferBusy = false; updateTransferControls();
  }
}
document.querySelectorAll('[data-section]').forEach((button) => button.addEventListener('click', () => selectSection(button.dataset.section)));
$('view-history').addEventListener('click', () => selectSection('history'));
$('refresh-overview').addEventListener('click', loadOverview);
$('refresh-accounts').addEventListener('click', loadAccounts);
$('refresh-history').addEventListener('click', loadHistory);
$('from-account').addEventListener('change', updateBalanceHint);
$('close-statement').addEventListener('click', () => {
  statementAccountId = null;
  $('statement-panel').hidden = true;
  $('accounts-main').hidden = false;
});
$('statement-previous').addEventListener('click', () => { if (statementPage > 0) { statementPage--; loadStatement(); } });
$('statement-next').addEventListener('click', () => { statementPage++; loadStatement(); });
$('history-previous').addEventListener('click', () => { if (historyPage > 0) { historyPage--; loadHistory(); } });
$('history-next').addEventListener('click', () => { historyPage++; loadHistory(); });
$('filter-form').addEventListener('submit', (event) => {
  event.preventDefault();
  if ($('filter-start').value && $('filter-end').value && $('filter-start').value > $('filter-end').value) {
    feedback('filter-feedback', 'Start date must be on or before end date.', 'error'); return;
  }
  feedback('filter-feedback', '');
  historyFilters = readFilters(); historyPage = 0; loadHistory();
});
$('clear-filters').addEventListener('click', () => {
  $('filter-form').reset(); feedback('filter-feedback', '');
  historyFilters = {}; historyPage = 0; loadHistory();
});
$('close-details').addEventListener('click', () => $('details-dialog').close());
$('details-dialog').addEventListener('close', () => detailsReturnFocus?.focus());
$('details-dialog').addEventListener('click', (event) => { if (event.target === $('details-dialog')) $('details-dialog').close(); });
$('create-form').addEventListener('submit', async (event) => {
  event.preventDefault();
  const name = $('display-name').value.trim();
  const amount = $('initial-balance').value.trim();
  if (!name || cents(amount) === null || amount.split('.')[0].length > 17) {
    feedback('create-feedback', 'Enter a name and a nonnegative balance with at most two decimals.', 'error'); return;
  }
  const button = $('create-submit'); button.disabled = true;
  feedback('create-feedback', 'Creating account…');
  try {
    const account = await api('/api/accounts', {
      method:'POST',headers:{'Content-Type':'application/json'},
      body:JSON.stringify({displayName:name,initialBalance:amount})
    });
    $('create-form').reset();
    feedback('create-feedback', 'Account #' + account.id + ' created.', 'success');
    await Promise.all([loadAccounts(), loadSummary()]);
  } catch (error) {
    feedback('create-feedback', error.uncertain ? 'Could not confirm creation. Refresh accounts before trying again.' : error.message, 'error');
  } finally { button.disabled = false; }
});
$('transfer-form').addEventListener('submit', (event) => {
  event.preventDefault();
  if (pending || transferBusy) return;
  const from = $('from-account').value, to = $('to-account').value;
  const amount = $('transfer-amount').value.trim();
  const value = cents(amount);
  if (!from || !to || from === to) { feedback('transfer-feedback', 'Choose two different accounts.', 'error'); return; }
  if (value === null || value <= 0n || amount.split('.')[0].length > 17) {
    feedback('transfer-feedback', 'Enter a positive amount with at most two decimals.', 'error'); return;
  }
  savePending({key:newKey(),payload:{fromAccountId:Number(from),toAccountId:Number(to),amount}});
  sendTransfer();
});
$('retry-transfer').addEventListener('click', sendTransfer);
$('discard-transfer').addEventListener('click', async () => {
  savePending(null);
  feedback('transfer-feedback', 'Pending attempt discarded. Review balances and history before creating another transfer.');
  await Promise.all([loadAccounts(), loadRecent(), loadSummary(), loadActivity()]);
});
updateTransferControls();
loadOverview();
