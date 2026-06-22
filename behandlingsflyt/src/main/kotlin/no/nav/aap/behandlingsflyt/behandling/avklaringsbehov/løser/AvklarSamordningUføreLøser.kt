package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarSamordningUføreLøsning
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.uførevurdering.SamordningUføreRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.uførevurdering.SamordningUføreVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.uførevurdering.SamordningUføreVurderingPeriode
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.uføre.UføreRepository
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.komponenter.httpklient.exception.UgyldigForespørselException
import no.nav.aap.komponenter.verdityper.Prosent
import no.nav.aap.lookup.repository.RepositoryProvider

class AvklarSamordningUføreLøser(
    private val samordningUføreRepository: SamordningUføreRepository,
    private val uføreRepository: UføreRepository,
    private val sakRepository: SakRepository,
) : AvklaringsbehovsLøser<AvklarSamordningUføreLøsning> {

    constructor(repositoryProvider: RepositoryProvider) : this(
        samordningUføreRepository = repositoryProvider.provide(),
        uføreRepository = repositoryProvider.provide(),
        sakRepository = repositoryProvider.provide(),
    )

    override fun løs(
        kontekst: AvklaringsbehovKontekst,
        løsning: AvklarSamordningUføreLøsning
    ): LøsningsResultat {
        validerAllePerioderErVurdert(kontekst.sakId(), kontekst.behandlingId(), løsning)
        validerIkkeDupliserteVurderinger(løsning)
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

    private fun validerIkkeDupliserteVurderinger(løsning: AvklarSamordningUføreLøsning) {
        val duplikatLøsningPåSammeVirkningstidspunkt = løsning.samordningUføreVurdering.vurderingPerioder
            .groupingBy { it.virkningstidspunkt }
            .eachCount()
            .filter { it.value > 1 }
            .keys

        if (duplikatLøsningPåSammeVirkningstidspunkt.isNotEmpty()) {
            throw UgyldigForespørselException("Det finnes duplikate vurderinger på samme virkningstidspunkt ${duplikatLøsningPåSammeVirkningstidspunkt.first()}. Dette er ikke tillatt - vennligst fjern en av radene.")
        }
    }

    private fun validerAllePerioderErVurdert(
        sakId: SakId,
        behandlingId: BehandlingId,
        løsning: AvklarSamordningUføreLøsning
    ) {
        val uføreGrunnlag = uføreRepository.hentHvisEksisterer(behandlingId = behandlingId)
        val vurderinger = løsning.samordningUføreVurdering.vurderingPerioder
        val rettighetsperiode = sakRepository.hent(sakId).rettighetsperiode

        val harVurdertAllePerioder = uføreGrunnlag?.vurderinger.orEmpty()
            .filter { it.uføregradTom == null || it.uføregradTom >= rettighetsperiode.fom }
            .all { uføre ->
                vurderinger.any { vurdering -> vurdering.virkningstidspunkt == uføre.virkningstidspunkt }
            }
        if (!harVurdertAllePerioder) {
            throw UgyldigForespørselException(message = "Har ikke vurdert alle perioder for samordning med delvis uføre.")
        }
    }


    override fun forBehov(): Definisjon {
        return Definisjon.AVKLAR_SAMORDNING_UFØRE
    }
}