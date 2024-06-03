-- fix wrong her_id
UPDATE BEHANDLER SET her_id='184444',updated_at=now() WHERE behandler_ref='c902b835-10ea-4c33-a196-9054b9002c1d';
-- invalidate duplicate
UPDATE BEHANDLER SET invalidated=now(),personident=null,updated_at=now() WHERE id=40301;
UPDATE BEHANDLER_ARBEIDSTAKER SET behandler_id=42837 WHERE behandler_id=40301;
-- resend failed dialogmelding
UPDATE BEHANDLER_DIALOGMELDING_BESTILLING SET sendt=null where uuid='fb0c661c-1c4f-4b50-97e6-294985c86248';
