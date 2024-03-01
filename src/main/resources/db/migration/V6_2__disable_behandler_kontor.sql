UPDATE BEHANDLER_KONTOR SET dialogmelding_enabled=null WHERE ID=1926;
UPDATE BEHANDLER SET invalidated=now(),updated_at=now() WHERE kontor_id=1926;
