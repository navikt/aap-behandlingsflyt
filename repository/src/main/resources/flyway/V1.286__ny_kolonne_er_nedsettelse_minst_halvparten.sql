alter table sykdom_vurdering
    add column er_nedsettelse_minst_halvparten text default null,
    add column er_nedsettelse_mer_enn_yrkesskadegrense text default null;