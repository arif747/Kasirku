package com.example

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.PointOfSale
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.auth.authStateFlow
import com.example.data.local.LocalPosRepository
import com.example.data.repository.PosDataSource
import com.example.data.repository.PosRepository
import com.example.ui.components.ReceiptDialog
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.PosRegisterScreen
import com.example.ui.screens.ProductsScreen
import com.example.ui.screens.StoreSettingsScreen
import com.example.ui.screens.WholesaleScreen
import com.example.ui.theme.KasirKuTheme
import com.example.ui.viewmodel.KasirViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

private const val PREFS_NAME = "kasirku_prefs"
private const val KEY_LOCAL_MODE = "is_local_mode"
private const val LOCAL_USER_ID = "local_merchant"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KasirKuTheme {
                AppNavigation()
            }
        }
    }
}

private enum class KasirTab(
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val tag: String,
) {
    KASIR(R.string.nav_kasir, Icons.Filled.PointOfSale, Icons.Outlined.PointOfSale, "nav_tab_kasir"),
    GROSIR(R.string.nav_grosir, Icons.Filled.LocalShipping, Icons.Outlined.LocalShipping, "nav_tab_grosir"),
    PRODUK(R.string.nav_produk, Icons.Filled.Inventory2, Icons.Outlined.Inventory2, "nav_tab_produk"),
    RIWAYAT(R.string.nav_riwayat, Icons.Filled.History, Icons.Outlined.History, "nav_tab_riwayat"),
    TOKO(R.string.nav_toko, Icons.Filled.Storefront, Icons.Outlined.Storefront, "nav_tab_toko"),
}

@Composable
fun AppNavigation(auth: FirebaseAuth = Firebase.auth) {
    val context = LocalContext.current
    val prefs = remember(context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    var isLocalMode by remember {
        mutableStateOf(prefs.getBoolean(KEY_LOCAL_MODE, false))
    }

    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val user = currentUser

    when {
        user != null -> {
            key("google_${user.uid}") {
                KasirMainShell(
                    sessionKey = "google_${user.uid}",
                    currentUserId = user.uid,
                    isLocalMode = false,
                    onSwitchToGoogleAuth = {},
                )
            }
        }

        isLocalMode -> {
            key("local_guest") {
                KasirMainShell(
                    sessionKey = "local_guest",
                    currentUserId = LOCAL_USER_ID,
                    isLocalMode = true,
                    onSwitchToGoogleAuth = {
                        prefs.edit().putBoolean(KEY_LOCAL_MODE, false).apply()
                        isLocalMode = false
                    },
                )
            }
        }

        else -> {
            AuthScreen(
                onAuthSuccess = {
                    prefs.edit().putBoolean(KEY_LOCAL_MODE, false).apply()
                    isLocalMode = false
                },
                onContinueWithoutAccount = {
                    prefs.edit().putBoolean(KEY_LOCAL_MODE, true).apply()
                    isLocalMode = true
                },
            )
        }
    }
}

@Composable
private fun KasirMainShell(
    sessionKey: String,
    currentUserId: String,
    isLocalMode: Boolean,
    onSwitchToGoogleAuth: () -> Unit,
) {
    val viewModel: KasirViewModel = viewModel(
        key = sessionKey,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val dataSource: PosDataSource = if (isLocalMode) {
                    LocalPosRepository(app)
                } else {
                    val databaseId = app.getString(R.string.firestore_database_id)
                    val db = FirebaseFirestore.getInstance(databaseId)
                    PosRepository(db)
                }
                KasirViewModel(dataSource, currentUserId)
            }
        }
    )

    val productsState by viewModel.productsState.collectAsStateWithLifecycle()
    val transactionsState by viewModel.transactionsState.collectAsStateWithLifecycle()
    val storeProfile by viewModel.storeProfileState.collectAsStateWithLifecycle()
    val cartState by viewModel.cartState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val activeReceipt by viewModel.activeReceipt.collectAsStateWithLifecycle()
    val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()
    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()

    var currentTab by rememberSaveable { mutableStateOf(KasirTab.KASIR) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(statusMessage) {
        val msg = statusMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearMessage()
        }
    }

    BackHandler(enabled = currentTab != KasirTab.KASIR) {
        currentTab = KasirTab.KASIR
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavRail = maxWidth >= 600.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                if (!useNavRail) {
                    NavigationBar {
                        KasirTab.entries.forEach { tab ->
                            val selected = currentTab == tab
                            NavigationBarItem(
                                selected = selected,
                                onClick = { currentTab = tab },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = stringResource(tab.labelRes),
                                    )
                                },
                                label = {
                                    Text(
                                        text = stringResource(tab.labelRes),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                },
                                modifier = Modifier.testTag(tab.tag),
                            )
                        }
                    }
                }
            },
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                if (useNavRail) {
                    NavigationRail {
                        KasirTab.entries.forEach { tab ->
                            val selected = currentTab == tab
                            NavigationRailItem(
                                selected = selected,
                                onClick = { currentTab = tab },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = stringResource(tab.labelRes),
                                    )
                                },
                                label = { Text(stringResource(tab.labelRes)) },
                                modifier = Modifier.testTag(tab.tag),
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f)) {
                    when (currentTab) {
                        KasirTab.KASIR -> PosRegisterScreen(
                            productsState = productsState,
                            storeProfile = storeProfile,
                            cart = cartState,
                            searchQuery = searchQuery,
                            selectedCategory = selectedCategory,
                            isSaving = isSaving,
                            onSearchQueryChange = viewModel::updateSearchQuery,
                            onCategorySelect = viewModel::selectCategory,
                            onAddToCart = viewModel::addToCart,
                            onAddCustomItem = viewModel::addCustomItemToCart,
                            onIncrementCartItem = viewModel::incrementCartItem,
                            onDecrementCartItem = viewModel::decrementCartItem,
                            onRemoveFromCart = viewModel::removeFromCart,
                            onClearCart = viewModel::clearCart,
                            onDiscountChange = viewModel::updateDiscount,
                            onCustomerNameChange = viewModel::updateCustomerName,
                            onNoteChange = viewModel::updateNote,
                            onPaymentMethodChange = viewModel::updatePaymentMethod,
                            onCashReceivedChange = viewModel::updateCashReceivedInput,
                            onComputeTotals = viewModel::computeTotals,
                            onConfirmPay = { viewModel.completeCheckout() },
                            onSeedStarterProducts = viewModel::seedStarterProducts,
                            onNavigateToProducts = { currentTab = KasirTab.PRODUK },
                        )

                        KasirTab.GROSIR -> WholesaleScreen(
                            productsState = productsState,
                            storeProfile = storeProfile,
                            cart = cartState,
                            isSaving = isSaving,
                            onAddWholesale = viewModel::addWholesaleToCart,
                            onIncrementCartItem = viewModel::incrementCartItem,
                            onDecrementCartItem = viewModel::decrementCartItem,
                            onRemoveFromCart = viewModel::removeFromCart,
                            onClearCart = viewModel::clearCart,
                            onDiscountChange = viewModel::updateDiscount,
                            onCustomerNameChange = viewModel::updateCustomerName,
                            onNoteChange = viewModel::updateNote,
                            onPaymentMethodChange = viewModel::updatePaymentMethod,
                            onCashReceivedChange = viewModel::updateCashReceivedInput,
                            onComputeTotals = viewModel::computeTotals,
                            onConfirmPay = { viewModel.completeCheckout() },
                            onSeedStarterProducts = viewModel::seedStarterProducts,
                            onNavigateToProducts = { currentTab = KasirTab.PRODUK },
                        )

                        KasirTab.PRODUK -> ProductsScreen(
                            productsState = productsState,
                            isSaving = isSaving,
                            onSaveProduct = viewModel::saveProduct,
                            onDeleteProduct = viewModel::deleteProduct,
                            onSeedStarterProducts = viewModel::seedStarterProducts,
                        )

                        KasirTab.RIWAYAT -> HistoryScreen(
                            transactionsState = transactionsState,
                            onSelectTransaction = viewModel::showReceipt,
                        )

                        KasirTab.TOKO -> StoreSettingsScreen(
                            storeProfile = storeProfile,
                            isLocalMode = isLocalMode,
                            isSaving = isSaving,
                            onSaveProfile = viewModel::saveStoreProfile,
                            onSwitchToGoogleAuth = onSwitchToGoogleAuth,
                        )
                    }
                }
            }
        }
    }

    val receipt = activeReceipt
    if (receipt != null) {
        ReceiptDialog(
            transaction = receipt,
            store = storeProfile,
            allowVoid = currentTab == KasirTab.RIWAYAT,
            onVoidTransaction = viewModel::voidTransaction,
            onDismiss = viewModel::dismissReceipt,
        )
    }
}
