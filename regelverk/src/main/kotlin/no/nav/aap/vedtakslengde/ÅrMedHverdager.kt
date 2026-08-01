package no.nav.aap.vedtakslengde

import no.nav.aap.underveis.Hverdager

/**
 * Antall mandag-fredager per år er bestemt til å være 261 + 261 + 262 for at kvoten skal bli riktig.
 * Se [Confluence](https://confluence.adeo.no/spaces/PAAP/pages/739025519/Kvoter+og+overganger+mellom+bestemmelser).
 */
enum class ÅrMedHverdager(val hverdagerIÅret: Hverdager){
    FØRSTE_ÅR(Hverdager(261)),
    ANDRE_ÅR(Hverdager(261)),
    TREDJE_ÅR(Hverdager(262)),
    ANNET(Hverdager(261))
}