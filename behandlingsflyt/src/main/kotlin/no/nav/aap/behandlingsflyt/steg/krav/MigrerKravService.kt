package no.nav.aap.behandlingsflyt.steg.krav

import java.time.ZoneOffset
import no.nav.aap.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokumentRepository
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.krav.KravVurdering
import no.nav.aap.krav.OverstyrMuligRettFra
import no.nav.aap.krav.RelevantKrav
import no.nav.aap.krav.RettighetsperiodeHarRett
import no.nav.aap.krav.RettighetsperiodeVurdering
import no.nav.aap.lookup.repository.RepositoryProvider
import org.slf4j.LoggerFactory

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
        val startDato = rettighetsperiodeVurdering.startDato
        if (startDato == null || !rettighetsperiodeVurdering.harRettUtoverSøknadsdato.kanUtledeOverstyrMuligRettFraÅrsak()) {
            throw IllegalStateException("Klarte ikke å utlede kravvurdering for løsning")
        }

        val eksisterendeKravGrunnlag = kravRepository.hentHvisEksisterer(behandlingId)

        // Antar at det nyeste kravet med type NyttKrav som skal overstyres
        // Vi kan få flere ved rent avslag
        val nyesteKrav = eksisterendeKravGrunnlag?.kravtidslinje()?.segmenter()?.maxByOrNull { it.fom() }?.verdi

        if (nyesteKrav == null) {
            log.info("Fant ikke NyttKrav for sak ${sakId}, lagrer ikke ned kravvurdering for rettighetsperiodeløsning")
            return
        }

        val søknadForKrav = mottattDokumentRepository.hentDokumenterAvType(sakId, InnsendingType.SØKNAD)
            .find { it.referanse == nyesteKrav.journalpostId }

        val nyVurdering = RelevantKrav(
            referanse = nyesteKrav.referanse,
            journalpostId = nyesteKrav.journalpostId,
            vurdertAv = rettighetsperiodeVurdering.vurdertAv,
            begrunnelse = rettighetsperiodeVurdering.begrunnelse,
            vurdertIBehandling = behandlingId,
            opprettet = rettighetsperiodeVurdering.vurdertDato.toInstant(ZoneOffset.UTC),
            søknadsdato = nyesteKrav.søknadsdato,
            overstyrMuligRettFra = OverstyrMuligRettFra(
                dato = startDato,
                årsak = rettighetsperiodeVurdering.harRettUtoverSøknadsdato.tilOverstyrMuligRettFraÅrsak()
            ),
            muligRettFra = startDato,
        )

        KravValidering.validerRelevantKrav(nyVurdering, søknadForKrav)

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
            ?.filterIsInstance<RelevantKrav>()
            ?.filter { it.erRettighetsperiodeVurdering() }
            ?.maxByOrNull { it.opprettet }

        if (kravSomSkalTilbakestilles == null) {
            log.info("Det finnes ingen krav å tilbakestille")
            return
        }

        val relevantKrav = RelevantKrav(
            referanse = kravSomSkalTilbakestilles.referanse,
            journalpostId = kravSomSkalTilbakestilles.journalpostId,
            vurdertAv = rettighetsperiodeVurdering.vurdertAv,
            begrunnelse = rettighetsperiodeVurdering.begrunnelse,
            vurdertIBehandling = behandlingId,
            opprettet = rettighetsperiodeVurdering.vurdertDato.toInstant(ZoneOffset.UTC),
            søknadsdato = kravSomSkalTilbakestilles.søknadsdato,
            overstyrMuligRettFra = null,
            muligRettFra = kravSomSkalTilbakestilles.muligRettFra,
        )
        val nyeVurderinger = eksisterendeKravGrunnlag.vurderinger
            .filterNot { it.vurdertIBehandling == behandlingId && it.referanse == kravSomSkalTilbakestilles.referanse } + relevantKrav

        kravRepository.lagre(
            behandlingId = behandlingId,
            vurderinger = nyeVurderinger.toSet()
        )
    }

    private fun KravVurdering.erRettighetsperiodeVurdering(): Boolean {
        return this is RelevantKrav && this.overstyrMuligRettFra != null
    }
}