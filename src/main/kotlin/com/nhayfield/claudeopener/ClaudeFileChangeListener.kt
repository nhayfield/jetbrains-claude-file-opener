package com.nhayfield.claudeopener

import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileContentChangeEvent
import com.intellij.openapi.vfs.newvfs.events.VFileCreateEvent
import com.intellij.openapi.vfs.newvfs.events.VFileEvent

private const val BULK_CHANGE_THRESHOLD = 5

private val EXCLUDED_PATH_SEGMENTS = setOf(
    "/.git/", "/vendor/", "/node_modules/", "/build/", "/target/",
    "/.idea/", "/__pycache__/", "/dist/", "/coverage/", "/out/"
)

private val SOURCE_EXTENSIONS = setOf(
    "php", "kt", "kts", "java", "js", "ts", "tsx", "jsx",
    "py", "rb", "go", "rs", "xml", "yaml", "yml", "json",
    "html", "css", "scss", "sh", "bash", "md", "gradle",
    "toml", "tf", "sql", "proto",
    // C/C++
    "c", "cpp", "cc", "cxx", "h", "hpp",
    // C#, Swift, Dart
    "cs", "swift", "dart",
    // Frontend frameworks
    "vue", "svelte",
    // CSS preprocessors
    "less", "sass",
    // JVM/Groovy/Scala
    "groovy", "scala",
    // GraphQL
    "graphql", "gql",
    // Config/infrastructure
    "hcl", "properties",
    // Scripting
    "lua", "pl", "pm",
    // Functional/niche
    "ex", "exs", "hs", "jl", "r",
    // Objective-C
    "m", "mm"
)

class ClaudeFileChangeListener : BulkFileListener {
    override fun after(events: List<VFileEvent>) {
        val filesToOpen = events
            .filter { !it.isFromSave }
            .mapNotNull { event ->
                when (event) {
                    is VFileContentChangeEvent -> event.file
                    is VFileCreateEvent -> event.file ?: event.parent.findChild(event.childName)
                    else -> null
                }
            }
            .filter { !it.isDirectory }
            .filter { EXCLUDED_PATH_SEGMENTS.none { seg -> it.path.contains(seg) } }
            .filter { it.extension in SOURCE_EXTENSIONS }

        if (filesToOpen.isEmpty()) return

        ApplicationManager.getApplication().invokeLater {
            val openProjects = ProjectManager.getInstance().openProjects
                .filter { !it.isDisposed }

            if (filesToOpen.size > BULK_CHANGE_THRESHOLD) {
                for (project in openProjects) {
                    val projectFiles = filesToOpen.filter { it.path.startsWith(project.basePath ?: return@filter false) }
                    if (projectFiles.isEmpty()) continue
                    promptBulkOpen(project, projectFiles)
                }
            } else {
                for (vFile in filesToOpen) {
                    for (project in openProjects) {
                        val basePath = project.basePath ?: continue
                        if (vFile.path.startsWith(basePath)) {
                            openAndNotify(project, vFile)
                        }
                    }
                }
            }
        }
    }

    private fun openAndNotify(project: Project, vFile: VirtualFile) {
        FileEditorManager.getInstance(project).openFile(vFile, false)
        NotificationGroupManager.getInstance()
            .getNotificationGroup("Claude File Opener")
            .createNotification("Opened: ${vFile.name}", NotificationType.INFORMATION)
            .notify(project)
    }

    private fun promptBulkOpen(project: Project, files: List<VirtualFile>) {
        val notification = NotificationGroupManager.getInstance()
            .getNotificationGroup("Claude File Opener")
            .createNotification(
                "${files.size} files changed — open all?",
                NotificationType.INFORMATION
            )
        notification.addAction(NotificationAction.createSimple("Open All") {
            notification.expire()
            val fem = FileEditorManager.getInstance(project)
            files.forEach { fem.openFile(it, false) }
        })
        notification.notify(project)
    }
}
