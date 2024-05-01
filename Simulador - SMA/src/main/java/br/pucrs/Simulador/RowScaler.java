package br.pucrs.Simulador;

import java.util.ArrayList;
import java.util.List;

public class RowScaler {
    private final List<Row> filas;

    public RowScaler(){
        filas = new ArrayList<>();
        filas.add(new Row());
    }

    public List<Row> getFilas() {
        return filas;
    }
}
