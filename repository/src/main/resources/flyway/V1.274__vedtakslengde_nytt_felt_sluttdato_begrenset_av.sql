alter table vedtakslengde_vurdering
    add column sluttdato_begrenset_av text[] NOT NULL DEFAULT ARRAY[]::text[]