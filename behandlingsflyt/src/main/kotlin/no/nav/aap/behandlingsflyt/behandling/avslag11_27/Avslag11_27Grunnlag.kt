package no.nav.aap.behandlingsflyt.behandling.avslag11_27

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.VurderingForKravGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.KravGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.RelevantSøknadKrav

class Avslag11_27Grunnlag(
    override val vurderinger: Set<Avslag11_27Vurdering>
) : VurderingForKravGrunnlag<Avslag11_27Vurdering> {
    companion object {
        /**
         * § 11-27 vurderes bare for krav fra søknad. Et migrert krav har et fattet vedtak i Arena og har
         * dermed allerede rett til ytelsen.
         */
        fun kravSomSkalVurderes(kravGrunnlag: KravGrunnlag): List<RelevantSøknadKrav> =
            kravGrunnlag.gjeldendeSøknadKrav().toList()
    }
}
