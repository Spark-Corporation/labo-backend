package org.sparkcorporation.lims.result.web;

/**
 * Per-dossier visual marker per CDC §X lines 244-247.
 *
 * <ul>
 *   <li>{@link #GREEN} — "Couleur verte autour du résultat pour signifier qu'aucune aberration
 *       n'est dans le dossier" — all results entered AND no out-of-range line.
 *   <li>{@link #RED} — "Contour rouge pour signifier qu'il y a une donnée hors-norme dans le
 *       dossier" — all results entered AND at least one out-of-range line.
 *   <li>{@link #PEN} — "Stylo sur le dossier pour signifier qu'il est partiellement rempli" — at
 *       least one active result still in DRAFT state.
 * </ul>
 */
public enum DossierMarker {
    GREEN,
    RED,
    PEN
}
