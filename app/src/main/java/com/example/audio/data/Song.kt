package com.example.audio.data

/**
 * Modèle de données représentant une chanson
 * Contient toutes les informations nécessaires pour afficher et jouer une chanson
 */
data class Song(
    val id: Int,              // Identifiant unique de la chanson
    val resourceId: Int,      // ID de la ressource audio dans res/raw
    val title: String,        // Titre de la chanson
    val artist: String,       // Nom de l'artiste
    val duration: String      // Durée au format "mm:ss"
)
