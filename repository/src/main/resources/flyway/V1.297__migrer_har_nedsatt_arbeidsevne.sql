update sykdom_vurdering
set har_nedsatt_arbeidsevne = case
                                  when ER_NEDSETTELSE_MINST_HALVPARTEN = 'JA_FORBIGÅENDE_PROBLEMER'
                                      then 'JA_FORBIGÅENDE_PROBLEMER'
                                  when ER_NEDSETTELSE_MER_ENN_YRKESSKADEGRENSE = 'JA_FORBIGÅENDE_PROBLEMER'
                                      then 'JA_FORBIGÅENDE_PROBLEMER'
                                  when ER_ARBEIDSEVNE_NEDSATT = true
                                      then 'JA'
                                  when ER_ARBEIDSEVNE_NEDSATT = false
                                      then 'NEI'
                                  else null
    end
where har_nedsatt_arbeidsevne is null;
