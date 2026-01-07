CREATE OR REPLACE VIEW gjeldende_vedtatte_behandlinger AS
SELECT *
FROM (SELECT s.id as                                                       s_sak_id,
             ROW_NUMBER()
             OVER (PARTITION BY b.sak_id ORDER BY v.vedtakstidspunkt DESC) rn,
             b.id as                                                       behandling_id
      FROM behandling b
               JOIN sak s ON b.sak_id = s.id
               join vedtak v on b.id = v.behandling_id
      WHERE b.status in ('AVSLUTTET', 'IVERKSETTES')) q
WHERE rn = 1;