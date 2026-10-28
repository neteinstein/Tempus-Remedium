package org.mariacf.tempusremedium.ui

import org.mariacf.tempusremedium.ui.sheet.SheetPage

/** Files the user saves and opens: the export and import of medications in Definições. */
interface FileTransfer {
    /** Offers [bytes] to save as [name] (a save dialog or a download). */
    suspend fun save(
        name: String,
        type: String,
        bytes: ByteArray,
    )

    /** The text of a file the user picks, or null when they cancel. */
    suspend fun open(): String?

    /** A PDF of [pages], each drawn in points on an A4 page (see SHEET_WIDTH). */
    suspend fun pdf(pages: List<SheetPage>): ByteArray
}
