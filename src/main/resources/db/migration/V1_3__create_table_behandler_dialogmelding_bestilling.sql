CREATE TABLE BEHANDLER_DIALOGMELDING_BESTILLING
(
    id            SERIAL PRIMARY KEY,
    uuid          VARCHAR(50) NOT NULL UNIQUE,
    behandler_id  INTEGER NOT NULL REFERENCES BEHANDLER_DIALOGMELDING (id) ON DELETE RESTRICT,
    arbeidstaker_personident   VARCHAR(11) NOT NULL,
    parent        VARCHAR(50),
    conversation  VARCHAR(50) NOT NULL,
    type          VARCHAR(50) NOT NULL,
    kode          INTEGER NOT NULL,
    tekst         TEXT,
    vedlegg       bytea,
    sendt         timestamptz,
    sendt_tries   INTEGER NOT NULL DEFAULT 0,
    created_at    timestamptz  NOT NULL,
    updated_at    timestamptz  NOT NULL
);

CREATE INDEX IX_BEHANDLER_DIALOGMELDING_BESTILLING_BEHANDLER_ID on BEHANDLER_DIALOGMELDING_BESTILLING (behandler_id);

GRANT SELECT ON BEHANDLER_DIALOGMELDING_BESTILLING TO cloudsqliamuser;
