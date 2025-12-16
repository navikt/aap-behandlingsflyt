-- Sett vurdert_i_behandling
UPDATE student_vurdering
SET vurdert_i_behandling = g.behandling_id
FROM student_grunnlag g
WHERE g.student_id = student_vurdering.id;

-- Migrer til å bruke student_vurderinger
DO
$$
    DECLARE
        grunnlag_rad      RECORD;
        ny_vurderinger_id BIGINT;
    BEGIN
        FOR grunnlag_rad IN
            SELECT id, student_id
            FROM student_grunnlag
            WHERE student_id is not null
            LOOP
                -- Opprett rader for student_vurderinger
                INSERT INTO student_vurderinger (opprettet_tid)
                VALUES (CURRENT_TIMESTAMP)
                RETURNING id INTO ny_vurderinger_id;

                -- Pek studentvurdering mot ny rad
                UPDATE student_vurdering
                SET student_vurderinger_id = ny_vurderinger_id
                WHERE id = grunnlag_rad.student_id;

                -- Pek studentgrunnlag mot ny rad
                UPDATE student_grunnlag
                SET student_vurderinger_id = ny_vurderinger_id
                WHERE id = grunnlag_rad.id;
            END LOOP;
    END
$$;