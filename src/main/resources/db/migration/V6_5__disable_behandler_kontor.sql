UPDATE BEHANDLER_KONTOR SET dialogmelding_enabled=null,dialogmelding_enabled_locked=true,updated_at=now() WHERE ID=303;
UPDATE BEHANDLER SET invalidated=now(),updated_at=now() WHERE kontor_id=303;
