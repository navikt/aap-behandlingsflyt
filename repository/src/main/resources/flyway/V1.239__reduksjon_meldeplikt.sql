alter table underveis_periode
    add column meldeplikt_gradering smallint default 0
    check (0 <= meldeplikt_gradering and meldeplikt_gradering <= 100);

alter table tilkjent_periode
    add column meldeplikt_gradering smallint default 0
    check (0 <= meldeplikt_gradering and meldeplikt_gradering <= 100);
