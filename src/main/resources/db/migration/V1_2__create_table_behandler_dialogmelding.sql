CREATE TABLE BEHANDLER_DIALOGMELDING
(
    id            SERIAL PRIMARY KEY,
    behandler_ref VARCHAR(50)  NOT NULL UNIQUE,
    type          VARCHAR(30)  NOT NULL,
    personident   VARCHAR(11),
    fornavn       VARCHAR(255) NOT NULL,
    mellomnavn    VARCHAR(255),
    etternavn     VARCHAR(255) NOT NULL,
    partner_id    VARCHAR(50)  NOT NULL UNIQUE,
    her_id        VARCHAR(50),
    parent_her_id VARCHAR(50),
    hpr_id        VARCHAR(50),
    kontor        VARCHAR(255),
    adresse       VARCHAR(255),
    postnummer    VARCHAR(4),
    poststed      VARCHAR(255),
    orgnummer     VARCHAR(9),
    telefon       VARCHAR(50),
    created_at    timestamptz  NOT NULL,
    updated_at    timestamptz  NOT NULL
);

CREATE TABLE BEHANDLER_DIALOGMELDING_ARBEIDSTAKER
(
    id                         SERIAL PRIMARY KEY,
    uuid                       VARCHAR(50) NOT NULL UNIQUE,
    arbeidstaker_personident   VARCHAR(11) NOT NULL,
    created_at                 timestamptz NOT NULL,
    behandler_dialogmelding_id INTEGER REFERENCES BEHANDLER_DIALOGMELDING (id) ON DELETE CASCADE
);

CREATE INDEX IX_BEHANDLER_DIALOGMELDING_ARBEIDSTAKER on BEHANDLER_DIALOGMELDING_ARBEIDSTAKER (arbeidstaker_personident, behandler_dialogmelding_id);

GRANT SELECT ON BEHANDLER_DIALOGMELDING TO cloudsqliamuser;
GRANT SELECT ON BEHANDLER_DIALOGMELDING_ARBEIDSTAKER TO cloudsqliamuser;
