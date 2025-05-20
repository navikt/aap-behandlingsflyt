package no.nav.aap.behandlingsflyt.faktagrunnlag.klage

import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.behandlendeenhet.BehandlendeEnhetRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.behandlendeenhet.BehandlendeEnhetVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.formkrav.FormkravRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.formkrav.FormkravVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.klagebehandling.kontor.KlagebehandlingKontorRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.klagebehandling.kontor.KlagevurderingKontor
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.klagebehandling.KlageInnstilling
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.klagebehandling.nay.KlagebehandlingNayRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.klagebehandling.nay.KlagevurderingNay
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.lookup.repository.RepositoryProvider

class KlageresultatUtleder(
    private val formkravRepository: FormkravRepository,
    private val behandlendeEnhetRepository: BehandlendeEnhetRepository,
    private val klagebehandlingKontorRepository: KlagebehandlingKontorRepository,
    private val klagebehandlingNayRepository: KlagebehandlingNayRepository
) {
    constructor(repositoryProvider: RepositoryProvider) : this(
        formkravRepository = repositoryProvider.provide(),
        behandlendeEnhetRepository = repositoryProvider.provide(),
        klagebehandlingKontorRepository = repositoryProvider.provide(),
        klagebehandlingNayRepository = repositoryProvider.provide()
    )

    fun utledKlagebehandlingResultat(behandlingId: BehandlingId): KlageResultat {
        val formkrav = formkravRepository.hentHvisEksisterer(behandlingId)
        val behandlendeEnhet = behandlendeEnhetRepository.hentHvisEksisterer(behandlingId)
        val klagebehandlingVurderingKontor = klagebehandlingKontorRepository.hentHvisEksisterer(behandlingId)
        val klagebehandlingVurderingNay = klagebehandlingNayRepository.hentHvisEksisterer(behandlingId)

        val innstilling = utledKlagebehandlingResultat(
            formkrav?.vurdering,
            behandlendeEnhet?.vurdering,
            klagebehandlingVurderingNay?.vurdering,
            klagebehandlingVurderingKontor?.vurdering
        )
        return innstilling
    }

    companion object {
        fun utledKlagebehandlingResultat(
            formkravVurdering: FormkravVurdering?,
            behandlendeEnhetVurdering: BehandlendeEnhetVurdering?,
            klagebehandlingNayVurdering: KlagevurderingNay?,
            klagebehandlingKontorVurdering: KlagevurderingKontor?
        ): KlageResultat {
            val manglerVurdering = manglerVurdering(
                formkravVurdering,
                behandlendeEnhetVurdering,
                klagebehandlingNayVurdering,
                klagebehandlingKontorVurdering
            )
            val skalOmgjøres =
                (klagebehandlingNayVurdering == null || klagebehandlingNayVurdering.innstilling == KlageInnstilling.OMGJØR)
                        && (klagebehandlingKontorVurdering == null || klagebehandlingKontorVurdering.innstilling == KlageInnstilling.OMGJØR)
            val skalOpprettholdes =
                (klagebehandlingNayVurdering == null || klagebehandlingNayVurdering.innstilling == KlageInnstilling.OPPRETTHOLD) && (klagebehandlingKontorVurdering == null || klagebehandlingKontorVurdering.innstilling == KlageInnstilling.OPPRETTHOLD)
            val erInkonsistent = erInkonsistent(
                klagebehandlingNayVurdering,
                klagebehandlingKontorVurdering
            )

            return when {
                manglerVurdering -> Ufullstendig(ÅrsakTilUfullstendigResultat.MANGLER_VURDERING)
                erInkonsistent -> Ufullstendig(ÅrsakTilUfullstendigResultat.INKONSISTENT_VURDERING)

                formkravVurdering?.erOppfylt() == false -> Avslått(årsak = ÅrsakTilAvslag.IKKE_OVERHOLDT_FORMKRAV)

                skalOmgjøres -> Omgjøres(
                    vilkårSomSkalOmgjøres = ((klagebehandlingNayVurdering?.vilkårSomOmgjøres
                        ?: emptyList()) + (klagebehandlingKontorVurdering?.vilkårSomOmgjøres ?: emptyList())).distinct()
                )

                skalOpprettholdes -> Opprettholdes(
                    vilkårSomSkalOpprettholdes = ((klagebehandlingNayVurdering?.vilkårSomOpprettholdes
                        ?: emptyList()) + (klagebehandlingKontorVurdering?.vilkårSomOpprettholdes
                        ?: emptyList())).distinct()
                )

                else -> DelvisOmgjøres(
                    vilkårSomSkalOmgjøres = ((klagebehandlingNayVurdering?.vilkårSomOmgjøres
                        ?: emptyList()) + (klagebehandlingKontorVurdering?.vilkårSomOmgjøres
                        ?: emptyList())).distinct(),
                    vilkårSomSkalOpprettholdes = ((klagebehandlingNayVurdering?.vilkårSomOpprettholdes
                        ?: emptyList()) + (klagebehandlingKontorVurdering?.vilkårSomOpprettholdes
                        ?: emptyList())).distinct()
                )


            }
        }

        private fun erInkonsistent(
            klagebehandlingNayVurdering: KlagevurderingNay?,
            klagebehandlingKontorVurdering: KlagevurderingKontor?
        ): Boolean {
            if (klagebehandlingNayVurdering == null || klagebehandlingKontorVurdering == null) {
                return false
            }
            return klagebehandlingKontorVurdering.vilkårSomOpprettholdes.intersect(
                klagebehandlingNayVurdering.vilkårSomOmgjøres.toSet()
            ).isNotEmpty()
                    || klagebehandlingKontorVurdering.vilkårSomOmgjøres.intersect(
                klagebehandlingNayVurdering.vilkårSomOpprettholdes.toSet()
            ).isNotEmpty()

        }

        private fun manglerVurdering(
            formkravVurdering: FormkravVurdering?,
            behandlendeEnhetVurdering: BehandlendeEnhetVurdering?,
            klagebehandlingNayVurdering: KlagevurderingNay?,
            klagebehandlingKontorVurdering: KlagevurderingKontor?
        ): Boolean {
            return when {
                formkravVurdering == null -> true
                formkravVurdering.erOppfylt() -> {
                    behandlendeEnhetVurdering == null
                            || (behandlendeEnhetVurdering.skalBehandlesAvNay && klagebehandlingNayVurdering == null)
                            || (behandlendeEnhetVurdering.skalBehandlesAvKontor && klagebehandlingKontorVurdering == null)
                }

                else -> false
            }
        }
    }

}