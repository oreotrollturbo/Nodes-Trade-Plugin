package org.oreo.trade_plugin.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.oreo.trade_plugin.TradePlugin;

import java.util.*;

public class TradeCommand implements TabExecutor, Listener {

    private static final Map<Player, Player> tradeRequests = new HashMap<>();
    private static final Map<Player, Inventory> activeTrades = new HashMap<>();

    private static final Map<Inventory, Player> inventoryToPlayer1 = new HashMap<>();
    private static final Map<Inventory, Player> inventoryToPlayer2 = new HashMap<>();

    private final List<Integer> player1Slots = new ArrayList<>(Arrays.asList(0, 1, 2, 3, 9, 10, 11, 12, 18, 19,
            20, 21, 27, 28, 29, 30, 36, 37, 38));

    private final List<Integer> player2Slots = new ArrayList<>(Arrays.asList(5, 6, 7, 8, 14, 15, 16, 17, 23, 24,
            25, 26, 32, 33, 34, 35, 42, 43, 44));


    private final String invName = "Trade Offer";

    private final TradePlugin tradePlugin;

    public TradeCommand(TradePlugin plugin) {
        this.tradePlugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be used by players.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            player.sendMessage(ChatColor.RED + "You must specify a player to trade with.");
            return true;
        }

        if (args[0].equalsIgnoreCase("accept")) {
            Player senderPlayer;
            synchronized (tradeRequests) {
                senderPlayer = tradeRequests.get(player);
            }

            if (senderPlayer == null) {
                player.sendMessage(ChatColor.RED + "You don't have any pending trade requests");
                return true;
            }

            synchronized (tradeRequests) {
                tradeRequests.remove(player);
            }

            Inventory tradeInv = createTradeInventory(player, senderPlayer);

            synchronized (activeTrades) {
                activeTrades.put(player, tradeInv);
                activeTrades.put(senderPlayer, tradeInv);
                inventoryToPlayer1.put(tradeInv, senderPlayer);
                inventoryToPlayer2.put(tradeInv, player);
            }

            openInventory(player, tradeInv);
            openInventory(senderPlayer, tradeInv);
            return true;
        } else {
            Player receiver = Bukkit.getPlayerExact(args[0]);

            if (receiver == null) {
                player.sendMessage(ChatColor.RED + "Invalid username.");
                return true;
            }

            if (receiver.equals(player)) {
                player.sendMessage(ChatColor.RED + "You cant trade with yourself");
                return true;
            }

            if (tradeRequests.containsKey(player)) {
                player.sendMessage(ChatColor.RED + "You already have an active trade request");
                return true;
            }

            player.sendMessage(ChatColor.GREEN + "Sending trade request to " + receiver.getName());
            receiver.sendMessage(ChatColor.GOLD + player.getName() + " is sending you a trade request. Use /trade accept to accept.");

            synchronized (tradeRequests) {
                tradeRequests.put(receiver, player);
            }

            receiver.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME,2,1);

            Bukkit.getServer().getScheduler()
                    .scheduleAsyncDelayedTask(tradePlugin, new Runnable() {
                        public void run() {

                            if (tradeRequests.containsKey(receiver)){
                                player.sendMessage(ChatColor.RED + "Request expired");
                                synchronized (tradeRequests) {
                                    tradeRequests.remove(receiver);
                                }
                            }

                        }
                    }, 1200);

            return true;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1){
            List<String> playerNames = new ArrayList<>();
            Player[] players = new Player[Bukkit.getServer().getOnlinePlayers().size()];
            Bukkit.getServer().getOnlinePlayers().toArray(players);
            for (int i = 0; i < players.length; i++){
                playerNames.add(players[i].getName());
            }
            playerNames.add("accept");

            return playerNames;
        }

        return null;
    }

    private Inventory createTradeInventory(Player player1, Player player2) {
        int rows = 5;
        Inventory inv = Bukkit.createInventory(null, 9 * rows, invName);
        initializeItems(inv);
        return inv;
    }

    private void initializeItems(Inventory inv) {
        inv.setItem(4, createGuiItem(Material.LIGHT_BLUE_STAINED_GLASS_PANE, " "));
        inv.setItem(13, createGuiItem(Material.LIGHT_BLUE_STAINED_GLASS_PANE, " "));
        inv.setItem(22, createGuiItem(Material.LIGHT_BLUE_STAINED_GLASS_PANE, " "));
        inv.setItem(31, createGuiItem(Material.LIGHT_BLUE_STAINED_GLASS_PANE, " "));
        inv.setItem(40, createGuiItem(Material.BARRIER, "Cancel ready","Cancel the ready state for both players"));
        inv.setItem(39, createGuiItem(Material.RED_WOOL, "Accept trade"));
        inv.setItem(41, createGuiItem(Material.RED_WOOL, "Accept trade"));
    }

    private ItemStack createGuiItem(final Material material, final String name, final String... lore) {
        final ItemStack item = new ItemStack(material, 1);
        final ItemMeta meta = item.getItemMeta();

        assert meta != null;
        meta.setDisplayName(name);
        meta.setLore(Arrays.asList(lore));

        item.setItemMeta(meta);
        return item;
    }

    private void openInventory(final Player player, Inventory inv) {
        player.openInventory(inv);
    }

    @EventHandler
    public void onInventoryClick(final InventoryClickEvent e) {
        if (!e.getView().getTitle().equals(invName)) return;

        final Player p = (Player) e.getWhoClicked();
        Inventory tradeInv = e.getInventory();

        if (e.isShiftClick() || !isOfCorrespondingSlot(e.getRawSlot() , p , tradeInv) ){
            e.setCancelled(true);
        }

        if (e.getAction() == InventoryAction.COLLECT_TO_CURSOR) {
            e.getWhoClicked().sendMessage("Detected");

            e.setResult(Event.Result.DENY);
            e.setCancelled(true);
            return;
        }

        if (e.getRawSlot() >= 45 && !e.isShiftClick()) {
            e.setCancelled(false);
            return;
        }

        boolean isPlayer1 = inventoryToPlayer1.get(tradeInv).equals(p);

        // If both trade acceptance slots are not green wool
        if (!(Objects.requireNonNull(tradeInv.getItem(39)).getType().equals(Material.GREEN_WOOL)
                || Objects.requireNonNull(tradeInv.getItem(41)).getType().equals(Material.GREEN_WOOL))) {

            if (isGuiItem(e.getRawSlot())) {
                e.setCancelled(true);
                return;
            }

            if (e.getRawSlot() == 40){
                tradeInv.setItem(39, createGuiItem(Material.RED_WOOL, "Accept trade"));
                tradeInv.setItem(41, createGuiItem(Material.RED_WOOL, "Accept trade"));
            }

            if (isPlayer1) {
                if (e.getRawSlot() == 39) {
                    tradeInv.setItem(39, createGuiItem(Material.GREEN_WOOL, "Trade Ready"));
                    e.setCancelled(true);
                } else if (!isPlayer1Slot(e.getRawSlot())) {
                    e.setCancelled(true);
                }
            } else {
                if (e.getRawSlot() == 41) {
                    tradeInv.setItem(41, createGuiItem(Material.GREEN_WOOL, "Trade Ready"));
                    e.setCancelled(true);
                } else if (!isPlayer2Slot(e.getRawSlot())) {
                    e.setCancelled(true);
                }
            }

        } else { // If at least one of the players has agreed
            int slot = e.getRawSlot();

            e.setCancelled(true);

            if (e.getRawSlot() == 40){
                tradeInv.setItem(41, createGuiItem(Material.RED_WOOL, "Accept trade"));
                tradeInv.setItem(39, createGuiItem(Material.RED_WOOL, "Accept trade"));
            }

            if (isPlayer1) {
                if (slot == 39) {
                    if (Objects.requireNonNull(tradeInv.getItem(slot)).getType().equals(Material.GREEN_WOOL)) {
                        tradeInv.setItem(39, createGuiItem(Material.RED_WOOL, "Accept trade"));
                    } else {
                        tradeInv.setItem(39, createGuiItem(Material.GREEN_WOOL, "Trade Ready"));

                        if ((Objects.requireNonNull(tradeInv.getItem(39))).getType().equals(Material.GREEN_WOOL)
                                && (Objects.requireNonNull(tradeInv.getItem(41))).getType().equals(Material.GREEN_WOOL)) {
                            handleTradeCompletion(p, true, tradeInv);
                        }
                    }
                }
            } else if (slot == 41) {
                if (Objects.requireNonNull(tradeInv.getItem(slot)).getType().equals(Material.GREEN_WOOL)) {
                    tradeInv.setItem(41, createGuiItem(Material.RED_WOOL, "Accept trade"));
                } else {
                    tradeInv.setItem(41, createGuiItem(Material.GREEN_WOOL, "Trade Ready"));
                    if ((Objects.requireNonNull(tradeInv.getItem(39))).getType().equals(Material.GREEN_WOOL)
                            && (Objects.requireNonNull(tradeInv.getItem(41))).getType().equals(Material.GREEN_WOOL)) {
                        handleTradeCompletion(p, true, tradeInv);
                    }
                }
            }
        }
    }

    @EventHandler
    private void oneReady(InventoryClickEvent e){
        if (!e.getView().getTitle().equals(invName)) return;

        Inventory tradeInv = e.getInventory();

        if ((Objects.requireNonNull(tradeInv.getItem(39)).getType().equals(Material.GREEN_WOOL)
                || Objects.requireNonNull(tradeInv.getItem(41)).getType().equals(Material.GREEN_WOOL))) {
            e.setCancelled(true);
        }
    }

    private boolean isGuiItem(int rawSlot) {
        return Arrays.asList(4, 13, 22, 31).contains(rawSlot);
    }

    private boolean isPlayer1Slot(int rawSlot) {
        return player1Slots.contains(rawSlot);
    }

    private boolean isPlayer2Slot(int rawSlot) {
        return player2Slots.contains(rawSlot);
    }

    private boolean isOfCorrespondingSlot(int rawSlot, Player player , Inventory tradeInv){

        boolean isPlayer1 = inventoryToPlayer1.get(tradeInv).equals(player);

        if (isPlayer1 && isPlayer1Slot(rawSlot)){
            return true;
        }else return isPlayer2Slot(rawSlot);
    }


    private void handleTradeCompletion(Player player, boolean accepted, Inventory inv) {
        Inventory tradeInv;
        synchronized (activeTrades) {
            tradeInv = activeTrades.get(player);
            if (tradeInv == null) return;

            boolean isPlayer1 = inventoryToPlayer1.get(tradeInv).equals(player);

            Player tradePartner = isPlayer1
                    ? inventoryToPlayer2.get(tradeInv)
                    : inventoryToPlayer1.get(tradeInv);

            activeTrades.remove(player);
            activeTrades.remove(tradePartner);
            inventoryToPlayer1.remove(tradeInv);
            inventoryToPlayer2.remove(tradeInv);

            String message = accepted ? ChatColor.GREEN + "Trade completed successfully." : ChatColor.RED + "Trade was rejected.";
            player.sendMessage(message);
            player.closeInventory();

            if (tradePartner != null) {
                tradePartner.closeInventory();
                tradePartner.sendMessage(message);
            }

            if (accepted){

                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING,1,5);
                assert tradePartner != null;
                tradePartner.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING,1,5);
                if (isPlayer1) {
                    processItems(player, tradePartner, inv, player1Slots, player2Slots);
                } else {
                    processItems(player, tradePartner, inv, player2Slots, player1Slots);
                }
            }else if (!accepted){

                player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_DESTROY,1,3);
                assert tradePartner != null;
                tradePartner.playSound(tradePartner.getLocation(), Sound.BLOCK_ANVIL_HIT,1,3);
                if (isPlayer1) {
                    processItems(tradePartner, player, inv, player1Slots, player2Slots);
                } else {
                    processItems(tradePartner, player, inv, player2Slots, player1Slots);
                }
            }

        }
        inv.clear(); // Clear it just in case
    }

    private void processItems(Player player, Player tradePartner, Inventory inv, List<Integer> playerSlots, List<Integer> partnerSlots) {

        playerSlots.forEach(slot -> {
            ItemStack item = inv.getItem(slot);
            if (item != null && tradePartner != null) {
                tradePartner.getInventory().addItem(item);
            }
        });

        partnerSlots.forEach(slot -> {
            ItemStack item = inv.getItem(slot);
            if (item != null) {
                player.getInventory().addItem(item);
            }
        });
    }


    @EventHandler
    public void onInventoryClose(InventoryCloseEvent e) {
        if (!e.getView().getTitle().equals(invName)) return;

        final Player p = (Player) e.getPlayer();

        handleTradeCompletion(p, false, e.getInventory());
    }

    @EventHandler
    public void onItemDraged(InventoryDragEvent e){

        if (!e.getView().getTitle().equals(invName)) return;

        final Player p = (Player) e.getWhoClicked();
        Inventory tradeInv = e.getInventory();

        boolean isPlayer1 = inventoryToPlayer1.get(tradeInv).equals(p);

        if (isPlayer1){

            for (int slot : e.getRawSlots()) {
                if (!isPlayer1Slot(slot) && !(slot >= 45)){
                    e.setCancelled(true);
                    return;
                }
            }

        }else {

            for (int slot : e.getRawSlots()) {
                if (!isPlayer2Slot(slot) && !(slot >= 45)){
                    e.setCancelled(true);
                    return;
                }
            }
        }

        if ((Objects.requireNonNull(tradeInv.getItem(39)).getType().equals(Material.GREEN_WOOL)
                || Objects.requireNonNull(tradeInv.getItem(41)).getType().equals(Material.GREEN_WOOL))) {
            e.setCancelled(true);
        }
    }
}