package no.nav.aap.behandlingsflyt.behandling

import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokumentRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.KravMedDato
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.KravRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.KravValidering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.KravVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.NyttKrav
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.OverstyrMuligRettFra
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.rettighetsperiode.RettighetsperiodeHarRett
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.rettighetsperiode.RettighetsperiodeVurdering
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.komponenter.verdityper.Bruker
import no.nav.aap.lookup.repository.RepositoryProvider
import org.slf4j.LoggerFactory
import java.time.ZoneOffset

class MigrerKravService(
    private val mottattDokumentRepository: MottattDokumentRepository,
    private val kravRepository: KravRepository
) {
    private val log = LoggerFactory.getLogger(MigrerKravService::class.java)

    constructor(repositoryProvider: RepositoryProvider) : this(
        mottattDokumentRepository = repositoryProvider.provide(),
        kravRepository = repositoryProvider.provide()
    )

    fun oppdaterKravForOverstyrtMuligRett(
        sakId: SakId,
        behandlingId: BehandlingId,
        rettighetsperiodeVurdering: RettighetsperiodeVurdering
    ) {
        if (rettighetsperiodeVurdering.startDato == null || !rettighetsperiodeVurdering.harRettUtoverSøknadsdato.kanUtledeOverstyrMuligRettFraÅrsak()) {
            throw IllegalStateException("Klarte ikke å utlede kravvurdering for løsning")
        }

        val eksisterendeKravGrunnlag = kravRepository.hentHvisEksisterer(behandlingId)

        // Antar at det nyeste kravet med type NyttKrav som skal overstyres
        // Vi kan få flere ved rent avslag
        val nyesteKrav = eksisterendeKravGrunnlag?.kravtidslinje()?.segmenter()?.maxBy { it.fom() }?.verdi

        if (nyesteKrav == null) {
            log.info("Fant ikke NyttKrav for sak ${sakId}, lagrer ikke ned kravvurdering for rettighetsperiodeløsning")
            return
        }

        val søknadForKrav = mottattDokumentRepository.hentDokumenterAvType(sakId, InnsendingType.SØKNAD)
            .find { it.referanse == nyesteKrav.journalpostId }

        val nyVurdering = NyttKrav(
            referanse = nyesteKrav.referanse,
            journalpostId = nyesteKrav.journalpostId,
            vurdertAv = Bruker(rettighetsperiodeVurdering.vurdertAv),
            begrunnelse = rettighetsperiodeVurdering.begrunnelse,
            vurdertIBehandling = behandlingId,
            opprettet = rettighetsperiodeVurdering.vurdertDato.toInstant(ZoneOffset.UTC),
            søknadsdato = (nyesteKrav as KravMedDato).søknadsdato,
            overstyrMuligRettFra = OverstyrMuligRettFra(
                dato = rettighetsperiodeVurdering.startDato,
                årsak = rettighetsperiodeVurdering.harRettUtoverSøknadsdato.tilOverstyrMuligRettFraÅrsak()
            ),
            muligRettFra = rettighetsperiodeVurdering.startDato,
        )

        KravValidering.validerKravMedDato(nyVurdering, søknadForKrav)

        kravRepository.lagre(
            behandlingId = behandlingId,
            vurderinger =
                eksisterendeKravGrunnlag.vurderinger
                    .filterNot { it.vurdertIBehandling == behandlingId && it.referanse == nyVurdering.referanse }
                    .toSet() + nyVurdering
        )
    }

    fun reverserKravForOverstyrtMuligRett(
        behandlingId: BehandlingId,
        rettighetsperiodeVurdering: RettighetsperiodeVurdering
    ) {
        if (rettighetsperiodeVurdering.harRettUtoverSøknadsdato != RettighetsperiodeHarRett.Nei) {
            throw IllegalStateException("Forventer at vurdering som skal reverseres har RettighetsperiodeHarRett.Nei")
        }

        val eksisterendeKravGrunnlag = kravRepository.hentHvisEksisterer(behandlingId)

        val kravSomSkalTilbakestilles = eksisterendeKravGrunnlag?.vurderinger
            ?.filterIsInstance<NyttKrav>()
            ?.filter { it.erRettighetsperiodeVurdering() }
            ?.maxBy { it.opprettet }

        if (kravSomSkalTilbakestilles == null) {
            log.info("Det finnes ingen krav å tilbakestille")
            return
        }

        val nyttKrav = NyttKrav(
            referanse = kravSomSkalTilbakestilles.referanse,
            journalpostId = kravSomSkalTilbakestilles.journalpostId,
            vurdertAv = Bruker(rettighetsperiodeVurdering.vurdertAv),
            begrunnelse = rettighetsperiodeVurdering.begrunnelse,
            vurdertIBehandling = behandlingId,
            opprettet = rettighetsperiodeVurdering.vurdertDato.toInstant(ZoneOffset.UTC),
            søknadsdato = kravSomSkalTilbakestilles.søknadsdato,
            overstyrMuligRettFra = null,
            muligRettFra = kravSomSkalTilbakestilles.muligRettFra,
        )

        /** Så lenge vi skrur av rettighetsperiodesteget før vi skrur på manuell løsning av krav, 
         * kan vi anta at alle overstyrte krav i inneværende behandling kan fjernes fra grunnlaget.
         * Det kan ha kommet andre automatiske kravurderinger i mellomtiden, og vi må legge til et nytt krav med datoer lik første krav,
         * i tilfelle det finnes overstyrte krav i vedtatte behandlinger.
         * */
        val nyeVurderinger = eksisterendeKravGrunnlag.vurderinger
            .filterNot { it.vurdertIBehandling == behandlingId && it.referanse == kravSomSkalTilbakestilles.referanse }
            .toSet() + nyttKrav

        kravRepository.lagre(
            behandlingId = behandlingId,
            vurderinger = nyeVurderinger
        )
    }

    private fun KravVurdering.erRettighetsperiodeVurdering(): Boolean {
        return this is NyttKrav && this.overstyrMuligRettFra != null
    }
}