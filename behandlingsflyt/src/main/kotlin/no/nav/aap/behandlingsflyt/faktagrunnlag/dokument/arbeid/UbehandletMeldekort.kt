package no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid

import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.Meldekort
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.MeldekortFraSaksbehandlerV0
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.MeldekortV0
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.TimerArbeid
import java.time.LocalDateTime

data class UbehandletMeldekort(
    val referanse: InnsendingReferanse,
    val timerArbeidPerPeriode: Set<ArbeidIPeriode>,
    val mottattTidspunkt: LocalDateTime,
    val harDuArbeidet: Boolean?,
    val digitalisertAvPostmottak: Boolean?,
    val begrunnelse: String? = null,
    val opprettetAv: String? = null,
) {
    companion object {
        fun fraKontrakt(
            meldekort: Meldekort,
            referanse: InnsendingReferanse,
            mottattTidspunkt: LocalDateTime,
            digitalisertAvPostmottak: Boolean?
        ): UbehandletMeldekort {
            return when (meldekort) {
                is MeldekortV0 -> UbehandletMeldekort(
                    referanse = referanse,
                    timerArbeidPerPeriode = meldekort.timerArbeidPerPeriode.map {
                        ArbeidIPeriode(
                            periode = Periode(it.fraOgMedDato, it.tilOgMedDato),
                            timerArbeid = TimerArbeid(it.timerArbeid.toBigDecimal())
                        )
                    }.toSet(),
                    mottattTidspunkt = mottattTidspunkt,
                    harDuArbeidet = meldekort.harDuArbeidet,
                    digitalisertAvPostmottak = digitalisertAvPostmottak
                )

                is MeldekortFraSaksbehandlerV0 -> UbehandletMeldekort(
                    referanse = referanse,
                    timerArbeidPerPeriode = meldekort.timerArbeidPerPeriode.map {
                        ArbeidIPeriode(
                            periode = Periode(it.fraOgMedDato, it.tilOgMedDato),
                            timerArbeid = TimerArbeid(it.timerArbeid.toBigDecimal())
                        )
                    }.toSet(),
                    mottattTidspunkt = mottattTidspunkt,
                    harDuArbeidet = meldekort.harDuArbeidet,
                    begrunnelse = meldekort.begrunnelse,
                    opprettetAv = meldekort.opprettetAv,
                    digitalisertAvPostmottak = false,
                )
            }
        }
    }
}
