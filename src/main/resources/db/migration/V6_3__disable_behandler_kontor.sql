UPDATE BEHANDLER_KONTOR SET dialogmelding_enabled=null,dialogmelding_enabled_locked=true,updated_at=now() WHERE ID=1310;
UPDATE BEHANDLER SET invalidated=now(),updated_at=now() WHERE kontor_id=1310;
