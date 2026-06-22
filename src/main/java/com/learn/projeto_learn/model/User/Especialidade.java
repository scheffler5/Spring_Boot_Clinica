package com.learn.projeto_learn.model.User;

public enum Especialidade {

    CLINICA_GERAL("Clínica Geral"),
    CARDIOLOGIA("Cardiologia"),
    DERMATOLOGIA("Dermatologia"),
    ENDOCRINOLOGIA("Endocrinologia"),
    GASTROENTEROLOGIA("Gastroenterologia"),
    GERIATRIA("Geriatria"),
    GINECOLOGIA_OBSTETRICIA("Ginecologia e Obstetrícia"),
    HEMATOLOGIA("Hematologia"),
    INFECTOLOGIA("Infectologia"),
    NEFROLOGIA("Nefrologia"),
    NEUROLOGIA("Neurologia"),
    NUTROLOGIA("Nutrologia"),
    OFTALMOLOGIA("Oftalmologia"),
    ONCOLOGIA("Oncologia"),
    ORTOPEDIA("Ortopedia e Traumatologia"),
    OTORRINOLARINGOLOGIA("Otorrinolaringologia"),
    PEDIATRIA("Pediatria"),
    PNEUMOLOGIA("Pneumologia"),
    PSIQUIATRIA("Psiquiatria"),
    PSICOLOGIA("Psicologia"),
    REUMATOLOGIA("Reumatologia"),
    UROLOGIA("Urologia");

    private final String descricao;

    Especialidade(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
