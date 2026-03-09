alter table vedtakslengde_vurdering add column sluttdato_aarsak text[];
update vedtakslengde_vurdering set sluttdato_aarsak = '{ETT_ÅR_VARIGHET}';
alter table vedtakslengde_vurdering alter column sluttdato_aarsak set not null;
