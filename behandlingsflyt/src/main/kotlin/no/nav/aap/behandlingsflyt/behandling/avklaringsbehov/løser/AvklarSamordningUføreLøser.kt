package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarSamordningUføreLøsning
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.uførevurdering.SamordningUføreRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.uførevurdering.SamordningUføreVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.uførevurdering.SamordningUføreVurderingPeriode
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.uføre.UføreRepository
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.httpklient.exception.UgyldigForespørselException
import no.nav.aap.komponenter.verdityper.Prosent
import no.nav.aap.lookup.repository.RepositoryProvider

class AvklarSamordningUføreLøser(
    private val samordningUføreRepository: SamordningUføreRepository,
    private val uføreRepository: UføreRepository,
) : AvklaringsbehovsLøser<AvklarSamordningUføreLøsning> {

    constructor(repositoryProvider: RepositoryProvider) : this(
        samordningUføreRepository = repositoryProvider.provide(),
        uføreRepository = repositoryProvider.provide(),
    )

    override fun løs(
        kontekst: AvklaringsbehovKontekst,
        løsning: AvklarSamordningUføreLøsning
    ): LøsningsResultat {
        validerAllePerioderErVurdert(kontekst.behandlingId(), løsning)
        samordningUføreRepository.lagre(
            kontekst.behandlingId(),
            SamordningUføreVurdering(
                begrunnelse = løsning.samordningUføreVurdering.begrunnelse,
                vurderingPerioder = løsning.samordningUføreVurdering.vurderingPerioder.map {
                    SamordningUføreVurderingPeriode(
                        virkningstidspunkt = it.virkningstidspunkt,
                        uføregradTilSamordning = it.uføregradTilSamordning.let(::Prosent)
                    )
                },
                vurdertAv = kontekst.bruker.ident
            )
        )
        return LøsningsResultat("Vurdert samordning uføre")
    }

    private fun validerAllePerioderErVurdert(behandlingId: BehandlingId, løsning: AvklarSamordningUføreLøsning) {
        val uføreGrunnlag = uføreRepository.hentHvisEksisterer(behandlingId = behandlingId)
        val vurderinger = løsning.samordningUføreVurdering.vurderingPerioder
        val harVurdertAllePerioder = uføreGrunnlag?.vurderinger?.all { uføre ->
            vurderinger.any { vurdering -> vurdering.virkningstidspunkt == uføre.virkningstidspunkt }
        } ?: true
        if (!harVurdertAllePerioder) {
            throw UgyldigForespørselException(message = "Har ikke vurdert alle perioder for samordning med delvis uføre")
        }
    }


    override fun forBehov(): Definisjon {
        return Definisjon.AVKLAR_SAMORDNING_UFØRE
    }
}