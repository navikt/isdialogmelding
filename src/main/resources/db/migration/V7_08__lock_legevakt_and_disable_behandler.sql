UPDATE BEHANDLER_KONTOR SET
dialogmelding_enabled=null,
dialogmelding_enabled_locked=true,
updated_at=now()
WHERE her_id = '94615';

UPDATE BEHANDLER SET invalidated=now(),updated_at=now() WHERE behandler_ref='494e967f-c1a5-4beb-9dc8-bedccb9a6d59';
