alter table sykepenge_vurdering
    add column gjelder_fra date;

alter table sykepenge_erstatning_grunnlag
    drop column vurdering_id;