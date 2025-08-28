package no.nav.aap.behandlingsflyt.faktagrunnlag.klage.resultat

import no.nav.aap.behandlingsflyt.behandling.trekkklage.TrekkKlageService
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
import java.time.LocalDate

interface IKlageresultatUtleder {
    fun utledKlagebehandlingResultat(behandlingId: BehandlingId): KlageResultat
}

class KlageresultatUtleder(
    private val formkravRepository: FormkravRepository,
    private val behandlendeEnhetRepository: BehandlendeEnhetRepository,
    private val klagebehandlingKontorRepository: KlagebehandlingKontorRepository,
    private val klagebehandlingNayRepository: KlagebehandlingNayRepository,
    private val trekkKlageService: TrekkKlageService
) : IKlageresultatUtleder {
    constructor(repositoryProvider: RepositoryProvider) : this(
        formkravRepository = repositoryProvider.provide(),
        behandlendeEnhetRepository = repositoryProvider.provide(),
        klagebehandlingKontorRepository = repositoryProvider.provide(),
        klagebehandlingNayRepository = repositoryProvider.provide(),
        trekkKlageService = TrekkKlageService(repositoryProvider)
    )

    override fun utledKlagebehandlingResultat(behandlingId: BehandlingId): KlageResultat {
        val erKlageTrukket = trekkKlageService.klageErTrukket(behandlingId)
        val formkrav = formkravRepository.hentHvisEksisterer(behandlingId)
        val behandlendeEnhet = behandlendeEnhetRepository.hentHvisEksisterer(behandlingId)
        val klagebehandlingVurderingKontor = klagebehandlingKontorRepository.hentHvisEksisterer(behandlingId)
        val klagebehandlingVurderingNay = klagebehandlingNayRepository.hentHvisEksisterer(behandlingId)

        val innstilling = utledKlagebehandlingResultat(
            erKlageTrukket,
            formkrav?.varsel?.svarfrist,
            formkrav?.vurdering,
            behandlendeEnhet?.vurdering,
            klagebehandlingVurderingNay?.vurdering,
            klagebehandlingVurderingKontor?.vurdering,
        )
        return innstilling
    }

    companion object {
        fun utledKlagebehandlingResultat(
            erKlageTrukket: Boolean,
            svarFrist: LocalDate?,
            formkravVurdering: FormkravVurdering?,
            behandlendeEnhetVurdering: BehandlendeEnhetVurdering?,
            klagebehandlingNayVurdering: KlagevurderingNay?,
            klagebehandlingKontorVurdering: KlagevurderingKontor?,
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

            val formkravIkkeOppfylltFortsattInnenforSvarfrist =
                formkravVurdering?.erIkkeOppfylt() == true && svarFrist != null && svarFrist > LocalDate.now()
            val formkravIkkeOppfylltOgEtterSvarfrist =
                formkravVurdering?.erIkkeOppfylt() == true && svarFrist != null && svarFrist <= LocalDate.now()

            return when {
                erKlageTrukket -> Trukket
                manglerVurdering -> Ufullstendig(ÅrsakTilUfullstendigResultat.MANGLER_VURDERING)
                formkravVurdering?.erFristOverholdt() == false -> Avslått(årsak = ÅrsakTilAvslag.IKKE_OVERHOLDT_FRIST)

                formkravIkkeOppfylltFortsattInnenforSvarfrist -> Ufullstendig(årsak = ÅrsakTilUfullstendigResultat.VENTER_PÅ_SVAR_FRA_BRUKER)
                formkravIkkeOppfylltOgEtterSvarfrist -> Avslått(årsak = ÅrsakTilAvslag.IKKE_OVERHOLDT_FORMKRAV)

                skalOmgjøres -> Omgjøres(
                    vilkårSomSkalOmgjøres = ((klagebehandlingNayVurdering?.vilkårSomOmgjøres
                        .orEmpty()) + (klagebehandlingKontorVurdering?.vilkårSomOmgjøres.orEmpty())).distinct()
                )

                skalOpprettholdes -> Opprettholdes(
                    vilkårSomSkalOpprettholdes = ((klagebehandlingNayVurdering?.vilkårSomOpprettholdes
                        .orEmpty()) + (klagebehandlingKontorVurdering?.vilkårSomOpprettholdes
                        .orEmpty())).distinct()
                )

                else -> DelvisOmgjøres(
                    vilkårSomSkalOmgjøres = ((klagebehandlingNayVurdering?.vilkårSomOmgjøres.orEmpty()) + (klagebehandlingKontorVurdering?.vilkårSomOmgjøres.orEmpty())).distinct(),
                    vilkårSomSkalOpprettholdes = ((klagebehandlingNayVurdering?.vilkårSomOpprettholdes.orEmpty()) + (klagebehandlingKontorVurdering?.vilkårSomOpprettholdes.orEmpty())).distinct()
                )

            }
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