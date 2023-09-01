ALTER TABLE BEHANDLER_KONTOR ADD COLUMN dialogmelding_enabled_locked BOOLEAN NOT NULL DEFAULT false;
UPDATE BEHANDLER_KONTOR SET dialogmelding_enabled_locked=true WHERE ID=2974;
UPDATE BEHANDLER_KONTOR SET dialogmelding_enabled_locked=true WHERE ID=1302;
