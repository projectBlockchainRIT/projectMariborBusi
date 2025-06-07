package utils

import java.io.File
import javax.swing.JFileChooser

fun chooseDirectory(title: String): File? {
    val fileChooser = JFileChooser()
    fileChooser.dialogTitle = title
    fileChooser.fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
    val result = fileChooser.showOpenDialog(null)
    return if (result == JFileChooser.APPROVE_OPTION) fileChooser.selectedFile else null
}
