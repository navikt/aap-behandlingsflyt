package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.SYSTEMBRUKER
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovMetadataUtleder
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovService
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderinger
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderingerImpl
import no.nav.aap.behandlingsflyt.behandling.vilkår.overganguføre.OvergangUføreFaktagrunnlag
import no.nav.aap.behandlingsflyt.behandling.vilkår.overganguføre.OvergangUføreVilkår
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokumentRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.uføre.UføreRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.uføre.tilTidslinje
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand.BistandRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.overgangufore.OvergangUføreGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.overgangufore.OvergangUføreRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.overgangufore.OvergangUføreVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.overgangufore.UføreSøknadVedtakResultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.overgangufore.OvergangUføreValidering.nårVurderingErKonsistentMedSykdomOgBistand
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykdomRepository
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.UførevedtakResultat
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.UførevedtakV0
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.tidslinje.orEmpty
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.lookup.repository.RepositoryProvider
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.LocalDate

class OvergangUføreSteg private constructor(
    private val vilkårsresultatRepository: VilkårsresultatRepository,
    private val sykdomRepository: SykdomRepository,
    private val overgangUføreRepository: OvergangUføreRepository,
    private val tidligereVurderinger: TidligereVurderinger,
    private val bistandRepository: BistandRepository,
    private val avklaringsbehovService: AvklaringsbehovService,
    private val mottattDokumentRepository: MottattDokumentRepository,
    private val uføreRepository: UføreRepository,
    private val unleashGateway: UnleashGateway,
) : BehandlingSteg, AvklaringsbehovMetadataUtleder {
    private val log = LoggerFactory.getLogger(javaClass)

    constructor(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider) : this(
        vilkårsresultatRepository = repositoryProvider.provide(),
        overgangUføreRepository = repositoryProvider.provide(),
        sykdomRepository = repositoryProvider.provide(),
        tidligereVurderinger = TidligereVurderingerImpl(repositoryProvider, gatewayProvider),
        bistandRepository = repositoryProvider.provide(),
        avklaringsbehovService = AvklaringsbehovService(repositoryProvider),
        mottattDokumentRepository = repositoryProvider.provide(),
        uføreRepository = repositoryProvider.provide(),
        unleashGateway = gatewayProvider.provide()
    )

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        if (erAutomatiskStans11_18(kontekst)) {
            val uførevedtak = hentUførevedtak(kontekst.sakId) ?: return Fullført
            lagreAutomatiskStans11_18(
                sakId = kontekst.sakId,
                behandlingId = kontekst.behandlingId,
                forrigeBehandlingId = kontekst.forrigeBehandlingId,
                virkningsdato = uførevedtak.virkningsdato,
            )
            avklaringsbehovService.oppdaterAvklaringsbehov(
                definisjon = Definisjon.AVKLAR_OVERGANG_UFORE,
                vedtakBehøverVurdering = { false },
                erTilstrekkeligVurdert = { true },
                tilbakestillGrunnlag = {},
                kontekst = kontekst
            )
        } else {
            val perioderSomIkkeErTilstrekkeligVurdert: () -> Set<Periode> =
                { perioderSomIkkeErTilstrekkeligVurdert(kontekst) }

            avklaringsbehovService.oppdaterAvklaringsbehovForPeriodisertYtelsesvilkårTilstrekkeligVurdert(
                kontekst = kontekst,
                definisjon = Definisjon.AVKLAR_OVERGANG_UFORE,
                tvingerAvklaringsbehov = setOf(
                    Vurderingsbehov.SYKDOM_ARBEVNE_BEHOV_FOR_BISTAND,
                    Vurderingsbehov.OVERGANG_UFORE,
                ),
                nårVurderingErRelevant = ::nårVurderingErRelevant,
                perioderSomIkkeErTilstrekkeligVurdert = perioderSomIkkeErTilstrekkeligVurdert,
                tilbakestillGrunnlag = {
                    val vedtatteVurderinger =
                        kontekst.forrigeBehandlingId?.let { overgangUføreRepository.hentHvisEksisterer(it) }?.vurderinger.orEmpty()
                    val aktiveVurderinger =
                        overgangUføreRepository.hentHvisEksisterer(kontekst.behandlingId)?.vurderinger.orEmpty()
                    if (vedtatteVurderinger.toSet() != aktiveVurderinger.toSet()) {
                        overgangUføreRepository.lagre(kontekst.behandlingId, vedtatteVurderinger)
                    }
                },
            )
        }

        when (kontekst.vurderingType) {
            VurderingType.FØRSTEGANGSBEHANDLING, VurderingType.REVURDERING -> {
                val vilkårsresultat = vilkårsresultatRepository.hent(kontekst.behandlingId)
                vilkårsresultat.leggTilHvisIkkeEksisterer(Vilkårtype.OVERGANGUFØREVILKÅRET)
                val grunnlag = OvergangUføreFaktagrunnlag(
                    rettighetsperiode = kontekst.rettighetsperiode,
                    overgangUføreGrunnlag = overgangUføreRepository.hentHvisEksisterer(kontekst.behandlingId),
                )
                OvergangUføreVilkår(vilkårsresultat).vurder(grunnlag = grunnlag)
                vilkårsresultatRepository.lagre(kontekst.behandlingId, vilkårsresultat)
            }

            else -> {} // Do nothing
        }

        return Fullført
    }

    private fun erAutomatiskStans11_18(kontekst: FlytKontekstMedPerioder): Boolean {
        if (unleashGateway.isDisabled(BehandlingsflytFeature.AutomatiskStans1118)) {
            return false
        }

        val uførevedtak = hentUførevedtak(kontekst.sakId) ?: return false
        return uførevedtak.resultat == UførevedtakResultat.INNV &&
                uførevedtak.virkningsdato.isAfter(LocalDate.now())
    }

    private fun lagreAutomatiskStans11_18(
        sakId: SakId,
        behandlingId: BehandlingId,
        forrigeBehandlingId: BehandlingId?,
        virkningsdato: LocalDate,
    ) {
        val uførevedtak = hentUførevedtak(sakId) ?: return
        log.info("Lagrer automatisk 11-18 for sak $sakId i behandling $behandlingId")
        val vedtakResultat = utledVedtakResultat(
            behandlingId = behandlingId,
            virkningsdato = virkningsdato,
        )

        val vedtatteVurderinger = forrigeBehandlingId
            ?.let { overgangUføreRepository.hentHvisEksisterer(it) }
            ?.vurderinger
            .orEmpty()
        val eksisterendeVurderinger = overgangUføreRepository.hentHvisEksisterer(behandlingId)?.vurderinger.orEmpty()
        val harAutomatiskVurderingAllerede = eksisterendeVurderinger.any {
            it.vurdertAv == SYSTEMBRUKER.ident && it.fom == uførevedtak.virkningsdato
        }
        if (harAutomatiskVurderingAllerede) return

        val automatiskVurdering = OvergangUføreVurdering(
            begrunnelse = "Automatisk opphør på grunn av vedtak om uføre",
            brukerHarSøktOmUføretrygd = true,
            brukerHarFåttVedtakOmUføretrygd = vedtakResultat,
            brukerRettPåAAP = false,
            fom = virkningsdato,
            tom = null,
            vurdertAv = SYSTEMBRUKER.ident,
            vurdertIBehandling = behandlingId,
            opprettet = Instant.now(),
        )

        overgangUføreRepository.lagre(
            behandlingId = behandlingId,
            overgangUføreVurderinger = (eksisterendeVurderinger + vedtatteVurderinger) + automatiskVurdering,
        )
    }

    private fun hentUførevedtak(sakId: SakId): UførevedtakV0? {
        val dokument = mottattDokumentRepository.hentDokumenterAvType(
            sakId = sakId,
            type = InnsendingType.UFØRE_VEDTAK_HENDELSE
        ).maxByOrNull { it.mottattTidspunkt } ?: return null

        return requireNotNull(dokument.strukturerteData<UførevedtakV0>()?.data) {
            "Fant ikke uførevedtak for sak $sakId"
        }
    }

    private fun utledVedtakResultat(
        behandlingId: BehandlingId,
        virkningsdato: LocalDate,
    ): UføreSøknadVedtakResultat {
        val uføregrad = uføreRepository.hentHvisEksisterer(behandlingId)
            ?.vurderinger
            .orEmpty()
            .tilTidslinje()
            .segment(virkningsdato)
            ?.verdi
            ?.prosentverdi()

        return when (uføregrad) {
            100 -> UføreSøknadVedtakResultat.JA_INNVILGET_FULL
            null -> UføreSøknadVedtakResultat.JA_INNVILGET_GRADERT
            else -> UføreSøknadVedtakResultat.JA_INNVILGET_GRADERT
        }
    }

    override fun nårVurderingErRelevant(kontekst: FlytKontekstMedPerioder): Tidslinje<Boolean> {
        val utfall = tidligereVurderinger.behandlingsutfall(kontekst, type())
        val sykdomsvurderinger =
            sykdomRepository.hentHvisEksisterer(kontekst.behandlingId)?.somSykdomsvurderingstidslinje().orEmpty()

        return Tidslinje.map2(
            utfall,
            sykdomsvurderinger
        ) { _, utfall, sykdomsvurering ->
            when (utfall) {
                TidligereVurderinger.IkkeBehandlingsgrunnlag, TidligereVurderinger.UunngåeligAvslag -> false
                is TidligereVurderinger.PotensieltOppfylt -> {
                    utfall.rettighetstype == null && sykdomsvurering?.erOppfyltForOrdinærEllerYrkesskadeSettBortIfraÅrsakssammenheng() == true
                }

                else -> false
            }
        }
    }

    /**
     * 1. Det må finnes en vurdering for alle relevante perioder
     *      Selv om man har samordning i starten av perioden så skal ikke 8-mnd perioden endres - skal derfor ha en vurdering i alle perioder det kan være en vurdering
     * 2. Ingen vurderinger med oppfylt 11-18 utenfor perioden der 11-5 er oppfylt og 11-6 ikke er oppfylt
     *      Kan innvilge 11-18 før kravdato
     */
    private fun perioderSomIkkeErTilstrekkeligVurdert(kontekst: FlytKontekstMedPerioder): Set<Periode> {
        val overgangUføreTidslinje = overgangUføreRepository.hentHvisEksisterer(kontekst.behandlingId)
            ?.somOvergangUforevurderingstidslinje().orEmpty()
        val sykdomstidslinje =
            sykdomRepository.hentHvisEksisterer(kontekst.behandlingId)?.somSykdomsvurderingstidslinje().orEmpty()
        val bistandstidslinje =
            bistandRepository.hentHvisEksisterer(kontekst.behandlingId)?.somBistandsvurderingstidslinje().orEmpty()

        val nårVurderingErKonsistent = nårVurderingErKonsistentMedSykdomOgBistand(
            overgangUføreTidslinje, sykdomstidslinje, bistandstidslinje, kontekst.rettighetsperiode.fom
        )

        val nårPåkrevdVurderingMangler =
            nårVurderingErRelevant(kontekst).leftJoin(overgangUføreTidslinje) { erRelevant, overgangUføreVurdering ->
                erRelevant && overgangUføreVurdering == null
            }

        return Tidslinje.map2(nårPåkrevdVurderingMangler, nårVurderingErKonsistent) { vurderingMangler, erKonsistent ->
            vurderingMangler == true || erKonsistent == false
        }.komprimer().filter { erUtilstrekkelig -> erUtilstrekkelig.verdi }.perioder().toSet()
    }

    override val stegType = type()

    companion object : FlytSteg {
        override fun konstruer(
            repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider
        ): BehandlingSteg {
            return OvergangUføreSteg(repositoryProvider, gatewayProvider)
        }

        override fun type(): StegType {
            return StegType.OVERGANG_UFORE
        }
    }
}
