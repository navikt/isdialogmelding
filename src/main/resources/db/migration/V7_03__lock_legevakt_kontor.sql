UPDATE BEHANDLER_KONTOR SET
dialogmelding_enabled=null,
dialogmelding_enabled_locked=true,
updated_at=now()
WHERE her_id IN ('2341','50087','2338');
