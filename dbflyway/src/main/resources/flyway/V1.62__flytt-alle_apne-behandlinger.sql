UPDATE STEG_HISTORIKK
set aktiv = false
WHERE exists(SELECT id
             FROM behandling
             WHERE behandling.status in ('UTREDES', 'IVERKSETTES')
               and behandling.id = steg_historikk.behandling_id)
  and aktiv = true;

INSERT INTO STEG_HISTORIKK (behandling_id, steg, status)
SELECT id, 'START_BEHANDLING', 'START'
FROM behandling
WHERE behandling.status in ('UTREDES', 'IVERKSETTES')
