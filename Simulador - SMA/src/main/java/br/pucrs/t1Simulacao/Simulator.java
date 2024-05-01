package br.pucrs.t1Simulacao;

import br.pucrs.t1Simulacao.Evento.TipoEnum;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

public class Simulator {

    private static final String DADOS = "application.yml";
    private int qtdNumRandom;
    private final EscalonadorDeFilas escalonadorDeFilas;
    private final List<Evento> eventosAgendados = new ArrayList<>();
    private double tempo;
    private double tempoAnterior = 0;
    private final HashMap<Integer, double[]> probabilidades = new HashMap<>();
    private int semente;
    private final Aleatorio aleatorios;

    public Simulator() {
        escalonadorDeFilas = new EscalonadorDeFilas();
        obterDadosYml();
        aleatorios = new Aleatorio(this.qtdNumRandom, semente);
    }

    public void simulacao () {
        while (aleatorios.getQtAleatorios() < this.qtdNumRandom) {
            var eventoAtual = eventosAgendados.remove(0);             //Remove o evento dos agendados, pois já está sendo executado

            //Variável tempoAnterior é utilizada para o cálculo de probabilidade
            tempoAnterior = tempo;
            tempo = eventoAtual.getTempo();

            var filaAtual = escalonadorDeFilas.getFilas().get(eventoAtual.getIdOrigem());

            var filaDestino = eventoAtual.getIdDestino() != null ? escalonadorDeFilas.getFilas().get(eventoAtual.getIdDestino()) : null;

            switch (eventoAtual.getTipo()) {
                case CHEGADA:
                    chegada(filaAtual);
                    break;
                case SAIDA:
                    saida(filaAtual);
                    break;
                case PASSAGEM:
                    passagem(filaAtual, filaDestino);
                    break;
            }

        }
        //Exibir probabilidades
        this.exibirProbabilidade();
    }

    private void passagem(Fila origem, Fila destino) {
        if (destino == null || origem == null) return;
        ajustarProbabilidade();
        origem.setPopulacaoAtual(origem.getPopulacaoAtual() - 1);

        if (origem.getPopulacaoAtual() >= origem.getServidores()) {
            var destinoProxFilaOrigem = sort(origem);
            if (destinoProxFilaOrigem != null) {
                addPassagem(origem, destinoProxFilaOrigem);
            } else {
                addEvento(origem, TipoEnum.SAIDA);
            }
        }

        if (filaPodeAtender(destino)) {
            destino.setPopulacaoAtual(destino.getPopulacaoAtual() + 1);
            if (destino.getPopulacaoAtual() <= destino.getServidores()) {
                var destino2 = sort(destino);
                if (destino2 != null) { // se for para outra fila
                    addPassagem(destino, destino2);
                } else {
                    addEvento(destino, TipoEnum.SAIDA);
                }
            }
        } else {
            destino.setPerdidos(destino.getPerdidos() + 1);
        }

    }

    private void chegada(Fila filaAtual) {
        ajustarProbabilidade();
        //Se ainda tempo espaço na fila
        if (filaPodeAtender(filaAtual)) {
            filaAtual.setPopulacaoAtual(filaAtual.getPopulacaoAtual() + 1);

            //Se só tem uma pessoa na fila ou nenhuma, essa pessoa já é atendida
            if (filaAtual.getPopulacaoAtual() <= filaAtual.getServidores()) {
                //System.out.println("EXECUTADO |" + eventoAtual.getTipo() + " | " + eventoAtual.getTempo());

                Fila destino = sort(filaAtual);
                if (destino != null) {
                    addPassagem(filaAtual, destino);
                } else {
                    addEvento(filaAtual, TipoEnum.SAIDA);
                }
            }
        } else {
            //Não conseguiu entrar na fila pois estava cheia. E contabilizada como uma pessoa perdida
            filaAtual.setPerdidos(filaAtual.getPerdidos() + 1);
        }

        addEvento(filaAtual, TipoEnum.CHEGADA);
    }

    private boolean filaPodeAtender(Fila filaAtual) {
        return filaAtual.getCapacidade() == -1 || filaAtual.getPopulacaoAtual() < filaAtual.getCapacidade();
    }

    private void saida(Fila filaAtual) {
        ajustarProbabilidade();
        filaAtual.setPopulacaoAtual(filaAtual.getPopulacaoAtual() - 1);

        //Se tem gente na espera pra ficar de frente para o servidor
        if (filaAtual.getPopulacaoAtual() >= filaAtual.getServidores()) {
            var destino = sort(filaAtual);
            if (destino != null) {
                addPassagem(filaAtual, destino);
            } else {
                addEvento(filaAtual, TipoEnum.SAIDA);
            }
        }
    }

    public void obterDadosYml() {
        final Map<String, Object> dados = obterArquivo(DADOS);
        if (dados == null) return;
        qtdNumRandom = (int) dados.get("numeros-aleatorios");
        semente = (int) dados.get("semente");

        final List<HashMap<String, Object>> dadosFilas = (List<HashMap<String, Object>>) dados.get("filas");

        List<Fila> filas = dadosFilas.stream().map(fila -> {
            var novaFila = new Fila();
            novaFila.setId(fila.containsKey("id") ? (int) fila.get("id") : 1);
            novaFila.setCapacidade((int) fila.get("capacidade"));
            novaFila.setChegadaInicial((double) fila.getOrDefault("chegada-inicial", -1.0));
            novaFila.setChegadaMaxima((double) fila.getOrDefault("chegada-maxima", -1.0));
            novaFila.setChegadaMinima((double) fila.getOrDefault("chegada-minima", -1.0));
            novaFila.setSaidaMaxima((double) fila.getOrDefault("saida-maxima", -1.0));
            novaFila.setSaidaMinima((double) fila.getOrDefault("saida-minima", -1.0));
            novaFila.setServidores((int) fila.get("servidores"));
            return novaFila;
        }).collect(Collectors.toList());

        List<LinkedHashMap<String, Object>> dadosRedes = (List<LinkedHashMap<String, Object>>) dados.get("redes");

        // Popula cada fila
        for (HashMap<String, Object> rede : dadosRedes) {

            int origem = (int) rede.get("origem");
            int destino = (int) rede.get("destino");
            double probabilidade = (double) rede.get("probabilidade");

            var filaOrigem = filas.stream().filter(f -> f.getId() == origem).findFirst().get();
            var filaDestino = filas.stream().filter(f -> f.getId() == destino).findFirst().get();

            filaOrigem.putToFilaDestino(destino, filaDestino);
            // propabilidade pega do yml
            filaOrigem.putToProbabilidades(destino, probabilidade);
        }


        escalonadorDeFilas.getFilas().addAll(filas); //Adiciona todas filas no escalonador
        escalonadorDeFilas.getFilas().remove(0); //primeira fila é vazia

        //Adiciona probabilidade % de chance de a fila estar com x pessoas em seu k de multiplas filas
        escalonadorDeFilas.getFilas().forEach(f -> {
            probabilidades.put(f.getId(), new double[f.getCapacidade() != -1 ? f.getCapacidade() + 1 : 10]);
        });

        //o primeiro evento precisa ser agendado
        Evento primeiroEvento = new Evento(TipoEnum.CHEGADA,
                escalonadorDeFilas.getFilas().get(0).getChegadaInicial(),
                escalonadorDeFilas.getFilas().get(0).getId());
        eventosAgendados.add(primeiroEvento);
    }

    private Map<String, Object> obterArquivo(String arquivoYml) {
        try {
            final InputStream inputStream = Simulator.class
                    .getClassLoader()
                    .getResourceAsStream(arquivoYml);

            final Map<String, Object> data = new Yaml().load(inputStream);
            return data;
        } catch (Exception e) {
            return null;
        }
    }

    public void addEvento(Fila filaAtual, TipoEnum tipoEnum) {
        double tempoCalculado = 0;
        if (tipoEnum == TipoEnum.SAIDA) {
            tempoCalculado = ((filaAtual.getSaidaMaxima() - filaAtual.getSaidaMinima())
                    * aleatorios.geraProximoAleatorio()
                    + filaAtual.getSaidaMinima()) + tempo;
        } else if (tipoEnum == TipoEnum.CHEGADA) {
             tempoCalculado = ((filaAtual.getChegadaMaxima() - filaAtual.getChegadaMinima())
                    * aleatorios.geraProximoAleatorio()
                    + filaAtual.getChegadaMinima()) + tempo;
        }
        eventosAgendados.add(new Evento(tipoEnum, tempoCalculado, filaAtual.getId()));
        eventosAgendados.sort(Comparator.comparingDouble(Evento::getTempo));
    }

    private void addPassagem(Fila filaOrigem, Fila filaDestino) {
        double tempoSaida =((filaOrigem.getSaidaMaxima() - filaOrigem.getSaidaMinima())
                * aleatorios.geraProximoAleatorio()
                + filaOrigem.getSaidaMinima()) + tempo;
        eventosAgendados.add(new Evento(TipoEnum.PASSAGEM, tempoSaida, filaOrigem.getId(), filaDestino.getId()));
        eventosAgendados.sort(Comparator.comparingDouble(Evento::getTempo));
    }

    private Fila sort(final Fila origem) {
        double intervalo = 0.0;
        final double aleatorio = aleatorios.geraProximoAleatorio();

        // faz o sort do hahmap da menor probabilidade para a maior, afim de calcular o intervalo
        Map<Integer, Double> sortedMap = origem.getProbabilidades().entrySet().stream()
                .sorted(Comparator.comparingDouble(Map.Entry::getValue))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> {
                            throw new AssertionError();
                        },
                        LinkedHashMap::new
                ));

        Fila filaDestino = null;

        for (Integer fila : sortedMap.keySet()) {
            intervalo += sortedMap.get(fila);
            if (aleatorio <= intervalo) { // se o numero aleatorio for menor que o do intervalo
                filaDestino = escalonadorDeFilas.getFilas().get(fila); // adiciona a fila destino
                break; // para iteração, pois ja achou o resultado
            }
        }
        return filaDestino;
    }

    public void ajustarProbabilidade() {
        escalonadorDeFilas.getFilas().forEach(fila -> {
            probabilidades.get(fila.getId())[fila.getPopulacaoAtual()] += this.tempo - this.tempoAnterior;
        });
    }

    public void exibirProbabilidade() {
        System.out.println(this.aleatorios.toString());


        probabilidades.forEach((id, pFilas) -> {
            System.out.println("- Fila: " + id);
            System.out.println("Probabilidades:");
            double porcentagem = 0;
            int i = 0;
            for (double item : pFilas) {
                porcentagem += (item / this.tempo);
                String result = String.format("Value %.4f", ((item / this.tempo) * 100));
                System.out.println("Posição " + i + " : " + result + "%");
                i++;
            }

            System.out.println(porcentagem * 100 + "%");
            System.out.println("Perdidos " + this.escalonadorDeFilas.getFilas().get(id).getPerdidos());
            System.out.println("Tempo total: " + tempo);
        });
    }
}

