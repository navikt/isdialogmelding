UPDATE BEHANDLER_KONTOR SET
dialogmelding_enabled=null,
dialogmelding_enabled_locked=true,
updated_at=now()
WHERE her_id IN ('148253','132881','106236','135010','87796','154370','137849','108871','103477','155891','114678');
