CREATE TABLE DIALOGMELDING_STATUS
(
    id            SERIAL PRIMARY KEY,
    uuid          VARCHAR(50) NOT NULL UNIQUE,
    bestilling_id INTEGER NOT NULL REFERENCES BEHANDLER_DIALOGMELDING_BESTILLING (id) ON DELETE CASCADE,
    status        VARCHAR(10) NOT NULL,
    tekst         VARCHAR(100),
    created_at    timestamptz  NOT NULL,
    updated_at    timestamptz  NOT NULL,
    published_at  timestamptz  NOT NULL
);

CREATE INDEX IX_DIALOGMELDING_STATUS_BESTILLING_ID on DIALOGMELDING_STATUS (bestilling_id);

GRANT SELECT ON DIALOGMELDING_STATUS TO cloudsqliamuser;
