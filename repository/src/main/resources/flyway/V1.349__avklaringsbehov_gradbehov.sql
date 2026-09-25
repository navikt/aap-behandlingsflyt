alter table avklaringsbehov_endring
    add column grad_behov            text,
    add column perioder_kan_vurderes daterange[];