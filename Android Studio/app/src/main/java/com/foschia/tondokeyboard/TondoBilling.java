package com.foschia.tondokeyboard;

import android.app.Activity;
import android.content.Context;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.AcknowledgePurchaseResponseListener;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.ProductDetailsResponseListener;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesResponseListener;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.QueryProductDetailsParams;
import com.android.billingclient.api.QueryPurchasesParams;
import com.google.common.collect.ImmutableList;

import java.util.List;

public class TondoBilling
{
	private Context context;
	private EncryptedSharedPreferences eSharedPreferences;
	private MainActivity mainActivity;
	private ShoppingPreferenceCategory shoppingPreferenceCategory;
	private BillingClient billingClient;
	private int billingStartTentatives = 0;
	private int acknoledgePurchaseTentatives = 0;
	private final String InAppPurchaseProductId = "pro_version_01";
	private List<ProductDetails> productDetailsList;

	private TondoBilling()
	{
	}

	public TondoBilling(Context context, EncryptedSharedPreferences eSharedPreferences)
	{
		this.context = context;
		this.eSharedPreferences = eSharedPreferences;
	}

	public void SetMainActivity(MainActivity mainActivity)
	{
		this.mainActivity = mainActivity;
	}

	public void SetShoppingPreferenceCategory(ShoppingPreferenceCategory shoppingPreferenceCategory)
	{
		this.shoppingPreferenceCategory = shoppingPreferenceCategory;
	}

	public void InitializeAndStartBilling()
	{
		Utils.DebugLog(Utils.LogType.INFO, "TondoBilling - InitializeAndStartBilling");

		if (billingClient == null)
		{
			billingClient = BillingClient.newBuilder(context)
					.setListener(purchasesUpdatedListener)
					.enablePendingPurchases()
					.build();
		}

		int connectionState = billingClient.getConnectionState();

		if (connectionState == BillingClient.ConnectionState.CONNECTING ||
				connectionState == BillingClient.ConnectionState.CONNECTED)
		{
			Utils.DebugLog(Utils.LogType.INFO, "TondoBilling - InitializeAndStartBilling connectionState CONNECTING || CONNECTED");
			return;
		}

		billingClient.startConnection(new BillingClientStateListener()
		{
			@Override
			public void onBillingSetupFinished(BillingResult billingResult)
			{
				Utils.DebugLog(Utils.LogType.INFO, "TondoBilling - onBillingSetupFinished billingResult: " + billingResult.getDebugMessage());

				if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK)
				{
					QueryPurchases();
					QueryProductDetails();
				}
				else
				{
					TryReinitializeAndStartBilling();
				}
			}

			@Override
			public void onBillingServiceDisconnected()
			{
				Utils.DebugLog(Utils.LogType.WARNING, "TondoBilling - onBillingServiceDisconnected");
				TryReinitializeAndStartBilling();
			}
		});
	}

	public void LaunchPurchaseFlow(Activity activity)
	{
		Utils.DebugLog(Utils.LogType.INFO, "TondoBilling - LaunchPurchaseFlow");

		if (productDetailsList == null || productDetailsList.size() == 0)
		{
			Utils.DebugLog(Utils.LogType.ERROR, "TondoBilling - LaunchPurchaseFlow productDetailsList == null || productDetailsList.size() == 0");
			return;
		}

		ProductDetails productDetails = productDetailsList.get(0);


		ImmutableList productDetailsParamsList =
				ImmutableList.of(
						BillingFlowParams.ProductDetailsParams.newBuilder()
								.setProductDetails(productDetails)
								.build()
				);

		BillingFlowParams billingFlowParams = BillingFlowParams.newBuilder()
				.setProductDetailsParamsList(productDetailsParamsList)
				.build();

		BillingResult billingResult = billingClient.launchBillingFlow(activity, billingFlowParams);
	}

	private void TryReinitializeAndStartBilling()
	{
		Utils.DebugLog(Utils.LogType.INFO, "TondoBilling - TryReinitializeAndStartBilling");

		billingStartTentatives++;
		if (billingStartTentatives <= 3)
		{
			InitializeAndStartBilling();
		}
		else
		{
			Utils.DebugLog(Utils.LogType.ERROR, "TondoBilling - Start billing failed for more than 3 times.");
		}
	}

	private PurchasesUpdatedListener purchasesUpdatedListener = new PurchasesUpdatedListener()
	{
		@Override
		public void onPurchasesUpdated(BillingResult billingResult, List<Purchase> purchases)
		{
			Utils.DebugLog(Utils.LogType.INFO, "TondoBilling - onPurchasesUpdated");

			if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && purchases != null)
			{
				HandlePurchases(purchases);
			}
			else if (billingResult.getResponseCode() != BillingClient.BillingResponseCode.USER_CANCELED)
			{
				TryReinitializeAndStartBilling();
			}
		}
	};

	private void QueryProductDetails()
	{
		Utils.DebugLog(Utils.LogType.INFO, "TondoBilling - QueryProductDetails");

		QueryProductDetailsParams queryProductDetailsParams =
				QueryProductDetailsParams.newBuilder()
						.setProductList(
								ImmutableList.of(
										QueryProductDetailsParams.Product.newBuilder()
												.setProductId(InAppPurchaseProductId)
												.setProductType(BillingClient.ProductType.INAPP)
												.build()))
						.build();

		billingClient.queryProductDetailsAsync(
				queryProductDetailsParams,
				new ProductDetailsResponseListener()
				{
					public void onProductDetailsResponse(BillingResult billingResult, List<ProductDetails> productDetailsList)
					{
						Utils.DebugLog(Utils.LogType.INFO, "TondoBilling - onProductDetailsResponse");
						SetProductDetailsList(productDetailsList);
					}
				}
		);
	}

	private void SetProductDetailsList(List<ProductDetails> productDetailsList)
	{
		this.productDetailsList = productDetailsList;
	}

	private void QueryPurchases()
	{
		Utils.DebugLog(Utils.LogType.INFO, "TondoBilling - QueryPurchases");

		billingClient.queryPurchasesAsync(
				QueryPurchasesParams.newBuilder()
						.setProductType(BillingClient.ProductType.INAPP)
						.build(),
				new PurchasesResponseListener()
				{
					public void onQueryPurchasesResponse(BillingResult billingResult, List purchases)
					{
						Utils.DebugLog(Utils.LogType.INFO, "TondoBilling - onQueryPurchasesResponse");

						if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && purchases != null)
						{
							HandlePurchases(purchases);
						}
					}
				}
		);
	}

	private void HandlePurchases(List<Purchase> purchases)
	{
		Utils.DebugLog(Utils.LogType.INFO, "TondoBilling - HandlePurchases. Purchases.size(): " + purchases.size());

		for (Purchase purchase : purchases)
		{
			Utils.DebugLog(Utils.LogType.INFO, "TondoBilling - HandlePurchases. Purchase: " + purchase.getProducts().get(0));
			AcknowledgePurchaseIfNeeded(purchase);
		}

		boolean proWasPurchased = WasProPurchased(purchases);

		if (mainActivity != null)
		{
			mainActivity.EnableShopButton(!proWasPurchased);
		}
		else if (shoppingPreferenceCategory != null)
		{
			shoppingPreferenceCategory.EnableShopButton(!proWasPurchased);
		}

		eSharedPreferences.putEncrypted(UnityInterface.PURCHASED_PRO_PREFERENCES_KEY, proWasPurchased);
	}

	private Boolean WasProPurchased(List<Purchase> purchases)
	{
		for (Purchase purchase : purchases)
		{
			if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED && purchase.getProducts().get(0).equals(InAppPurchaseProductId))
			{
				return true;
			}
		}
		return false;
	}

	private void AcknowledgePurchaseIfNeeded(Purchase purchase)
	{
		Utils.DebugLog(Utils.LogType.INFO, "TondoBilling - AcknowledgePurchaseIfNeeded");

		AcknowledgePurchaseResponseListener acknowledgePurchaseResponseListener = new AcknowledgePurchaseResponseListener()
		{
			@Override
			public void onAcknowledgePurchaseResponse(BillingResult billingResult)
			{
				Utils.DebugLog(Utils.LogType.INFO, "TondoBilling - onAcknowledgePurchaseResponse");

				if (billingResult.getResponseCode() != BillingClient.BillingResponseCode.OK &&
						billingResult.getResponseCode() != BillingClient.BillingResponseCode.USER_CANCELED)
				{
					TryReacknoledgePurchase(purchase);
				}
			}
		};

		if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED)
		{
			if (!purchase.isAcknowledged())
			{
				AcknowledgePurchaseParams acknowledgePurchaseParams =
						AcknowledgePurchaseParams.newBuilder()
								.setPurchaseToken(purchase.getPurchaseToken())
								.build();
				billingClient.acknowledgePurchase(acknowledgePurchaseParams, acknowledgePurchaseResponseListener);

				Utils.DebugLog(Utils.LogType.INFO, "TondoBilling - Purchase acknowledged");
			}
		}
	}

	private void TryReacknoledgePurchase(Purchase purchase)
	{
		Utils.DebugLog(Utils.LogType.INFO, "TondoBilling - TryReacknoledgePurchase");

		acknoledgePurchaseTentatives++;
		if (acknoledgePurchaseTentatives <= 3)
		{
			AcknowledgePurchaseIfNeeded(purchase);
		}
		else
		{
			Utils.DebugLog(Utils.LogType.ERROR, "TondoBilling - Acknoledge purchase failed for more than 3 times.");
		}
	}
}
