package no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid

import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.FraværÅrsakV0
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.Meldekort
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.MeldekortV0
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.TimerArbeid
import no.nav.aap.verdityper.dokument.JournalpostId
import java.time.LocalDateTime

data class UbehandletMeldekort(
    val journalpostId: JournalpostId,
    val timerArbeidPerPeriode: Set<ArbeidIPeriode>,
    val mottattTidspunkt: LocalDateTime,
    val harDuArbeidet: Boolean,
    val digitalisertAvPostmottak: Boolean?,
    val fravær: Set<FraværForDag>,
) {
    companion object {
        fun fraKontrakt(
            meldekort: Meldekort,
            journalpostId: JournalpostId,
            mottattTidspunkt: LocalDateTime,
            digitalisertAvPostmottak: Boolean?
        ): UbehandletMeldekort {
            return when (meldekort) {
                is MeldekortV0 -> UbehandletMeldekort(
                    journalpostId = journalpostId,
                    timerArbeidPerPeriode = meldekort.timerArbeidPerPeriode.map {
                        ArbeidIPeriode(
                            periode = Periode(it.fraOgMedDato, it.tilOgMedDato),
                            timerArbeid = TimerArbeid(it.timerArbeid.toBigDecimal())
                        )
                    }.toSet(),
                    mottattTidspunkt = mottattTidspunkt,
                    harDuArbeidet = meldekort.harDuArbeidet,
                    digitalisertAvPostmottak = digitalisertAvPostmottak,
                    fravær = meldekort.fravær?.map { fravær ->
                        FraværForDag(
                            dato = fravær.dato,
                            fraværÅrsak = when (fravær.fraværÅrsak) {
                                FraværÅrsakV0.SYKDOM_ELLER_SKADE ->
                                    FraværÅrsak.SYKDOM_ELLER_SKADE

                                FraværÅrsakV0.OMSORG_FØRSTE_SKOLEDAG_TILVENNING_ELLER_ANNEN_OPPFØLGING_BARN ->
                                    FraværÅrsak.OMSORG_FØRSTE_SKOLEDAG_TILVENNING_ELLER_ANNEN_OPPFØLGING_BARN

                                FraværÅrsakV0.OMSORG_PLEIE_I_HJEMMET_AV_NÆR_PÅRØRENDE ->
                                    FraværÅrsak.OMSORG_PLEIE_I_HJEMMET_AV_NÆR_PÅRØRENDE

                                FraværÅrsakV0.OMSORG_DØDSFALL_I_FAMILIE_ELLER_VENNEKRETS ->
                                    FraværÅrsak.OMSORG_DØDSFALL_I_FAMILIE_ELLER_VENNEKRETS

                                FraværÅrsakV0.OMSORG_MEDDOMMER_ELLER_ANDRE_OFFENTLIGE_PLIKTER ->
                                    FraværÅrsak.OMSORG_MEDDOMMER_ELLER_ANDRE_OFFENTLIGE_PLIKTER

                                FraværÅrsakV0.OMSORG_ANNEN_STERK_GRUNN ->
                                    FraværÅrsak.OMSORG_ANNEN_STERK_GRUNN

                                FraværÅrsakV0.ANNET ->
                                    FraværÅrsak.ANNET
                            }
                        )
                    }?.toSet() ?: emptySet()
                )
            }
        }
    }
}
