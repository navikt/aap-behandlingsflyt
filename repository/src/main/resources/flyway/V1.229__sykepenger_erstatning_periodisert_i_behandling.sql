ALTER TABLE sykepenge_vurdering
    ADD COLUMN VURDERT_I_BEHANDLING     BIGINT REFERENCES BEHANDLING (ID),
    ADD COLUMN gjelder_tom              DATE;


-- Vi har oppdaget noen tilfeller av vurderinger for sykepenge-erstatning i test som ikke er knyttet til noen grunnlag, dette er for å rydde opp

delete from sykepenge_vurdering
where id in (
    SELECT distinct(sv.id)
    FROM sykepenge_vurdering sv
    LEFT JOIN sykepenge_vurderinger vurderinger ON vurderinger.id = sv.vurderinger_id
    LEFT JOIN sykepenge_erstatning_grunnlag grunnlag ON grunnlag.vurderinger_id = vurderinger.id
    WHERE grunnlag.id IS NULL
);

delete from sykepenge_vurderinger
where id in (
    SELECT distinct(vurderinger.id)
    FROM sykepenge_vurderinger vurderinger
    LEFT JOIN sykepenge_erstatning_grunnlag grunnlag ON grunnlag.vurderinger_id = vurderinger.id
    WHERE grunnlag.id IS NULL
);