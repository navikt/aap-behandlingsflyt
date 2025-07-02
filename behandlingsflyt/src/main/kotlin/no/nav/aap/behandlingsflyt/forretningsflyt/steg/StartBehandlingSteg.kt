package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.ÅrsakTilSettPåVent
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningVurderingRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FantVentebehov
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.flyt.steg.Ventebehov
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.ÅrsakTilBehandling
import no.nav.aap.lookup.repository.RepositoryProvider
import org.slf4j.LoggerFactory

class StartBehandlingSteg private constructor(
    private val vilkårsresultatRepository: VilkårsresultatRepository,
    private val samordningVurderingRepository: SamordningVurderingRepository,
) : BehandlingSteg {

    private val logger = LoggerFactory.getLogger(StartBehandlingSteg::class.java)

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        if (kontekst.behandlingType == TypeBehandling.Førstegangsbehandling) {
            val vilkårsresultat = vilkårsresultatRepository.hent(kontekst.behandlingId)
            val rettighetsperiode = kontekst.rettighetsperiode
            Vilkårtype
                .entries
                .filter { it.obligatorisk }
                .forEach { vilkårstype ->
                    vilkårsresultat
                        .leggTilHvisIkkeEksisterer(vilkårstype)
                        .leggTilIkkeVurdertPeriode(rettighetsperiode)
                }

            vilkårsresultatRepository.lagre(kontekst.behandlingId, vilkårsresultat)
        }

        if (kontekst.behandlingType == TypeBehandling.Revurdering) {
            if (kontekst.årsakerTilBehandling.contains(ÅrsakTilBehandling.REVURDER_SAMORDNING)) {
                val ventTil =
                    requireNotNull(samordningVurderingRepository.hentHvisEksisterer(kontekst.behandlingId))
                    { "Forventet å finne samordningvurdering ved revurdering med årsak ${ÅrsakTilBehandling.REVURDER_SAMORDNING}" }
                logger.info("Fant samordningdato, setter på vent.")
                return FantVentebehov(
                    Ventebehov(
                        definisjon = Definisjon.SAMORDNING_VENT_PA_VIRKNINGSTIDSPUNKT,
                        grunn = ÅrsakTilSettPåVent.VENTER_PÅ_OPPLYSNINGER,
                        frist = ventTil.fristNyRevurdering!!
                    )
                )
            }
        }

        if (kontekst.behandlingType == TypeBehandling.SvarFraAndreinstans) return Fullført

        return Fullført
    }

    companion object : FlytSteg {
        override fun konstruer(repositoryProvider: RepositoryProvider): BehandlingSteg {
            return StartBehandlingSteg(
                repositoryProvider.provide(),
                repositoryProvider.provide(),
            )
        }

        override fun type(): StegType {
            return StegType.START_BEHANDLING
        }

        override fun toString(): String {
            return "FlytSteg(type:${type()})"
        }
    }
}
