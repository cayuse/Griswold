package net.deepbondi.cayuse;

import net.milkbowl.vault.economy.EconomyResponse;
import net.minecraft.server.EnchantmentInstance;
import net.minecraft.server.EnchantmentManager;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class Interactor {
	
	public static double basicToolsPrice = 10.0;
	public static double basicArmorPrice = 10.0;
	public static double enchantmentPrice = 30.0;
	
	public static boolean enableEnchants = true;
	public static double addEnchantmentPrice = 50.0;
	public static int maxEnchantBonus = 5;
	public static boolean clearEnchantments = false;
	
	private final static List<Integer> repairableTools;
	private final static List<Integer> repairableArmor;
	static {
		repairableTools = new LinkedList<Integer>();
		repairableArmor = new LinkedList<Integer>();
		
		for (int i = 256; i <= 259; i++) repairableTools.add(i); // IRON TOOLS AND ZIPPO
		for (int i = 267; i <= 279; i++) repairableTools.add(i); // WOODEN, STONE, DIAMOND TOOLS
		for (int i = 283; i <= 286; i++) repairableTools.add(i); // GOLDEN TOOLS
		for (int i = 290; i <= 294; i++) repairableTools.add(i); // HOES
		repairableTools.add(359); // SCISSORS
		repairableTools.add(Material.BOW.getId()); // BOW
		repairableTools.add(Material.FISHING_ROD.getId()); // FISHING ROD
		
		for (int i = 298; i <= 317; i++) repairableArmor.add(i); // ALL ARMOR
	}
	
	private static Set<Interaction> interactions = new HashSet<Interaction>();
	
	public static void interact(Player player, Repairer repairman) {
		final ItemStack item = player.getItemInHand();
		
		double price = Math.round(getPrice(repairman, item));

		if (item.getType() == Material.AIR) {
			player.sendMessage(String.format(Lang.name_format, repairman.name)+Lang.chat_noitem);
			return;
		}

		if (checkCanRepair(player, repairman, item)) {
			Interaction interaction = new Interaction(player, repairman.entity, item, item.getDurability(), System.currentTimeMillis());
			
			// INTERACTS SECOND TIME
			
			for (Interaction inter : interactions) {
				if (interaction.equals(inter)) {
					
					if (item.getDurability() != 0 && (
							repairman.type.equalsIgnoreCase("armor") || 
							repairman.type.equalsIgnoreCase("tools") ||
							repairman.type.equalsIgnoreCase("both") ||
							repairman.type.equalsIgnoreCase("all")
						)) {
						 EconomyResponse r = null;
						if (Griswold.economy == null || Griswold.economy.getBalance(player.getName()) >= price) {
							if (Griswold.economy != null) r = Griswold.economy.withdrawPlayer(player.getName(), price);
				            if(Griswold.economy == null || r.transactionSuccess()) {
								item.setDurability((short) 0);
					            inter.valid = false; // INVALIDATE INTERACTION
								player.sendMessage(String.format(Lang.name_format, repairman.name)+Lang.chat_done);
				            } else {
					            inter.valid = false; // INVALIDATE INTERACTION
				            	player.sendMessage(String.format(Lang.name_format, repairman.name)+ChatColor.RED+Lang.chat_error);
				            }
							return;
						} else {
							inter.valid = false; // INVALIDATE INTERACTION
							player.sendMessage(String.format(Lang.name_format, repairman.name)+Lang.chat_poor);
							return;
						}
					} else if (enableEnchants && item.getDurability() == 0 && (repairman.type.equalsIgnoreCase("enchant") || repairman.type.equalsIgnoreCase("all"))) {
							price = addEnchantmentPrice;
							EconomyResponse r = null;
							if (Griswold.economy == null || Griswold.economy.getBalance(player.getName()) >= price) {
								if (Griswold.economy != null) r = Griswold.economy.withdrawPlayer(player.getName(), price);
					            if(Griswold.economy == null || r.transactionSuccess()) {
						            if (clearEnchantments) {
							            for (Enchantment enchantToDel : item.getEnchantments().keySet()) {
								            item.removeEnchantment(enchantToDel);
							            }
						            }

									net.minecraft.server.ItemStack vanillaItem = CraftItemStack.createNMSItemStack(item);
									int bonus = (new Random()).nextInt(maxEnchantBonus);
									List<?> list = EnchantmentManager.b(new Random(), vanillaItem, bonus);
									if (list != null) {
					                   for (Object obj : list) {
					                        EnchantmentInstance instance = (EnchantmentInstance) obj;
					                        item.addEnchantment(org.bukkit.enchantments.Enchantment.getById(instance.enchantment.id), instance.level);
					                    }
										inter.valid = false; // INVALIDATE INTERACTION
					                    player.sendMessage(String.format(Lang.name_format, repairman.name)+Lang.chat_enchant_success);
					                } else {
										inter.valid = false; // INVALIDATE INTERACTION
										player.sendMessage(String.format(Lang.name_format, repairman.name)+Lang.chat_enchant_failed);
									}
									return;
							
					            } else {
						            inter.valid = false; // INVALIDATE INTERACTION
									player.sendMessage(String.format(Lang.name_format, repairman.name)+Lang.chat_poor);
									return;
								}
							}
					} else {
						inter.valid = false; // INVALIDATE INTERACTION
		            	player.sendMessage(String.format(Lang.name_format, repairman.name)+ChatColor.RED+Lang.chat_error);
					}
				}
			}
			
			// INTERACTS FIRST TIME
			
			if (interactions.size() > 10) interactions.clear(); // THIS SUCK, I KNOW

			if (item.getDurability() != 0) {
				// NEEDS REPAIR
				if (!repairman.type.equalsIgnoreCase("enchant")){
					// CAN REPAIR
					interactions.add(interaction);
					if (Griswold.economy != null) player.sendMessage(String.format(ChatColor.GOLD+"<"+repairman.name+"> "+ChatColor.WHITE+
							Lang.chat_cost, price, Griswold.economy.currencyNamePlural()));
					else player.sendMessage(String.format(Lang.name_format, repairman.name)+Lang.chat_free);
					player.sendMessage(String.format(Lang.name_format, repairman.name)+Lang.chat_agreed);
				} else {
					// CANNOT REPAIR
					player.sendMessage(String.format(Lang.name_format, repairman.name)+Lang.chat_needs_repair);
				}
			} else {
				// NEEDS ENCHANT
				if (enableEnchants) { // ENCHANTS ENABLED
					price = addEnchantmentPrice;
					if (repairman.type.equalsIgnoreCase("enchant") || repairman.type.equalsIgnoreCase("all")) { // CAN ENCHANT
						interactions.add(interaction);
						if (Griswold.economy != null) player.sendMessage(String.format(String.format(Lang.name_format, repairman.name)+
								Lang.chat_enchant_cost, price, Griswold.economy.currencyNamePlural()));
						else player.sendMessage(String.format(Lang.name_format, repairman.name)+Lang.chat_enchant_free);
						player.sendMessage(String.format(Lang.name_format, repairman.name)+Lang.chat_agreed);
					} else { // CANNOT ENCHANT
						player.sendMessage(String.format(Lang.name_format, repairman.name)+Lang.chat_norepair); // NO REPAIR NEEDED, CAN NOT ENCHANT
					}
				} else { // ENCHANTS DISABLED
					player.sendMessage(String.format(Lang.name_format, repairman.name)+Lang.chat_norepair); // NO REPAIR NEEDED, CAN NOT ENCHANT
				}
			}
		} else {
			player.sendMessage(String.format(Lang.name_format, repairman.name)+Lang.chat_cannot);
		}
	}

	private static boolean checkCanRepair(Player player, Repairer repairman, ItemStack item) {
		if (repairman.type.equalsIgnoreCase("all")) {
			if (item.getDurability() != 0) {
				if (repairableArmor.contains(item.getTypeId())) {
					// check for armor perm
					return ((Griswold.permission == null) || Griswold.permission.has(player, "griswold.armor"));
				} else return ((repairableTools.contains(item.getTypeId())) &&       // check tools perm
							((Griswold.permission == null) || Griswold.permission.has(player, "griswold.tools")));
			} else {
				return ((Griswold.permission == null) || Griswold.permission.has(player, "griswold.enchant"));
			}
		} else if (repairman.type.equalsIgnoreCase("both")) {
			if (repairableArmor.contains(item.getTypeId())) {
				return ((Griswold.permission == null) || Griswold.permission.has(player, "griswold.armor"));
			} else return ((repairableTools.contains(item.getTypeId())) &&
					((Griswold.permission == null) || Griswold.permission.has(player, "griswold.tools")));
		} else if (repairman.type.equalsIgnoreCase("tools")) {
			return ((Griswold.permission == null) || Griswold.permission.has(player, "griswold.tools"));
		} else if (repairman.type.equalsIgnoreCase("armor")) {
			return ((Griswold.permission == null) || Griswold.permission.has(player, "griswold.armor"));
		} else if (repairman.type.equalsIgnoreCase("enchant")) {
			return ((Griswold.permission == null) || Griswold.permission.has(player, "griswold.enchant"));
		}
		return false;
	}

	private static double getPrice(Repairer repairman, ItemStack item) {
		if (Griswold.economy == null) return 0.0;
		double price = 0;
		if (repairableTools.contains(item.getTypeId())) price = basicToolsPrice;
		else if (repairableTools.contains(item.getTypeId())) price = basicArmorPrice;

		price += item.getDurability();

		Map<Enchantment, Integer> enchantments = item.getEnchantments();

		if (!enchantments.isEmpty()) {
			for (int i = 0; i<enchantments.size(); i++) {
				Object[] enchantsLevels = enchantments.values().toArray();
				price = price + enchantmentPrice * Integer.parseInt(enchantsLevels[i].toString());
			}
		}
		return price * repairman.cost;
	}
}

class Interaction {
	Player player;
	Entity repairman;
	ItemStack item;
	int damage;
	long time;
	boolean valid;
	public Interaction(Player player, Entity repairman, ItemStack item, int dmg, long time) {
		this.item = item;
		this.damage = dmg;
		this.player = player;
		this.repairman = repairman;
		this.time = time;
		this.valid = true;
	}

	public boolean equals(Interaction inter) {
		int delta = (int) (time-inter.time);
		return ((inter.item.equals(item)) &&
				(inter.valid) &&
				(inter.damage == damage) &&
				(inter.player.equals(player)) &&
				(inter.repairman.equals(repairman)) &&
				(delta < Griswold.timeout));
	}
}