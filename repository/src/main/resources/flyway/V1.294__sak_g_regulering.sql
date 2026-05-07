CREATE TABLE g_regulering_historikk (
    sak_id          BIGINT  NOT NULL REFERENCES sak(id),
    regulerings_aar INT     NOT NULL,
    opprettet_tid   TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    PRIMARY KEY (sak_id, regulerings_aar)
);
