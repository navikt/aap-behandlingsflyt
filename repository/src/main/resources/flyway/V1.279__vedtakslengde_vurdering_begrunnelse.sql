alter table vedtakslengde_vurdering add column begrunnelse text;
update vedtakslengde_vurdering set begrunnelse = 'Automatisk vurdert';
alter table vedtakslengde_vurdering alter column begrunnelse set not null;
