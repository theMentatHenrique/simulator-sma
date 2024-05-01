package br.pucrs.Simulador;

public class Event {
    private TipoEnum tipo;
    private double tempo; //Tempo do evento
    private Integer idOrigem;
    private Integer idDestino;

    public Event(TipoEnum tipo, double tempo, int idOrigem) {
        this.idOrigem = idOrigem;
        this.tipo = tipo;
        this.tempo = tempo;
    }

    public Event(TipoEnum tipo, double tempo, int idOrigem, int idDestino) {
        this.tipo = tipo;
        this.tempo = tempo;
        this.idOrigem = idOrigem;
        this.idDestino = idDestino;
    }

    public TipoEnum getTipo() {
        return tipo;
    }

    public double getTempo() {
        return tempo;
    }

    public Integer getIdOrigem() {
        return idOrigem;
    }

    public Integer getIdDestino() {
        return idDestino;
    }

    public enum TipoEnum {
        CHEGADA,
        SAIDA,
        PASSAGEM;
    }

}
