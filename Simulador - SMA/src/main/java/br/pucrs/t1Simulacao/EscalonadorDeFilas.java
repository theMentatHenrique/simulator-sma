package br.pucrs.t1Simulacao;

import java.util.ArrayList;
import java.util.List;

public class EscalonadorDeFilas {
    private final List<Fila> filas;

    public EscalonadorDeFilas(){
        filas = new ArrayList<>();
        filas.add(new Fila());
    }

    public List<Fila> getFilas() {
        return filas;
    }
}
