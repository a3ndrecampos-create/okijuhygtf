package com.arka.ganhos.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.arka.ganhos.core.TRIAL_DAYS
import com.arka.ganhos.core.trialDaysLeft
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Situação de acesso do usuário: teste grátis, assinatura ativa ou bloqueado. */
data class AccessState(
    val trialDaysLeft: Int = TRIAL_DAYS,
    val subscribed: Boolean = false,
    /** Preço mensal vindo da Play Store (ex.: "R$ 19,90"); nulo até carregar. */
    val price: String? = null,
    /** Ainda consultando a Play Store pela primeira vez. */
    val checking: Boolean = true,
    val message: String? = null
) {
    val hasAccess: Boolean get() = subscribed || trialDaysLeft > 0
}

/**
 * Teste grátis de [TRIAL_DAYS] dias contado no aparelho (desde a instalação) e depois assinatura mensal
 * pelo Google Play Billing.
 *
 * Configure no Play Console: assinatura com ID [PRODUCT_ID] e um plano base mensal (renovação automática).
 */
class BillingManager(private val ctx: Context) : PurchasesUpdatedListener {
    companion object {
        const val PRODUCT_ID = "arka_mensal"
        private const val DAY = 24L * 60 * 60 * 1000
        /** Sem conseguir falar com a Play Store, a última confirmação de assinatura vale por este tempo. */
        private const val OFFLINE_GRACE = 7 * DAY
        fun manageUrl(pkg: String) =
            "https://play.google.com/store/account/subscriptions?sku=$PRODUCT_ID&package=$pkg"
    }

    private val prefs = ctx.getSharedPreferences("access", Context.MODE_PRIVATE)

    // Estes campos precisam existir antes do StateFlow abaixo, que chama compute() na criação.
    private var details: ProductDetails? = null
    private var checking = true
    private var message: String? = null
    private var connecting = false

    private val _state = MutableStateFlow(compute())
    val state: StateFlow<AccessState> = _state.asStateFlow()

    private val client: BillingClient = BillingClient.newBuilder(ctx)
        .setListener(this)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .enableAutoServiceReconnection()
        .build()

    fun start() {
        connect()
    }

    /** Chamar ao voltar para o app: reconecta se preciso e confere a assinatura de novo. */
    fun refresh() {
        publish()
        if (client.isReady) {
            queryDetails()
            queryPurchases()
        } else {
            connect()
        }
    }

    /** Usado pelo serviço de leitura antes de analisar uma oferta. */
    fun hasAccess(): Boolean {
        publish()
        return _state.value.hasAccess
    }

    fun launchPurchase(activity: Activity) {
        val d = details
        val offer = d?.subscriptionOfferDetails?.let { list -> list.firstOrNull { it.offerId == null } ?: list.firstOrNull() }
        if (d == null || offer == null) {
            message = "Não foi possível carregar a assinatura. Confira sua conexão e tente de novo."
            publish()
            refresh()
            return
        }
        val pp = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(d)
            .setOfferToken(offer.offerToken)
            .build()
        val flow = BillingFlowParams.newBuilder().setProductDetailsParamsList(listOf(pp)).build()
        val r = client.launchBillingFlow(activity, flow)
        if (r.responseCode != BillingClient.BillingResponseCode.OK) {
            message = "Não foi possível abrir a tela de assinatura (código ${r.responseCode})."
            publish()
        }
    }

    // ---------- Play Billing ----------

    private fun connect() {
        if (connecting) return
        connecting = true
        try {
            client.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(result: BillingResult) {
                    connecting = false
                    if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                        queryDetails()
                        queryPurchases()
                    } else {
                        checking = false
                        publish()
                    }
                }

                override fun onBillingServiceDisconnected() {
                    connecting = false  // a reconexão é automática (enableAutoServiceReconnection)
                }
            })
        } catch (e: Exception) {
            connecting = false
            checking = false
            publish()
        }
    }

    private fun queryDetails() {
        val product = QueryProductDetailsParams.Product.newBuilder()
            .setProductId(PRODUCT_ID)
            .setProductType(BillingClient.ProductType.SUBS)
            .build()
        val params = QueryProductDetailsParams.newBuilder().setProductList(listOf(product)).build()
        client.queryProductDetailsAsync(params) { result, queryResult ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                details = queryResult.productDetailsList.firstOrNull()
                publish()
            }
        }
    }

    private fun queryPurchases() {
        val params = QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build()
        client.queryPurchasesAsync(params) { result, purchases -> handle(result, purchases, true) }
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> handle(result, purchases.orEmpty(), false)
            BillingClient.BillingResponseCode.USER_CANCELED -> {}
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> queryPurchases()
            else -> {
                message = "A compra não foi concluída (código ${result.responseCode})."
                publish()
            }
        }
    }

    private fun handle(result: BillingResult, purchases: List<Purchase>, fullList: Boolean) {
        checking = false
        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
            publish()
            return
        }
        val active = purchases.filter {
            it.purchaseState == Purchase.PurchaseState.PURCHASED && it.products.contains(PRODUCT_ID)
        }
        // A Play cancela e reembolsa a compra se não for reconhecida em 3 dias.
        active.filter { !it.isAcknowledged }.forEach { p ->
            client.acknowledgePurchase(
                AcknowledgePurchaseParams.newBuilder().setPurchaseToken(p.purchaseToken).build()
            ) { }
        }
        if (active.isNotEmpty()) {
            prefs.edit().putBoolean("sub_active", true).putLong("sub_verified_at", System.currentTimeMillis()).apply()
            message = null
        } else if (fullList) {
            // lista completa sem assinatura ativa: expirou, foi cancelada ou nunca existiu
            prefs.edit().putBoolean("sub_active", false).apply()
        }
        publish()
    }

    // ---------- Estado ----------

    private fun publish() {
        _state.value = compute()
    }

    private fun trialStart(now: Long): Long {
        val first = try {
            ctx.packageManager.getPackageInfo(ctx.packageName, 0).firstInstallTime
        } catch (e: Exception) {
            now
        }
        val saved = prefs.getLong("trial_start", 0L)
        val start = if (saved == 0L) first.coerceAtMost(now) else minOf(saved, first)
        if (start != saved) prefs.edit().putLong("trial_start", start).apply()
        return start
    }

    /** Relógio que não anda para trás: voltar a data do aparelho não estende o teste. */
    private fun safeNow(): Long {
        val raw = System.currentTimeMillis()
        val last = prefs.getLong("last_seen", 0L)
        val now = maxOf(raw, last)
        val advance = raw - last
        if (last == 0L || (advance > 60_000L && advance < 40 * DAY)) prefs.edit().putLong("last_seen", raw).apply()
        return now
    }

    private fun compute(): AccessState {
        val now = safeNow()
        val left = trialDaysLeft(trialStart(now), now)
        val verified = prefs.getLong("sub_verified_at", 0L)
        val sub = prefs.getBoolean("sub_active", false) && (now - verified) < OFFLINE_GRACE
        val price = details?.subscriptionOfferDetails?.firstOrNull()?.pricingPhases?.pricingPhaseList?.lastOrNull()?.formattedPrice
        return AccessState(left, sub, price, checking, message)
    }
}
