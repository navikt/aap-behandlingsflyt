CREATE OR REPLACE VIEW gjeldende_vedtatte_behandlinger AS
SELECT *
FROM (SELECT b.sak_id as                                                       sak_id,
             ROW_NUMBER()
             OVER (PARTITION BY b.sak_id ORDER BY v.vedtakstidspunkt DESC) rn,
             b.id as                                                       behandling_id
      FROM behandling b
               join vedtak v on b.id = v.behandling_id
      WHERE b.status in ('AVSLUTTET', 'IVERKSETTES')) q
WHERE rn = 1;

CREATE INDEX IDX_BEHANDLING_STATUS ON BEHANDLING (status);