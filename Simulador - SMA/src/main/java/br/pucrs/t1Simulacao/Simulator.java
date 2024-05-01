package br.pucrs.t1Simulacao;

import br.pucrs.t1Simulacao.Event.TipoEnum;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

public class Simulator {

    private static final String DADOS = "application.yml";
    private int qtdNumRandom;
    private final RowScaler escalonadorDeFilas;
    private final List<Event> eventosAgendados = new ArrayList<>();
    private double tempo;
    private double tempoAnterior = 0;
    private final HashMap<Integer, double[]> probabilidades = new HashMap<>();
    private final Random aleatorios;

    public Simulator() {
        escalonadorDeFilas = new RowScaler();
        Map<String, Object> paper = getPaper(DADOS);
        mapperYml(paper);
        aleatorios = new Random(this.qtdNumRandom, (int) paper.get("semente"));
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
        //printa as probabilidades
        this.printProbability();
    }

    private void passagem(Row origem, Row destino) {
        if (destino == null || origem == null) return;
        updateProbability();
        origem.setPopulacaoAtual(origem.getPopulacaoAtual() - 1);

        if (origem.getPopulacaoAtual() >= origem.getServidores()) {
            var destinoProxFilaOrigem = sort(origem);
            if (destinoProxFilaOrigem != null) {
                addPassagem(origem, destinoProxFilaOrigem);
            } else {
                addEvent(origem, TipoEnum.SAIDA);
            }
        }

        if (filaPodeAtender(destino)) {
            destino.setPopulacaoAtual(destino.getPopulacaoAtual() + 1);
            if (destino.getPopulacaoAtual() <= destino.getServidores()) {
                var destino2 = sort(destino);
                if (destino2 != null) { // se for para outra fila
                    addPassagem(destino, destino2);
                } else {
                    addEvent(destino, TipoEnum.SAIDA);
                }
            }
        } else {
            destino.setPerdidos(destino.getPerdidos() + 1);
        }

    }

    private void chegada(Row filaAtual) {
        updateProbability();
        //Se ainda tempo espaço na fila
        if (filaPodeAtender(filaAtual)) {
            filaAtual.setPopulacaoAtual(filaAtual.getPopulacaoAtual() + 1);

            if (filaAtual.getPopulacaoAtual() <= filaAtual.getServidores()) {

                Row destino = sort(filaAtual);
                if (destino != null) {
                    addPassagem(filaAtual, destino);
                } else {
                    addEvent(filaAtual, TipoEnum.SAIDA);
                }
            }
        } else {
            filaAtual.setPerdidos(filaAtual.getPerdidos() + 1);
        }
        addEvent(filaAtual, TipoEnum.CHEGADA);
    }

    private boolean filaPodeAtender(Row filaAtual) {
        return filaAtual.getCapacidade() == -1 || filaAtual.getPopulacaoAtual() < filaAtual.getCapacidade();
    }

    private void saida(Row filaAtual) {
        updateProbability();
        filaAtual.setPopulacaoAtual(filaAtual.getPopulacaoAtual() - 1);
        if (filaAtual.getPopulacaoAtual() >= filaAtual.getServidores()) {
            var destino = sort(filaAtual);
            if (destino != null) {
                addPassagem(filaAtual, destino);
            } else {
                addEvent(filaAtual, TipoEnum.SAIDA);
            }
        }
    }

    public void mapperYml(Map<String, Object> dados) {
        if (dados == null) return;
        qtdNumRandom = (int) dados.get("numeros-aleatorios");

        final List<HashMap<String, Object>> dadosFilas = (List<HashMap<String, Object>>) dados.get("filas");

        List<Row> filas = dadosFilas.stream().map(fila -> {
            var novaFila = new Row();
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
            var filaOrigem = filas.stream().filter(f -> f.getId() == (int) rede.get("origem")).findFirst().get();
            filaOrigem.putToFilaDestino((int) rede.get("destino"),
                    filas.stream().filter(f -> f.getId() == (int) rede.get("destino")).findFirst().get());

            filaOrigem.putToProbabilidades((int) rede.get("destino"), (double) rede.get("probabilidade"));
        }

        escalonadorDeFilas.getFilas().addAll(filas); //Adiciona todas filas no escalonador
        escalonadorDeFilas.getFilas().remove(0); //primeira fila é vazia
        escalonadorDeFilas.getFilas().forEach(f -> {
            probabilidades.put(f.getId(), new double[f.getCapacidade() != -1 ? f.getCapacidade() + 1 : 10]);
        });

        Event primeiroEvento = new Event(TipoEnum.CHEGADA,
                escalonadorDeFilas.getFilas().get(0).getChegadaInicial(),
                escalonadorDeFilas.getFilas().get(0).getId());
        eventosAgendados.add(primeiroEvento);
    }

    private Map<String, Object> getPaper(String arquivoYml) {
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

    public void addEvent(Row filaAtual, TipoEnum tipoEnum) {
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
        eventosAgendados.add(new Event(tipoEnum, tempoCalculado, filaAtual.getId()));
        eventosAgendados.sort(Comparator.comparingDouble(Event::getTempo));
    }

    private void addPassagem(Row filaOrigem, Row filaDestino) {
        double tempoSaida =((filaOrigem.getSaidaMaxima() - filaOrigem.getSaidaMinima())
                * aleatorios.geraProximoAleatorio()
                + filaOrigem.getSaidaMinima()) + tempo;
        eventosAgendados.add(new Event(TipoEnum.PASSAGEM, tempoSaida, filaOrigem.getId(), filaDestino.getId()));
        eventosAgendados.sort(Comparator.comparingDouble(Event::getTempo));
    }

    private Row sort(final Row origem) {
        double intervalo = 0.0;
        final double random = aleatorios.geraProximoAleatorio();

        // faz o sort do hahmap da menor probabilidade para a maior, afim de calcular o intervalo
        Map<Integer, Double> sortedMap = origem.getProbabilidades().entrySet().stream()
                .sorted(Comparator.comparingDouble(Map.Entry::getValue))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> {
                            throw new AssertionError();
                        },
                        LinkedHashMap::new
                ));

        Row filaDestino = null;
        for (Integer fila : sortedMap.keySet()) {
            intervalo += sortedMap.get(fila);
            if (random <= intervalo) {
                filaDestino = escalonadorDeFilas.getFilas().get(fila);
                break;
            }
        }
        return filaDestino;
    }

    public void updateProbability() {
        escalonadorDeFilas.getFilas().forEach(fila -> {
            probabilidades.get(fila.getId())[fila.getPopulacaoAtual()] += this.tempo - this.tempoAnterior;
        });
    }

    public void printProbability() {
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

