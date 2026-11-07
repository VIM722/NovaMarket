const form = document.querySelector('#order-form');
const category = document.querySelector('#category');
const register = document.querySelector('#register');
const detail = document.querySelector('#detail');
const feedback = document.querySelector('#feedback');
const caseButtons = [...document.querySelectorAll('[data-case]')];

const amounts = new Intl.NumberFormat('es-PE', {
  style: 'currency', currency: 'PEN', minimumFractionDigits: 2
});
const dates = new Intl.DateTimeFormat('es-PE', { dateStyle: 'medium', timeStyle: 'medium' });
let orders = [];
let selectedId;
let confirmingId;

function element(tag, value, className) {
  const node = document.createElement(tag);
  if (value !== undefined) node.textContent = value;
  if (className) node.className = className;
  return node;
}

function notify(message = '', error = false) {
  feedback.textContent = message;
  feedback.classList.toggle('error', error);
  feedback.hidden = !message;
}

async function request(path, data) {
  const options = data === undefined ? {} : {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data)
  };
  const response = await fetch('/api' + path, options);
  if (!response.ok) {
    const problem = await response.json().catch(() => ({}));
    throw new Error(problem.detail || 'La operación no se pudo completar.');
  }
  return response.json();
}

function status(order) {
  const confirmed = order.status === 'CONFIRMADO';
  return element('span', confirmed ? 'Confirmado' : 'Por confirmar', confirmed ? 'badge confirmed' : 'badge pending');
}

function renderDetail(order) {
  const heading = element('div', undefined, 'order-heading');
  const customer = element('div');
  customer.append(element('strong', '#' + order.id + ' · ' + order.customer),
    element('p', order.category.label + ' · ' + order.category.rule));
  heading.append(customer, status(order));

  const totals = element('dl', undefined, 'totals');
  for (const [label, value, className] of [
    ['Importe original', order.amount, ''],
    ['Descuento aplicado', order.discount, ''],
    ['Importe final', order.total, 'grand-total']
  ]) {
    const row = element('div', undefined, className);
    row.append(element('dt', label), element('dd', amounts.format(value)));
    totals.append(row);
  }
  detail.replaceChildren(heading, totals);

  if (order.status !== 'CONFIRMADO') {
    const button = element('button', 'Confirmar pedido', 'primary confirm-button');
    button.type = 'button';
    button.disabled = confirmingId !== undefined;
    button.addEventListener('click', () => confirmOrder(order.id));
    detail.append(button);
    return;
  }

  const event = element('div', undefined, 'event');
  event.append(element('strong', 'PEDIDO_CONFIRMADO'),
    element('p', dates.format(new Date(order.confirmation.occurredAt))),
    element('code', order.confirmation.id));

  const reactions = element('ul', undefined, 'reactions');
  for (const reaction of order.reactions) {
    const item = element('li');
    const label = element('strong');
    label.append(
      element('span', reaction.successful ? '✓ Procesado ·' : '× Falló ·',
        reaction.successful ? 'reaction-status' : 'reaction-status failed'),
      document.createTextNode(reaction.area)
    );
    item.append(label, element('p', reaction.message));
    reactions.append(item);
  }
  detail.append(event, reactions);
}

function render() {
  const confirmed = orders.filter(order => order.status === 'CONFIRMADO').length;
  document.querySelector('#count-all').textContent = orders.length;
  document.querySelector('#count-pending').textContent = orders.length - confirmed;
  document.querySelector('#count-confirmed').textContent = confirmed;

  const selected = orders.find(order => order.id === selectedId);
  if (selected) renderDetail(selected);

  const empty = document.querySelector('#empty-history');
  empty.textContent = 'Todavía no se han registrado pedidos.';
  empty.hidden = orders.length > 0;
  document.querySelector('#history-table').hidden = orders.length === 0;
  const rows = orders.map(order => {
    const row = element('tr');
    if (order.id === selectedId) row.className = 'selected';
    for (const value of ['#' + order.id, order.customer, order.category.label,
      amounts.format(order.discount), amounts.format(order.total)]) {
      row.append(element('td', value));
    }
    const state = element('td');
    state.append(status(order));
    const action = element('td');
    const view = element('button', 'Ver', 'view');
    view.type = 'button';
    view.setAttribute('aria-label', 'Ver pedido ' + order.id);
    view.addEventListener('click', () => {
      selectedId = order.id;
      render();
      const title = document.querySelector('#detail-title');
      title.tabIndex = -1;
      title.focus();
    });
    action.append(view);
    row.append(state, action);
    return row;
  });
  document.querySelector('#history-rows').replaceChildren(...rows);
}

async function confirmOrder(id) {
  if (confirmingId !== undefined) return;
  confirmingId = id;
  detail.querySelector('button').disabled = true;
  notify();
  try {
    const confirmed = await request('/pedidos/' + id + '/confirmar', {});
    orders = orders.map(order => order.id === id ? confirmed : order);
    const failures = confirmed.reactions.some(reaction => !reaction.successful);
    notify(failures ? 'Pedido #' + id + ' confirmado con reacciones fallidas.' : 'Pedido #' + id + ' confirmado.', failures);
  } catch (error) {
    notify(error.message, true);
  } finally {
    confirmingId = undefined;
    render();
  }
}

form.addEventListener('submit', async event => {
  event.preventDefault();
  if (register.disabled) return;
  const data = new FormData(form);
  register.disabled = true;
  notify();
  try {
    const order = await request('/pedidos', {
      customer: data.get('customer'),
      category: data.get('category'),
      amount: Number(data.get('amount'))
    });
    orders.unshift(order);
    selectedId = order.id;
    form.reset();
    notify('Pedido #' + order.id + ' registrado. Ya puedes revisar el cálculo y confirmarlo.');
    render();
  } catch (error) {
    notify(error.message, true);
  } finally {
    register.disabled = false;
  }
});

const samples = {
  NUEVO: 'Ana Torres',
  FRECUENTE: 'Diego Salas',
  NO_IDENTIFICADO: 'Venta sin identificar'
};
for (const button of caseButtons) {
  button.addEventListener('click', () => {
    form.elements.customer.value = samples[button.dataset.case];
    category.value = button.dataset.case;
    form.elements.amount.value = '200.00';
    form.elements.amount.focus();
  });
}

async function load() {
  try {
    const [policies, history] = await Promise.all([request('/politicas'), request('/pedidos')]);
    category.replaceChildren(...policies.map(policy => {
      const option = element('option', policy.label + ' · ' + policy.rule);
      option.value = policy.code;
      option.defaultSelected = policy.code === 'NUEVO';
      return option;
    }));
    orders = history;
    selectedId = orders[0]?.id;
    register.disabled = policies.length === 0;
    for (const button of caseButtons) {
      button.disabled = !policies.some(policy => policy.code === button.dataset.case);
    }
    render();
  } catch {
    category.options[0].textContent = 'Categorías no disponibles';
    document.querySelector('#empty-history').textContent = 'No se pudo cargar el historial.';
    notify('No se pudo conectar con NOVA Market. Comprueba que Spring Boot está en ejecución y recarga la página.', true);
  }
}
load();
