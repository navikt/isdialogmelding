UPDATE BEHANDLER_KONTOR SET
dialogmelding_enabled=null,
dialogmelding_enabled_locked=true,
updated_at=now()
WHERE her_id='87796' and partner_id='217';
