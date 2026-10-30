"use strict";

const elemento = (id) => document.getElementById(id);
const moeda = new Intl.NumberFormat("pt-BR", { style: "currency", currency: "BRL" });
const dataHora = new Intl.DateTimeFormat("pt-BR", { dateStyle: "short", timeStyle: "short" });
const campo = elemento("valor");
const dialogo = elemento("confirmacao");
let conta = null;
let operacao = "sacar";
let oculto = false;
let conectado = false;
let atualizando = false;
let enviando = false;
let pedido = null;

function dinheiro(valor) {
  return oculto ? "••••••" : moeda.format(Number(valor));
}

function aviso(texto, erro = false) {
  elemento("aviso").textContent = texto;
  elemento("aviso").hidden = !texto;
  elemento("aviso").classList.toggle("error", erro);
}

function erroCampo(texto = "") {
  elemento("erro-valor").textContent = texto;
  elemento("erro-valor").hidden = !texto;
  campo.setAttribute("aria-invalid", String(Boolean(texto)));
}

function conexao(online) {
  conectado = online;
  elemento("conexao").textContent = online ? "Conectado ao Java local" : "Servidor desconectado";
  elemento("conexao").classList.toggle("offline", !online);
  elemento("continuar").disabled = !online || enviando;
}

function renderizar() {
  if (!conta) return;
  elemento("saldo").textContent = dinheiro(conta.saldo);
  elemento("titular").textContent = conta.titular;
  elemento("presente").textContent = conta.boasVindas;
  const movimentos = conta.movimentos.filter(m => elemento("filtro").value === "TODOS" || m.tipo === elemento("filtro").value);
  elemento("quantidade").textContent = movimentos.length;
  elemento("totais").textContent = `Total depositado: ${dinheiro(conta.entradas)} · Total sacado: ${dinheiro(conta.saidas)}`;
  elemento("extrato-vazio").hidden = movimentos.length > 0;
  elemento("tabela-container").hidden = movimentos.length === 0;
  const linhas = movimentos.map((movimento) => {
    const entrada = movimento.tipo === "DEPOSITO";
    const linha = document.createElement("tr");
    const celula = document.createElement("td");
    const descricao = document.createElement("div");
    descricao.className = "movement-cell";
    const icone = document.createElement("span");
    icone.className = entrada ? "in" : "out";
    icone.textContent = entrada ? "↙" : "↗";
    icone.setAttribute("aria-hidden", "true");
    const texto = document.createElement("div");
    const titulo = document.createElement("strong");
    titulo.textContent = entrada ? "Depósito" : "Saque";
    const detalhe = document.createElement("small");
    detalhe.textContent = `Operação #${movimento.id}`;
    texto.append(titulo, detalhe);
    descricao.append(icone, texto);
    celula.append(descricao);
    const data = document.createElement("td");
    data.className = "date-cell";
    data.textContent = movimento.data;
    const valor = document.createElement("td");
    valor.className = entrada ? "amount-in" : "amount-out";
    valor.textContent = oculto ? "••••••" : `${entrada ? "+" : "−"} ${movimento.valorTexto}`;
    const saldo = document.createElement("td");
    saldo.textContent = oculto ? "••••••" : movimento.saldoTexto;
    linha.append(celula, data, valor, saldo);
    return linha;
  });
  elemento("movimentos").replaceChildren(...linhas);
  if (dialogo.open && pedido) preencherConfirmacao();
}

async function requisitar(caminho, opcoes = {}) {
  const controle = new AbortController();
  const limite = setTimeout(() => controle.abort(), 8000);
  try {
    const resposta = await fetch(caminho, { ...opcoes, signal: controle.signal, cache: "no-store" });
    const dados = await resposta.json();
    if (!resposta.ok) {
      const erro = new Error(dados.erro || "Não foi possível concluir a operação.");
      erro.status = resposta.status;
      throw erro;
    }
    return dados;
  } finally {
    clearTimeout(limite);
  }
}

async function atualizar(manual = false) {
  if (atualizando || enviando) return;
  atualizando = true;
  elemento("atualizar").disabled = true;
  try {
    const dados = await requisitar("/api/conta");
    if (enviando) return;
    if (conta && conta.csrfToken === dados.csrfToken && dados.totalOperacoes < conta.totalOperacoes) return;
    // Um token diferente significa que o processo Java foi reiniciado.
    if (conta && conta.csrfToken !== dados.csrfToken) {
      pedido = null;
      dialogo.close();
      aviso("O programa foi reiniciado. Os dados salvos foram recuperados.");
    }
    conta = dados;
    conexao(true);
    renderizar();
    if (manual) aviso("Saldo e extrato atualizados.");
  } catch (erro) {
    conexao(false);
    if (manual || !conta) aviso("Não foi possível conectar. Verifique se o programa Java está aberto no terminal.", true);
  } finally {
    atualizando = false;
    elemento("atualizar").disabled = false;
  }
}

function valorValido() {
  const texto = campo.value.trim();
  if (!/^[0-9]{1,9}([,.][0-9]{1,2})?$/.test(texto)) {
    throw new Error("Informe um valor como 100 ou 100,50, sem separador de milhar.");
  }
  const valor = Number(texto.replace(",", "."));
  if (valor <= 0) throw new Error("O valor deve ser maior que zero.");
  // A prévia usa centavos inteiros; a validação definitiva ocorre no Java.
  const centavos = Math.round(valor * 100);
  if (operacao === "sacar" && centavos > Math.round(Number(conta.saldo) * 100)) {
    throw new Error("Saldo insuficiente para realizar esse saque.");
  }
  return (centavos / 100).toFixed(2);
}

function preencherConfirmacao() {
  elemento("confirmar-tipo").textContent = pedido.tipo === "sacar" ? "Saque" : "Depósito";
  elemento("confirmar-valor").textContent = dinheiro(pedido.valor);
  const saldo = Math.round(Number(conta.saldo) * 100);
  const valor = Math.round(Number(pedido.valor) * 100);
  elemento("confirmar-saldo").textContent = dinheiro((saldo + (pedido.tipo === "sacar" ? -valor : valor)) / 100);
}

document.querySelectorAll("[data-operacao]").forEach((botao) => {
  botao.addEventListener("click", () => {
    operacao = botao.dataset.operacao;
    document.querySelectorAll("[data-operacao]").forEach((item) => {
      item.setAttribute("aria-pressed", String(item.dataset.operacao === operacao));
    });
    elemento("rotulo-valor").textContent = `Quanto você quer ${operacao}?`;
    elemento("texto-continuar").textContent = operacao === "sacar" ? "Continuar com saque" : "Continuar com depósito";
    erroCampo();
  });
});

document.querySelectorAll("[data-valor]").forEach((botao) => {
  botao.addEventListener("click", () => {
    campo.value = `${botao.dataset.valor},00`;
    erroCampo();
    campo.focus();
  });
});
campo.addEventListener("input", () => erroCampo());

elemento("form-operacao").addEventListener("submit", (evento) => {
  evento.preventDefault();
  if (!conectado || enviando) return;
  try {
    const valor = valorValido();
    // Uma repetição após falha de rede reutiliza o identificador, evitando débito duplo.
    if (!pedido || pedido.tipo !== operacao || pedido.valor !== valor) {
      pedido = { tipo: operacao, valor, pedidoId: crypto.randomUUID() };
    }
    erroCampo();
    preencherConfirmacao();
    elemento("erro-confirmacao").hidden = true;
    dialogo.showModal();
  } catch (erro) {
    erroCampo(erro.message);
    campo.focus();
  }
});

elemento("confirmar").addEventListener("click", async () => {
  if (enviando || !pedido) return;
  enviando = true;
  elemento("confirmar").disabled = true;
  elemento("cancelar").disabled = true;
  elemento("confirmar").textContent = "Processando…";
  elemento("erro-confirmacao").hidden = true;
  conexao(conectado);
  const atual = pedido;
  try {
    // Atualizações periódicas não podem sobrescrever este resultado com um saldo antigo.
    const dados = await requisitar(`/api/${atual.tipo}`, {
      method: "POST",
      headers: { "Content-Type": "application/x-www-form-urlencoded", "X-CSRF-Token": conta.csrfToken },
      body: new URLSearchParams({ valor: atual.valor, pedidoId: atual.pedidoId })
    });
    conta = dados;
    conexao(true);
    renderizar();
    dialogo.close();
    pedido = null;
    campo.value = "";
    aviso(`${atual.tipo === "sacar" ? "Saque" : "Depósito"} realizado. Seu saldo e extrato foram atualizados.`);
  } catch (erro) {
    elemento("erro-confirmacao").textContent = erro.status && erro.status < 500
      ? erro.message
      : "A confirmação não chegou. Confira o extrato antes de uma nova operação. Repetir esta confirmação usa o mesmo identificador.";
    elemento("erro-confirmacao").hidden = false;
    if (!erro.status) conexao(false);
  } finally {
    enviando = false;
    elemento("confirmar").disabled = false;
    elemento("cancelar").disabled = false;
    elemento("confirmar").textContent = "Confirmar operação";
    conexao(conectado);
  }
});

elemento("cancelar").addEventListener("click", () => dialogo.close());
dialogo.addEventListener("cancel", (evento) => { if (enviando) evento.preventDefault(); });
elemento("alternar-saldo").addEventListener("click", () => {
  oculto = !oculto;
  elemento("alternar-saldo").setAttribute("aria-pressed", String(oculto));
  elemento("alternar-saldo").setAttribute("aria-label", oculto ? "Mostrar saldo" : "Ocultar saldo");
  renderizar();
});
elemento("atualizar").addEventListener("click", () => atualizar(true));
elemento("abrir-ajuda").addEventListener("click", () => elemento("ajuda").showModal());
elemento("fechar-ajuda").addEventListener("click", () => elemento("ajuda").close());
document.addEventListener("visibilitychange", () => { if (!document.hidden) atualizar(); });
atualizar();
setInterval(() => { if (!document.hidden) atualizar(); }, 3000);

elemento("filtro").addEventListener("change", renderizar);
