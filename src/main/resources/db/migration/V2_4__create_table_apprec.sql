CREATE TABLE APPREC
(
    id            SERIAL PRIMARY KEY,
    uuid          VARCHAR(50) NOT NULL UNIQUE,
    bestilling_id INTEGER NOT NULL REFERENCES BEHANDLER_DIALOGMELDING_BESTILLING (id) ON DELETE RESTRICT,
    statusKode    VARCHAR(10) NOT NULL,
    statusTekst   TEXT NOT NULL,
    feilKode      VARCHAR(10),
    feilTekst     TEXT,
    created_at    timestamptz  NOT NULL,
    updated_at    timestamptz  NOT NULL
);

CREATE INDEX IX_APPREC_BESTILLING_ID on APPREC (bestilling_id);

GRANT SELECT ON APPREC TO cloudsqliamuser;
