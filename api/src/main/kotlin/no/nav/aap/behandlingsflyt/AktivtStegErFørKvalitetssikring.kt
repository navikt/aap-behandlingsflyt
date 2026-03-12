package no.nav.aap.behandlingsflyt

import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling

fun kanLøseBehovSomSkalVæreLåstEtterKvalitetssikring(løsesISteg: StegType, behandling: Behandling): Boolean {
    val erAktivtStegFørKvalitetssikring = behandling.flyt().erStegFør(behandling.aktivtSteg(), StegType.KVALITETSSIKRING)
    val skalLøsesFørEllerIAktivtSteg = behandling.flyt().erStegFørEllerLik(løsesISteg, behandling.aktivtSteg())
    return skalLøsesFørEllerIAktivtSteg && erAktivtStegFørKvalitetssikring
}