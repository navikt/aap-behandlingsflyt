ALTER TABLE MELLOMLAGRET_VURDERING
    ADD CONSTRAINT mellomlagret_vurdering_unik_behandling_kode UNIQUE (behandling_id, avklaringsbehov_kode);
