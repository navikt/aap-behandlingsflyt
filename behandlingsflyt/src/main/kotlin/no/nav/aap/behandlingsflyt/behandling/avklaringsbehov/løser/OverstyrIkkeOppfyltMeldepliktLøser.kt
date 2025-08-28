package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.OverstyrIkkeOppfyltMeldepliktLøsning
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.MeldepliktRimeligGrunnPerioder
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.MeldepliktRimeligGrunnRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.RimeligGrunnVurdering
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.lookup.repository.RepositoryProvider

class OverstyrIkkeOppfyltMeldepliktLøser(
    private val behandlingRepository: BehandlingRepository,
    private val meldepliktRimeligGrunnRepository: MeldepliktRimeligGrunnRepository,
) : AvklaringsbehovsLøser<OverstyrIkkeOppfyltMeldepliktLøsning> {

    constructor(repositoryProvider: RepositoryProvider) : this(
        behandlingRepository = repositoryProvider.provide(),
        meldepliktRimeligGrunnRepository = repositoryProvider.provide(),
    )

    override fun løs(
        kontekst: AvklaringsbehovKontekst,
        løsning: OverstyrIkkeOppfyltMeldepliktLøsning
    ): LøsningsResultat {
        val behandling = behandlingRepository.hent(kontekst.kontekst.behandlingId)
        val rimeligGrunnVurderinger =
            løsning.rimeligGrunnVurderinger.map {
                RimeligGrunnVurdering(
                    harRimeligGrunn = it.harRimeligGrunn,
                    fraDato = it.fraDato,
                    begrunnelse = it.begrunnelse,
                    vurdertAv = kontekst.bruker.ident,
                    opprettetTid = null,
                )
            }
        val eksisterendeRimeligGrunnPerioder = MeldepliktRimeligGrunnPerioder(
            behandling.forrigeBehandlingId?.let { meldepliktRimeligGrunnRepository.hentHvisEksisterer(it) }?.vurderinger
                .orEmpty()
        )

        val nyeRimeligGrunnPerioder =
            eksisterendeRimeligGrunnPerioder.leggTil(MeldepliktRimeligGrunnPerioder(rimeligGrunnVurderinger))

        meldepliktRimeligGrunnRepository.lagre(
            behandlingId = behandling.id,
            vurderinger = nyeRimeligGrunnPerioder.gjeldendeRimeligGrunnVurderinger()
        )

        return LøsningsResultat(
            begrunnelse = "Vurdert rimelig grunn for ikke overholdt meldeplikt",
            kreverToTrinn = rimeligGrunnVurderinger.minstEnRimeligGrunn()
        )
    }

    override fun forBehov(): Definisjon {
        return Definisjon.OVERSTYR_IKKE_OPPFYLT_MELDEPLIKT
    }

    private fun List<RimeligGrunnVurdering>.minstEnRimeligGrunn(): Boolean {
        return this.any { it.harRimeligGrunn }
    }
}
