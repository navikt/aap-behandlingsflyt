
create table sporing (
    id bigserial primary key,
    opprettet_tid timestamp(3) not null,
    behandling_id bigint not null references behandling(id),
    sporingsnokkel text not null, -- nøkkel som identifiserer hvilken kode som kjørte
    versjon text not null, -- gitsha som forteller hvilken versjon av kodebasen som kjørte
    grunnlag jsonb not null -- serialisering av input to koden som kjørte
);

create unique index uidx_sporing_behandling_nokkel on sporing(behandling_id, sporingsnokkel);