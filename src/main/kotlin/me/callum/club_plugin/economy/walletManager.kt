package me.callum.club_plugin.economy

class walletManager {
}

/*

when a new player joins, check if they have a wallet. if not, create one
for them and give them a starting amount of coins.

if a player is killed by another player, transfer some of their coins.
if a player dies otherwise, burn their coins.

@EventHandler
public void onPlayerJoin(PlayerJoinEvent event) {
    Player player = event.getPlayer();
    UUID playerUUID = player.getUniqueId();

    if (!WalletManager.hasWallet(playerUUID)) {
        WalletManager.createWallet(playerUUID);
    }
}

 */