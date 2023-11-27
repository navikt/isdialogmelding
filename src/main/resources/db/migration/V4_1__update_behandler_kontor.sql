UPDATE BEHANDLER_KONTOR SET
her_id='2604',
navn='KOLBU LEGESENTER',
orgnummer='979352344',
dialogmelding_enabled=now(),
dialogmelding_enabled_locked=false,
updated_at=now()
WHERE ID=261;

UPDATE BEHANDLER SET her_id='166127',updated_at=now() WHERE id=11791;

UPDATE BEHANDLER_KONTOR SET her_id='166193',updated_at=now() WHERE id=1913;

-- legevakt (kan ikke sende dialogmelding til disse)
UPDATE BEHANDLER_KONTOR SET dialogmelding_enabled=null, dialogmelding_enabled_locked=true, updated_at=now() WHERE id=1546;
UPDATE BEHANDLER_KONTOR SET dialogmelding_enabled=null, dialogmelding_enabled_locked=true, updated_at=now() WHERE id=1679;
