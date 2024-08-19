UPDATE BEHANDLER SET invalidated=now(), hpr_id='10039219' WHERE behandler_ref='d19908b2-01ee-4de5-9e95-bce4f8b8ef9e';

UPDATE BEHANDLER SET hpr_id='9581391' WHERE behandler_ref='9f1f7bde-1c27-4af1-9e5a-9ae825381d0e';
UPDATE BEHANDLER SET invalidated=null WHERE behandler_ref='e1990940-a82c-4882-ba74-ad4a1be3aaff';

UPDATE BEHANDLER_KONTOR SET her_id='179561' WHERE partner_id='48084';

UPDATE BEHANDLER_KONTOR SET her_id='178068',navn='DURBAN AS',orgnummer='991160981',dialogmelding_enabled=now() WHERE id=358;
UPDATE BEHANDLER SET invalidated=now(),updated_at=now() WHERE kontor_id=358;
UPDATE BEHANDLER SET invalidated=null,updated_at=now() WHERE kontor_id=358 AND behandler_ref='75b356c8-e236-460f-83b1-dd58a49f2475';
