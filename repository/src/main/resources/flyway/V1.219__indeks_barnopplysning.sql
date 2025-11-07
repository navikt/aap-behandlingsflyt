CREATE INDEX IDX_BARNOPPLYSING_BGB_ID ON barnopplysning (bgb_id);
CREATE INDEX IDX_BARNOPPLYSING_PERSON_ID ON barnopplysning (person_id);
CREATE INDEX IDX_BARNOPPLYSING_GRUNNLAG_REGISTER_BARN_ID on barnopplysning_grunnlag (register_barn_id);
CREATE INDEX IDX_BARNOPPLYSING_GRUNNLAG_OPPGITT_BARN on barnopplysning_grunnlag (oppgitt_barn_id);
CREATE INDEX IDX_BARNOPPLYSING_GRUNNLAG_VURDERTE_BARN on barnopplysning_grunnlag (vurderte_barn_id);

CREATE INDEX IDX_PERSON_IDENT_PERSON_ID on person_ident (person_id);

