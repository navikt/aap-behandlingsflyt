UPDATE sykepenge_vurdering sv1
SET gjelder_fra = lower(sv3.rettighetsperiode)
FROM (
    SELECT sv2.id, s.rettighetsperiode
    FROM sykepenge_vurdering sv2
    LEFT JOIN sykepenge_vurderinger vurderinger ON vurderinger.id = sv2.vurderinger_id
    LEFT JOIN sykepenge_erstatning_grunnlag grunnlag ON grunnlag.vurderinger_id = vurderinger.id
    LEFT JOIN behandling b ON b.id = grunnlag.behandling_id
    LEFT JOIN sak s ON s.id = b.sak_id
) as sv3
WHERE sv1.id = sv3.id AND sv1.gjelder_fra IS NULL;


ALTER TABLE sykepenge_vurdering
    ALTER COLUMN gjelder_fra SET NOT NULL;

