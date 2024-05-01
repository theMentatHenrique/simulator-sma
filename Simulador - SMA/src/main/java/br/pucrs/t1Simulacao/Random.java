package br.pucrs.t1Simulacao;

public class Random {
    private final int a;
    private final long c;
    private final double mod;
    private final int semente;
    private final int tamanho;
    private double ultimoAleatorio;
    private int qtAleatorios;

    public Random(int tamanho, int semente) {
        a = 16807;
        c = 11;
        mod = Math.pow(2,31) - 1;
        this.semente = semente;
        this.tamanho = tamanho;
        ultimoAleatorio = semente;
        qtAleatorios = 0;
    }

    public double geraProximoAleatorio() {
        setUltimoAleatorio(((getA() * getUltimoAleatorio() + getC()) % getMod()));
        setQtAleatorios(getQtAleatorios() + 1);
        return getUltimoAleatorio() / getMod();
    }

    /**
     * c = constante usada para maior variação dos números gerados
     * a = número
     * mod = número grande
     */
    public int getA() {
        return a;
    }

    public long getC() {
        return c;
    }

    public double getMod() {
        return mod;
    }

    public int getSemente() {return semente;}

    private int getTamanho() {return tamanho;}

    public double getUltimoAleatorio() {
        return ultimoAleatorio;
    }

    public void setUltimoAleatorio(double ultimoAleatorio) {
        this.ultimoAleatorio = ultimoAleatorio;
    }

    public int getQtAleatorios() {
        return qtAleatorios;
    }

    public void setQtAleatorios(int qtAleatorios) {
        this.qtAleatorios = qtAleatorios;
    }

    @Override
    public String toString() {
        return "Gerador: \n" +
                " a=" + getA() +
                " \n c=" + getC() +
                " \n mod=" + getMod() +
                " \n semente=" + getSemente() +
                " \n tamanho=" + getTamanho() +
                " \n ultimoAleatorio=" + getUltimoAleatorio() +
                " \n qtAleatorios=" + getQtAleatorios();
    }


}
