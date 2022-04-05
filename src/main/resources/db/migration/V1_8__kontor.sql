CREATE TABLE BEHANDLER_DIALOGMELDING_KONTOR
(
    id            SERIAL PRIMARY KEY,
    partner_id    VARCHAR(50)  NOT NULL UNIQUE,
    her_id        VARCHAR(50),
    navn          VARCHAR(255),
    adresse       VARCHAR(255),
    postnummer    VARCHAR(4),
    poststed      VARCHAR(255),
    orgnummer     VARCHAR(9),
    dialogmelding_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    created_at    timestamptz  NOT NULL,
    updated_at    timestamptz  NOT NULL
);

INSERT INTO BEHANDLER_DIALOGMELDING_KONTOR(partner_id, her_id, navn, adresse, postnummer,
poststed, orgnummer, dialogmelding_enabled, created_at, updated_at)
SELECT partner_id, parent_her_id, kontor, adresse, postnummer,
poststed, orgnummer, TRUE, created_at, updated_at FROM BEHANDLER_DIALOGMELDING B1
WHERE updated_at = (select max(updated_at) FROM BEHANDLER_DIALOGMELDING B2 WHERE B2.partner_id = B1.partner_id);

ALTER TABLE BEHANDLER_DIALOGMELDING ADD COLUMN KONTOR_ID INTEGER REFERENCES BEHANDLER_DIALOGMELDING_KONTOR (id) ON DELETE RESTRICT;

UPDATE BEHANDLER_DIALOGMELDING B SET KONTOR_ID=(SELECT ID FROM BEHANDLER_DIALOGMELDING_KONTOR K WHERE K.partner_id=B.partner_id);

ALTER TABLE BEHANDLER_DIALOGMELDING
DROP COLUMN partner_id,
DROP COLUMN parent_her_id,
DROP COLUMN kontor,
DROP COLUMN adresse,
DROP COLUMN postnummer,
DROP COLUMN poststed,
DROP COLUMN orgnummer;
