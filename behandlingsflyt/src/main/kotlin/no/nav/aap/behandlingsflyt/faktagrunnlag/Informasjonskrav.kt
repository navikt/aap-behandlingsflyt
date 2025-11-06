package no.nav.aap.behandlingsflyt.faktagrunnlag

import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekst
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder

interface InformasjonskravInput
interface InformasjonskravRegisterdata

object IngenInput : InformasjonskravInput
object IngenRegisterData : InformasjonskravRegisterdata

/**
 * Et _Informasjonskrav_ har ansvar for å hente inn nødvendig informasjon for et gitt [BehandlingSteg].
 */
interface Informasjonskrav<out InformasjonskravInput : no.nav.aap.behandlingsflyt.faktagrunnlag.InformasjonskravInput, out Registerdata : InformasjonskravRegisterdata> {
    val navn: InformasjonskravNavn

    enum class Endret {
        ENDRET,
        IKKE_ENDRET,
    }

    fun erRelevant(kontekst: FlytKontekstMedPerioder, steg: StegType, oppdatert: InformasjonskravOppdatert?): Boolean

    /**
     * Generer input som trengs for å hente ut data register. Denne metoden må _aldri_ kjøres i flere tråder om det gjøres
     * databasekall.
     *
     * Det skal heller ikke gjøres kall mot integrasjoner i implementasjonen av denne metoden.
     */
    fun klargjør(kontekst: FlytKontekstMedPerioder): InformasjonskravInput

    /**
     * Hent data fra register basert på [input]. Skal _ikke_ gjøre databasekall.
     *
     * Trygt så lenge man bruker samme instans av informasjonskravet til å kalle hentInput og hentData.
     */
    fun hentData(input: @UnsafeVariance InformasjonskravInput): Registerdata


    /**
     * Tar i mot registerdata, input, og sjekker mot hva som er persistert, og sammenligner endringer. Implementasjoner
     * av denne metoden skal _ikke_ gjøre kall mot integrasjoner.
     *
     * Om det ikke finnes noen endringer i persistert data, returneres [Endret.IKKE_ENDRET].
     */
    fun oppdater(input: @UnsafeVariance InformasjonskravInput, registerdata: @UnsafeVariance Registerdata, kontekst: FlytKontekstMedPerioder): Endret

    fun flettOpplysningerFraAtomærBehandling(kontekst: FlytKontekst) = Endret.IKKE_ENDRET
}