package com.catovicajdin.expensetracker.data.statement

import android.content.Context
import android.net.Uri
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.encryption.InvalidPasswordException
import com.tom_roush.pdfbox.text.PDFTextStripper
import com.tom_roush.pdfbox.text.TextPosition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Pulls every drawn character, with its position, out of a PDF the user picked. All of it happens on
 * the device: the file is opened through the handle the document picker hands over, and nothing is
 * uploaded anywhere.
 *
 * Only the extraction lives here. What the characters mean is [StatementParser]'s problem, which
 * keeps the parsing rules free of any PDF library and testable on their own.
 */
object PdfGlyphReader {

    sealed interface Result {
        data class Success(val glyphs: List<Glyph>) : Result
        data object NeedsPassword : Result
        data class Failure(val detail: String) : Result
    }

    suspend fun read(context: Context, uri: Uri, password: String? = null): Result = withContext(Dispatchers.IO) {
        // Loads the font metrics the library needs; safe to call more than once.
        PDFBoxResourceLoader.init(context.applicationContext)
        try {
            val stream = context.contentResolver.openInputStream(uri)
                ?: return@withContext Result.Failure("That file could not be opened.")
            stream.use { input ->
                val document = if (password.isNullOrEmpty()) PDDocument.load(input) else PDDocument.load(input, password)
                document.use { pdf ->
                    val glyphs = mutableListOf<Glyph>()
                    val stripper = object : PDFTextStripper() {
                        override fun writeString(text: String, textPositions: List<TextPosition>) {
                            for (position in textPositions) {
                                glyphs.add(
                                    Glyph(
                                        text = position.unicode,
                                        x0 = position.xDirAdj,
                                        x1 = position.xDirAdj + position.widthDirAdj,
                                        top = position.yDirAdj,
                                        page = currentPageNo,
                                    ),
                                )
                            }
                        }
                    }
                    stripper.sortByPosition = true
                    stripper.getText(pdf)
                    Result.Success(glyphs)
                }
            }
        } catch (e: InvalidPasswordException) {
            Result.NeedsPassword
        } catch (e: Exception) {
            Result.Failure(e.message ?: "That file could not be read as a PDF.")
        }
    }
}
