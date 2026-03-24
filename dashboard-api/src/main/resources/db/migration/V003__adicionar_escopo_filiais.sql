CREATE TABLE acesso.setor_filiais_permitidas (
    setor_id     BIGINT        NOT NULL REFERENCES acesso.setores(id),
    filial_nome  NVARCHAR(120) NOT NULL,
    PRIMARY KEY (setor_id, filial_nome)
);
