UPDATE behandler_kontor SET dialogmelding_enabled=null WHERE id=2974;
UPDATE behandler SET invalidated=now() WHERE kontor_id=2974;
