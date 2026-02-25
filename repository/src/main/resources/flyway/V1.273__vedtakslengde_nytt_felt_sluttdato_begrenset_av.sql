alter table vedtakslengde_vurdering
    add column sluttdato_begrenset_av TEXT[] DEFAULT ARRAY []::TEXT[];