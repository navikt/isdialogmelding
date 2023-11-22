UPDATE BEHANDLER_KONTOR SET
her_id='172431',
navn='GAMLE KRAGERØVEI LEGESENTER',
orgnummer='929846923',
dialogmelding_enabled=now(),
dialogmelding_enabled_locked=false,
updated_at=now()
WHERE ID=803;

UPDATE BEHANDLER SET invalidated=now() where id=5848;
UPDATE BEHANDLER SET invalidated=now() where id=11301;
UPDATE BEHANDLER SET invalidated=now() where id=19562;
UPDATE BEHANDLER SET invalidated=now() where id=21530;
UPDATE BEHANDLER SET invalidated=now() where id=23706;
UPDATE BEHANDLER SET invalidated=now() where id=24390;
UPDATE BEHANDLER SET invalidated=now() where id=24646;
UPDATE BEHANDLER SET invalidated=now() where id=25862;
UPDATE BEHANDLER SET invalidated=now() where id=29992;

UPDATE BEHANDLER SET her_id='172463' where id=2490;
UPDATE BEHANDLER SET her_id='172464' where id=798;
UPDATE BEHANDLER SET her_id='172940' where id=22306;
UPDATE BEHANDLER SET her_id='172466' where id=4594;

UPDATE BEHANDLER_KONTOR SET
her_id='172135',
navn='KVERNEVIK LEGEKONTOR',
orgnummer='930010081',
dialogmelding_enabled=now(),
dialogmelding_enabled_locked=false,
updated_at=now()
WHERE ID=488;

UPDATE BEHANDLER SET invalidated=now() where id=1931;
UPDATE BEHANDLER SET invalidated=now() where id=578;
UPDATE BEHANDLER SET invalidated=now() where id=4383;
UPDATE BEHANDLER SET invalidated=now() where id=5758;
UPDATE BEHANDLER SET invalidated=now() where id=6808;
UPDATE BEHANDLER SET invalidated=now() where id=11137;
UPDATE BEHANDLER SET invalidated=now() where id=21407;
