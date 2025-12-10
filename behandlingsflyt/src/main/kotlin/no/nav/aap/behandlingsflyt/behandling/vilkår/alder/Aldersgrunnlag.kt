package no.nav.aap.behandlingsflyt.behandling.vilkår.alder

import no.nav.aap.behandlingsflyt.faktagrunnlag.Faktagrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.AlderMedMåneder
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.Fødselsdato
import no.nav.aap.komponenter.type.Periode
import java.time.LocalDate

class Aldersgrunnlag(
    val periode: Periode,
    private val fødselsdato: Fødselsdato,
    val øvreGrenseForAutomatiskAvslag: AlderMedMåneder
) : Faktagrunnlag {
    fun fyller(alder: Int): LocalDate {
        return fødselsdato.toLocalDate().plusYears(alder.toLong())
    }

    fun alderMedMånederPåSøknadsdato(): AlderMedMåneder = fødselsdato.alderMedMånederPåDato(periode.fom)
}
