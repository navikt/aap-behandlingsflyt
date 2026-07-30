CREATE TABLE jobb_arkiv
(
    ID            BIGINT       NOT NULL PRIMARY KEY,
    STATUS        VARCHAR(50)  NOT NULL,
    TYPE          VARCHAR(50)  NOT NULL,
    SAK_ID        BIGINT       NULL,
    BEHANDLING_ID BIGINT       NULL,
    parameters    text         NULL,
    payload       text         NULL,
    NESTE_KJORING TIMESTAMP(3) NOT NULL,
    OPPRETTET_TID TIMESTAMP(3) NOT NULL
);
CREATE TABLE jobb_historikk_arkiv
(
    ID            BIGINT       NOT NULL PRIMARY KEY,
    JOBB_ID       BIGINT       NOT NULL,
    STATUS        VARCHAR(50)  NOT NULL,
    FEILMELDING   TEXT         NULL,
    OPPRETTET_TID TIMESTAMP(3) NOT NULL
);