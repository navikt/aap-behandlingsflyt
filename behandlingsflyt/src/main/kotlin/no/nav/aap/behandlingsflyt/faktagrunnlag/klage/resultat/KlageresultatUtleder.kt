package no.nav.aap.behandlingsflyt.faktagrunnlag.klage.resultat

import no.nav.aap.behandlingsflyt.behandling.trekkklage.TrekkKlageService
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.behandlendeenhet.BehandlendeEnhetRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.behandlendeenhet.BehandlendeEnhetVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.effektueravvistpåformkrav.EffektuerAvvistPåFormkravRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.effektueravvistpåformkrav.EffektuerAvvistPåFormkravVurdering
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
    private val klagebehandlingNayRepository: KlagebehandlingNayRepository,
    private val effektuerAvvistPåFormkravRepository: EffektuerAvvistPåFormkravRepository,
    private val trekkKlageService: TrekkKlageService
) {
    constructor(repositoryProvider: RepositoryProvider) : this(
        formkravRepository = repositoryProvider.provide(),
        behandlendeEnhetRepository = repositoryProvider.provide(),
        klagebehandlingKontorRepository = repositoryProvider.provide(),
        klagebehandlingNayRepository = repositoryProvider.provide(),
        effektuerAvvistPåFormkravRepository = repositoryProvider.provide(),
        trekkKlageService = TrekkKlageService(repositoryProvider)
    )

    fun utledKlagebehandlingResultat(behandlingId: BehandlingId): KlageResultat {
        val erKlageTrukket = trekkKlageService.klageErTrukket(behandlingId)
        val formkrav = formkravRepository.hentHvisEksisterer(behandlingId)
        val behandlendeEnhet = behandlendeEnhetRepository.hentHvisEksisterer(behandlingId)
        val klagebehandlingVurderingKontor = klagebehandlingKontorRepository.hentHvisEksisterer(behandlingId)
        val klagebehandlingVurderingNay = klagebehandlingNayRepository.hentHvisEksisterer(behandlingId)
        val effektuerAvvistPåFormkravRepository = effektuerAvvistPåFormkravRepository.hentHvisEksisterer(behandlingId)

        val innstilling = utledKlagebehandlingResultat(
            erKlageTrukket,
            formkrav?.vurdering,
            behandlendeEnhet?.vurdering,
            klagebehandlingVurderingNay?.vurdering,
            klagebehandlingVurderingKontor?.vurdering,
            effektuerAvvistPåFormkravRepository?.vurdering
        )
        return innstilling
    }

    companion object {
        fun utledKlagebehandlingResultat(
            erKlageTrukket: Boolean,
            formkravVurdering: FormkravVurdering?,
            behandlendeEnhetVurdering: BehandlendeEnhetVurdering?,
            klagebehandlingNayVurdering: KlagevurderingNay?,
            klagebehandlingKontorVurdering: KlagevurderingKontor?,
            effektuerAvvistPåFormkravVurdering: EffektuerAvvistPåFormkravVurdering?
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
            val erInkonsistentFormkravVurdering =  erInkonsistentFormkravVurdering(
                formkravVurdering,
                effektuerAvvistPåFormkravVurdering
            )

            return when {
                erKlageTrukket -> Trukket
                manglerVurdering -> Ufullstendig(ÅrsakTilUfullstendigResultat.MANGLER_VURDERING)
                erInkonsistentFormkravVurdering -> Ufullstendig(ÅrsakTilUfullstendigResultat.INKONSISTENT_FORMKRAV_VURDERING)
                formkravVurdering?.erFristOverholdt() == false -> Avslått(årsak = ÅrsakTilAvslag.IKKE_OVERHOLDT_FRIST)
                effektuerAvvistPåFormkravVurdering?.skalEndeligAvvises == true -> Avslått(årsak = ÅrsakTilAvslag.IKKE_OVERHOLDT_FORMKRAV)
                formkravVurdering?.erOppfylt() == false -> Ufullstendig(ÅrsakTilUfullstendigResultat.VENTER_PÅ_SVAR_FRA_BRUKER)

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

        private fun erInkonsistentFormkravVurdering(
            formkravVurdering: FormkravVurdering?,
            effektuerAvvistPåFormkravVurdering: EffektuerAvvistPåFormkravVurdering?
        ): Boolean {
            if (formkravVurdering == null || effektuerAvvistPåFormkravVurdering == null) {
                return false
            }
            return formkravVurdering.erOppfylt() == effektuerAvvistPåFormkravVurdering.skalEndeligAvvises
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