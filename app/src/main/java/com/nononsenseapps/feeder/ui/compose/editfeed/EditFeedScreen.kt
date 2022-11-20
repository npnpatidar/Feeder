package com.nononsenseapps.feeder.ui.compose.editfeed

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester.Companion.createRefs
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.shouldShowRationale
import com.nononsenseapps.feeder.R
import com.nononsenseapps.feeder.archmodel.PREF_VAL_OPEN_WITH_BROWSER
import com.nononsenseapps.feeder.archmodel.PREF_VAL_OPEN_WITH_CUSTOM_TAB
import com.nononsenseapps.feeder.archmodel.PREF_VAL_OPEN_WITH_READER
import com.nononsenseapps.feeder.ui.compose.components.AutoCompleteFoo
import com.nononsenseapps.feeder.ui.compose.components.OkCancelWithContent
import com.nononsenseapps.feeder.ui.compose.feed.ExplainPermissionDialog
import com.nononsenseapps.feeder.ui.compose.modifiers.interceptKey
import com.nononsenseapps.feeder.ui.compose.settings.GroupTitle
import com.nononsenseapps.feeder.ui.compose.settings.RadioButtonSetting
import com.nononsenseapps.feeder.ui.compose.settings.SwitchSetting
import com.nononsenseapps.feeder.ui.compose.theme.FeederTheme
import com.nononsenseapps.feeder.ui.compose.theme.LocalDimens
import com.nononsenseapps.feeder.ui.compose.utils.ImmutableHolder
import com.nononsenseapps.feeder.ui.compose.utils.LocalWindowSize
import com.nononsenseapps.feeder.ui.compose.utils.ScreenType
import com.nononsenseapps.feeder.ui.compose.utils.getScreenType
import com.nononsenseapps.feeder.ui.compose.utils.rememberApiPermissionState

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CreateFeedScreen(
    onNavigateUp: () -> Unit,
    createFeedScreenViewModel: CreateFeedScreenViewModel,
    onSaved: (Long) -> Unit,
) {
    val viewState by createFeedScreenViewModel.viewState.collectAsState()

    val notificationsPermissionState = rememberApiPermissionState(
        permission = "android.permission.POST_NOTIFICATIONS",
        minimumApiLevel = 33,
    ) { value ->
        createFeedScreenViewModel.setNotify(value)
    }

    val shouldShowExplanationForPermission by remember {
        derivedStateOf {
            notificationsPermissionState.status.shouldShowRationale
        }
    }

    var permissionDismissed by rememberSaveable {
        mutableStateOf(true)
    }

    val windowSize = LocalWindowSize()

    val screenType by remember(windowSize) {
        derivedStateOf {
            getScreenType(windowSize)
        }
    }

    EditFeedScreen(
        screenType = screenType,
        onNavigateUp = onNavigateUp,
        viewState = viewState,
        setUrl = createFeedScreenViewModel::setUrl,
        setTitle = createFeedScreenViewModel::setTitle,
        setTag = createFeedScreenViewModel::setTag,
        setFullTextByDefault = createFeedScreenViewModel::setFullTextByDefault,
        setNotify = createFeedScreenViewModel::setNotify,
        setArticleOpener = createFeedScreenViewModel::setArticleOpener,
        setAlternateId = createFeedScreenViewModel::setAlternateId,
        showPermissionExplanation = shouldShowExplanationForPermission && !permissionDismissed,
        onPermissionExplanationDismissed = {
            permissionDismissed = true
        },
        onPermissionExplanationOk = {
            notificationsPermissionState.launchPermissionRequest()
        },
        onOk = {
            val feedId = createFeedScreenViewModel.saveAndRequestSync()
            onSaved(feedId)
        },
        onCancel = {
            onNavigateUp()
        }
    )
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun EditFeedScreen(
    onNavigateUp: () -> Unit,
    onOk: (Long) -> Unit,
    editFeedScreenViewModel: EditFeedScreenViewModel,
) {
    val viewState by editFeedScreenViewModel.viewState.collectAsState()

    val notificationsPermissionState = rememberApiPermissionState(
        permission = "android.permission.POST_NOTIFICATIONS",
        minimumApiLevel = 33,
    ) { value ->
        editFeedScreenViewModel.setNotify(value)
    }

    val shouldShowExplanationForPermission by remember {
        derivedStateOf {
            notificationsPermissionState.status.shouldShowRationale
        }
    }

    var permissionDismissed by rememberSaveable {
        mutableStateOf(true)
    }

    fun setNotify(value: Boolean) {
        if (!value) {
            editFeedScreenViewModel.setNotify(value)
        } else {
            when (notificationsPermissionState.status) {
                is PermissionStatus.Denied -> {
                    if (notificationsPermissionState.status.shouldShowRationale) {
                        // Dialog is shown inside EditFeedScreen with a button
                        permissionDismissed = false
                    } else {
                        notificationsPermissionState.launchPermissionRequest()
                    }
                }
                PermissionStatus.Granted -> editFeedScreenViewModel.setNotify(value)
            }
        }
    }

    val windowSize = LocalWindowSize()

    val screenType by remember(windowSize) {
        derivedStateOf {
            getScreenType(windowSize)
        }
    }

    EditFeedScreen(
        screenType = screenType,
        onNavigateUp = onNavigateUp,
        viewState = viewState,
        setUrl = editFeedScreenViewModel::setUrl,
        setTitle = editFeedScreenViewModel::setTitle,
        setTag = editFeedScreenViewModel::setTag,
        setFullTextByDefault = editFeedScreenViewModel::setFullTextByDefault,
        setNotify = ::setNotify,
        setArticleOpener = editFeedScreenViewModel::setArticleOpener,
        setAlternateId = editFeedScreenViewModel::setAlternateId,
        showPermissionExplanation = shouldShowExplanationForPermission && !permissionDismissed,
        onPermissionExplanationDismissed = {
            permissionDismissed = true
        },
        onPermissionExplanationOk = {
            notificationsPermissionState.launchPermissionRequest()
        },
        onOk = {
            editFeedScreenViewModel.saveInBackgroundAndRequestSync()
            onOk(editFeedScreenViewModel.feedId)
        },
        onCancel = {
            onNavigateUp()
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditFeedScreen(
    screenType: ScreenType,
    onNavigateUp: () -> Unit,
    viewState: EditFeedViewState,
    setUrl: (String) -> Unit,
    setTitle: (String) -> Unit,
    setTag: (String) -> Unit,
    setFullTextByDefault: (Boolean) -> Unit,
    setNotify: (Boolean) -> Unit,
    setArticleOpener: (String) -> Unit,
    setAlternateId: (Boolean) -> Unit,
    showPermissionExplanation: Boolean,
    onPermissionExplanationDismissed: () -> Unit,
    onPermissionExplanationOk: () -> Unit,
    onOk: () -> Unit,
    onCancel: () -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
        rememberTopAppBarState()
    )

    Scaffold(
        modifier = Modifier
            .nestedScroll(scrollBehavior.nestedScrollConnection)
            .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Horizontal)),
        contentWindowInsets = WindowInsets.statusBars,
        topBar = {
            TopAppBar(
                scrollBehavior = scrollBehavior,
                title = {
                    Text(
                        text = stringResource(id = R.string.edit_feed),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = stringResource(R.string.go_back),
                        )
                    }
                },
            )
        }
    ) { padding ->
        EditFeedView(
            screenType = screenType,
            viewState = viewState,
            setUrl = setUrl,
            setTitle = setTitle,
            setTag = setTag,
            setFullTextByDefault = setFullTextByDefault,
            setNotify = setNotify,
            setArticleOpener = setArticleOpener,
            setAlternateId = setAlternateId,
            onOk = onOk,
            onCancel = onCancel,
            modifier = Modifier.padding(padding)
        )

        if (showPermissionExplanation) {
            ExplainPermissionDialog(
                explanation = R.string.explanation_permission_notifications,
                onDismiss = onPermissionExplanationDismissed,
                onOk = onPermissionExplanationOk,
            )
        }
    }
}

@Composable
fun EditFeedView(
    screenType: ScreenType,
    viewState: EditFeedViewState,
    setUrl: (String) -> Unit,
    setTitle: (String) -> Unit,
    setTag: (String) -> Unit,
    setFullTextByDefault: (Boolean) -> Unit,
    setNotify: (Boolean) -> Unit,
    setArticleOpener: (String) -> Unit,
    setAlternateId: (Boolean) -> Unit,
    onOk: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier,
) {
    val dimens = LocalDimens.current

    OkCancelWithContent(
        onOk = {
            onOk()
        },
        onCancel = onCancel,
        okEnabled = viewState.isOkToSave,
        modifier = modifier
            .padding(horizontal = LocalDimens.current.margin)
    ) {
        if (screenType == ScreenType.DUAL) {
            Row(
                modifier = Modifier.width(dimens.maxContentWidth),
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f, fill = true)
                        .padding(horizontal = dimens.margin, vertical = 8.dp)
                ) {
                    leftContent(
                        viewState = viewState,
                        setUrl = setUrl,
                        setTitle = setTitle,
                        setTag = setTag
                    )
                }

                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f, fill = true)
                        .padding(horizontal = dimens.margin, vertical = 8.dp)
                ) {
                    rightContent(
                        viewState = viewState,
                        setFullTextByDefault = setFullTextByDefault,
                        setNotify = setNotify,
                        setArticleOpener = setArticleOpener,
                        setAlternateId = setAlternateId
                    )
                }
            }
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.width(dimens.maxContentWidth)
            ) {
                leftContent(
                    viewState = viewState,
                    setUrl = setUrl,
                    setTitle = setTitle,
                    setTag = setTag
                )

                Divider(
                    modifier = Modifier.fillMaxWidth()
                )

                rightContent(
                    viewState = viewState,
                    setFullTextByDefault = setFullTextByDefault,
                    setNotify = setNotify,
                    setArticleOpener = setArticleOpener,
                    setAlternateId = setAlternateId
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun ColumnScope.leftContent(
    viewState: EditFeedViewState,
    setUrl: (String) -> Unit,
    setTitle: (String) -> Unit,
    setTag: (String) -> Unit,
) {
    val filteredTags by remember(viewState.allTags, viewState.tag) {
        derivedStateOf {
            ImmutableHolder(
                viewState.allTags.filter { tag ->
                    tag.isNotBlank() && tag.startsWith(viewState.tag, ignoreCase = true)
                }
            )
        }
    }

    val (focusTitle, focusTag) = createRefs()
    val focusManager = LocalFocusManager.current

    var tagHasFocus by rememberSaveable { mutableStateOf(false) }

    TextField(
        value = viewState.url,
        onValueChange = setUrl,
        label = {
            Text(stringResource(id = R.string.url))
        },
        isError = viewState.isNotValidUrl,
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.None,
            autoCorrect = false,
            keyboardType = KeyboardType.Uri,
            imeAction = ImeAction.Next
        ),
        keyboardActions = KeyboardActions(
            onNext = {
                focusTitle.requestFocus()
            }
        ),
        singleLine = true,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .interceptKey(Key.Enter) {
                focusTitle.requestFocus()
            }
            .interceptKey(Key.Escape) {
                focusManager.clearFocus()
            }
    )
    AnimatedVisibility(visible = viewState.isNotValidUrl) {
        Text(
            textAlign = TextAlign.Center,
            text = stringResource(R.string.invalid_url),
            style = MaterialTheme.typography.labelMedium.copy(color = MaterialTheme.colorScheme.error),
        )
    }
    OutlinedTextField(
        value = viewState.title,
        onValueChange = setTitle,
        placeholder = {
            Text(viewState.defaultTitle)
        },
        label = {
            Text(stringResource(id = R.string.title))
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Words,
            autoCorrect = true,
            keyboardType = KeyboardType.Text,
            imeAction = ImeAction.Next
        ),
        keyboardActions = KeyboardActions(
            onNext = {
                focusTag.requestFocus()
            }
        ),
        modifier = Modifier
            .focusRequester(focusTitle)
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .interceptKey(Key.Enter) {
                focusTag.requestFocus()
            }
            .interceptKey(Key.Escape) {
                focusManager.clearFocus()
            }
    )

    AutoCompleteFoo(
        displaySuggestions = tagHasFocus,
        suggestions = filteredTags,
        onSuggestionClicked = { tag ->
            setTag(tag)
            focusManager.clearFocus()
        },
        suggestionContent = {
            Box(
                contentAlignment = Alignment.CenterStart,
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .height(48.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    text = it,
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    ) {
        OutlinedTextField(
            value = viewState.tag,
            onValueChange = setTag,
            label = {
                Text(stringResource(id = R.string.tag))
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                autoCorrect = true,
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    focusManager.clearFocus()
                }
            ),
            modifier = Modifier
                .focusRequester(focusTag)
                .onFocusChanged {
                    tagHasFocus = it.isFocused
                }
                .fillMaxWidth()
                .heightIn(min = 64.dp)
                .interceptKey(Key.Enter) {
                    focusManager.clearFocus()
                }
                .interceptKey(Key.Escape) {
                    focusManager.clearFocus()
                }
        )
    }
}

@Composable
fun ColumnScope.rightContent(
    viewState: EditFeedViewState,
    setFullTextByDefault: (Boolean) -> Unit,
    setNotify: (Boolean) -> Unit,
    setArticleOpener: (String) -> Unit,
    setAlternateId: (Boolean) -> Unit,
) {
    SwitchSetting(
        title = stringResource(id = R.string.fetch_full_articles_by_default),
        checked = viewState.fullTextByDefault,
        onCheckedChanged = setFullTextByDefault,
        icon = null
    )
    SwitchSetting(
        title = stringResource(id = R.string.notify_for_new_items),
        checked = viewState.notify,
        onCheckedChanged = setNotify,
        icon = null
    )
    SwitchSetting(
        title = stringResource(id = R.string.generate_extra_unique_ids),
        description = stringResource(id = R.string.only_enable_for_bad_id_feeds),
        checked = viewState.alternateId,
        onCheckedChanged = setAlternateId,
        icon = null
    )
    Divider(
        modifier = Modifier.fillMaxWidth()
    )
    GroupTitle(
        startingSpace = false,
        height = 48.dp
    ) {
        Text(stringResource(id = R.string.open_item_by_default_with))
    }
    RadioButtonSetting(
        title = stringResource(id = R.string.use_app_default),
        selected = viewState.isOpenItemWithAppDefault,
        minHeight = 48.dp,
        icon = null,
        onClick = {
            setArticleOpener("")
        }
    )
    RadioButtonSetting(
        title = stringResource(id = R.string.open_in_reader),
        selected = viewState.isOpenItemWithReader,
        minHeight = 48.dp,
        icon = null,
        onClick = {
            setArticleOpener(PREF_VAL_OPEN_WITH_READER)
        }
    )
    RadioButtonSetting(
        title = stringResource(id = R.string.open_in_custom_tab),
        selected = viewState.isOpenItemWithCustomTab,
        minHeight = 48.dp,
        icon = null,
        onClick = {
            setArticleOpener(PREF_VAL_OPEN_WITH_CUSTOM_TAB)
        }
    )
    RadioButtonSetting(
        title = stringResource(id = R.string.open_in_default_browser),
        selected = viewState.isOpenItemWithBrowser,
        minHeight = 48.dp,
        icon = null,
        onClick = {
            setArticleOpener(PREF_VAL_OPEN_WITH_BROWSER)
        }
    )
}

@Preview("Edit Feed Phone")
@Composable
fun PreviewEditFeedScreenPhone() {
    FeederTheme {
        EditFeedScreen(
            screenType = ScreenType.SINGLE,
            onNavigateUp = {},
            onOk = {},
            onCancel = {},
            viewState = EditFeedViewState(),
            setUrl = {},
            setTitle = {},
            setTag = {},
            setFullTextByDefault = {},
            setNotify = {},
            setArticleOpener = {},
            setAlternateId = {},
            showPermissionExplanation = true,
            onPermissionExplanationDismissed = {},
            onPermissionExplanationOk = {},
        )
    }
}

@Preview("Edit Feed Foldable", device = Devices.FOLDABLE)
@Preview("Edit Feed Tablet", device = Devices.PIXEL_C)
@Composable
fun PreviewEditFeedScreenLarge() {
    FeederTheme {
        EditFeedScreen(
            screenType = ScreenType.DUAL,
            onNavigateUp = {},
            onOk = {},
            onCancel = {},
            viewState = EditFeedViewState(),
            setUrl = {},
            setTitle = {},
            setTag = {},
            setFullTextByDefault = {},
            setNotify = {},
            setArticleOpener = {},
            setAlternateId = {},
            showPermissionExplanation = true,
            onPermissionExplanationDismissed = {},
            onPermissionExplanationOk = {},
        )
    }
}
