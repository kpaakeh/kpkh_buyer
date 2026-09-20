package net.kpkh_buyer;

import net.fabricmc.api.ModInitializer;
import net.kpkh_buyer.economy.EconomyCommands;
import net.kpkh_buyer.economy.EconomyConfig;
import net.kpkh_buyer.economy.EconomyEvents;
import net.kpkh_buyer.economy.EconomyManager;
import net.kpkh_buyer.economy.net.EconomyNetworking;
import net.kpkh_buyer.item.ModItems;
import net.minecraft.resources.Identifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Kpkh_buyer implements ModInitializer {
	public static final String MOD_ID = "kpkh_buyer";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		EconomyConfig.load();           // ← добавить
        BuyerConfig.load();               // загружаем цены
        EconomyCommands.register();       // /bal и /pay
        EconomyEvents.register();         // запоминаем имена при входе
		EconomyNetworking.registerCommon();
		BuyerConfig.load();  
		ModItems.initialize();
		ModRecipes.initialize();
		kpkh_block.initialize();          // создаёт BUYER_BLOCK и BUYER_ITEM
		ModBlockEntities.initialize();    // использует BUYER_BLOCK
		ModScreenHandlers.initialize();   // регистрирует MenuType
		ModCreativeTabs.initialize();     // использует BUYER_ITEM
		LOGGER.info("Hello Fabric world!");
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}