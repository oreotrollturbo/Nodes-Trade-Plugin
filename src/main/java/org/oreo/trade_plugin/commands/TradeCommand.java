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

    private static final Map<Player, Player> tradeRequests = new HashMap<>(); // A list for all trade requests that are active
    private static final Map<Player, Inventory> activeTrades = new HashMap<>(); // A list for all trade requests happening

    /**
     * These are used to which player trading corresponds to what inventory
     * and if they are player 1 or player 2
     */
    private static final Map<Inventory, Player> inventoryToPlayer1 = new HashMap<>();
    private static final Map<Inventory, Player> inventoryToPlayer2 = new HashMap<>();

    /**
     * All the slots that correspond to player 1
     */
    private final List<Integer> player1Slots = new ArrayList<>(Arrays.asList(0, 1, 2, 3, 9, 10, 11, 12, 18, 19,
            20, 21, 27, 28, 29, 30, 36, 37, 38));

    /**
     * All the slots that correspond to player 1
     */
    private final List<Integer> player2Slots = new ArrayList<>(Arrays.asList(5, 6, 7, 8, 14, 15, 16, 17, 23, 24,
            25, 26, 32, 33, 34, 35, 42, 43, 44));


    /**
     * The name of the inventory
     */
    private final String invName = "Trade Offer";

    private final TradePlugin tradePlugin; //Plugin instance

    /**
     * @param plugin passing in the plugin
     * Gets the plugin instance and registers events
     */
    public TradeCommand(TradePlugin plugin) {
        this.tradePlugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be used by players.");
            return true; //Make sure the sender is a player
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

            Inventory tradeInv = createTradeInventory(player, senderPlayer, 5);

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

    /**
     * Adds "accept" to the autoComplete menu
     */
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

    /**
     * @param player1 set the first player of the trade
     * @param player2 set the second player of the trade
     * @param rows Add how many rows you want the inventory to be
     * @return inventory
     *
     * Creates the trade inventory with the set amount of rows
     */
    private Inventory createTradeInventory(Player player1, Player player2, int rows) {

        Inventory inv = Bukkit.createInventory(null, 9 * rows, invName);
        initializeItems(inv);
        return inv;
    }

    /**
     * @param inv pass in the inventory you want the items to be added to
     * Simple method to initialise items in a new inventory
     */
    private void initializeItems(Inventory inv) {
        inv.setItem(4, createGuiItem(Material.LIGHT_BLUE_STAINED_GLASS_PANE, " "));
        inv.setItem(13, createGuiItem(Material.LIGHT_BLUE_STAINED_GLASS_PANE, " "));
        inv.setItem(22, createGuiItem(Material.LIGHT_BLUE_STAINED_GLASS_PANE, " "));
        inv.setItem(31, createGuiItem(Material.LIGHT_BLUE_STAINED_GLASS_PANE, " "));

        inv.setItem(44, createGuiItem(Material.LIGHT_BLUE_STAINED_GLASS_PANE, " "));
        inv.setItem(43, createGuiItem(Material.LIGHT_BLUE_STAINED_GLASS_PANE, " "));
        inv.setItem(42, createGuiItem(Material.LIGHT_BLUE_STAINED_GLASS_PANE, " "));

        inv.setItem(38, createGuiItem(Material.LIGHT_BLUE_STAINED_GLASS_PANE, " "));
        inv.setItem(37, createGuiItem(Material.LIGHT_BLUE_STAINED_GLASS_PANE, " "));
        inv.setItem(36, createGuiItem(Material.LIGHT_BLUE_STAINED_GLASS_PANE, " "));

        inv.setItem(40, createGuiItem(Material.BARRIER, "Cancel ready","Cancel the ready state for both players"));
        inv.setItem(39, createGuiItem(Material.RED_WOOL, "Accept trade"));
        inv.setItem(41, createGuiItem(Material.RED_WOOL, "Accept trade"));
    }

    /**
     * @param material set the material it should be (what item)
     * @param name How it should be called like any renamed item from an anvil
     * @param lore This is to set extra text under the name itself it is optional
     * @return it just returns the item now created
     * This is a helper method to create items for a gui
     */
    private ItemStack createGuiItem(final Material material, final String name, final String... lore) {
        final ItemStack item = new ItemStack(material, 1);
        final ItemMeta meta = item.getItemMeta();

        assert meta != null;
        meta.setDisplayName(name);
        meta.setLore(Arrays.asList(lore));

        item.setItemMeta(meta);
        return item;
    }

    /**
     * @param player get the player to know who to open the inventory
     * @param inv To know what inventory is even getting opened
     * Opens the already made gui for the player
     */
    private void openInventory(final Player player, Inventory inv) {
        player.openInventory(inv);
    }

    /**
     * Checks if it's the trade inventory open , stops shift clicking , stops the COLLECT_TO_CURSOR (double click to gather)
     * Lets you click on your inventory as long as neither player is "trade ready"
     */
    @EventHandler
    public void onInventoryClick(final InventoryClickEvent e) {
        if (!e.getView().getTitle().equals(invName)) return;

        final Player p = (Player) e.getWhoClicked();
        Inventory tradeInv = e.getInventory();

        if (e.isShiftClick() || !isOfCorrespondingSlot(e.getRawSlot() , p , tradeInv) ){
            e.setCancelled(true);
        }

        if (e.getAction() == InventoryAction.COLLECT_TO_CURSOR) {

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

    /**
     * Checks if either of the players are "ready" by checking if either of the wools are green
     * if they are then it cancels any click event so you cant drag in/out stuff out of the trade inventory
     */
    @EventHandler
    private void oneReady(InventoryClickEvent e){
        if (!e.getView().getTitle().equals(invName)) return;

        Inventory tradeInv = e.getInventory();

        if ((Objects.requireNonNull(tradeInv.getItem(39)).getType().equals(Material.GREEN_WOOL)
                || Objects.requireNonNull(tradeInv.getItem(41)).getType().equals(Material.GREEN_WOOL))) {
            e.setCancelled(true);
        }
    }

    /**
     * @param rawSlot get the slot clicked
     * @return
     * Checks if the items are of the trade gui
     */
    private boolean isGuiItem(int rawSlot) {
        return Arrays.asList(4, 13, 22, 31,44,43,42,36,37,38).contains(rawSlot);
    }

    /**
     * @param rawSlot get the slot clicked
     * @return int
     * Checks if the clicked slot is for player 1
     */
    private boolean isPlayer1Slot(int rawSlot) {
        return player1Slots.contains(rawSlot);
    }

    /**
     * @param rawSlot get the slot clicked
     * @return boolean
     * Checks if the slot is for player 2
     */
    private boolean isPlayer2Slot(int rawSlot) {
        return player2Slots.contains(rawSlot);
    }

    /**
     * @param rawSlot get the slot clicked
     * @param player get the player involved
     * @param tradeInv get the inventory of the trade
     * @return boolean
     * Checks if the slot corresponds to the player that clicked
     * the method does this by checking the player1 list and seeing if the player is in it
     * if he is not it assumes hes player 2
     */
    private boolean isOfCorrespondingSlot(int rawSlot, Player player , Inventory tradeInv){

        boolean isPlayer1 = inventoryToPlayer1.get(tradeInv).equals(player);

        if (isPlayer1 && isPlayer1Slot(rawSlot)){
            return true;
        }else return isPlayer2Slot(rawSlot);
    }


    /**
     * @param player Get the player that triggered the method
     * @param accepted Check if the trade was accepted or denied
     * @param inv Get the inventory of the trade
     * This method handles everything when a trade is completed
     * it closes the inventory for both players sends the appropriate message gives the items
     * and removes the trade from the list
     */
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

    /**
     * @param player get the player to who caused the event
     * @param tradePartner get the other player who is in the trade
     * @param inv get the trade inventory
     * @param playerSlots gets the first players slots to know who to give the items to
     * @param partnerSlots gets the second players slots to know who to give the items to
     * This is a method that's only called by the handleTradeCompletion method
     * It checks if the trade was completed or cancelled and gives the items accordingly
     */
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


    /**
     * @param e event
     * When the trade inventory is closed it calls the handleTradeCompletion method
     */
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent e) {
        if (!e.getView().getTitle().equals(invName)) return;

        final Player p = (Player) e.getPlayer();

        handleTradeCompletion(p, false, e.getInventory());
    }

    /**
     * @param e it's just the event
     * Makes sure you cant drag items into the other players inventory
     *          if that happens it
     */
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