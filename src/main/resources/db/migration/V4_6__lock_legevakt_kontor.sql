UPDATE BEHANDLER_KONTOR SET
dialogmelding_enabled=null,
dialogmelding_enabled_locked=true,
updated_at=now()
WHERE ID IN (1677,1522,1536,1739,1740,1495,1469,1485,1486,1680,3032,1218,2234);
