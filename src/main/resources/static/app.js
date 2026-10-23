const $ = (id) => document.getElementById(id);
const pendingStorageKey = 'transact.pendingTransfer';
let accounts = [];
let pending = loadPending();
let transferBusy = false;

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
  } catch { /* This page still retains the attempt until it is closed. */ }
  updateTransferControls();
}

function money(value) {
  const raw = String(value ?? '0');
  const match = raw.match(/^(\d+)(?:\.(\d+))?$/);
  if (!match) return raw;
  const whole = match[1].replace(/\B(?=(\d{3})+(?!\d))/g, ',');
  return `${whole}.${(match[2] || '').padEnd(2, '0').slice(0, 2)}`;
}

function cents(value) {
  const match = String(value).match(/^(\d+)(?:\.(\d{1,2}))?$/);
  return match ? BigInt(match[1]) * 100n + BigInt((match[2] || '').padEnd(2, '0')) : null;
}

function feedback(id, message, kind = '') {
  const node = $(id);
  node.textContent = message;
  node.className = `feedback ${kind}`;
}

async function api(path, options = {}) {
  let response;
  try { response = await fetch(path, options); }
  catch { throw { uncertain: true, message: 'Could not reach the server. Check the connection and try again.' }; }
  let data;
  try { data = await response.json(); }
  catch { data = null; }
  if (!response.ok) {
    throw {
      uncertain: response.status >= 500,
      message: response.status >= 500 ? 'The server could not confirm the request.' : (data?.error || 'The request could not be completed.')
    };
  }
  if (data === null) throw { uncertain: true, message: 'The server response could not be confirmed.' };
  return data;
}

function accountName(id) {
  const account = accounts.find((item) => item.id === id);
  return account ? account.displayName : `Account #${id}`;
}

function renderAccounts() {
  const list = $('accounts-list');
  list.replaceChildren();
  const state = $('accounts-state');
  state.hidden = accounts.length > 0;
  state.textContent = accounts.length ? '' : 'No accounts yet. Create one to get started.';
  for (const account of accounts) {
    const row = document.createElement('div'); row.className = 'account-item';
    const identity = document.createElement('div');
    const name = document.createElement('div'); name.className = 'account-name'; name.textContent = account.displayName;
    const id = document.createElement('div'); id.className = 'account-id'; id.textContent = `Account #${account.id}`;
    identity.append(name, id);
    const balance = document.createElement('div'); balance.className = 'account-balance';
    balance.textContent = money(account.balance);
    const unit = document.createElement('small'); unit.textContent = 'virtual units'; balance.append(unit);
    row.append(identity, balance); list.append(row);
  }
}

function populateSelect(id, selected) {
  const select = $(id);
  select.replaceChildren();
  const placeholder = document.createElement('option');
  placeholder.value = ''; placeholder.textContent = 'Select account'; select.append(placeholder);
  for (const account of accounts) {
    const option = document.createElement('option');
    option.value = String(account.id);
    option.textContent = `${account.displayName} · #${account.id}`;
    select.append(option);
  }
  select.value = selected || '';
}

function updateBalanceHint() {
  const account = accounts.find((item) => String(item.id) === $('from-account').value);
  $('available-balance').textContent = account
    ? `Available: ${money(account.balance)} virtual units`
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
  const state = $('accounts-state');
  const refresh = $('refresh-accounts');
  refresh.disabled = true;
  state.hidden = false; state.className = 'state'; state.textContent = 'Loading accounts…';
  try {
    const result = await api('/api/accounts');
    accounts = result;
    const from = $('from-account').value, to = $('to-account').value;
    renderAccounts();
    populateSelect('from-account', from);
    populateSelect('to-account', to);
    updateBalanceHint();
    updateTransferControls();
  } catch {
    state.hidden = false; state.className = 'state error';
    state.textContent = 'Accounts could not be loaded. Try refreshing.';
  } finally { refresh.disabled = false; }
}

function cell(row, text, className = '') {
  const td = document.createElement('td');
  td.textContent = text;
  td.className = className;
  row.append(td);
  return td;
}

async function loadHistory() {
  const state = $('history-state');
  const refresh = $('refresh-history');
  refresh.disabled = true;
  state.hidden = false; state.className = 'state'; state.textContent = 'Loading transfers…';
  try {
    const transfers = await api('/api/transfers');
    const body = $('history-body'); body.replaceChildren();
    $('history-table').hidden = transfers.length === 0;
    state.hidden = transfers.length > 0;
    state.textContent = transfers.length ? '' : 'No transfers yet. Your activity will appear here.';
    for (const transfer of transfers) {
      const row = document.createElement('tr');
      cell(row, `#${transfer.id}`);
      cell(row, accountName(transfer.fromAccountId));
      cell(row, accountName(transfer.toAccountId));
      cell(row, money(transfer.amount), 'amount');
      const status = cell(row, '');
      const pill = document.createElement('span'); pill.className = 'status-pill';
      pill.textContent = transfer.status; status.append(pill);
      const when = new Date(transfer.createdAt);
      cell(row, Number.isNaN(when.getTime()) ? '—' : when.toLocaleString());
      body.append(row);
    }
  } catch {
    state.hidden = false; state.className = 'state error';
    state.textContent = 'Transfer history could not be loaded. Try refreshing.';
  } finally { refresh.disabled = false; }
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
      method: 'POST',
      headers: { 'Content-Type': 'application/json', 'Idempotency-Key': pending.key },
      body: JSON.stringify(pending.payload), signal: controller.signal
    });
    savePending(null);
    $('transfer-form').reset();
    feedback('transfer-feedback', `Transfer #${result.id} completed.`, 'success');
    await Promise.all([loadAccounts(), loadHistory()]);
  } catch (error) {
    if (error.uncertain) {
      feedback('transfer-feedback', `${error.message} Retry with the same key to confirm the outcome.`, 'error');
    } else {
      savePending(null);
      feedback('transfer-feedback', error.message || 'Transfer rejected.', 'error');
      await Promise.all([loadAccounts(), loadHistory()]);
    }
  } finally {
    clearTimeout(timeout);
    transferBusy = false; updateTransferControls();
  }
}

$('create-form').addEventListener('submit', async (event) => {
  event.preventDefault();
  const name = $('display-name').value.trim();
  const amount = $('initial-balance').value.trim();
  if (!name || cents(amount) === null || cents(amount) < 0n || amount.split('.')[0].length > 17) {
    feedback('create-feedback', 'Enter a name and a nonnegative balance with at most two decimals.', 'error'); return;
  }
  const button = $('create-submit'); button.disabled = true;
  feedback('create-feedback', 'Creating account…');
  try {
    const account = await api('/api/accounts', {
      method: 'POST', headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ displayName: name, initialBalance: amount })
    });
    $('create-form').reset();
    feedback('create-feedback', `Account #${account.id} created.`, 'success');
    await loadAccounts();
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
  savePending({ key: newKey(), payload: { fromAccountId: Number(from), toAccountId: Number(to), amount } });
  sendTransfer();
});

$('retry-transfer').addEventListener('click', sendTransfer);
$('discard-transfer').addEventListener('click', async () => {
  savePending(null);
  feedback('transfer-feedback', 'Pending attempt discarded. Review balances and history before creating another transfer.');
  await Promise.all([loadAccounts(), loadHistory()]);
});
$('refresh-accounts').addEventListener('click', loadAccounts);
$('refresh-history').addEventListener('click', loadHistory);
$('from-account').addEventListener('change', updateBalanceHint);

updateTransferControls();
loadAccounts().finally(loadHistory);
