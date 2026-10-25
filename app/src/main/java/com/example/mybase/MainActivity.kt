package com.example.mybase

import android.os.Bundle
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.mybase.core.navigation.Destination
import com.example.mybase.core.navigation.MyBaseBottomNavigation
import com.example.mybase.core.navigation.MyBaseNavigationRail
import com.example.mybase.feature.focus.StopwatchDialog
import com.example.mybase.feature.focus.StopwatchViewModel
import com.example.mybase.feature.focus.StopwatchViewModelFactory
import com.example.mybase.feature.focus.PreferenceStopwatchSessionStorage
import com.example.mybase.data.local.SharedPreferencesStringStore
import com.example.mybase.feature.home.CaptureDialog
import com.example.mybase.feature.home.HomeHeader
import com.example.mybase.feature.home.HomeScreen
import com.example.mybase.feature.home.DailyDashboardMapper
import com.example.mybase.feature.home.HomeViewModel
import com.example.mybase.feature.home.HomeViewModelFactory
import com.example.mybase.feature.home.SearchDialog
import com.example.mybase.feature.knowledge.KnowledgeScreen
import com.example.mybase.feature.knowledge.KnowledgeViewModel
import com.example.mybase.feature.knowledge.KnowledgeViewModelFactory
import com.example.mybase.feature.library.LibraryScreen
import com.example.mybase.feature.library.LibraryViewModel
import com.example.mybase.feature.library.LibraryViewModelFactory
import com.example.mybase.feature.library.PdfReaderScreen
import com.example.mybase.feature.library.PdfReaderViewModel
import com.example.mybase.feature.library.PdfReaderViewModelFactory
import com.example.mybase.feature.library.TextDocumentScreen
import com.example.mybase.feature.library.TextDocumentViewModel
import com.example.mybase.feature.library.TextDocumentViewModelFactory
import com.example.mybase.feature.library.previewKind
import com.example.mybase.feature.library.DocumentPreviewKind
import com.example.mybase.data.document.AndroidDocumentGateway
import com.example.mybase.data.document.AndroidPdfPageSource
import com.example.mybase.data.document.PdfThumbnailStore
import com.example.mybase.data.document.AndroidDocumentTextSource
import com.example.mybase.data.document.DocumentLocation
import java.time.LocalDate
import com.example.mybase.feature.personal.PersonalScreen
import com.example.mybase.feature.planner.PlannerScreen
import com.example.mybase.feature.planner.PlannerViewModel
import com.example.mybase.feature.planner.PlannerViewModelFactory
import com.example.mybase.feature.projects.ProjectsScreen
import com.example.mybase.ui.theme.MyBaseColors
import com.example.mybase.ui.theme.MyBaseTheme
import com.example.mybase.app.AppViewModel
import com.example.mybase.app.MyBaseApplication

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val myBaseApplication = application as MyBaseApplication
        val repository = myBaseApplication.entryRepository
        val documentAccess = AndroidDocumentGateway(contentResolver)
        val pdfPageSource = AndroidPdfPageSource(contentResolver)
        val thumbnailStore = PdfThumbnailStore(pdfPageSource, documentAccess)
        val homeViewModel =
            ViewModelProvider(this, HomeViewModelFactory(repository))[HomeViewModel::class.java]
        val plannerViewModel = ViewModelProvider(
            this,
            PlannerViewModelFactory(repository)
        )[PlannerViewModel::class.java]
        val knowledgeViewModel = ViewModelProvider(
            this,
            KnowledgeViewModelFactory(repository)
        )[KnowledgeViewModel::class.java]
        val libraryViewModel = ViewModelProvider(
            this,
            LibraryViewModelFactory(
                myBaseApplication.libraryResourceRepository,
                myBaseApplication.documentReadingStateRepository,
                myBaseApplication.pdfHighlightRepository,
                documentAccess
            )
        )[LibraryViewModel::class.java]
        val pdfReaderViewModel = ViewModelProvider(
            this,
            PdfReaderViewModelFactory(
                pdfPageSource,
                myBaseApplication.documentReadingStateRepository,
                myBaseApplication.pdfHighlightRepository,
                myBaseApplication.pdfTextHighlightRepository
            )
        )[PdfReaderViewModel::class.java]
        val textDocumentViewModel = ViewModelProvider(
            this,
            TextDocumentViewModelFactory(AndroidDocumentTextSource(contentResolver))
        )[TextDocumentViewModel::class.java]
        val appViewModel = ViewModelProvider(this)[AppViewModel::class.java]
        val timerPreferences = getSharedPreferences("mybase_local", MODE_PRIVATE)
        val stopwatchStorage = PreferenceStopwatchSessionStorage(SharedPreferencesStringStore(timerPreferences))
        val stopwatchViewModel = ViewModelProvider(
            this,
            StopwatchViewModelFactory(stopwatchStorage) {
                Settings.Global.getInt(contentResolver, Settings.Global.BOOT_COUNT, -1).toLong()
            }
        )[StopwatchViewModel::class.java]

        setContent {
            MyBaseTheme {
                MyBaseApp(
                    appViewModel,
                    homeViewModel,
                    plannerViewModel,
                    knowledgeViewModel,
                    libraryViewModel,
                    thumbnailStore,
                    pdfReaderViewModel,
                    textDocumentViewModel,
                    documentAccess,
                    stopwatchViewModel,
                    myBaseApplication.documentReadingStateRepository,
                    ::openLibraryLink,
                    ::openExternalDocument
                )
            }
        }
    }
}

private fun MainActivity.openLibraryLink(value: String) {
    if (!LibraryViewModel.isValidWebUrl(value)) {
        Toast.makeText(this, "This link is not a valid web address.", Toast.LENGTH_SHORT).show()
        return
    }
    try {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(value.trim())))
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(this, "No app can open this link.", Toast.LENGTH_SHORT).show()
    } catch (_: IllegalArgumentException) {
        Toast.makeText(this, "This link could not be opened.", Toast.LENGTH_SHORT).show()
    } catch (_: SecurityException) {
        Toast.makeText(this, "This link could not be opened.", Toast.LENGTH_SHORT).show()
    }
}

private fun MainActivity.openExternalDocument(value: String) {
    try {
        val uri = Uri.parse(value)
        val mimeType = contentResolver.getType(uri) ?: if (value.endsWith(
                ".pdf",
                true
            )
        ) "application/pdf" else null
        val intent = Intent(Intent.ACTION_VIEW).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        if (mimeType == null) intent.data = uri else intent.setDataAndType(uri, mimeType)
        startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(this, "No app can open this document.", Toast.LENGTH_SHORT).show()
    } catch (_: Exception) {
        Toast.makeText(this, "This document could not be opened.", Toast.LENGTH_SHORT).show()
    }
}

@Composable
private fun MyBaseApp(
    appViewModel: AppViewModel,
    homeViewModel: HomeViewModel,
    plannerViewModel: PlannerViewModel,
    knowledgeViewModel: KnowledgeViewModel,
    libraryViewModel: LibraryViewModel,
    thumbnailStore: PdfThumbnailStore,
    pdfReaderViewModel: PdfReaderViewModel,
    textDocumentViewModel: TextDocumentViewModel,
    documentAccess: AndroidDocumentGateway,
    stopwatchViewModel: StopwatchViewModel,
    readingStateRepository: com.example.mybase.data.repository.DocumentReadingStateRepository,
    onOpenLibraryLink: (String) -> Unit,
    onOpenExternalDocument: (String) -> Unit
) {
    val appState by appViewModel.uiState.collectAsStateWithLifecycle()
    val state by homeViewModel.uiState.collectAsStateWithLifecycle()
    val stopwatchState by stopwatchViewModel.uiState.collectAsStateWithLifecycle()
    val readingStates by readingStateRepository.states.collectAsStateWithLifecycle()
    val knowledgeState by knowledgeViewModel.uiState.collectAsStateWithLifecycle()
    val libraryState by libraryViewModel.uiState.collectAsStateWithLifecycle()
    val readerState by pdfReaderViewModel.uiState.collectAsStateWithLifecycle()
    val textReaderState by textDocumentViewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val filePicker =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
            uris.forEach { uri ->
                val retained = documentAccess.retainFilePermission(uri)
                val info = documentAccess.queryLocation(uri.toString()) ?: DocumentLocation(
                    uri = uri.normalizeScheme().toString(),
                    name = uri.lastPathSegment?.substringAfterLast('/')?.ifBlank { null }
                        ?: "Untitled document",
                    mimeType = context.contentResolver.getType(uri).orEmpty(),
                    isDirectory = false
                )
                if (info.name == "Untitled document" && !documentAccess.isReadable(info.uri)) Toast.makeText(
                    context,
                    "This file could not be accessed.",
                    Toast.LENGTH_SHORT
                ).show()
                else {
                    val added =
                        libraryViewModel.addDocumentReference(info.uri, info.name, info.mimeType)
                    if (!added) Toast.makeText(
                        context,
                        "This document is already in your Library.",
                        Toast.LENGTH_SHORT
                    ).show()
                    if (!retained) Toast.makeText(
                        context,
                        "Saved this file reference, but the provider may not keep access after restart.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    val folderPicker =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
            if (uri != null) {
                val retained = documentAccess.retainTreePermission(uri)
                libraryViewModel.folderPermissionGranted(uri.toString())
                if (!retained) Toast.makeText(
                    context,
                    "Folder access may not persist after restart.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .background(MyBaseColors.Cream)
    ) {
        val wide = maxWidth >= 760.dp
        Row(Modifier.fillMaxSize()) {
            if (wide) MyBaseNavigationRail(appState.selectedDestination, appViewModel::navigateTo)
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                HomeHeader(
                    onSearch = homeViewModel::showSearch,
                    onSettings = { appViewModel.navigateTo(Destination.Personal) }
                )
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    if (readerState.documentId.isNotBlank()) {
                        PdfReaderScreen(
                            state = readerState,
                            onBack = pdfReaderViewModel::closeReader,
                            onPrevious = pdfReaderViewModel::previousPage,
                            onNext = pdfReaderViewModel::nextPage,
                            onBookmark = pdfReaderViewModel::addBookmark,
                            onShowBookmarks = pdfReaderViewModel::showBookmarks,
                            onJumpTo = pdfReaderViewModel::goToPage,
                            onRemoveBookmark = pdfReaderViewModel::removeBookmark,
                            onAddHighlight = pdfReaderViewModel::addHighlight,
                            onRemoveHighlight = pdfReaderViewModel::removeHighlight,
                            onShowHighlights = pdfReaderViewModel::showHighlights,
                            onOpenExternal = onOpenExternalDocument,
                            onSelectText = pdfReaderViewModel::selectText,
                            onSaveSelectedText = pdfReaderViewModel::saveSelectedText,
                            onClearTextSelection = pdfReaderViewModel::clearTextSelection,
                            onRemoveTextHighlight = pdfReaderViewModel::removeTextHighlight,
                            onNavigateInternal = pdfReaderViewModel::resolveInternalDestination,
                            onEnsurePage = pdfReaderViewModel::ensurePageRendered,
                            onEnsureThumbnail = pdfReaderViewModel::ensureThumbnailRendered,
                            onVisiblePage = pdfReaderViewModel::onViewportPageChanged,
                            onToggleThumbnails = pdfReaderViewModel::toggleThumbnails
                        )
                    } else if (textReaderState.documentId.isNotBlank()) {
                        TextDocumentScreen(
                            state = textReaderState,
                            onBack = textDocumentViewModel::close,
                            onRaw = textDocumentViewModel::showRaw,
                            onOpenLink = onOpenLibraryLink,
                            onOpenExternal = onOpenExternalDocument
                        )
                    } else when (appState.selectedDestination) {
                        Destination.Home -> HomeScreen(
                            dashboard = DailyDashboardMapper.map(
                                entries = state.entries,
                                resources = libraryState.resources,
                                readingStates = readingStates,
                                today = LocalDate.now()
                            ),
                            stopwatch = stopwatchState,
                            onCapture = homeViewModel::showCapture,
                            onCreateTask = { plannerViewModel.selectToday(); plannerViewModel.showCreateTask(); appViewModel.navigateTo(Destination.Planner) },
                            onCreateNote = { knowledgeViewModel.createNote(); appViewModel.navigateTo(Destination.Knowledge) },
                            onAddResource = { libraryViewModel.createResource(); appViewModel.navigateTo(Destination.Library) },
                            onStartFocus = { stopwatchViewModel.start(); appViewModel.showStopwatch() },
                            onPauseFocus = stopwatchViewModel::pause,
                            onStopwatch = appViewModel::showStopwatch,
                            onToggleTask = homeViewModel::toggleTask,
                            onOpenTask = { task -> plannerViewModel.editTask(task.id); appViewModel.navigateTo(Destination.Planner) },
                            onOpenNote = { note -> knowledgeViewModel.editNote(note); appViewModel.navigateTo(Destination.Knowledge) },
                            onOpenResource = { resource ->
                                when (previewKind(resource)) {
                                    DocumentPreviewKind.Pdf -> pdfReaderViewModel.open(resource.id, resource.reference)
                                    DocumentPreviewKind.PlainText, DocumentPreviewKind.Markdown -> textDocumentViewModel.open(resource)
                                    DocumentPreviewKind.External -> onOpenExternalDocument(resource.reference)
                                }
                            },
                            onOpenKnowledge = { appViewModel.navigateTo(Destination.Knowledge) },
                            onOpenLibrary = { appViewModel.navigateTo(Destination.Library) }
                        )

                        Destination.Planner -> PlannerScreen(plannerViewModel)
                        Destination.Knowledge -> KnowledgeScreen(
                            state = knowledgeState,
                            onSearch = knowledgeViewModel::setSearchQuery,
                            onCategory = knowledgeViewModel::selectCategory,
                            onTag = knowledgeViewModel::selectTag,
                            onCreate = knowledgeViewModel::createNote,
                            onEdit = knowledgeViewModel::editNote,
                            onRequestDelete = knowledgeViewModel::requestDelete,
                            onDismissEditor = knowledgeViewModel::dismissEditor,
                            onKeepEditing = knowledgeViewModel::keepEditing,
                            onDiscard = knowledgeViewModel::discardChanges,
                            onTitle = knowledgeViewModel::updateTitle,
                            onBody = knowledgeViewModel::updateBody,
                            onCategories = knowledgeViewModel::updateCategories,
                            onTags = knowledgeViewModel::updateTags,
                            onSave = knowledgeViewModel::saveNote,
                            onCancelDelete = knowledgeViewModel::cancelDelete,
                            onConfirmDelete = knowledgeViewModel::confirmDelete
                        )

                        Destination.Projects -> ProjectsScreen()
                        Destination.Library -> LibraryScreen(
                            state = libraryState,
                            thumbnailStore = thumbnailStore,
                            onSearch = libraryViewModel::setSearchQuery,
                            onType = libraryViewModel::selectType,
                            onCategory = libraryViewModel::selectCategory,
                            onTag = libraryViewModel::selectTag,
                            onCreate = libraryViewModel::createResource,
                            onInfo = libraryViewModel::showInfo,
                            onDismissInfo = libraryViewModel::dismissInfo,
                            onEdit = libraryViewModel::editResource,
                            onRequestDelete = libraryViewModel::requestDelete,
                            onOpenLink = onOpenLibraryLink,
                            onOpenExternal = onOpenExternalDocument,
                            onPreview = { resource ->
                                when (previewKind(resource)) {
                                    DocumentPreviewKind.Pdf -> pdfReaderViewModel.open(
                                        resource.id,
                                        resource.reference
                                    )

                                    DocumentPreviewKind.PlainText, DocumentPreviewKind.Markdown -> textDocumentViewModel.open(
                                        resource
                                    )

                                    DocumentPreviewKind.External -> onOpenExternalDocument(resource.reference)
                                }
                            },
                            onAddFile = { filePicker.launch(arrayOf("application/pdf", "*/*")) },
                            onBrowseFiles = libraryViewModel::showFolderBrowser,
                            onChooseFolder = { folderPicker.launch(null) },
                            onCloseBrowser = libraryViewModel::closeFolderBrowser,
                            onBrowseFolder = libraryViewModel::browseFolder,
                            onEnterFolder = libraryViewModel::enterFolder,
                            onBackFolder = libraryViewModel::backFolder,
                            onAddFolderItem = libraryViewModel::addFolderItem,
                            onDismissEditor = libraryViewModel::requestDismissEditor,
                            onKeepEditing = libraryViewModel::keepEditing,
                            onDiscard = libraryViewModel::discardChanges,
                            onTitle = libraryViewModel::updateTitle,
                            onResourceType = libraryViewModel::updateType,
                            onReference = libraryViewModel::updateReference,
                            onDescription = libraryViewModel::updateDescription,
                            onCategoryDraft = libraryViewModel::updateCategory,
                            onTagsDraft = libraryViewModel::updateTags,
                            onSave = libraryViewModel::saveResource,
                            onCancelDelete = libraryViewModel::cancelDelete,
                            onConfirmDelete = libraryViewModel::confirmDelete,
                            onClearFilters = {
                                libraryViewModel.setSearchQuery(""); libraryViewModel.selectType(
                                null
                            ); libraryViewModel.selectCategory(null); libraryViewModel.selectTag(
                                null
                            )
                            }
                        )

                        Destination.Personal -> PersonalScreen()
                    }
                }
                if (!wide) MyBaseBottomNavigation(
                    appState.selectedDestination,
                    appViewModel::navigateTo
                )
            }
        }

        if (state.isCaptureVisible) {
            CaptureDialog(
                state = state,
                onTitleChange = homeViewModel::updateCaptureTitle,
                onKindChange = homeViewModel::updateCaptureKind,
                onDismiss = homeViewModel::hideCapture,
                onSave = homeViewModel::capture
            )
        }
        if (state.isSearchVisible) {
            SearchDialog(
                state = state,
                onQueryChange = homeViewModel::updateSearchQuery,
                onDismiss = homeViewModel::hideSearch,
                onDone = {
                    knowledgeViewModel.setSearchQuery(state.searchQuery)
                    homeViewModel.hideSearch()
                    appViewModel.navigateTo(Destination.Knowledge)
                }
            )
        }
        if (appState.isStopwatchVisible) {
            StopwatchDialog(
                state = stopwatchState,
                onStart = stopwatchViewModel::start,
                onPause = stopwatchViewModel::pause,
                onReset = stopwatchViewModel::reset,
                onDismiss = appViewModel::hideStopwatch
            )
        }
    }
}
