package no.nav.aap.behandlingsflyt.prosessering

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.Underveisperiode
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.Meldekort
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.MeldekortRepository
import no.nav.aap.behandlingsflyt.kontrakt.datadeling.ArbeidIPeriodeDTO
import no.nav.aap.behandlingsflyt.kontrakt.datadeling.DetaljertMeldekortDTO
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.komponenter.type.Periode

class DatadelingMeldekortService(
    private val saksRepository: SakRepository,
    private val underveisRepository: UnderveisRepository,
    private val meldekortRepository: MeldekortRepository,
) {

    internal fun opprettKontraktObjekter(
        sakId: SakId,
        behandlingId: BehandlingId
    ): List<DetaljertMeldekortDTO> {
        val sak = saksRepository.hent(sakId)
        val personIdent = sak.person.aktivIdent()
        val underveisGrunnlag = underveisRepository.hent(behandlingId)

        val meldekortene = meldekortRepository.hent(behandlingId).meldekort()
        val kontraktObjekter = meldekortene.map { meldekort ->
            tilKontrakt(meldekort, personIdent, sak, behandlingId, underveisGrunnlag)
        }
        return kontraktObjekter
    }

    private fun tilKontrakt(
        meldekort: Meldekort,
        personIdent: Ident,
        sak: Sak,
        behandlingId: BehandlingId,
        underveisGrunnlag: UnderveisGrunnlag
    ): DetaljertMeldekortDTO {

        val underveisPeriode = finnUnderveisperiode(meldekort, underveisGrunnlag.perioder)
        val meldePeriode = underveisPeriode.meldePeriode
        val meldepliktStatus = underveisPeriode.meldepliktStatus
        val rettighetsType = underveisPeriode.rettighetsType
        val avslagsårsak = underveisPeriode.avslagsårsak

        // TODO: vurder sammen med NKS om vi har mer relevant info å sende med
        // Vi kan også sjekke for fritak fra meldeplikt, og rimelig grunn for å ikke oppfylle meldeplikt
        // men denne koden skrives om pt. så best å vente.

        return DetaljertMeldekortDTO(
            personIdent = personIdent.identifikator,
            saksnummer = sak.saksnummer,
            behandlingId = behandlingId.id,
            journalpostId = meldekort.journalpostId.identifikator,
            meldeperiodeFom = meldePeriode.fom,
            meldeperiodeTom = meldePeriode.fom,
            mottattTidspunkt = meldekort.mottattTidspunkt,
            timerArbeidPerPeriode = meldekort.timerArbeidPerPeriode.map {
                ArbeidIPeriodeDTO(
                    it.periode.fom,
                    it.periode.tom,
                    it.timerArbeid.antallTimer
                )
            },
            meldepliktStatusKode = meldepliktStatus?.name,
            rettighetsTypeKode = rettighetsType?.name,
            avslagsårsakKode = avslagsårsak?.name
        )
    }

    private fun finnUnderveisperiode(
        meldekort: Meldekort, underveisPerioder: List<Underveisperiode>
    ): Underveisperiode {
        val arbeidsperiode = arbeidsperiodeFraMeldekort(meldekort)
        return underveisPerioder.first {
            // Alle arbeidsperiodene i meldekortet må være innenfor en og samme underveisperiode.
            // Underveisperioder skal ikke overlappe med hverandre.
            it.meldePeriode.inneholder(arbeidsperiode)
        }
    }

    /**
    @return Tidsperioden meldekortet inneholder arbeidstimer for.
    Merk at det kan være dager i perioden meldekortet gjelder for som det ikke er rapportert timer på.
    @param meldekort -
     **/
    private fun arbeidsperiodeFraMeldekort(meldekort: Meldekort): Periode {
        val arbeidPerioder = meldekort.timerArbeidPerPeriode.map { it.periode }
        val arbeidsPerioderStart = arbeidPerioder.minBy { it.fom }.fom
        val arbeidsPerioderSlutt = arbeidPerioder.maxBy { it.tom }.tom

        return Periode(arbeidsPerioderStart, arbeidsPerioderSlutt)
    }
}
