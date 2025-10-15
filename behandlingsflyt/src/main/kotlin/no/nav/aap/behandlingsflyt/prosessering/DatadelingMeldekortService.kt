package no.nav.aap.behandlingsflyt.prosessering

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.meldeperiode.MeldeperiodeRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.Meldekort
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.MeldekortRepository
import no.nav.aap.behandlingsflyt.kontrakt.datadeling.DetaljertTimeArbeidetListeDTO
import no.nav.aap.behandlingsflyt.kontrakt.datadeling.MeldtArbeidIPeriodeDTO
import no.nav.aap.behandlingsflyt.kontrakt.datadeling.UnderveisStatuserDTO
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.behandlingsflyt.prosessering.OpplysningerOmArbeidFraMeldekort.Companion.mergePrioriterHøyre
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.TimerArbeid
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDateTime

class DatadelingMeldekortService(
    private val saksRepository: SakRepository,
    private val underveisRepository: UnderveisRepository,
    private val meldekortRepository: MeldekortRepository,
    private val meldeperiodeRepository: MeldeperiodeRepository,
) {
    private val log = org.slf4j.LoggerFactory.getLogger(javaClass)

    internal fun opprettKontraktObjekter(
        sakId: SakId, behandlingId: BehandlingId
    ): List<DetaljertTimeArbeidetListeDTO> {
        val sak = saksRepository.hent(sakId)
        val personIdent = sak.person.aktivIdent()

        val meldekortene = meldekortRepository.hentHvisEksisterer(behandlingId)?.meldekort().orEmpty()
        if (meldekortene.isEmpty()) {
            return emptyList()
        }

        val meldePeriodene = meldeperiodeRepository.hent(behandlingId)
        if (meldePeriodene.isEmpty()) {
            log.warn("Ingen meldeperioder funnet for behandlingId=${behandlingId.id}")
            return emptyList()
        }

        val underveisGrunnlag = underveisRepository.hentHvisEksisterer(behandlingId)
        if (underveisGrunnlag == null) {
            log.warn("Ingen UnderveisGrunnlag funnet for behandlingId=${behandlingId.id}")
            // hvis det ikke finnes UnderveisGrunnlag, finnes det heller ingen meldekort å rapportere
            return emptyList()
        }

        val timerArbeidetTidslinje = somTidslinje(meldekortene)

        val meldeperiodeTilArbeidIPeriode = meldePeriodene.associateWith { meldePeriode ->
            timerArbeidetTidslinje.segmenter().filter { it.periode.overlapper(meldePeriode) }
        }

        val meldeperiodeMedArbeid = meldeperiodeTilArbeidIPeriode.mapValues { (_, segment) ->
            segment.map {
                MeldtArbeidIPeriodeDTO(
                    it.periode.fom,
                    it.periode.tom,
                    it.verdi.timerArbeid.antallTimer,
                    it.verdi.opplysningerFørstMottatt,
                    it.verdi.journalpostId
                )
            }
        }.filterValues { it.isNotEmpty() }

        return meldeperiodeMedArbeid.map { (meldeperiode, meldtArbeidIPeriodeDTO) ->
            tilKontrakt(
                meldeperiode, meldtArbeidIPeriodeDTO, personIdent, sak.saksnummer, behandlingId, underveisGrunnlag,
            )
        }

    }

    internal fun tilKontrakt(
        meldeperiode: Periode,
        meldtArbeidIPeriode: List<MeldtArbeidIPeriodeDTO>,
        personIdent: Ident,
        saksnummer: Saksnummer,
        behandlingId: BehandlingId,
        underveisGrunnlag: UnderveisGrunnlag,
    ): DetaljertTimeArbeidetListeDTO {
        val underVeisPerioder = underveisGrunnlag.perioder.filter {
            it.meldePeriode == meldeperiode
        }

        return DetaljertTimeArbeidetListeDTO(
            personIdent = personIdent.identifikator,
            saksnummer = saksnummer,
            behandlingId = behandlingId.id,
            meldeperiodeFom = meldeperiode.fom,
            meldeperiodeTom = meldeperiode.tom,
            detaljertArbeidIPeriode = meldtArbeidIPeriode,
            underveisStatuser = underVeisPerioder.map {
                UnderveisStatuserDTO(
                    it.periode.fom,
                    it.periode.tom,
                    it.meldepliktStatus?.name,
                    it.rettighetsType?.name,
                    it.avslagsårsak?.name
                )
            },
        )

    }
}

fun somTidslinje(meldekort: List<Meldekort>): Tidslinje<OpplysningerOmArbeidFraMeldekort> {
    var tidslinje = Tidslinje<OpplysningerOmArbeidFraMeldekort>()

    for (meldekort in meldekort.sortedBy { it.mottattTidspunkt }) {
        tidslinje = tidslinje.outerJoin(meldekort.somTidslinje()) { tidligereOpplysninger, meldekortopplysninger ->
            /* Opplysninger fra nyeste meldekort, opplysningerFørstMottatt fra eldste meldekort */
            val timerArbeidetOpplysninger = OpplysningerOmArbeidFraMeldekort(
                timerArbeid = meldekortopplysninger?.let { (timerArbeidet, antallDager) ->
                    TimerArbeid(
                        timerArbeidet.antallTimer.divide(
                            BigDecimal(antallDager),
                            3,
                            RoundingMode.HALF_UP
                        )
                    )
                } ?: error("Vi sender bare inn non-null verdier i tidslinjen, så dette skal ikke kunne skje"),
                opplysningerFørstMottatt = meldekort.mottattTidspunkt,
                journalpostId = meldekort.journalpostId.identifikator
            )

            mergePrioriterHøyre(tidligereOpplysninger, timerArbeidetOpplysninger)
        }
    }

    return tidslinje
}

class OpplysningerOmArbeidFraMeldekort(
    val timerArbeid: TimerArbeid,
    val opplysningerFørstMottatt: LocalDateTime,
    val journalpostId: String
) {
    companion object {
        fun mergePrioriterHøyre(
            venstre: OpplysningerOmArbeidFraMeldekort?,
            høyre: OpplysningerOmArbeidFraMeldekort
        ): OpplysningerOmArbeidFraMeldekort {
            val eldsteJournalpostId = listOfNotNull(venstre?.journalpostId, høyre.journalpostId).first()
            val eldsteTidspunkt = listOfNotNull(venstre?.opplysningerFørstMottatt, høyre.opplysningerFørstMottatt).min()
            return OpplysningerOmArbeidFraMeldekort(
                timerArbeid = høyre.timerArbeid,
                opplysningerFørstMottatt = eldsteTidspunkt,
                journalpostId = eldsteJournalpostId,
            )
        }

    }
}
