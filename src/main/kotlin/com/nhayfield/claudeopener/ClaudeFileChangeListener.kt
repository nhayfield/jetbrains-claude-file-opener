package com.nhayfield.claudeopener

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileContentChangeEvent
import com.intellij.openapi.vfs.newvfs.events.VFileCreateEvent
import com.intellij.openapi.vfs.newvfs.events.VFileEvent

private val EXCLUDED_PATH_SEGMENTS = setOf(
    "/.git/", "/vendor/", "/node_modules/", "/build/", "/target/",
    "/.idea/", "/__pycache__/", "/dist/", "/coverage/"
)

private val SOURCE_EXTENSIONS = setOf(
    "php", "kt", "kts", "java", "js", "ts", "tsx", "jsx",
    "py", "rb", "go", "rs", "xml", "yaml", "yml", "json",
    "html", "css", "scss", "sh", "bash", "md", "gradle"
)

class ClaudeFileChangeListener : BulkFileListener {
    override fun after(events: List<VFileEvent>) {
        val filesToOpen = events
            .filter { it.isFromRefresh }
            .filter { it is VFileContentChangeEvent || it is VFileCreateEvent }
            .mapNotNull { it.file }
            .filter { !it.isDirectory }
            .filter { EXCLUDED_PATH_SEGMENTS.none { seg -> it.path.contains(seg) } }
            .filter { it.extension in SOURCE_EXTENSIONS }

        if (filesToOpen.isEmpty()) return

        ApplicationManager.getApplication().invokeLater {
            val openProjects = ProjectManager.getInstance().openProjects
                .filter { !it.isDisposed }

            for (vFile in filesToOpen) {
                for (project in openProjects) {
                    val basePath = project.basePath ?: continue
                    if (vFile.path.startsWith(basePath)) {
                        FileEditorManager.getInstance(project).openFile(vFile, false)
                        NotificationGroupManager.getInstance()
                            .getNotificationGroup("Claude File Opener")
                            .createNotification("Opened: ${vFile.name}", NotificationType.INFORMATION)
                            .notify(project)
                    }
                }
            }
        }
    }
}
