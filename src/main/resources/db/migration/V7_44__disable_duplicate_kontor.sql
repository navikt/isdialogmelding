UPDATE BEHANDLER_KONTOR SET
dialogmelding_enabled=null,
dialogmelding_enabled_locked=true,
updated_at=now()
WHERE partner_id='11154';

UPDATE BEHANDLER_KONTOR SET HER_ID='199119' WHERE PARTNER_ID='48764';