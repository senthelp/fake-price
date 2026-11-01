package com.pathetictry.pricejoke;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;

public class PriceJokeClient implements ClientModInitializer {
    private static boolean openNext;

    @Override
    public void onInitializeClient() {
        PriceStore.load();

        ItemTooltipCallback.EVENT.register((stack, context, flag, lines) -> PriceTooltip.apply(stack, lines));

        // Client command: handled on this machine, never sent to the server.
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(ClientCommandManager.literal("price").executes(ctx -> {
                    openNext = true;
                    return 1;
                })));

        // Open on the next tick so the chat screen closing doesn't close ours.
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            if (openNext) {
                openNext = false;
                mc.setScreen(new PriceScreen());
            }
        });
    }
}
